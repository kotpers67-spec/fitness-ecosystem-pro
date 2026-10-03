package com.athleteapp.pro.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "athlete_profile")
data class AthleteProfileEntity(
    @PrimaryKey val id: Long = 1,
    val clientUuid: String = java.util.UUID.randomUUID().toString(),
    val athleteIdInCoachBase: Long = 1,
    val fullName: String = "",
    val phone: String = "",
    val goal: String = "",
    val notes: String = "",
    val restrictions: String = "",
    val avatarPath: String? = null,
    val photoUri: String? = null,
    val avatarBase64: String? = null,
    val pairingPin: String = "",
    val isPairedWithCoach: Boolean = false,
    val pairedCoachName: String = "",
    val pairedCoachPhone: String = "",
    val pairedCoachPhotoUri: String? = null,
    val pairedCoachAvatarBase64: String? = null,
    val isPrivateLeaderboard: Boolean = false
)

@Entity(tableName = "assigned_exercises")
data class AssignedExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscleGroup: String,
    val defaultRestSeconds: Int = 90,
    val description: String = ""
)

@Entity(tableName = "my_workout_sessions")
data class MyWorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val notes: String = "",
    val completed: Boolean = false,
    val isSelfWorkoutAllowed: Boolean = false
)

@Entity(
    tableName = "my_workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = MyWorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AssignedExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("sessionId"),
        Index("exerciseId")
    ]
)
data class MyWorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val muscleGroup: String,
    val exerciseOrder: Int,
    val setNumber: Int,
    val targetWeightKg: Double = 0.0,
    val targetReps: Int = 10,
    val actualWeightKg: Double = 0.0,
    val actualReps: Int = 10,
    val isCompleted: Boolean = false,
    val rpe: Double? = null
)

@Entity(tableName = "my_anthropometry")
data class MyAnthropometryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val weightKg: Double,
    val chestCm: Double? = null,
    val waistCm: Double? = null,
    val hipsCm: Double? = null,
    val bicepsCm: Double? = null
)

@Entity(tableName = "athlete_app_settings")
data class AthleteAppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val currentThemeName: String = "Cyber Lime",
    val language: String = "ru", // "ru", "en"
    val githubToken: String = "",
    val githubRepo: String = "trainer-pro-sync",
    val coachGitHubOwner: String = "kotpe",
    val athleteId: Long = 1,
    val autoStartTimer: Boolean = true,
    val defaultRestTimeSeconds: Int = 90
)
