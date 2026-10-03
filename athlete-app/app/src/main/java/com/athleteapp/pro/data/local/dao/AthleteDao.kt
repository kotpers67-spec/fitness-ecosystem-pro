package com.athleteapp.pro.data.local.dao

import androidx.room.*
import com.athleteapp.pro.data.local.entities.*
import kotlinx.coroutines.flow.Flow

data class AthleteSetHistory(
    val date: String,
    val weightKg: Double,
    val reps: Int,
    val isCompleted: Boolean
)

@Dao
interface AthleteDao {

    // Profile
    @Query("SELECT * FROM athlete_profile WHERE id = 1")
    fun getProfile(): Flow<AthleteProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: AthleteProfileEntity)

    // Exercises
    @Query("SELECT * FROM assigned_exercises ORDER BY muscleGroup ASC, name ASC")
    fun getAllExercises(): Flow<List<AssignedExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: AssignedExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<AssignedExerciseEntity>): List<Long>

    // Sessions & Sets
    @Query("SELECT * FROM my_workout_sessions WHERE date = :date LIMIT 1")
    suspend fun getSessionByDate(date: String): MyWorkoutSessionEntity?

    @Query("SELECT * FROM my_workout_sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<MyWorkoutSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: MyWorkoutSessionEntity): Long

    @Update
    suspend fun updateSession(session: MyWorkoutSessionEntity)

    @Query("SELECT * FROM my_workout_sets WHERE sessionId = :sessionId ORDER BY exerciseOrder ASC, setNumber ASC")
    fun getSetsForSession(sessionId: Long): Flow<List<MyWorkoutSetEntity>>

    @Query("SELECT * FROM my_workout_sets WHERE sessionId = :sessionId ORDER BY exerciseOrder ASC, setNumber ASC")
    suspend fun getSetsForSessionSync(sessionId: Long): List<MyWorkoutSetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(workoutSet: MyWorkoutSetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<MyWorkoutSetEntity>)

    @Update
    suspend fun updateSet(workoutSet: MyWorkoutSetEntity)

    @Delete
    suspend fun deleteSet(workoutSet: MyWorkoutSetEntity)

    @Query("DELETE FROM my_workout_sets WHERE sessionId = :sessionId")
    suspend fun deleteSetsForSession(sessionId: Long)

    @Query("SELECT * FROM my_workout_sets WHERE isCompleted = 1 ORDER BY id DESC LIMIT 50")
    suspend fun getRecentCompletedSets(): List<MyWorkoutSetEntity>

    @Query("SELECT * FROM my_workout_sets WHERE exerciseId = :exerciseId AND isCompleted = 1 ORDER BY id DESC LIMIT 20")
    suspend fun getCompletedSetsForExercise(exerciseId: Long): List<MyWorkoutSetEntity>

    @Query("SELECT * FROM my_workout_sets ORDER BY sessionId ASC, exerciseOrder ASC, setNumber ASC")
    suspend fun getAllSetsSync(): List<MyWorkoutSetEntity>

    @Query("SELECT * FROM my_workout_sessions ORDER BY date ASC")
    suspend fun getAllSessionsSync(): List<MyWorkoutSessionEntity>

    @Query("SELECT * FROM my_anthropometry ORDER BY date ASC")
    suspend fun getAllAnthropometrySync(): List<MyAnthropometryEntity>

    @Query("SELECT * FROM assigned_exercises ORDER BY id ASC")
    suspend fun getAllExercisesSync(): List<AssignedExerciseEntity>

    // Anthropometry
    @Query("SELECT * FROM my_anthropometry ORDER BY date ASC")
    fun getAllAnthropometry(): Flow<List<MyAnthropometryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnthropometry(entry: MyAnthropometryEntity): Long

    // History for Charts
    @Query("""
        SELECT s.date, ws.actualWeightKg as weightKg, ws.actualReps as reps, ws.isCompleted
        FROM my_workout_sets ws
        INNER JOIN my_workout_sessions s ON ws.sessionId = s.id
        WHERE ws.exerciseId = :exerciseId
        ORDER BY s.date ASC, ws.setNumber ASC
    """)
    fun getExerciseHistory(exerciseId: Long): Flow<List<AthleteSetHistory>>

    // Settings
    @Query("SELECT * FROM athlete_app_settings WHERE id = 1")
    fun getSettings(): Flow<AthleteAppSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AthleteAppSettingsEntity)
}
