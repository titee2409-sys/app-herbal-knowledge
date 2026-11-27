package com.example.data

import android.content.Context
import com.example.R
import com.example.model.FormulaBookmark
import com.example.model.HerbalCategory
import com.example.model.HerbalFormula
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class HerbalRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("กรุณาเข้าสู่ระบบด้วย Google ก่อนใช้งานคลังข้อมูล")
    }

    fun observeCustomFormulas(userId: String): Flow<List<HerbalFormula>> = flow {
        val path = "users/$userId/formulas"
        emitAll(
            db.collection("users").document(userId).collection("formulas")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        HerbalFormula::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeBookmarks(userId: String): Flow<List<FormulaBookmark>> = flow {
        val path = "users/$userId/bookmarks"
        emitAll(
            db.collection("users").document(userId).collection("bookmarks")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FormulaBookmark::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveCustomFormula(formula: HerbalFormula): Result<Unit> {
        val isCreate = formula.id.isBlank()
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/formulas"
        return try {
            val uid = requireUserId()
            val cleanName = formula.name.trim()
            val cleanEnglishName = formula.englishName.trim()
            val cleanTaste = formula.tasteProfile.trim()
            val cleanIndications = formula.indications.trim()
            val cleanIngredients = formula.ingredientsSummary.trim()
            val cleanPreparation = formula.preparationAndDosage.trim()
            val cleanPrecautions = formula.precautions.trim()
            val cleanReference = formula.referenceSource.trim()
            val validCategory = HerbalCategory.fromCode(formula.category).code

            require(cleanName.length in 1..100) { "ชื่อตำรับยาต้องมีความยาว 1-100 ตัวอักษร" }
            require(cleanEnglishName.length <= 100) { "ชื่อภาษาอังกฤษต้องไม่เกิน 100 ตัวอักษร" }
            require(cleanTaste.length <= 60) { "รสยาต้องไม่เกิน 60 ตัวอักษร" }
            require(cleanIndications.length in 1..600) { "สรรพคุณต้องมีความยาว 1-600 ตัวอักษร" }
            require(cleanIngredients.length in 1..1000) { "ส่วนประกอบต้องมีความยาว 1-1000 ตัวอักษร" }
            require(cleanPreparation.length in 1..800) { "วิธีปรุงและขนาดรับประทานต้องมีความยาว 1-800 ตัวอักษร" }
            require(cleanPrecautions.length <= 600) { "ข้อควรระวังต้องไม่เกิน 600 ตัวอักษร" }
            require(cleanReference.length <= 150) { "ที่มาของตำรับต้องไม่เกิน 150 ตัวอักษร" }

            val collectionRef = db.collection("users").document(uid).collection("formulas")
            if (isCreate) {
                val docRef = collectionRef.document()
                docPath = docRef.path
                val payload = mapOf(
                    "userId" to uid,
                    "name" to cleanName,
                    "englishName" to cleanEnglishName,
                    "category" to validCategory,
                    "tasteProfile" to cleanTaste,
                    "indications" to cleanIndications,
                    "ingredientsSummary" to cleanIngredients,
                    "preparationAndDosage" to cleanPreparation,
                    "precautions" to cleanPrecautions,
                    "referenceSource" to cleanReference,
                    "externalUse" to formula.externalUse,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                require(isValidFirestoreId(formula.id)) { "รหัสตำรับยาไม่ถูกต้อง" }
                val docRef = collectionRef.document(formula.id)
                docPath = docRef.path
                val updatePayload = mapOf(
                    "name" to cleanName,
                    "englishName" to cleanEnglishName,
                    "category" to validCategory,
                    "tasteProfile" to cleanTaste,
                    "indications" to cleanIndications,
                    "ingredientsSummary" to cleanIngredients,
                    "preparationAndDosage" to cleanPreparation,
                    "precautions" to cleanPrecautions,
                    "referenceSource" to cleanReference,
                    "externalUse" to formula.externalUse,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (isCreate) OperationType.CREATE else OperationType.UPDATE,
                docPath
            )
            Result.failure(e)
        }
    }

    suspend fun deleteCustomFormula(formulaId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/formulas/$formulaId"
        return try {
            val uid = requireUserId()
            require(formulaId.isNotBlank() && isValidFirestoreId(formulaId)) {
                "รหัสตำรับยาไม่ถูกต้อง"
            }
            val docRef = db.collection("users").document(uid).collection("formulas").document(formulaId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertBookmark(
        formulaId: String,
        formulaName: String,
        favorite: Boolean,
        personalNote: String,
        isExistingBookmark: Boolean
    ): Result<Unit> {
        val docId = sanitizeDocumentId(formulaId)
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/bookmarks/$docId"
        return try {
            val uid = requireUserId()
            val cleanName = formulaName.trim().take(100)
            val cleanNote = personalNote.trim()
            require(docId.isNotBlank() && isValidFirestoreId(docId)) { "รหัสอ้างอิงตำรับไม่ถูกต้อง" }
            require(cleanName.length in 1..100) { "ชื่อตำรับยาต้องไม่ว่างและไม่เกิน 100 ตัวอักษร" }
            require(cleanNote.length <= 500) { "บันทึกส่วนตัวต้องไม่เกิน 500 ตัวอักษร" }

            val docRef = db.collection("users").document(uid).collection("bookmarks").document(docId)
            docPath = docRef.path

            if (!favorite && cleanNote.isEmpty()) {
                if (isExistingBookmark) {
                    docRef.delete().await()
                }
                return Result.success(Unit)
            }

            if (isExistingBookmark) {
                docRef.update(
                    mapOf(
                        "formulaName" to cleanName,
                        "personalNote" to cleanNote,
                        "favorite" to favorite,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "formulaId" to formulaId.take(128),
                        "formulaName" to cleanName,
                        "personalNote" to cleanNote,
                        "favorite" to favorite,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (isExistingBookmark) OperationType.UPDATE else OperationType.CREATE,
                docPath
            )
            Result.failure(e)
        }
    }

    private fun isValidFirestoreId(id: String): Boolean =
        id.length in 1..128 && id.matches(Regex("^[a-zA-Z0-9_\\-]+$"))

    private fun sanitizeDocumentId(raw: String): String =
        raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
}
