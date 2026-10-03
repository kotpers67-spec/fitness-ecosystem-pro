package com.trainerapp.pro.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.trainerapp.pro.data.local.dao.TrainerDao;
import com.trainerapp.pro.data.local.dao.TrainerDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class TrainerDatabase_Impl extends TrainerDatabase {
  private volatile TrainerDao _trainerDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(3) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `clients` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `fullName` TEXT NOT NULL, `phone` TEXT NOT NULL, `goal` TEXT NOT NULL, `membershipStatus` TEXT NOT NULL, `membershipExpiryDate` TEXT NOT NULL, `notes` TEXT NOT NULL, `clientUuid` TEXT NOT NULL, `pairingCode` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `photoUri` TEXT, `avatarBase64` TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `anthropometry` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `clientId` INTEGER NOT NULL, `date` TEXT NOT NULL, `weightKg` REAL NOT NULL, `chestCm` REAL, `waistCm` REAL, `hipsCm` REAL, `bicepsCm` REAL, `thighCm` REAL, FOREIGN KEY(`clientId`) REFERENCES `clients`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_anthropometry_clientId` ON `anthropometry` (`clientId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `muscleGroup` TEXT NOT NULL, `defaultRestSeconds` INTEGER NOT NULL, `isCustom` INTEGER NOT NULL, `description` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `workout_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `clientId` INTEGER NOT NULL, `date` TEXT NOT NULL, `comments` TEXT NOT NULL, `completed` INTEGER NOT NULL, `isSelfWorkoutAllowed` INTEGER NOT NULL, FOREIGN KEY(`clientId`) REFERENCES `clients`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sessions_clientId` ON `workout_sessions` (`clientId`)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_workout_sessions_clientId_date` ON `workout_sessions` (`clientId`, `date`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `workout_sets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` INTEGER NOT NULL, `exerciseId` INTEGER NOT NULL, `exerciseOrder` INTEGER NOT NULL, `setNumber` INTEGER NOT NULL, `weightKg` REAL NOT NULL, `reps` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, `rpe` REAL, FOREIGN KEY(`sessionId`) REFERENCES `workout_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sets_sessionId` ON `workout_sets` (`sessionId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sets_exerciseId` ON `workout_sets` (`exerciseId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `appointments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `clientId` INTEGER NOT NULL, `dateTime` TEXT NOT NULL, `status` TEXT NOT NULL, `notes` TEXT NOT NULL, FOREIGN KEY(`clientId`) REFERENCES `clients`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_appointments_clientId` ON `appointments` (`clientId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `app_settings` (`id` INTEGER NOT NULL, `currentThemeName` TEXT NOT NULL, `currentLayoutStyleName` TEXT NOT NULL, `selectedClientId` INTEGER, `language` TEXT NOT NULL, `githubToken` TEXT NOT NULL, `githubRepo` TEXT NOT NULL, `soundEnabled` INTEGER NOT NULL, `vibrationEnabled` INTEGER NOT NULL, `autoStartTimer` INTEGER NOT NULL, `defaultRestTimeSeconds` INTEGER NOT NULL, `bleSyncEnabled` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '4fb7365c4c385f2f830b12bfb055ee08')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `clients`");
        db.execSQL("DROP TABLE IF EXISTS `anthropometry`");
        db.execSQL("DROP TABLE IF EXISTS `exercises`");
        db.execSQL("DROP TABLE IF EXISTS `workout_sessions`");
        db.execSQL("DROP TABLE IF EXISTS `workout_sets`");
        db.execSQL("DROP TABLE IF EXISTS `appointments`");
        db.execSQL("DROP TABLE IF EXISTS `app_settings`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsClients = new HashMap<String, TableInfo.Column>(12);
        _columnsClients.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsClients.put("fullName", new TableInfo.Column("fullName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsClients.put("phone", new TableInfo.Column("phone", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsClients.put("goal", new TableInfo.Column("goal", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsClients.put("membershipStatus", new TableInfo.Column("membershipStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsClients.put("membershipExpiryDate", new TableInfo.Column("membershipExpiryDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsClients.put("notes", new TableInfo.Column("notes", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsClients.put("clientUuid", new TableInfo.Column("clientUuid", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsClients.put("pairingCode", new TableInfo.Column("pairingCode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsClients.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsClients.put("photoUri", new TableInfo.Column("photoUri", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsClients.put("avatarBase64", new TableInfo.Column("avatarBase64", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysClients = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesClients = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoClients = new TableInfo("clients", _columnsClients, _foreignKeysClients, _indicesClients);
        final TableInfo _existingClients = TableInfo.read(db, "clients");
        if (!_infoClients.equals(_existingClients)) {
          return new RoomOpenHelper.ValidationResult(false, "clients(com.trainerapp.pro.data.local.entities.ClientEntity).\n"
                  + " Expected:\n" + _infoClients + "\n"
                  + " Found:\n" + _existingClients);
        }
        final HashMap<String, TableInfo.Column> _columnsAnthropometry = new HashMap<String, TableInfo.Column>(9);
        _columnsAnthropometry.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnthropometry.put("clientId", new TableInfo.Column("clientId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnthropometry.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnthropometry.put("weightKg", new TableInfo.Column("weightKg", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnthropometry.put("chestCm", new TableInfo.Column("chestCm", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnthropometry.put("waistCm", new TableInfo.Column("waistCm", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnthropometry.put("hipsCm", new TableInfo.Column("hipsCm", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnthropometry.put("bicepsCm", new TableInfo.Column("bicepsCm", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnthropometry.put("thighCm", new TableInfo.Column("thighCm", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAnthropometry = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysAnthropometry.add(new TableInfo.ForeignKey("clients", "CASCADE", "NO ACTION", Arrays.asList("clientId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesAnthropometry = new HashSet<TableInfo.Index>(1);
        _indicesAnthropometry.add(new TableInfo.Index("index_anthropometry_clientId", false, Arrays.asList("clientId"), Arrays.asList("ASC")));
        final TableInfo _infoAnthropometry = new TableInfo("anthropometry", _columnsAnthropometry, _foreignKeysAnthropometry, _indicesAnthropometry);
        final TableInfo _existingAnthropometry = TableInfo.read(db, "anthropometry");
        if (!_infoAnthropometry.equals(_existingAnthropometry)) {
          return new RoomOpenHelper.ValidationResult(false, "anthropometry(com.trainerapp.pro.data.local.entities.AnthropometryEntity).\n"
                  + " Expected:\n" + _infoAnthropometry + "\n"
                  + " Found:\n" + _existingAnthropometry);
        }
        final HashMap<String, TableInfo.Column> _columnsExercises = new HashMap<String, TableInfo.Column>(6);
        _columnsExercises.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("muscleGroup", new TableInfo.Column("muscleGroup", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("defaultRestSeconds", new TableInfo.Column("defaultRestSeconds", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("isCustom", new TableInfo.Column("isCustom", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysExercises = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesExercises = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoExercises = new TableInfo("exercises", _columnsExercises, _foreignKeysExercises, _indicesExercises);
        final TableInfo _existingExercises = TableInfo.read(db, "exercises");
        if (!_infoExercises.equals(_existingExercises)) {
          return new RoomOpenHelper.ValidationResult(false, "exercises(com.trainerapp.pro.data.local.entities.ExerciseEntity).\n"
                  + " Expected:\n" + _infoExercises + "\n"
                  + " Found:\n" + _existingExercises);
        }
        final HashMap<String, TableInfo.Column> _columnsWorkoutSessions = new HashMap<String, TableInfo.Column>(6);
        _columnsWorkoutSessions.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSessions.put("clientId", new TableInfo.Column("clientId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSessions.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSessions.put("comments", new TableInfo.Column("comments", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSessions.put("completed", new TableInfo.Column("completed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSessions.put("isSelfWorkoutAllowed", new TableInfo.Column("isSelfWorkoutAllowed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWorkoutSessions = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysWorkoutSessions.add(new TableInfo.ForeignKey("clients", "CASCADE", "NO ACTION", Arrays.asList("clientId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesWorkoutSessions = new HashSet<TableInfo.Index>(2);
        _indicesWorkoutSessions.add(new TableInfo.Index("index_workout_sessions_clientId", false, Arrays.asList("clientId"), Arrays.asList("ASC")));
        _indicesWorkoutSessions.add(new TableInfo.Index("index_workout_sessions_clientId_date", true, Arrays.asList("clientId", "date"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoWorkoutSessions = new TableInfo("workout_sessions", _columnsWorkoutSessions, _foreignKeysWorkoutSessions, _indicesWorkoutSessions);
        final TableInfo _existingWorkoutSessions = TableInfo.read(db, "workout_sessions");
        if (!_infoWorkoutSessions.equals(_existingWorkoutSessions)) {
          return new RoomOpenHelper.ValidationResult(false, "workout_sessions(com.trainerapp.pro.data.local.entities.WorkoutSessionEntity).\n"
                  + " Expected:\n" + _infoWorkoutSessions + "\n"
                  + " Found:\n" + _existingWorkoutSessions);
        }
        final HashMap<String, TableInfo.Column> _columnsWorkoutSets = new HashMap<String, TableInfo.Column>(9);
        _columnsWorkoutSets.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSets.put("sessionId", new TableInfo.Column("sessionId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSets.put("exerciseId", new TableInfo.Column("exerciseId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSets.put("exerciseOrder", new TableInfo.Column("exerciseOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSets.put("setNumber", new TableInfo.Column("setNumber", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSets.put("weightKg", new TableInfo.Column("weightKg", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSets.put("reps", new TableInfo.Column("reps", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSets.put("isCompleted", new TableInfo.Column("isCompleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSets.put("rpe", new TableInfo.Column("rpe", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWorkoutSets = new HashSet<TableInfo.ForeignKey>(2);
        _foreignKeysWorkoutSets.add(new TableInfo.ForeignKey("workout_sessions", "CASCADE", "NO ACTION", Arrays.asList("sessionId"), Arrays.asList("id")));
        _foreignKeysWorkoutSets.add(new TableInfo.ForeignKey("exercises", "CASCADE", "NO ACTION", Arrays.asList("exerciseId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesWorkoutSets = new HashSet<TableInfo.Index>(2);
        _indicesWorkoutSets.add(new TableInfo.Index("index_workout_sets_sessionId", false, Arrays.asList("sessionId"), Arrays.asList("ASC")));
        _indicesWorkoutSets.add(new TableInfo.Index("index_workout_sets_exerciseId", false, Arrays.asList("exerciseId"), Arrays.asList("ASC")));
        final TableInfo _infoWorkoutSets = new TableInfo("workout_sets", _columnsWorkoutSets, _foreignKeysWorkoutSets, _indicesWorkoutSets);
        final TableInfo _existingWorkoutSets = TableInfo.read(db, "workout_sets");
        if (!_infoWorkoutSets.equals(_existingWorkoutSets)) {
          return new RoomOpenHelper.ValidationResult(false, "workout_sets(com.trainerapp.pro.data.local.entities.WorkoutSetEntity).\n"
                  + " Expected:\n" + _infoWorkoutSets + "\n"
                  + " Found:\n" + _existingWorkoutSets);
        }
        final HashMap<String, TableInfo.Column> _columnsAppointments = new HashMap<String, TableInfo.Column>(5);
        _columnsAppointments.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppointments.put("clientId", new TableInfo.Column("clientId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppointments.put("dateTime", new TableInfo.Column("dateTime", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppointments.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppointments.put("notes", new TableInfo.Column("notes", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAppointments = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysAppointments.add(new TableInfo.ForeignKey("clients", "CASCADE", "NO ACTION", Arrays.asList("clientId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesAppointments = new HashSet<TableInfo.Index>(1);
        _indicesAppointments.add(new TableInfo.Index("index_appointments_clientId", false, Arrays.asList("clientId"), Arrays.asList("ASC")));
        final TableInfo _infoAppointments = new TableInfo("appointments", _columnsAppointments, _foreignKeysAppointments, _indicesAppointments);
        final TableInfo _existingAppointments = TableInfo.read(db, "appointments");
        if (!_infoAppointments.equals(_existingAppointments)) {
          return new RoomOpenHelper.ValidationResult(false, "appointments(com.trainerapp.pro.data.local.entities.AppointmentEntity).\n"
                  + " Expected:\n" + _infoAppointments + "\n"
                  + " Found:\n" + _existingAppointments);
        }
        final HashMap<String, TableInfo.Column> _columnsAppSettings = new HashMap<String, TableInfo.Column>(12);
        _columnsAppSettings.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppSettings.put("currentThemeName", new TableInfo.Column("currentThemeName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppSettings.put("currentLayoutStyleName", new TableInfo.Column("currentLayoutStyleName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppSettings.put("selectedClientId", new TableInfo.Column("selectedClientId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppSettings.put("language", new TableInfo.Column("language", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppSettings.put("githubToken", new TableInfo.Column("githubToken", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppSettings.put("githubRepo", new TableInfo.Column("githubRepo", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppSettings.put("soundEnabled", new TableInfo.Column("soundEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppSettings.put("vibrationEnabled", new TableInfo.Column("vibrationEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppSettings.put("autoStartTimer", new TableInfo.Column("autoStartTimer", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppSettings.put("defaultRestTimeSeconds", new TableInfo.Column("defaultRestTimeSeconds", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppSettings.put("bleSyncEnabled", new TableInfo.Column("bleSyncEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAppSettings = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAppSettings = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAppSettings = new TableInfo("app_settings", _columnsAppSettings, _foreignKeysAppSettings, _indicesAppSettings);
        final TableInfo _existingAppSettings = TableInfo.read(db, "app_settings");
        if (!_infoAppSettings.equals(_existingAppSettings)) {
          return new RoomOpenHelper.ValidationResult(false, "app_settings(com.trainerapp.pro.data.local.entities.AppSettingsEntity).\n"
                  + " Expected:\n" + _infoAppSettings + "\n"
                  + " Found:\n" + _existingAppSettings);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "4fb7365c4c385f2f830b12bfb055ee08", "da4c494618b92d77899571a31e15efd7");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "clients","anthropometry","exercises","workout_sessions","workout_sets","appointments","app_settings");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `clients`");
      _db.execSQL("DELETE FROM `anthropometry`");
      _db.execSQL("DELETE FROM `exercises`");
      _db.execSQL("DELETE FROM `workout_sessions`");
      _db.execSQL("DELETE FROM `workout_sets`");
      _db.execSQL("DELETE FROM `appointments`");
      _db.execSQL("DELETE FROM `app_settings`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(TrainerDao.class, TrainerDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public TrainerDao trainerDao() {
    if (_trainerDao != null) {
      return _trainerDao;
    } else {
      synchronized(this) {
        if(_trainerDao == null) {
          _trainerDao = new TrainerDao_Impl(this);
        }
        return _trainerDao;
      }
    }
  }
}
