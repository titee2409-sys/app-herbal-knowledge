package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class HerbRemedy(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val properties: String = "",
    val components: String = "",
    val instructions: String = "",
    val precautions: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
