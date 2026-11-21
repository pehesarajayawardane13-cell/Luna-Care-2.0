package com.example.data

import android.content.Context
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.DailyLog
import com.example.model.UserProfile
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class LunaCareRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUid: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        if (expectedUid != null && uid != expectedUid) {
            throw IllegalStateException("Signed-in user changed; refusing cross-user access.")
        }
        return uid
    }

    fun observeProfile(userId: String): Flow<UserProfile?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) {
                    null
                } else {
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.GET, "users/$userId")
                }
                throw error
            }

    fun observeDailyLogs(userId: String): Flow<Map<String, DailyLog>> =
        db.collection("users").document(userId).collection("daily_logs")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    DailyLog::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).associateBy { it.date.ifBlank { it.id } }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(
                        exceptionToLog,
                        OperationType.LIST,
                        "users/$userId/daily_logs"
                    )
                }
                throw error
            }

    suspend fun upsertProfile(userId: String, profile: UserProfile): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid)
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val updatePayload = profile.toCreateMap(uid).toMutableMap().apply {
                    remove("userId")
                    remove("createdAt")
                }
                docRef.update(updatePayload).await()
            } else {
                docRef.set(profile.toCreateMap(uid)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfileFields(userId: String, fields: Map<String, Any>): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val payload = fields.toMutableMap().apply {
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.collection("users").document(uid).update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun upsertDailyLog(userId: String, log: DailyLog): Result<Unit> {
        val dateId = log.date
        val path = "users/$userId/daily_logs/$dateId"
        return try {
            require(dateId.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
                "Invalid date format: $dateId"
            }
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid)
                .collection("daily_logs").document(dateId)
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(log.toUpdateMap()).await()
            } else {
                docRef.set(log.toCreateMap(uid, dateId)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun importBackup(
        userId: String,
        profile: UserProfile,
        logs: Map<String, DailyLog>
    ): Result<Unit> {
        return try {
            val uid = requireUserId(userId)
            upsertProfile(uid, profile).getOrThrow()
            logs.values.forEach { log ->
                upsertDailyLog(uid, log.copy(userId = uid)).getOrThrow()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$userId")
            Result.failure(e)
        }
    }
}
