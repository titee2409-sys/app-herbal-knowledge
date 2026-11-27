package com.example.data

import android.content.Context
import com.example.R
import com.example.model.HerbRemedy
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

interface HerbRemedyDataSource {
    fun observeRemedies(userId: String): Flow<List<HerbRemedy>>
    suspend fun getAllRemedies(): Result<List<HerbRemedy>>
    suspend fun getRemedyById(remedyId: String): Result<HerbRemedy?>
    suspend fun createRemedy(remedy: HerbRemedy): Result<String>
    suspend fun updateRemedy(remedyId: String, remedy: HerbRemedy): Result<Unit>
    suspend fun deleteRemedy(remedyId: String): Result<Unit>
}

class HerbRemedyRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : HerbRemedyDataSource {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun remediesCollection(userId: String) =
        db.collection("users").document(userId).collection("remedies")

    override fun observeRemedies(userId: String): Flow<List<HerbRemedy>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId/remedies"
        emitAll(
            remediesCollection(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        HerbRemedy::class.java,
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

    override suspend fun getAllRemedies(): Result<List<HerbRemedy>> {
        var collectionPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/remedies"
        return try {
            val uid = requireUserId()
            collectionPath = "users/$uid/remedies"
            val snapshot = remediesCollection(uid).get().await()
            val remedies = snapshot.toObjects(
                HerbRemedy::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(remedies)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, collectionPath)
            Result.failure(e)
        }
    }

    override suspend fun getRemedyById(remedyId: String): Result<HerbRemedy?> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/remedies/$remedyId"
        return try {
            val uid = requireUserId()
            require(isValidFirestoreId(remedyId)) { "Invalid remedy ID format." }
            val docRef = remediesCollection(uid).document(remedyId)
            docPath = docRef.path
            val snapshot = docRef.get().await()
            val remedy = snapshot.toObject(
                HerbRemedy::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(remedy)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docPath)
            Result.failure(e)
        }
    }

    override suspend fun createRemedy(remedy: HerbRemedy): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/remedies"
        return try {
            val uid = requireUserId()
            val validated = validateAndTrim(remedy)
            val collectionRef = remediesCollection(uid)
            val docRef = if (remedy.id.isNotBlank()) {
                require(isValidFirestoreId(remedy.id)) { "Invalid remedy ID format." }
                collectionRef.document(remedy.id)
            } else {
                collectionRef.document()
            }
            docPath = docRef.path

            val payload = mapOf(
                "userId" to uid,
                "name" to validated.name,
                "properties" to validated.properties,
                "components" to validated.components,
                "instructions" to validated.instructions,
                "precautions" to validated.precautions,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun updateRemedy(remedyId: String, remedy: HerbRemedy): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/remedies/$remedyId"
        return try {
            val uid = requireUserId()
            require(isValidFirestoreId(remedyId)) { "Invalid remedy ID format." }
            val validated = validateAndTrim(remedy)
            val docRef = remediesCollection(uid).document(remedyId)
            docPath = docRef.path

            val updatePayload = mapOf(
                "name" to validated.name,
                "properties" to validated.properties,
                "components" to validated.components,
                "instructions" to validated.instructions,
                "precautions" to validated.precautions,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updatePayload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun deleteRemedy(remedyId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/remedies/$remedyId"
        return try {
            val uid = requireUserId()
            require(isValidFirestoreId(remedyId)) { "Invalid remedy ID format." }
            val docRef = remediesCollection(uid).document(remedyId)
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

    private fun validateAndTrim(remedy: HerbRemedy): HerbRemedy {
        val cleanName = remedy.name.trim()
        val cleanProperties = remedy.properties.trim()
        val cleanComponents = remedy.components.trim()
        val cleanInstructions = remedy.instructions.trim()
        val cleanPrecautions = remedy.precautions.trim()

        require(cleanName.length in 1..120) { "Remedy name must be between 1 and 120 characters." }
        require(cleanProperties.length in 1..800) { "Remedy properties must be between 1 and 800 characters." }
        require(cleanComponents.length in 1..1000) { "Remedy components must be between 1 and 1000 characters." }
        require(cleanInstructions.length in 1..800) { "Remedy instructions must be between 1 and 800 characters." }
        require(cleanPrecautions.length <= 600) { "Remedy precautions must be at most 600 characters." }

        return remedy.copy(
            name = cleanName,
            properties = cleanProperties,
            components = cleanComponents,
            instructions = cleanInstructions,
            precautions = cleanPrecautions
        )
    }

    private fun isValidFirestoreId(id: String): Boolean =
        id.length in 1..128 && id.matches(Regex("^[a-zA-Z0-9_\\-]+$"))
}
