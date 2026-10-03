package com.trainerapp.pro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.trainerapp.pro.data.local.dao.TrainerDao
import com.trainerapp.pro.data.local.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import androidx.room.migration.Migration

@Database(
    entities = [
        ClientEntity::class,
        AnthropometryEntity::class,
        ExerciseEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSetEntity::class,
        AppointmentEntity::class,
        AppSettingsEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class TrainerDatabase : RoomDatabase() {
    abstract fun trainerDao(): TrainerDao

    companion object {
        @Volatile
        private var INSTANCE: TrainerDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE clients ADD COLUMN clientUuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE clients ADD COLUMN pairingCode TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN isSelfWorkoutAllowed INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): TrainerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrainerDatabase::class.java,
                    "trainer_pro.db"
                )
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
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
                        populateInitialData(database.trainerDao())
                    }
                }
            }

            suspend fun populateInitialData(dao: TrainerDao) {
                dao.saveSettings(
                    AppSettingsEntity(
                        id = 1,
                        currentThemeName = "Cyber Lime",
                        currentLayoutStyleName = "Notebook Classic"
                    )
                )

                val exercises = listOf(
                    // Грудь
                    ExerciseEntity(name = "Жим штанги лежа", muscleGroup = "Грудь", defaultRestSeconds = 120),
                    ExerciseEntity(name = "Жим гантелей на наклонной скамье", muscleGroup = "Грудь", defaultRestSeconds = 90),
                    ExerciseEntity(name = "Отжимания на брусьях (акцент на грудь)", muscleGroup = "Грудь", defaultRestSeconds = 90),
                    ExerciseEntity(name = "Сведение рук в кроссовере", muscleGroup = "Грудь", defaultRestSeconds = 60),
                    ExerciseEntity(name = "Жим в тренажере Хаммер", muscleGroup = "Грудь", defaultRestSeconds = 90),
                    ExerciseEntity(name = "Пуловер с гантелью", muscleGroup = "Грудь", defaultRestSeconds = 60),

                    // Спина
                    ExerciseEntity(name = "Становая тяга", muscleGroup = "Спина", defaultRestSeconds = 180),
                    ExerciseEntity(name = "Тяга штанги в наклоне", muscleGroup = "Спина", defaultRestSeconds = 90),
                    ExerciseEntity(name = "Подтягивания широким хватом", muscleGroup = "Спина", defaultRestSeconds = 90),
                    ExerciseEntity(name = "Тяга верхнего блока к груди", muscleGroup = "Спина", defaultRestSeconds = 75),
                    ExerciseEntity(name = "Тяга горизонтального блока к поясу", muscleGroup = "Спина", defaultRestSeconds = 75),
                    ExerciseEntity(name = "Тяга гантели одной рукой", muscleGroup = "Спина", defaultRestSeconds = 75),
                    ExerciseEntity(name = "Гиперэкстензия", muscleGroup = "Спина", defaultRestSeconds = 60),

                    // Ноги & Ягодицы
                    ExerciseEntity(name = "Приседания со штангой на плечах", muscleGroup = "Ноги", defaultRestSeconds = 150),
                    ExerciseEntity(name = "Жим ногами в тренажере", muscleGroup = "Ноги", defaultRestSeconds = 120),
                    ExerciseEntity(name = "Румынская тяга со штангой", muscleGroup = "Ноги", defaultRestSeconds = 90),
                    ExerciseEntity(name = "Выпады с гантелями на месте", muscleGroup = "Ноги", defaultRestSeconds = 75),
                    ExerciseEntity(name = "Сгибания ног лежа в тренажере", muscleGroup = "Ноги", defaultRestSeconds = 60),
                    ExerciseEntity(name = "Разгибания ног сидя в тренажере", muscleGroup = "Ноги", defaultRestSeconds = 60),
                    ExerciseEntity(name = "Подъем на носки стоя (икры)", muscleGroup = "Ноги", defaultRestSeconds = 45),
                    ExerciseEntity(name = "Ягодичный мостик со штангой", muscleGroup = "Ноги", defaultRestSeconds = 90),

                    // Плечи (Дельты)
                    ExerciseEntity(name = "Армейский жим стоя", muscleGroup = "Плечи", defaultRestSeconds = 120),
                    ExerciseEntity(name = "Жим гантелей сидя", muscleGroup = "Плечи", defaultRestSeconds = 90),
                    ExerciseEntity(name = "Махи гантелями через стороны", muscleGroup = "Плечи", defaultRestSeconds = 60),
                    ExerciseEntity(name = "Тяга штанги к подбородку", muscleGroup = "Плечи", defaultRestSeconds = 75),
                    ExerciseEntity(name = "Махи в наклоне на заднюю дельту", muscleGroup = "Плечи", defaultRestSeconds = 60),
                    ExerciseEntity(name = "Махи в кроссовере назад (Face Pull)", muscleGroup = "Плечи", defaultRestSeconds = 60),

                    // Руки (Бицепс / Трицепс)
                    ExerciseEntity(name = "Подъем штанги на бицепс стоя", muscleGroup = "Руки", defaultRestSeconds = 60),
                    ExerciseEntity(name = "Молотковые сгибания (Hummer)", muscleGroup = "Руки", defaultRestSeconds = 60),
                    ExerciseEntity(name = "Сгибания на скамье Скотта", muscleGroup = "Руки", defaultRestSeconds = 60),
                    ExerciseEntity(name = "Французский жим лежа со штангой", muscleGroup = "Руки", defaultRestSeconds = 75),
                    ExerciseEntity(name = "Разгибания рук на верхнем блоке (канат)", muscleGroup = "Руки", defaultRestSeconds = 60),
                    ExerciseEntity(name = "Жим узким хватом", muscleGroup = "Руки", defaultRestSeconds = 90),

                    // Пресс и Кор
                    ExerciseEntity(name = "Скручивания на наклонной скамье", muscleGroup = "Пресс/Кор", defaultRestSeconds = 45),
                    ExerciseEntity(name = "Подъем ног в висе на турнике", muscleGroup = "Пресс/Кор", defaultRestSeconds = 60),
                    ExerciseEntity(name = "Планка на предплечьях", muscleGroup = "Пресс/Кор", defaultRestSeconds = 45),
                    ExerciseEntity(name = "Молитва (скручивания на блоке)", muscleGroup = "Пресс/Кор", defaultRestSeconds = 45)
                )
                dao.insertExercises(exercises)
            }
        }
    }
}
