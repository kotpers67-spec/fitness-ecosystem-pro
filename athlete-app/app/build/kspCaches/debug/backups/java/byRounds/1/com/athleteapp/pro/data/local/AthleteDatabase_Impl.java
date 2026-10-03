package com.athleteapp.pro.data.local;

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
import com.athleteapp.pro.data.local.dao.AthleteDao;
import com.athleteapp.pro.data.local.dao.AthleteDao_Impl;
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
public final class AthleteDatabase_Impl extends AthleteDatabase {
  private volatile AthleteDao _athleteDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(3) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `athlete_profile` (`id` INTEGER NOT NULL, `clientUuid` TEXT NOT NULL, `athleteIdInCoachBase` INTEGER NOT NULL, `fullName` TEXT NOT NULL, `phone` TEXT NOT NULL, `goal` TEXT NOT NULL, `notes` TEXT NOT NULL, `restrictions` TEXT NOT NULL, `avatarPath` TEXT, `photoUri` TEXT, `pairingPin` TEXT NOT NULL, `isPairedWithCoach` INTEGER NOT NULL, `pairedCoachName` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `assigned_exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `muscleGroup` TEXT NOT NULL, `defaultRestSeconds` INTEGER NOT NULL, `description` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `my_workout_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `notes` TEXT NOT NULL, `completed` INTEGER NOT NULL, `isSelfWorkoutAllowed` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `my_workout_sets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` INTEGER NOT NULL, `exerciseId` INTEGER NOT NULL, `exerciseName` TEXT NOT NULL, `muscleGroup` TEXT NOT NULL, `exerciseOrder` INTEGER NOT NULL, `setNumber` INTEGER NOT NULL, `targetWeightKg` REAL NOT NULL, `targetReps` INTEGER NOT NULL, `actualWeightKg` REAL NOT NULL, `actualReps` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, `rpe` REAL, FOREIGN KEY(`sessionId`) REFERENCES `my_workout_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`exerciseId`) REFERENCES `assigned_exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_my_workout_sets_sessionId` ON `my_workout_sets` (`sessionId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_my_workout_sets_exerciseId` ON `my_workout_sets` (`exerciseId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `my_anthropometry` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `weightKg` REAL NOT NULL, `chestCm` REAL, `waistCm` REAL, `hipsCm` REAL, `bicepsCm` REAL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `athlete_app_settings` (`id` INTEGER NOT NULL, `currentThemeName` TEXT NOT NULL, `language` TEXT NOT NULL, `githubToken` TEXT NOT NULL, `githubRepo` TEXT NOT NULL, `coachGitHubOwner` TEXT NOT NULL, `athleteId` INTEGER NOT NULL, `autoStartTimer` INTEGER NOT NULL, `defaultRestTimeSeconds` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'e64a9bbb924d244fae88b2c3136128fb')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `athlete_profile`");
        db.execSQL("DROP TABLE IF EXISTS `assigned_exercises`");
        db.execSQL("DROP TABLE IF EXISTS `my_workout_sessions`");
        db.execSQL("DROP TABLE IF EXISTS `my_workout_sets`");
        db.execSQL("DROP TABLE IF EXISTS `my_anthropometry`");
        db.execSQL("DROP TABLE IF EXISTS `athlete_app_settings`");
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
        final HashMap<String, TableInfo.Column> _columnsAthleteProfile = new HashMap<String, TableInfo.Column>(13);
        _columnsAthleteProfile.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAthleteProfile.put("clientUuid", new TableInfo.Column("clientUuid", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAthleteProfile.put("athleteIdInCoachBase", new TableInfo.Column("athleteIdInCoachBase", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAthleteProfile.put("fullName", new TableInfo.Column("fullName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAthleteProfile.put("phone", new TableInfo.Column("phone", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAthleteProfile.put("goal", new TableInfo.Column("goal", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAthleteProfile.put("notes", new TableInfo.Column("notes", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAthleteProfile.put("restrictions", new TableInfo.Column("restrictions", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAthleteProfile.put("avatarPath", new TableInfo.Column("avatarPath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAthleteProfile.put("photoUri", new TableInfo.Column("photoUri", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAthleteProfile.put("pairingPin", new TableInfo.Column("pairingPin", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAthleteProfile.put("isPairedWithCoach", new TableInfo.Column("isPairedWithCoach", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAthleteProfile.put("pairedCoachName", new TableInfo.Column("pairedCoachName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAthleteProfile = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAthleteProfile = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAthleteProfile = new TableInfo("athlete_profile", _columnsAthleteProfile, _foreignKeysAthleteProfile, _indicesAthleteProfile);
        final TableInfo _existingAthleteProfile = TableInfo.read(db, "athlete_profile");
        if (!_infoAthleteProfile.equals(_existingAthleteProfile)) {
          return new RoomOpenHelper.ValidationResult(false, "athlete_profile(com.athleteapp.pro.data.local.entities.AthleteProfileEntity).\n"
                  + " Expected:\n" + _infoAthleteProfile + "\n"
                  + " Found:\n" + _existingAthleteProfile);
        }
        final HashMap<String, TableInfo.Column> _columnsAssignedExercises = new HashMap<String, TableInfo.Column>(5);
        _columnsAssignedExercises.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAssignedExercises.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAssignedExercises.put("muscleGroup", new TableInfo.Column("muscleGroup", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAssignedExercises.put("defaultRestSeconds", new TableInfo.Column("defaultRestSeconds", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAssignedExercises.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAssignedExercises = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAssignedExercises = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAssignedExercises = new TableInfo("assigned_exercises", _columnsAssignedExercises, _foreignKeysAssignedExercises, _indicesAssignedExercises);
        final TableInfo _existingAssignedExercises = TableInfo.read(db, "assigned_exercises");
        if (!_infoAssignedExercises.equals(_existingAssignedExercises)) {
          return new RoomOpenHelper.ValidationResult(false, "assigned_exercises(com.athleteapp.pro.data.local.entities.AssignedExerciseEntity).\n"
                  + " Expected:\n" + _infoAssignedExercises + "\n"
                  + " Found:\n" + _existingAssignedExercises);
        }
        final HashMap<String, TableInfo.Column> _columnsMyWorkoutSessions = new HashMap<String, TableInfo.Column>(5);
        _columnsMyWorkoutSessions.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMyWorkoutSessions.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMyWorkoutSessions.put("notes", new TableInfo.Column("notes", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMyWorkoutSessions.put("completed", new TableInfo.Column("completed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMyWorkoutSessions.put("isSelfWorkoutAllowed", new TableInfo.Column("isSelfWorkoutAllowed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMyWorkoutSessions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMyWorkoutSessions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMyWorkoutSessions = new TableInfo("my_workout_sessions", _columnsMyWorkoutSessions, _foreignKeysMyWorkoutSessions, _indicesMyWorkoutSessions);
        final TableInfo _existingMyWorkoutSessions = TableInfo.read(db, "my_workout_sessions");
        if (!_infoMyWorkoutSessions.equals(_existingMyWorkoutSessions)) {
          return new RoomOpenHelper.ValidationResult(false, "my_workout_sessions(com.athleteapp.pro.data.local.entities.MyWorkoutSessionEntity).\n"
                  + " Expected:\n" + _infoMyWorkoutSessions + "\n"
                  + " Found:\n" + _existingMyWorkoutSessions);
        }
        final HashMap<String, TableInfo.Column> _columnsMyWorkoutSets = new HashMap<String, TableInfo.Column>(13);
        _columnsMyWorkoutSets.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMyWorkoutSets.put("sessionId", new TableInfo.Column("sessionId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMyWorkoutSets.put("exerciseId", new TableInfo.Column("exerciseId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMyWorkoutSets.put("exerciseName", new TableInfo.Column("exerciseName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMyWorkoutSets.put("muscleGroup", new TableInfo.Column("muscleGroup", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMyWorkoutSets.put("exerciseOrder", new TableInfo.Column("exerciseOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMyWorkoutSets.put("setNumber", new TableInfo.Column("setNumber", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMyWorkoutSets.put("targetWeightKg", new TableInfo.Column("targetWeightKg", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMyWorkoutSets.put("targetReps", new TableInfo.Column("targetReps", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMyWorkoutSets.put("actualWeightKg", new TableInfo.Column("actualWeightKg", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMyWorkoutSets.put("actualReps", new TableInfo.Column("actualReps", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMyWorkoutSets.put("isCompleted", new TableInfo.Column("isCompleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMyWorkoutSets.put("rpe", new TableInfo.Column("rpe", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMyWorkoutSets = new HashSet<TableInfo.ForeignKey>(2);
        _foreignKeysMyWorkoutSets.add(new TableInfo.ForeignKey("my_workout_sessions", "CASCADE", "NO ACTION", Arrays.asList("sessionId"), Arrays.asList("id")));
        _foreignKeysMyWorkoutSets.add(new TableInfo.ForeignKey("assigned_exercises", "CASCADE", "NO ACTION", Arrays.asList("exerciseId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesMyWorkoutSets = new HashSet<TableInfo.Index>(2);
        _indicesMyWorkoutSets.add(new TableInfo.Index("index_my_workout_sets_sessionId", false, Arrays.asList("sessionId"), Arrays.asList("ASC")));
        _indicesMyWorkoutSets.add(new TableInfo.Index("index_my_workout_sets_exerciseId", false, Arrays.asList("exerciseId"), Arrays.asList("ASC")));
        final TableInfo _infoMyWorkoutSets = new TableInfo("my_workout_sets", _columnsMyWorkoutSets, _foreignKeysMyWorkoutSets, _indicesMyWorkoutSets);
        final TableInfo _existingMyWorkoutSets = TableInfo.read(db, "my_workout_sets");
        if (!_infoMyWorkoutSets.equals(_existingMyWorkoutSets)) {
          return new RoomOpenHelper.ValidationResult(false, "my_workout_sets(com.athleteapp.pro.data.local.entities.MyWorkoutSetEntity).\n"
                  + " Expected:\n" + _infoMyWorkoutSets + "\n"
                  + " Found:\n" + _existingMyWorkoutSets);
        }
        final HashMap<String, TableInfo.Column> _columnsMyAnthropometry = new HashMap<String, TableInfo.Column>(7);
        _columnsMyAnthropometry.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMyAnthropometry.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMyAnthropometry.put("weightKg", new TableInfo.Column("weightKg", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMyAnthropometry.put("chestCm", new TableInfo.Column("chestCm", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMyAnthropometry.put("waistCm", new TableInfo.Column("waistCm", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMyAnthropometry.put("hipsCm", new TableInfo.Column("hipsCm", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMyAnthropometry.put("bicepsCm", new TableInfo.Column("bicepsCm", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMyAnthropometry = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMyAnthropometry = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMyAnthropometry = new TableInfo("my_anthropometry", _columnsMyAnthropometry, _foreignKeysMyAnthropometry, _indicesMyAnthropometry);
        final TableInfo _existingMyAnthropometry = TableInfo.read(db, "my_anthropometry");
        if (!_infoMyAnthropometry.equals(_existingMyAnthropometry)) {
          return new RoomOpenHelper.ValidationResult(false, "my_anthropometry(com.athleteapp.pro.data.local.entities.MyAnthropometryEntity).\n"
                  + " Expected:\n" + _infoMyAnthropometry + "\n"
                  + " Found:\n" + _existingMyAnthropometry);
        }
        final HashMap<String, TableInfo.Column> _columnsAthleteAppSettings = new HashMap<String, TableInfo.Column>(9);
        _columnsAthleteAppSettings.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAthleteAppSettings.put("currentThemeName", new TableInfo.Column("currentThemeName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAthleteAppSettings.put("language", new TableInfo.Column("language", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAthleteAppSettings.put("githubToken", new TableInfo.Column("githubToken", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAthleteAppSettings.put("githubRepo", new TableInfo.Column("githubRepo", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAthleteAppSettings.put("coachGitHubOwner", new TableInfo.Column("coachGitHubOwner", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAthleteAppSettings.put("athleteId", new TableInfo.Column("athleteId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAthleteAppSettings.put("autoStartTimer", new TableInfo.Column("autoStartTimer", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAthleteAppSettings.put("defaultRestTimeSeconds", new TableInfo.Column("defaultRestTimeSeconds", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAthleteAppSettings = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAthleteAppSettings = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAthleteAppSettings = new TableInfo("athlete_app_settings", _columnsAthleteAppSettings, _foreignKeysAthleteAppSettings, _indicesAthleteAppSettings);
        final TableInfo _existingAthleteAppSettings = TableInfo.read(db, "athlete_app_settings");
        if (!_infoAthleteAppSettings.equals(_existingAthleteAppSettings)) {
          return new RoomOpenHelper.ValidationResult(false, "athlete_app_settings(com.athleteapp.pro.data.local.entities.AthleteAppSettingsEntity).\n"
                  + " Expected:\n" + _infoAthleteAppSettings + "\n"
                  + " Found:\n" + _existingAthleteAppSettings);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "e64a9bbb924d244fae88b2c3136128fb", "26e71d4047352cb0fbd167ef0ff6f280");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "athlete_profile","assigned_exercises","my_workout_sessions","my_workout_sets","my_anthropometry","athlete_app_settings");
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
      _db.execSQL("DELETE FROM `athlete_profile`");
      _db.execSQL("DELETE FROM `assigned_exercises`");
      _db.execSQL("DELETE FROM `my_workout_sessions`");
      _db.execSQL("DELETE FROM `my_workout_sets`");
      _db.execSQL("DELETE FROM `my_anthropometry`");
      _db.execSQL("DELETE FROM `athlete_app_settings`");
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
    _typeConvertersMap.put(AthleteDao.class, AthleteDao_Impl.getRequiredConverters());
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
  public AthleteDao athleteDao() {
    if (_athleteDao != null) {
      return _athleteDao;
    } else {
      synchronized(this) {
        if(_athleteDao == null) {
          _athleteDao = new AthleteDao_Impl(this);
        }
        return _athleteDao;
      }
    }
  }
}
