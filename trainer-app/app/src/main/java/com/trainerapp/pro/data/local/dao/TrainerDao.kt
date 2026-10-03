package com.trainerapp.pro.data.local.dao

import androidx.room.*
import com.trainerapp.pro.data.local.entities.*
import kotlinx.coroutines.flow.Flow

data class SetHistoryItem(
    val date: String,
    val weightKg: Double,
    val reps: Int,
    val isCompleted: Boolean
)

@Dao
interface TrainerDao {

    // Clients
    @Query("SELECT * FROM clients ORDER BY fullName ASC")
    fun getAllClients(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getClientById(id: Long): ClientEntity?

    @Query("SELECT * FROM clients WHERE clientUuid = :uuid LIMIT 1")
    suspend fun getClientByUuid(uuid: String): ClientEntity?

    @Query("SELECT * FROM clients WHERE pairingCode = :code LIMIT 1")
    suspend fun getClientByPairingCode(code: String): ClientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: ClientEntity): Long

    @Update
    suspend fun updateClient(client: ClientEntity)

    @Delete
    suspend fun deleteClient(client: ClientEntity)

    // Exercises
    @Query("SELECT * FROM exercises ORDER BY muscleGroup ASC, name ASC")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: Long): ExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity)

    @Delete
    suspend fun deleteExercise(exercise: ExerciseEntity)

    // Sessions & Sets
    @Query("SELECT * FROM workout_sessions WHERE clientId = :clientId AND date = :date LIMIT 1")
    suspend fun getSessionByClientAndDate(clientId: Long, date: String): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE clientId = :clientId ORDER BY date DESC")
    fun getSessionsForClient(clientId: Long): Flow<List<WorkoutSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_sessions WHERE clientId = :clientId AND date < :currentDate AND completed = 1 ORDER BY date DESC LIMIT 1")
    suspend fun getLastCompletedSessionBefore(clientId: Long, currentDate: String): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY exerciseOrder ASC, setNumber ASC")
    fun getSetsForSession(sessionId: Long): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY exerciseOrder ASC, setNumber ASC")
    suspend fun getSetsForSessionSync(sessionId: Long): List<WorkoutSetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(workoutSet: WorkoutSetEntity): Long

    @Update
    suspend fun updateSet(workoutSet: WorkoutSetEntity)

    @Delete
    suspend fun deleteSet(workoutSet: WorkoutSetEntity)

    @Query("DELETE FROM workout_sets WHERE sessionId = :sessionId AND exerciseId = :exerciseId")
    suspend fun deleteExerciseFromSession(sessionId: Long, exerciseId: Long)

    // Previous Weight Hint Lookup
    @Query("""
        SELECT ws.* FROM workout_sets ws
        INNER JOIN workout_sessions s ON ws.sessionId = s.id
        WHERE s.clientId = :clientId AND ws.exerciseId = :exerciseId AND s.date < :currentDate AND ws.isCompleted = 1
        ORDER BY s.date DESC, ws.setNumber DESC LIMIT 1
    """)
    suspend fun getLastExerciseSet(clientId: Long, exerciseId: Long, currentDate: String): WorkoutSetEntity?

    // Anthropometry
    @Query("SELECT * FROM anthropometry WHERE clientId = :clientId ORDER BY date ASC")
    fun getAnthropometryForClient(clientId: Long): Flow<List<AnthropometryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnthropometry(entry: AnthropometryEntity): Long

    // Exercise History for Analytics
    @Query("""
        SELECT s.date, ws.weightKg, ws.reps, ws.isCompleted
        FROM workout_sets ws
        INNER JOIN workout_sessions s ON ws.sessionId = s.id
        WHERE s.clientId = :clientId AND ws.exerciseId = :exerciseId
        ORDER BY s.date ASC, ws.setNumber ASC
    """)
    fun getExerciseHistory(clientId: Long, exerciseId: Long): Flow<List<SetHistoryItem>>

    // Settings
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getSettings(): Flow<AppSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettingsEntity)

    // Full export data queries
    @Query("SELECT * FROM clients")
    suspend fun getAllClientsSync(): List<ClientEntity>

    @Query("SELECT * FROM exercises")
    suspend fun getAllExercisesSync(): List<ExerciseEntity>

    @Query("SELECT * FROM workout_sessions")
    suspend fun getAllSessionsSync(): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_sets")
    suspend fun getAllSetsSync(): List<WorkoutSetEntity>

    @Query("SELECT * FROM anthropometry")
    suspend fun getAllAnthropometrySync(): List<AnthropometryEntity>

    // Appointments
    @Query("SELECT * FROM appointments WHERE clientId = :clientId ORDER BY dateTime ASC")
    fun getAppointmentsForClient(clientId: Long): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE id = :id")
    suspend fun getAppointmentById(id: Long): AppointmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: AppointmentEntity): Long

    @Update
    suspend fun updateAppointment(appointment: AppointmentEntity)

    @Delete
    suspend fun deleteAppointment(appointment: AppointmentEntity)

    @Query("SELECT * FROM appointments")
    suspend fun getAllAppointmentsSync(): List<AppointmentEntity>

    // Scoped queries for Athlete Sync
    @Query("SELECT * FROM workout_sessions WHERE clientId = :clientId ORDER BY date ASC")
    suspend fun getSessionsForClientSync(clientId: Long): List<WorkoutSessionEntity>

    @Query("SELECT * FROM anthropometry WHERE clientId = :clientId ORDER BY date ASC")
    suspend fun getAnthropometryForClientSync(clientId: Long): List<AnthropometryEntity>
}
