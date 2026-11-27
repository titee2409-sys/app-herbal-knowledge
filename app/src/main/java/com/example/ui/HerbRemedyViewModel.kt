package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.HerbRemedyDataSource
import com.example.model.HerbRemedy
import com.example.model.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HerbRemedyViewModel(
    private val repository: HerbRemedyDataSource,
    private val currentUserId: String
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isMutating = MutableStateFlow(false)
    val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    val remediesState: StateFlow<UiState<List<HerbRemedy>>> =
        if (currentUserId.isBlank()) {
            flowOf(UiState.Error("กรุณาเข้าสู่ระบบก่อนใช้งานคลังข้อมูลสมุนไพร"))
        } else {
            combine(
                repository.observeRemedies(currentUserId),
                _searchQuery
            ) { remedies, query ->
                val filtered = filterRemedies(remedies, query)
                UiState.Success(filtered) as UiState<List<HerbRemedy>>
            }.catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing herb remedies", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "ไม่สามารถโหลดข้อมูลสมุนไพรได้"))
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun createRemedy(remedy: HerbRemedy, onSuccess: (String) -> Unit = {}) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.createRemedy(remedy)
            _isMutating.value = false
            result.onSuccess { createdId ->
                _feedbackMessage.value = "บันทึกข้อมูลสมุนไพร \"${remedy.name.trim()}\" เรียบร้อยแล้ว"
                onSuccess(createdId)
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "บันทึกข้อมูลสมุนไพรไม่สำเร็จ"
            }
        }
    }

    fun updateRemedy(remedyId: String, remedy: HerbRemedy, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.updateRemedy(remedyId, remedy)
            _isMutating.value = false
            result.onSuccess {
                _feedbackMessage.value = "อัปเดตข้อมูลสมุนไพร \"${remedy.name.trim()}\" เรียบร้อยแล้ว"
                onSuccess()
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "อัปเดตข้อมูลสมุนไพรไม่สำเร็จ"
            }
        }
    }

    fun deleteRemedy(remedyId: String, remedyName: String = "", onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.deleteRemedy(remedyId)
            _isMutating.value = false
            result.onSuccess {
                _feedbackMessage.value = if (remedyName.isNotBlank()) {
                    "ลบข้อมูลสมุนไพร \"$remedyName\" แล้ว"
                } else {
                    "ลบข้อมูลสมุนไพรเรียบร้อยแล้ว"
                }
                onSuccess()
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "ลบข้อมูลสมุนไพรไม่สำเร็จ"
            }
        }
    }

    companion object {
        private const val TAG = "HerbRemedyVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L

        fun filterRemedies(remedies: List<HerbRemedy>, query: String): List<HerbRemedy> {
            val tokens = query.trim().lowercase()
                .split(Regex("\\s+"))
                .filter { it.isNotEmpty() }
            if (tokens.isEmpty()) return remedies

            return remedies.filter { remedy ->
                val searchableFields = listOf(
                    remedy.name.lowercase(),
                    remedy.properties.lowercase(),
                    remedy.components.lowercase(),
                    remedy.instructions.lowercase(),
                    remedy.precautions.lowercase()
                )
                tokens.all { token ->
                    searchableFields.any { field -> field.contains(token) }
                }
            }
        }

        fun provideFactory(
            repository: HerbRemedyDataSource,
            currentUserId: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(HerbRemedyViewModel::class.java)) {
                    return HerbRemedyViewModel(repository, currentUserId) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
