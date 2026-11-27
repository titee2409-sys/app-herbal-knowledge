package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class HerbalCategory(
    val code: String,
    val thaiLabel: String,
    val shortLabel: String,
    val description: String
) {
    RESPIRATORY(
        code = "RESPIRATORY",
        thaiLabel = "ระบบทางเดินหายใจ",
        shortLabel = "หายใจ/ไอ",
        description = "แก้ไอ ขับเสมหะ เจ็บคอ และอาการระคายเคืองลำคอ"
    ),
    DIGESTIVE(
        code = "DIGESTIVE",
        thaiLabel = "ระบบทางเดินอาหาร",
        shortLabel = "ทางเดินอาหาร",
        description = "บรรเทาท้องอืด ท้องเฟ้อ จุกเสียด และท้องเสีย"
    ),
    FEVER(
        code = "FEVER",
        thaiLabel = "แก้ไข้/ตัวร้อน",
        shortLabel = "แก้ไข้",
        description = "ลดไข้ตัวร้อน ไข้เปลี่ยนฤดู และอาการหวัด"
    ),
    TONIC(
        code = "TONIC",
        thaiLabel = "บำรุงธาตุ/ยาหอม",
        shortLabel = "บำรุง/ยาหอม",
        description = "แก้ลมวิงเวียน บำรุงหัวใจ และปรับสมดุลธาตุทั้ง 4"
    ),
    MUSCLE(
        code = "MUSCLE",
        thaiLabel = "กล้ามเนื้อและข้อ",
        shortLabel = "ปวดเมื่อย",
        description = "บรรเทาปวดเมื่อย เคล็ดขัดยอก ฟกช้ำ และกษัยเส้น"
    ),
    WOMENS_HEALTH(
        code = "WOMENS_HEALTH",
        thaiLabel = "ตำรับสตรี",
        shortLabel = "ตำรับสตรี",
        description = "ปรับสมดุลรอบเดือน แก้จุกเสียด และขับน้ำคาวปลา"
    ),
    CUSTOM(
        code = "CUSTOM",
        thaiLabel = "ตำรับอื่นๆ",
        shortLabel = "อื่นๆ",
        description = "ตำรับยาเฉพาะทางและสูตรสมุนไพรที่บันทึกเพิ่มเติม"
    );

    companion object {
        fun fromCode(raw: String): HerbalCategory =
            entries.firstOrNull { it.code == raw } ?: CUSTOM
    }
}

@Keep
@IgnoreExtraProperties
data class HerbalFormula(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val englishName: String = "",
    val category: String = HerbalCategory.CUSTOM.code,
    val tasteProfile: String = "",
    val indications: String = "",
    val ingredientsSummary: String = "",
    val preparationAndDosage: String = "",
    val precautions: String = "",
    val referenceSource: String = "",
    val externalUse: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FormulaBookmark(
    @DocumentId val id: String = "",
    val userId: String = "",
    val formulaId: String = "",
    val formulaName: String = "",
    val personalNote: String = "",
    val favorite: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class HerbalCatalogEntry(
    val id: String,
    val name: String,
    val englishName: String,
    val category: HerbalCategory,
    val tasteProfile: String,
    val indications: String,
    val ingredientsSummary: String,
    val preparationAndDosage: String,
    val precautions: String,
    val referenceSource: String,
    val externalUse: Boolean,
    val customOwned: Boolean,
    val nationalList: Boolean,
    val scientificName: String = "",
    val botanicalFamily: String = "",
    val partUsed: String = "",
    val ubuCrudeDrug: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val bookmark: FormulaBookmark? = null
) {
    val favorite: Boolean get() = bookmark?.favorite == true
    val personalNote: String get() = bookmark?.personalNote.orEmpty()
}

data class ThaiTasteInfo(
    val name: String,
    val properties: String,
    val exampleHerbs: String,
    val caution: String
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
