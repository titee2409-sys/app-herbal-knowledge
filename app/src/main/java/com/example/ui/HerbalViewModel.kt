package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HerbalRepository
import com.example.data.ThaiHerbalCatalog
import com.example.model.FormulaBookmark
import com.example.model.HerbalCatalogEntry
import com.example.model.HerbalCategory
import com.example.model.HerbalFormula
import com.example.model.UiState
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DatabaseTab(val label: String) {
    CATALOG("คลังยา"),
    SAVED("บันทึกไว้"),
    MY_FORMULAS("ตำรับฉัน"),
    REMEDIES("สมุนไพร")
}

enum class CatalogSourceFilter(val label: String) {
    ALL("ทั้งหมด"),
    FORMULAS("ตำรับยาผสม"),
    UBU_HERBS("สมุนไพร ม.อุบลฯ")
}

data class HerbalFilterState(
    val searchQuery: String = "",
    val selectedCategory: HerbalCategory? = null,
    val selectedTaste: String? = null,
    val selectedSource: CatalogSourceFilter = CatalogSourceFilter.ALL,
    val activeTab: DatabaseTab = DatabaseTab.CATALOG
)

class HerbalViewModel(
    private val repository: HerbalRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _filterState = MutableStateFlow(HerbalFilterState())
    val filterState: StateFlow<HerbalFilterState> = _filterState.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _isMutating = MutableStateFlow(false)
    val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

    private val customFormulasFlow = repository.observeCustomFormulas(currentUserId)
    private val bookmarksFlow = repository.observeBookmarks(currentUserId)

    val catalogState: StateFlow<UiState<List<HerbalCatalogEntry>>> = combine(
        customFormulasFlow,
        bookmarksFlow
    ) { customFormulas, bookmarks ->
        val bookmarkByFormulaId = bookmarks.associateBy { it.formulaId }

        val customEntries = customFormulas
            .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            .map { formula ->
                HerbalCatalogEntry(
                    id = formula.id,
                    name = formula.name,
                    englishName = formula.englishName,
                    category = HerbalCategory.fromCode(formula.category),
                    tasteProfile = formula.tasteProfile,
                    indications = formula.indications,
                    ingredientsSummary = formula.ingredientsSummary,
                    preparationAndDosage = formula.preparationAndDosage,
                    precautions = formula.precautions,
                    referenceSource = formula.referenceSource,
                    externalUse = formula.externalUse,
                    customOwned = true,
                    nationalList = false,
                    createdAt = formula.createdAt,
                    updatedAt = formula.updatedAt,
                    bookmark = bookmarkByFormulaId[formula.id]
                )
            }

        val standardEntries = ThaiHerbalCatalog.standardFormulas.map { entry ->
            entry.copy(bookmark = bookmarkByFormulaId[entry.id])
        }

        UiState.Success(customEntries + standardEntries) as UiState<List<HerbalCatalogEntry>>
    }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing herbal database", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "ไม่สามารถโหลดข้อมูลตำรับยาได้"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    fun updateSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun selectCategory(category: HerbalCategory?) {
        _filterState.value = _filterState.value.copy(selectedCategory = category)
    }

    fun selectTaste(taste: String?) {
        _filterState.value = _filterState.value.copy(selectedTaste = taste)
    }

    fun selectSource(source: CatalogSourceFilter) {
        _filterState.value = _filterState.value.copy(selectedSource = source)
    }

    fun selectTab(tab: DatabaseTab) {
        _filterState.value = _filterState.value.copy(activeTab = tab)
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun toggleFavorite(entry: HerbalCatalogEntry) {
        val currentBookmark = entry.bookmark
        val nextFavorite = !entry.favorite
        val existingNote = currentBookmark?.personalNote.orEmpty()
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.upsertBookmark(
                formulaId = entry.id,
                formulaName = entry.name,
                favorite = nextFavorite,
                personalNote = existingNote,
                isExistingBookmark = currentBookmark != null
            )
            _isMutating.value = false
            result.onSuccess {
                _feedbackMessage.value = if (nextFavorite) {
                    "บันทึก \"${entry.name}\" ในรายการโปรดแล้ว"
                } else {
                    "นำ \"${entry.name}\" ออกจากรายการโปรดแล้ว"
                }
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "บันทึกรายการโปรดไม่สำเร็จ"
            }
        }
    }

    fun savePersonalNote(entry: HerbalCatalogEntry, note: String) {
        val currentBookmark = entry.bookmark
        val keepFavorite = currentBookmark?.favorite ?: true
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.upsertBookmark(
                formulaId = entry.id,
                formulaName = entry.name,
                favorite = keepFavorite,
                personalNote = note,
                isExistingBookmark = currentBookmark != null
            )
            _isMutating.value = false
            result.onSuccess {
                _feedbackMessage.value = "บันทึกหมายเหตุส่วนตัวเรียบร้อยแล้ว"
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "บันทึกหมายเหตุไม่สำเร็จ"
            }
        }
    }

    fun saveCustomFormula(
        existingId: String?,
        name: String,
        englishName: String,
        category: HerbalCategory,
        tasteProfile: String,
        indications: String,
        ingredientsSummary: String,
        preparationAndDosage: String,
        precautions: String,
        referenceSource: String,
        externalUse: Boolean,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val formula = HerbalFormula(
                id = existingId.orEmpty(),
                userId = currentUserId,
                name = name,
                englishName = englishName,
                category = category.code,
                tasteProfile = tasteProfile,
                indications = indications,
                ingredientsSummary = ingredientsSummary,
                preparationAndDosage = preparationAndDosage,
                precautions = precautions,
                referenceSource = referenceSource,
                externalUse = externalUse
            )
            val result = repository.saveCustomFormula(formula)
            _isMutating.value = false
            result.onSuccess {
                _feedbackMessage.value = if (existingId.isNullOrBlank()) {
                    "เพิ่มตำรับยา \"$name\" เข้าคลังส่วนตัวแล้ว"
                } else {
                    "อัปเดตตำรับยา \"$name\" เรียบร้อยแล้ว"
                }
                onSaved()
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "บันทึกตำรับยาไม่สำเร็จ"
            }
        }
    }

    fun deleteCustomFormula(entry: HerbalCatalogEntry, onDeleted: () -> Unit) {
        if (!entry.customOwned) return
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.deleteCustomFormula(entry.id)
            _isMutating.value = false
            result.onSuccess {
                _feedbackMessage.value = "ลบตำรับยา \"${entry.name}\" แล้ว"
                onDeleted()
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "ลบตำรับยาไม่สำเร็จ"
            }
        }
    }

    private companion object {
        const val TAG = "HerbalVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
