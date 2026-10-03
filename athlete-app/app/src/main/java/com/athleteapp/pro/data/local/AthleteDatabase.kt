package com.athleteapp.pro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.athleteapp.pro.data.local.dao.AthleteDao
import com.athleteapp.pro.data.local.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

@Database(
    entities = [
        AthleteProfileEntity::class,
        AssignedExerciseEntity::class,
        MyWorkoutSessionEntity::class,
        MyWorkoutSetEntity::class,
        MyAnthropometryEntity::class,
        AthleteAppSettingsEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AthleteDatabase : RoomDatabase() {
    abstract fun athleteDao(): AthleteDao

    companion object {
        @Volatile
        private var INSTANCE: AthleteDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `my_workout_sets_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sessionId` INTEGER NOT NULL,
                        `exerciseId` INTEGER NOT NULL,
                        `exerciseName` TEXT NOT NULL,
                        `muscleGroup` TEXT NOT NULL,
                        `exerciseOrder` INTEGER NOT NULL,
                        `setNumber` INTEGER NOT NULL,
                        `targetWeightKg` REAL NOT NULL,
                        `targetReps` INTEGER NOT NULL,
                        `actualWeightKg` REAL NOT NULL,
                        `actualReps` INTEGER NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `rpe` REAL,
                        FOREIGN KEY(`sessionId`) REFERENCES `my_workout_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`exerciseId`) REFERENCES `assigned_exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO `my_workout_sets_new` (
                        `id`, `sessionId`, `exerciseId`, `exerciseName`, `muscleGroup`,
                        `exerciseOrder`, `setNumber`, `targetWeightKg`, `targetReps`,
                        `actualWeightKg`, `actualReps`, `isCompleted`, `rpe`
                    )
                    SELECT `id`, `sessionId`, `exerciseId`, `exerciseName`, `muscleGroup`,
                           `exerciseOrder`, `setNumber`, `targetWeightKg`, `targetReps`,
                           `actualWeightKg`, `actualReps`, `isCompleted`, `rpe`
                    FROM `my_workout_sets`
                """.trimIndent())

                db.execSQL("DROP TABLE `my_workout_sets`")
                db.execSQL("ALTER TABLE `my_workout_sets_new` RENAME TO `my_workout_sets`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_my_workout_sets_sessionId` ON `my_workout_sets` (`sessionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_my_workout_sets_exerciseId` ON `my_workout_sets` (`exerciseId`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `athlete_profile` ADD COLUMN `clientUuid` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `athlete_profile` ADD COLUMN `restrictions` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `athlete_profile` ADD COLUMN `avatarPath` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `athlete_profile` ADD COLUMN `photoUri` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `athlete_profile` ADD COLUMN `pairingPin` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `athlete_profile` ADD COLUMN `isPairedWithCoach` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `athlete_profile` ADD COLUMN `pairedCoachName` TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE `athlete_profile` SET `clientUuid` = 'f47ac10b-58cc-4372-a567-0e02b2c3d479' WHERE `clientUuid` = ''")
                db.execSQL("UPDATE `athlete_profile` SET `pairingPin` = '739102' WHERE `pairingPin` = ''")

                db.execSQL("ALTER TABLE `my_workout_sessions` ADD COLUMN `isSelfWorkoutAllowed` INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AthleteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AthleteDatabase::class.java,
                    "athlete_pro.db"
                )
                .addCallback(DatabaseCallback(scope))
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.athleteDao())
                    }
                }
            }

            suspend fun populateInitialData(dao: AthleteDao) {
                val newUuid = java.util.UUID.randomUUID().toString()
                val newPin = String.format("%06d", (100000..999999).random())
                dao.saveProfile(
                    AthleteProfileEntity(
                        clientUuid = newUuid,
                        pairingPin = newPin,
                        isPairedWithCoach = false,
                        fullName = "",
                        phone = "",
                        goal = "",
                        restrictions = ""
                    )
                )
                dao.saveSettings(AthleteAppSettingsEntity())

                // Preload basic assigned exercises
                val ex1 = AssignedExerciseEntity(name = "Жим штанги лежа", muscleGroup = "Грудь", defaultRestSeconds = 120)
                val ex2 = AssignedExerciseEntity(name = "Жим гантелей наклонный", muscleGroup = "Грудь", defaultRestSeconds = 90)
                val ex3 = AssignedExerciseEntity(name = "Разведения гантелей", muscleGroup = "Грудь", defaultRestSeconds = 60)
                val ex4 = AssignedExerciseEntity(name = "Приседания со штангой", muscleGroup = "Ноги", defaultRestSeconds = 120)
                val ex5 = AssignedExerciseEntity(name = "Тяга верхнего блока", muscleGroup = "Спина", defaultRestSeconds = 90)
                dao.insertExercises(listOf(ex1, ex2, ex3, ex4, ex5))
            }
        }
    }
}
