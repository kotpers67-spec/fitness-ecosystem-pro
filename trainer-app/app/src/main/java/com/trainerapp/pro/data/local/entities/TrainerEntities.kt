package com.trainerapp.pro.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

import java.util.UUID

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val phone: String = "",
    val goal: String = "",
    val membershipStatus: String = "Активен", // "Активен", "Заканчивается", "Истёк"
    val membershipExpiryDate: String = "",
    val notes: String = "",
    val clientUuid: String = UUID.randomUUID().toString(),
    val pairingCode: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "anthropometry",
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("clientId")]
)
data class AnthropometryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val date: String, // YYYY-MM-DD
    val weightKg: Double,
    val chestCm: Double? = null,
    val waistCm: Double? = null,
    val hipsCm: Double? = null,
    val bicepsCm: Double? = null,
    val thighCm: Double? = null
)

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscleGroup: String, // "Грудь", "Спина", "Ноги", "Плечи", "Руки", "Пресс/Кор", "Кардио"
    val defaultRestSeconds: Int = 90,
    val isCustom: Boolean = false,
    val description: String = ""
)

@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("clientId"), Index(value = ["clientId", "date"], unique = true)]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val date: String, // YYYY-MM-DD
    val comments: String = "",
    val completed: Boolean = false,
    val isSelfWorkoutAllowed: Boolean = false
)

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("exerciseId")]
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val exerciseOrder: Int, // 1..8
    val setNumber: Int,    // 1, 2, 3, 4, ...
    val weightKg: Double,
    val reps: Int,
    val isCompleted: Boolean = false,
    val rpe: Double? = null
)

@Entity(
    tableName = "appointments",
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("clientId")]
)
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val dateTime: String, // YYYY-MM-DD HH:mm
    val status: String = "Запланировано", // "Запланировано", "Завершено", "Отменено"
    val notes: String = ""
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val currentThemeName: String = "Cyber Lime",
    val currentLayoutStyleName: String = "Notebook Classic",
    val selectedClientId: Long? = null,
    val language: String = "ru", // "ru", "en"
    val githubToken: String = "",
    val githubRepo: String = "trainer-pro-sync",
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val autoStartTimer: Boolean = true,
    val defaultRestTimeSeconds: Int = 90,
    val bleSyncEnabled: Boolean = false
)
