package com.athleteapp.pro.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.athleteapp.pro.data.local.entities.AssignedExerciseEntity;
import com.athleteapp.pro.data.local.entities.AthleteAppSettingsEntity;
import com.athleteapp.pro.data.local.entities.AthleteProfileEntity;
import com.athleteapp.pro.data.local.entities.MyAnthropometryEntity;
import com.athleteapp.pro.data.local.entities.MyWorkoutSessionEntity;
import com.athleteapp.pro.data.local.entities.MyWorkoutSetEntity;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AthleteDao_Impl implements AthleteDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<AthleteProfileEntity> __insertionAdapterOfAthleteProfileEntity;

  private final EntityInsertionAdapter<AssignedExerciseEntity> __insertionAdapterOfAssignedExerciseEntity;

  private final EntityInsertionAdapter<MyWorkoutSessionEntity> __insertionAdapterOfMyWorkoutSessionEntity;

  private final EntityInsertionAdapter<MyWorkoutSetEntity> __insertionAdapterOfMyWorkoutSetEntity;

  private final EntityInsertionAdapter<MyAnthropometryEntity> __insertionAdapterOfMyAnthropometryEntity;

  private final EntityInsertionAdapter<AthleteAppSettingsEntity> __insertionAdapterOfAthleteAppSettingsEntity;

  private final EntityDeletionOrUpdateAdapter<MyWorkoutSetEntity> __deletionAdapterOfMyWorkoutSetEntity;

  private final EntityDeletionOrUpdateAdapter<MyWorkoutSessionEntity> __updateAdapterOfMyWorkoutSessionEntity;

  private final EntityDeletionOrUpdateAdapter<MyWorkoutSetEntity> __updateAdapterOfMyWorkoutSetEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteSetsForSession;

  public AthleteDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfAthleteProfileEntity = new EntityInsertionAdapter<AthleteProfileEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `athlete_profile` (`id`,`clientUuid`,`athleteIdInCoachBase`,`fullName`,`phone`,`goal`,`notes`,`restrictions`,`avatarPath`,`photoUri`,`avatarBase64`,`pairingPin`,`isPairedWithCoach`,`pairedCoachName`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AthleteProfileEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getClientUuid());
        statement.bindLong(3, entity.getAthleteIdInCoachBase());
        statement.bindString(4, entity.getFullName());
        statement.bindString(5, entity.getPhone());
        statement.bindString(6, entity.getGoal());
        statement.bindString(7, entity.getNotes());
        statement.bindString(8, entity.getRestrictions());
        if (entity.getAvatarPath() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getAvatarPath());
        }
        if (entity.getPhotoUri() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getPhotoUri());
        }
        if (entity.getAvatarBase64() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getAvatarBase64());
        }
        statement.bindString(12, entity.getPairingPin());
        final int _tmp = entity.isPairedWithCoach() ? 1 : 0;
        statement.bindLong(13, _tmp);
        statement.bindString(14, entity.getPairedCoachName());
      }
    };
    this.__insertionAdapterOfAssignedExerciseEntity = new EntityInsertionAdapter<AssignedExerciseEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `assigned_exercises` (`id`,`name`,`muscleGroup`,`defaultRestSeconds`,`description`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AssignedExerciseEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getMuscleGroup());
        statement.bindLong(4, entity.getDefaultRestSeconds());
        statement.bindString(5, entity.getDescription());
      }
    };
    this.__insertionAdapterOfMyWorkoutSessionEntity = new EntityInsertionAdapter<MyWorkoutSessionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `my_workout_sessions` (`id`,`date`,`notes`,`completed`,`isSelfWorkoutAllowed`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MyWorkoutSessionEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getDate());
        statement.bindString(3, entity.getNotes());
        final int _tmp = entity.getCompleted() ? 1 : 0;
        statement.bindLong(4, _tmp);
        final int _tmp_1 = entity.isSelfWorkoutAllowed() ? 1 : 0;
        statement.bindLong(5, _tmp_1);
      }
    };
    this.__insertionAdapterOfMyWorkoutSetEntity = new EntityInsertionAdapter<MyWorkoutSetEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `my_workout_sets` (`id`,`sessionId`,`exerciseId`,`exerciseName`,`muscleGroup`,`exerciseOrder`,`setNumber`,`targetWeightKg`,`targetReps`,`actualWeightKg`,`actualReps`,`isCompleted`,`rpe`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MyWorkoutSetEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getSessionId());
        statement.bindLong(3, entity.getExerciseId());
        statement.bindString(4, entity.getExerciseName());
        statement.bindString(5, entity.getMuscleGroup());
        statement.bindLong(6, entity.getExerciseOrder());
        statement.bindLong(7, entity.getSetNumber());
        statement.bindDouble(8, entity.getTargetWeightKg());
        statement.bindLong(9, entity.getTargetReps());
        statement.bindDouble(10, entity.getActualWeightKg());
        statement.bindLong(11, entity.getActualReps());
        final int _tmp = entity.isCompleted() ? 1 : 0;
        statement.bindLong(12, _tmp);
        if (entity.getRpe() == null) {
          statement.bindNull(13);
        } else {
          statement.bindDouble(13, entity.getRpe());
        }
      }
    };
    this.__insertionAdapterOfMyAnthropometryEntity = new EntityInsertionAdapter<MyAnthropometryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `my_anthropometry` (`id`,`date`,`weightKg`,`chestCm`,`waistCm`,`hipsCm`,`bicepsCm`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MyAnthropometryEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getDate());
        statement.bindDouble(3, entity.getWeightKg());
        if (entity.getChestCm() == null) {
          statement.bindNull(4);
        } else {
          statement.bindDouble(4, entity.getChestCm());
        }
        if (entity.getWaistCm() == null) {
          statement.bindNull(5);
        } else {
          statement.bindDouble(5, entity.getWaistCm());
        }
        if (entity.getHipsCm() == null) {
          statement.bindNull(6);
        } else {
          statement.bindDouble(6, entity.getHipsCm());
        }
        if (entity.getBicepsCm() == null) {
          statement.bindNull(7);
        } else {
          statement.bindDouble(7, entity.getBicepsCm());
        }
      }
    };
    this.__insertionAdapterOfAthleteAppSettingsEntity = new EntityInsertionAdapter<AthleteAppSettingsEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `athlete_app_settings` (`id`,`currentThemeName`,`language`,`githubToken`,`githubRepo`,`coachGitHubOwner`,`athleteId`,`autoStartTimer`,`defaultRestTimeSeconds`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AthleteAppSettingsEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getCurrentThemeName());
        statement.bindString(3, entity.getLanguage());
        statement.bindString(4, entity.getGithubToken());
        statement.bindString(5, entity.getGithubRepo());
        statement.bindString(6, entity.getCoachGitHubOwner());
        statement.bindLong(7, entity.getAthleteId());
        final int _tmp = entity.getAutoStartTimer() ? 1 : 0;
        statement.bindLong(8, _tmp);
        statement.bindLong(9, entity.getDefaultRestTimeSeconds());
      }
    };
    this.__deletionAdapterOfMyWorkoutSetEntity = new EntityDeletionOrUpdateAdapter<MyWorkoutSetEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `my_workout_sets` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MyWorkoutSetEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfMyWorkoutSessionEntity = new EntityDeletionOrUpdateAdapter<MyWorkoutSessionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `my_workout_sessions` SET `id` = ?,`date` = ?,`notes` = ?,`completed` = ?,`isSelfWorkoutAllowed` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MyWorkoutSessionEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getDate());
        statement.bindString(3, entity.getNotes());
        final int _tmp = entity.getCompleted() ? 1 : 0;
        statement.bindLong(4, _tmp);
        final int _tmp_1 = entity.isSelfWorkoutAllowed() ? 1 : 0;
        statement.bindLong(5, _tmp_1);
        statement.bindLong(6, entity.getId());
      }
    };
    this.__updateAdapterOfMyWorkoutSetEntity = new EntityDeletionOrUpdateAdapter<MyWorkoutSetEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `my_workout_sets` SET `id` = ?,`sessionId` = ?,`exerciseId` = ?,`exerciseName` = ?,`muscleGroup` = ?,`exerciseOrder` = ?,`setNumber` = ?,`targetWeightKg` = ?,`targetReps` = ?,`actualWeightKg` = ?,`actualReps` = ?,`isCompleted` = ?,`rpe` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MyWorkoutSetEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getSessionId());
        statement.bindLong(3, entity.getExerciseId());
        statement.bindString(4, entity.getExerciseName());
        statement.bindString(5, entity.getMuscleGroup());
        statement.bindLong(6, entity.getExerciseOrder());
        statement.bindLong(7, entity.getSetNumber());
        statement.bindDouble(8, entity.getTargetWeightKg());
        statement.bindLong(9, entity.getTargetReps());
        statement.bindDouble(10, entity.getActualWeightKg());
        statement.bindLong(11, entity.getActualReps());
        final int _tmp = entity.isCompleted() ? 1 : 0;
        statement.bindLong(12, _tmp);
        if (entity.getRpe() == null) {
          statement.bindNull(13);
        } else {
          statement.bindDouble(13, entity.getRpe());
        }
        statement.bindLong(14, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteSetsForSession = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM my_workout_sets WHERE sessionId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object saveProfile(final AthleteProfileEntity profile,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfAthleteProfileEntity.insert(profile);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertExercise(final AssignedExerciseEntity exercise,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfAssignedExerciseEntity.insertAndReturnId(exercise);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertExercises(final List<AssignedExerciseEntity> exercises,
      final Continuation<? super List<Long>> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<List<Long>>() {
      @Override
      @NonNull
      public List<Long> call() throws Exception {
        __db.beginTransaction();
        try {
          final List<Long> _result = __insertionAdapterOfAssignedExerciseEntity.insertAndReturnIdsList(exercises);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertSession(final MyWorkoutSessionEntity session,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfMyWorkoutSessionEntity.insertAndReturnId(session);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertSet(final MyWorkoutSetEntity workoutSet,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfMyWorkoutSetEntity.insertAndReturnId(workoutSet);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertSets(final List<MyWorkoutSetEntity> sets,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMyWorkoutSetEntity.insert(sets);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAnthropometry(final MyAnthropometryEntity entry,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfMyAnthropometryEntity.insertAndReturnId(entry);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object saveSettings(final AthleteAppSettingsEntity settings,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfAthleteAppSettingsEntity.insert(settings);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteSet(final MyWorkoutSetEntity workoutSet,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfMyWorkoutSetEntity.handle(workoutSet);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateSession(final MyWorkoutSessionEntity session,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfMyWorkoutSessionEntity.handle(session);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateSet(final MyWorkoutSetEntity workoutSet,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfMyWorkoutSetEntity.handle(workoutSet);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteSetsForSession(final long sessionId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteSetsForSession.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, sessionId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteSetsForSession.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<AthleteProfileEntity> getProfile() {
    final String _sql = "SELECT * FROM athlete_profile WHERE id = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"athlete_profile"}, new Callable<AthleteProfileEntity>() {
      @Override
      @Nullable
      public AthleteProfileEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfClientUuid = CursorUtil.getColumnIndexOrThrow(_cursor, "clientUuid");
          final int _cursorIndexOfAthleteIdInCoachBase = CursorUtil.getColumnIndexOrThrow(_cursor, "athleteIdInCoachBase");
          final int _cursorIndexOfFullName = CursorUtil.getColumnIndexOrThrow(_cursor, "fullName");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfGoal = CursorUtil.getColumnIndexOrThrow(_cursor, "goal");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfRestrictions = CursorUtil.getColumnIndexOrThrow(_cursor, "restrictions");
          final int _cursorIndexOfAvatarPath = CursorUtil.getColumnIndexOrThrow(_cursor, "avatarPath");
          final int _cursorIndexOfPhotoUri = CursorUtil.getColumnIndexOrThrow(_cursor, "photoUri");
          final int _cursorIndexOfAvatarBase64 = CursorUtil.getColumnIndexOrThrow(_cursor, "avatarBase64");
          final int _cursorIndexOfPairingPin = CursorUtil.getColumnIndexOrThrow(_cursor, "pairingPin");
          final int _cursorIndexOfIsPairedWithCoach = CursorUtil.getColumnIndexOrThrow(_cursor, "isPairedWithCoach");
          final int _cursorIndexOfPairedCoachName = CursorUtil.getColumnIndexOrThrow(_cursor, "pairedCoachName");
          final AthleteProfileEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpClientUuid;
            _tmpClientUuid = _cursor.getString(_cursorIndexOfClientUuid);
            final long _tmpAthleteIdInCoachBase;
            _tmpAthleteIdInCoachBase = _cursor.getLong(_cursorIndexOfAthleteIdInCoachBase);
            final String _tmpFullName;
            _tmpFullName = _cursor.getString(_cursorIndexOfFullName);
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final String _tmpGoal;
            _tmpGoal = _cursor.getString(_cursorIndexOfGoal);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpRestrictions;
            _tmpRestrictions = _cursor.getString(_cursorIndexOfRestrictions);
            final String _tmpAvatarPath;
            if (_cursor.isNull(_cursorIndexOfAvatarPath)) {
              _tmpAvatarPath = null;
            } else {
              _tmpAvatarPath = _cursor.getString(_cursorIndexOfAvatarPath);
            }
            final String _tmpPhotoUri;
            if (_cursor.isNull(_cursorIndexOfPhotoUri)) {
              _tmpPhotoUri = null;
            } else {
              _tmpPhotoUri = _cursor.getString(_cursorIndexOfPhotoUri);
            }
            final String _tmpAvatarBase64;
            if (_cursor.isNull(_cursorIndexOfAvatarBase64)) {
              _tmpAvatarBase64 = null;
            } else {
              _tmpAvatarBase64 = _cursor.getString(_cursorIndexOfAvatarBase64);
            }
            final String _tmpPairingPin;
            _tmpPairingPin = _cursor.getString(_cursorIndexOfPairingPin);
            final boolean _tmpIsPairedWithCoach;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPairedWithCoach);
            _tmpIsPairedWithCoach = _tmp != 0;
            final String _tmpPairedCoachName;
            _tmpPairedCoachName = _cursor.getString(_cursorIndexOfPairedCoachName);
            _result = new AthleteProfileEntity(_tmpId,_tmpClientUuid,_tmpAthleteIdInCoachBase,_tmpFullName,_tmpPhone,_tmpGoal,_tmpNotes,_tmpRestrictions,_tmpAvatarPath,_tmpPhotoUri,_tmpAvatarBase64,_tmpPairingPin,_tmpIsPairedWithCoach,_tmpPairedCoachName);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<AssignedExerciseEntity>> getAllExercises() {
    final String _sql = "SELECT * FROM assigned_exercises ORDER BY muscleGroup ASC, name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"assigned_exercises"}, new Callable<List<AssignedExerciseEntity>>() {
      @Override
      @NonNull
      public List<AssignedExerciseEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfMuscleGroup = CursorUtil.getColumnIndexOrThrow(_cursor, "muscleGroup");
          final int _cursorIndexOfDefaultRestSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "defaultRestSeconds");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final List<AssignedExerciseEntity> _result = new ArrayList<AssignedExerciseEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AssignedExerciseEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpMuscleGroup;
            _tmpMuscleGroup = _cursor.getString(_cursorIndexOfMuscleGroup);
            final int _tmpDefaultRestSeconds;
            _tmpDefaultRestSeconds = _cursor.getInt(_cursorIndexOfDefaultRestSeconds);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            _item = new AssignedExerciseEntity(_tmpId,_tmpName,_tmpMuscleGroup,_tmpDefaultRestSeconds,_tmpDescription);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getSessionByDate(final String date,
      final Continuation<? super MyWorkoutSessionEntity> $completion) {
    final String _sql = "SELECT * FROM my_workout_sessions WHERE date = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, date);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<MyWorkoutSessionEntity>() {
      @Override
      @Nullable
      public MyWorkoutSessionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "completed");
          final int _cursorIndexOfIsSelfWorkoutAllowed = CursorUtil.getColumnIndexOrThrow(_cursor, "isSelfWorkoutAllowed");
          final MyWorkoutSessionEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final boolean _tmpCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfCompleted);
            _tmpCompleted = _tmp != 0;
            final boolean _tmpIsSelfWorkoutAllowed;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsSelfWorkoutAllowed);
            _tmpIsSelfWorkoutAllowed = _tmp_1 != 0;
            _result = new MyWorkoutSessionEntity(_tmpId,_tmpDate,_tmpNotes,_tmpCompleted,_tmpIsSelfWorkoutAllowed);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<MyWorkoutSessionEntity>> getAllSessions() {
    final String _sql = "SELECT * FROM my_workout_sessions ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"my_workout_sessions"}, new Callable<List<MyWorkoutSessionEntity>>() {
      @Override
      @NonNull
      public List<MyWorkoutSessionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "completed");
          final int _cursorIndexOfIsSelfWorkoutAllowed = CursorUtil.getColumnIndexOrThrow(_cursor, "isSelfWorkoutAllowed");
          final List<MyWorkoutSessionEntity> _result = new ArrayList<MyWorkoutSessionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MyWorkoutSessionEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final boolean _tmpCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfCompleted);
            _tmpCompleted = _tmp != 0;
            final boolean _tmpIsSelfWorkoutAllowed;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsSelfWorkoutAllowed);
            _tmpIsSelfWorkoutAllowed = _tmp_1 != 0;
            _item = new MyWorkoutSessionEntity(_tmpId,_tmpDate,_tmpNotes,_tmpCompleted,_tmpIsSelfWorkoutAllowed);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<MyWorkoutSetEntity>> getSetsForSession(final long sessionId) {
    final String _sql = "SELECT * FROM my_workout_sets WHERE sessionId = ? ORDER BY exerciseOrder ASC, setNumber ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, sessionId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"my_workout_sets"}, new Callable<List<MyWorkoutSetEntity>>() {
      @Override
      @NonNull
      public List<MyWorkoutSetEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfExerciseId = CursorUtil.getColumnIndexOrThrow(_cursor, "exerciseId");
          final int _cursorIndexOfExerciseName = CursorUtil.getColumnIndexOrThrow(_cursor, "exerciseName");
          final int _cursorIndexOfMuscleGroup = CursorUtil.getColumnIndexOrThrow(_cursor, "muscleGroup");
          final int _cursorIndexOfExerciseOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "exerciseOrder");
          final int _cursorIndexOfSetNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "setNumber");
          final int _cursorIndexOfTargetWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "targetWeightKg");
          final int _cursorIndexOfTargetReps = CursorUtil.getColumnIndexOrThrow(_cursor, "targetReps");
          final int _cursorIndexOfActualWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "actualWeightKg");
          final int _cursorIndexOfActualReps = CursorUtil.getColumnIndexOrThrow(_cursor, "actualReps");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
          final int _cursorIndexOfRpe = CursorUtil.getColumnIndexOrThrow(_cursor, "rpe");
          final List<MyWorkoutSetEntity> _result = new ArrayList<MyWorkoutSetEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MyWorkoutSetEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpSessionId;
            _tmpSessionId = _cursor.getLong(_cursorIndexOfSessionId);
            final long _tmpExerciseId;
            _tmpExerciseId = _cursor.getLong(_cursorIndexOfExerciseId);
            final String _tmpExerciseName;
            _tmpExerciseName = _cursor.getString(_cursorIndexOfExerciseName);
            final String _tmpMuscleGroup;
            _tmpMuscleGroup = _cursor.getString(_cursorIndexOfMuscleGroup);
            final int _tmpExerciseOrder;
            _tmpExerciseOrder = _cursor.getInt(_cursorIndexOfExerciseOrder);
            final int _tmpSetNumber;
            _tmpSetNumber = _cursor.getInt(_cursorIndexOfSetNumber);
            final double _tmpTargetWeightKg;
            _tmpTargetWeightKg = _cursor.getDouble(_cursorIndexOfTargetWeightKg);
            final int _tmpTargetReps;
            _tmpTargetReps = _cursor.getInt(_cursorIndexOfTargetReps);
            final double _tmpActualWeightKg;
            _tmpActualWeightKg = _cursor.getDouble(_cursorIndexOfActualWeightKg);
            final int _tmpActualReps;
            _tmpActualReps = _cursor.getInt(_cursorIndexOfActualReps);
            final boolean _tmpIsCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCompleted);
            _tmpIsCompleted = _tmp != 0;
            final Double _tmpRpe;
            if (_cursor.isNull(_cursorIndexOfRpe)) {
              _tmpRpe = null;
            } else {
              _tmpRpe = _cursor.getDouble(_cursorIndexOfRpe);
            }
            _item = new MyWorkoutSetEntity(_tmpId,_tmpSessionId,_tmpExerciseId,_tmpExerciseName,_tmpMuscleGroup,_tmpExerciseOrder,_tmpSetNumber,_tmpTargetWeightKg,_tmpTargetReps,_tmpActualWeightKg,_tmpActualReps,_tmpIsCompleted,_tmpRpe);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getSetsForSessionSync(final long sessionId,
      final Continuation<? super List<MyWorkoutSetEntity>> $completion) {
    final String _sql = "SELECT * FROM my_workout_sets WHERE sessionId = ? ORDER BY exerciseOrder ASC, setNumber ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, sessionId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MyWorkoutSetEntity>>() {
      @Override
      @NonNull
      public List<MyWorkoutSetEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfExerciseId = CursorUtil.getColumnIndexOrThrow(_cursor, "exerciseId");
          final int _cursorIndexOfExerciseName = CursorUtil.getColumnIndexOrThrow(_cursor, "exerciseName");
          final int _cursorIndexOfMuscleGroup = CursorUtil.getColumnIndexOrThrow(_cursor, "muscleGroup");
          final int _cursorIndexOfExerciseOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "exerciseOrder");
          final int _cursorIndexOfSetNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "setNumber");
          final int _cursorIndexOfTargetWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "targetWeightKg");
          final int _cursorIndexOfTargetReps = CursorUtil.getColumnIndexOrThrow(_cursor, "targetReps");
          final int _cursorIndexOfActualWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "actualWeightKg");
          final int _cursorIndexOfActualReps = CursorUtil.getColumnIndexOrThrow(_cursor, "actualReps");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
          final int _cursorIndexOfRpe = CursorUtil.getColumnIndexOrThrow(_cursor, "rpe");
          final List<MyWorkoutSetEntity> _result = new ArrayList<MyWorkoutSetEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MyWorkoutSetEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpSessionId;
            _tmpSessionId = _cursor.getLong(_cursorIndexOfSessionId);
            final long _tmpExerciseId;
            _tmpExerciseId = _cursor.getLong(_cursorIndexOfExerciseId);
            final String _tmpExerciseName;
            _tmpExerciseName = _cursor.getString(_cursorIndexOfExerciseName);
            final String _tmpMuscleGroup;
            _tmpMuscleGroup = _cursor.getString(_cursorIndexOfMuscleGroup);
            final int _tmpExerciseOrder;
            _tmpExerciseOrder = _cursor.getInt(_cursorIndexOfExerciseOrder);
            final int _tmpSetNumber;
            _tmpSetNumber = _cursor.getInt(_cursorIndexOfSetNumber);
            final double _tmpTargetWeightKg;
            _tmpTargetWeightKg = _cursor.getDouble(_cursorIndexOfTargetWeightKg);
            final int _tmpTargetReps;
            _tmpTargetReps = _cursor.getInt(_cursorIndexOfTargetReps);
            final double _tmpActualWeightKg;
            _tmpActualWeightKg = _cursor.getDouble(_cursorIndexOfActualWeightKg);
            final int _tmpActualReps;
            _tmpActualReps = _cursor.getInt(_cursorIndexOfActualReps);
            final boolean _tmpIsCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCompleted);
            _tmpIsCompleted = _tmp != 0;
            final Double _tmpRpe;
            if (_cursor.isNull(_cursorIndexOfRpe)) {
              _tmpRpe = null;
            } else {
              _tmpRpe = _cursor.getDouble(_cursorIndexOfRpe);
            }
            _item = new MyWorkoutSetEntity(_tmpId,_tmpSessionId,_tmpExerciseId,_tmpExerciseName,_tmpMuscleGroup,_tmpExerciseOrder,_tmpSetNumber,_tmpTargetWeightKg,_tmpTargetReps,_tmpActualWeightKg,_tmpActualReps,_tmpIsCompleted,_tmpRpe);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getRecentCompletedSets(
      final Continuation<? super List<MyWorkoutSetEntity>> $completion) {
    final String _sql = "SELECT * FROM my_workout_sets WHERE isCompleted = 1 ORDER BY id DESC LIMIT 50";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MyWorkoutSetEntity>>() {
      @Override
      @NonNull
      public List<MyWorkoutSetEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfExerciseId = CursorUtil.getColumnIndexOrThrow(_cursor, "exerciseId");
          final int _cursorIndexOfExerciseName = CursorUtil.getColumnIndexOrThrow(_cursor, "exerciseName");
          final int _cursorIndexOfMuscleGroup = CursorUtil.getColumnIndexOrThrow(_cursor, "muscleGroup");
          final int _cursorIndexOfExerciseOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "exerciseOrder");
          final int _cursorIndexOfSetNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "setNumber");
          final int _cursorIndexOfTargetWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "targetWeightKg");
          final int _cursorIndexOfTargetReps = CursorUtil.getColumnIndexOrThrow(_cursor, "targetReps");
          final int _cursorIndexOfActualWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "actualWeightKg");
          final int _cursorIndexOfActualReps = CursorUtil.getColumnIndexOrThrow(_cursor, "actualReps");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
          final int _cursorIndexOfRpe = CursorUtil.getColumnIndexOrThrow(_cursor, "rpe");
          final List<MyWorkoutSetEntity> _result = new ArrayList<MyWorkoutSetEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MyWorkoutSetEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpSessionId;
            _tmpSessionId = _cursor.getLong(_cursorIndexOfSessionId);
            final long _tmpExerciseId;
            _tmpExerciseId = _cursor.getLong(_cursorIndexOfExerciseId);
            final String _tmpExerciseName;
            _tmpExerciseName = _cursor.getString(_cursorIndexOfExerciseName);
            final String _tmpMuscleGroup;
            _tmpMuscleGroup = _cursor.getString(_cursorIndexOfMuscleGroup);
            final int _tmpExerciseOrder;
            _tmpExerciseOrder = _cursor.getInt(_cursorIndexOfExerciseOrder);
            final int _tmpSetNumber;
            _tmpSetNumber = _cursor.getInt(_cursorIndexOfSetNumber);
            final double _tmpTargetWeightKg;
            _tmpTargetWeightKg = _cursor.getDouble(_cursorIndexOfTargetWeightKg);
            final int _tmpTargetReps;
            _tmpTargetReps = _cursor.getInt(_cursorIndexOfTargetReps);
            final double _tmpActualWeightKg;
            _tmpActualWeightKg = _cursor.getDouble(_cursorIndexOfActualWeightKg);
            final int _tmpActualReps;
            _tmpActualReps = _cursor.getInt(_cursorIndexOfActualReps);
            final boolean _tmpIsCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCompleted);
            _tmpIsCompleted = _tmp != 0;
            final Double _tmpRpe;
            if (_cursor.isNull(_cursorIndexOfRpe)) {
              _tmpRpe = null;
            } else {
              _tmpRpe = _cursor.getDouble(_cursorIndexOfRpe);
            }
            _item = new MyWorkoutSetEntity(_tmpId,_tmpSessionId,_tmpExerciseId,_tmpExerciseName,_tmpMuscleGroup,_tmpExerciseOrder,_tmpSetNumber,_tmpTargetWeightKg,_tmpTargetReps,_tmpActualWeightKg,_tmpActualReps,_tmpIsCompleted,_tmpRpe);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getCompletedSetsForExercise(final long exerciseId,
      final Continuation<? super List<MyWorkoutSetEntity>> $completion) {
    final String _sql = "SELECT * FROM my_workout_sets WHERE exerciseId = ? AND isCompleted = 1 ORDER BY id DESC LIMIT 20";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, exerciseId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MyWorkoutSetEntity>>() {
      @Override
      @NonNull
      public List<MyWorkoutSetEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfExerciseId = CursorUtil.getColumnIndexOrThrow(_cursor, "exerciseId");
          final int _cursorIndexOfExerciseName = CursorUtil.getColumnIndexOrThrow(_cursor, "exerciseName");
          final int _cursorIndexOfMuscleGroup = CursorUtil.getColumnIndexOrThrow(_cursor, "muscleGroup");
          final int _cursorIndexOfExerciseOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "exerciseOrder");
          final int _cursorIndexOfSetNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "setNumber");
          final int _cursorIndexOfTargetWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "targetWeightKg");
          final int _cursorIndexOfTargetReps = CursorUtil.getColumnIndexOrThrow(_cursor, "targetReps");
          final int _cursorIndexOfActualWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "actualWeightKg");
          final int _cursorIndexOfActualReps = CursorUtil.getColumnIndexOrThrow(_cursor, "actualReps");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
          final int _cursorIndexOfRpe = CursorUtil.getColumnIndexOrThrow(_cursor, "rpe");
          final List<MyWorkoutSetEntity> _result = new ArrayList<MyWorkoutSetEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MyWorkoutSetEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpSessionId;
            _tmpSessionId = _cursor.getLong(_cursorIndexOfSessionId);
            final long _tmpExerciseId;
            _tmpExerciseId = _cursor.getLong(_cursorIndexOfExerciseId);
            final String _tmpExerciseName;
            _tmpExerciseName = _cursor.getString(_cursorIndexOfExerciseName);
            final String _tmpMuscleGroup;
            _tmpMuscleGroup = _cursor.getString(_cursorIndexOfMuscleGroup);
            final int _tmpExerciseOrder;
            _tmpExerciseOrder = _cursor.getInt(_cursorIndexOfExerciseOrder);
            final int _tmpSetNumber;
            _tmpSetNumber = _cursor.getInt(_cursorIndexOfSetNumber);
            final double _tmpTargetWeightKg;
            _tmpTargetWeightKg = _cursor.getDouble(_cursorIndexOfTargetWeightKg);
            final int _tmpTargetReps;
            _tmpTargetReps = _cursor.getInt(_cursorIndexOfTargetReps);
            final double _tmpActualWeightKg;
            _tmpActualWeightKg = _cursor.getDouble(_cursorIndexOfActualWeightKg);
            final int _tmpActualReps;
            _tmpActualReps = _cursor.getInt(_cursorIndexOfActualReps);
            final boolean _tmpIsCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCompleted);
            _tmpIsCompleted = _tmp != 0;
            final Double _tmpRpe;
            if (_cursor.isNull(_cursorIndexOfRpe)) {
              _tmpRpe = null;
            } else {
              _tmpRpe = _cursor.getDouble(_cursorIndexOfRpe);
            }
            _item = new MyWorkoutSetEntity(_tmpId,_tmpSessionId,_tmpExerciseId,_tmpExerciseName,_tmpMuscleGroup,_tmpExerciseOrder,_tmpSetNumber,_tmpTargetWeightKg,_tmpTargetReps,_tmpActualWeightKg,_tmpActualReps,_tmpIsCompleted,_tmpRpe);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getAllSetsSync(final Continuation<? super List<MyWorkoutSetEntity>> $completion) {
    final String _sql = "SELECT * FROM my_workout_sets ORDER BY sessionId ASC, exerciseOrder ASC, setNumber ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MyWorkoutSetEntity>>() {
      @Override
      @NonNull
      public List<MyWorkoutSetEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfExerciseId = CursorUtil.getColumnIndexOrThrow(_cursor, "exerciseId");
          final int _cursorIndexOfExerciseName = CursorUtil.getColumnIndexOrThrow(_cursor, "exerciseName");
          final int _cursorIndexOfMuscleGroup = CursorUtil.getColumnIndexOrThrow(_cursor, "muscleGroup");
          final int _cursorIndexOfExerciseOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "exerciseOrder");
          final int _cursorIndexOfSetNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "setNumber");
          final int _cursorIndexOfTargetWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "targetWeightKg");
          final int _cursorIndexOfTargetReps = CursorUtil.getColumnIndexOrThrow(_cursor, "targetReps");
          final int _cursorIndexOfActualWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "actualWeightKg");
          final int _cursorIndexOfActualReps = CursorUtil.getColumnIndexOrThrow(_cursor, "actualReps");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
          final int _cursorIndexOfRpe = CursorUtil.getColumnIndexOrThrow(_cursor, "rpe");
          final List<MyWorkoutSetEntity> _result = new ArrayList<MyWorkoutSetEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MyWorkoutSetEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpSessionId;
            _tmpSessionId = _cursor.getLong(_cursorIndexOfSessionId);
            final long _tmpExerciseId;
            _tmpExerciseId = _cursor.getLong(_cursorIndexOfExerciseId);
            final String _tmpExerciseName;
            _tmpExerciseName = _cursor.getString(_cursorIndexOfExerciseName);
            final String _tmpMuscleGroup;
            _tmpMuscleGroup = _cursor.getString(_cursorIndexOfMuscleGroup);
            final int _tmpExerciseOrder;
            _tmpExerciseOrder = _cursor.getInt(_cursorIndexOfExerciseOrder);
            final int _tmpSetNumber;
            _tmpSetNumber = _cursor.getInt(_cursorIndexOfSetNumber);
            final double _tmpTargetWeightKg;
            _tmpTargetWeightKg = _cursor.getDouble(_cursorIndexOfTargetWeightKg);
            final int _tmpTargetReps;
            _tmpTargetReps = _cursor.getInt(_cursorIndexOfTargetReps);
            final double _tmpActualWeightKg;
            _tmpActualWeightKg = _cursor.getDouble(_cursorIndexOfActualWeightKg);
            final int _tmpActualReps;
            _tmpActualReps = _cursor.getInt(_cursorIndexOfActualReps);
            final boolean _tmpIsCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCompleted);
            _tmpIsCompleted = _tmp != 0;
            final Double _tmpRpe;
            if (_cursor.isNull(_cursorIndexOfRpe)) {
              _tmpRpe = null;
            } else {
              _tmpRpe = _cursor.getDouble(_cursorIndexOfRpe);
            }
            _item = new MyWorkoutSetEntity(_tmpId,_tmpSessionId,_tmpExerciseId,_tmpExerciseName,_tmpMuscleGroup,_tmpExerciseOrder,_tmpSetNumber,_tmpTargetWeightKg,_tmpTargetReps,_tmpActualWeightKg,_tmpActualReps,_tmpIsCompleted,_tmpRpe);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getAllSessionsSync(
      final Continuation<? super List<MyWorkoutSessionEntity>> $completion) {
    final String _sql = "SELECT * FROM my_workout_sessions ORDER BY date ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MyWorkoutSessionEntity>>() {
      @Override
      @NonNull
      public List<MyWorkoutSessionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "completed");
          final int _cursorIndexOfIsSelfWorkoutAllowed = CursorUtil.getColumnIndexOrThrow(_cursor, "isSelfWorkoutAllowed");
          final List<MyWorkoutSessionEntity> _result = new ArrayList<MyWorkoutSessionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MyWorkoutSessionEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final boolean _tmpCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfCompleted);
            _tmpCompleted = _tmp != 0;
            final boolean _tmpIsSelfWorkoutAllowed;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsSelfWorkoutAllowed);
            _tmpIsSelfWorkoutAllowed = _tmp_1 != 0;
            _item = new MyWorkoutSessionEntity(_tmpId,_tmpDate,_tmpNotes,_tmpCompleted,_tmpIsSelfWorkoutAllowed);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getAllAnthropometrySync(
      final Continuation<? super List<MyAnthropometryEntity>> $completion) {
    final String _sql = "SELECT * FROM my_anthropometry ORDER BY date ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MyAnthropometryEntity>>() {
      @Override
      @NonNull
      public List<MyAnthropometryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weightKg");
          final int _cursorIndexOfChestCm = CursorUtil.getColumnIndexOrThrow(_cursor, "chestCm");
          final int _cursorIndexOfWaistCm = CursorUtil.getColumnIndexOrThrow(_cursor, "waistCm");
          final int _cursorIndexOfHipsCm = CursorUtil.getColumnIndexOrThrow(_cursor, "hipsCm");
          final int _cursorIndexOfBicepsCm = CursorUtil.getColumnIndexOrThrow(_cursor, "bicepsCm");
          final List<MyAnthropometryEntity> _result = new ArrayList<MyAnthropometryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MyAnthropometryEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final double _tmpWeightKg;
            _tmpWeightKg = _cursor.getDouble(_cursorIndexOfWeightKg);
            final Double _tmpChestCm;
            if (_cursor.isNull(_cursorIndexOfChestCm)) {
              _tmpChestCm = null;
            } else {
              _tmpChestCm = _cursor.getDouble(_cursorIndexOfChestCm);
            }
            final Double _tmpWaistCm;
            if (_cursor.isNull(_cursorIndexOfWaistCm)) {
              _tmpWaistCm = null;
            } else {
              _tmpWaistCm = _cursor.getDouble(_cursorIndexOfWaistCm);
            }
            final Double _tmpHipsCm;
            if (_cursor.isNull(_cursorIndexOfHipsCm)) {
              _tmpHipsCm = null;
            } else {
              _tmpHipsCm = _cursor.getDouble(_cursorIndexOfHipsCm);
            }
            final Double _tmpBicepsCm;
            if (_cursor.isNull(_cursorIndexOfBicepsCm)) {
              _tmpBicepsCm = null;
            } else {
              _tmpBicepsCm = _cursor.getDouble(_cursorIndexOfBicepsCm);
            }
            _item = new MyAnthropometryEntity(_tmpId,_tmpDate,_tmpWeightKg,_tmpChestCm,_tmpWaistCm,_tmpHipsCm,_tmpBicepsCm);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getAllExercisesSync(
      final Continuation<? super List<AssignedExerciseEntity>> $completion) {
    final String _sql = "SELECT * FROM assigned_exercises ORDER BY id ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<AssignedExerciseEntity>>() {
      @Override
      @NonNull
      public List<AssignedExerciseEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfMuscleGroup = CursorUtil.getColumnIndexOrThrow(_cursor, "muscleGroup");
          final int _cursorIndexOfDefaultRestSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "defaultRestSeconds");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final List<AssignedExerciseEntity> _result = new ArrayList<AssignedExerciseEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AssignedExerciseEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpMuscleGroup;
            _tmpMuscleGroup = _cursor.getString(_cursorIndexOfMuscleGroup);
            final int _tmpDefaultRestSeconds;
            _tmpDefaultRestSeconds = _cursor.getInt(_cursorIndexOfDefaultRestSeconds);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            _item = new AssignedExerciseEntity(_tmpId,_tmpName,_tmpMuscleGroup,_tmpDefaultRestSeconds,_tmpDescription);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<MyAnthropometryEntity>> getAllAnthropometry() {
    final String _sql = "SELECT * FROM my_anthropometry ORDER BY date ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"my_anthropometry"}, new Callable<List<MyAnthropometryEntity>>() {
      @Override
      @NonNull
      public List<MyAnthropometryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weightKg");
          final int _cursorIndexOfChestCm = CursorUtil.getColumnIndexOrThrow(_cursor, "chestCm");
          final int _cursorIndexOfWaistCm = CursorUtil.getColumnIndexOrThrow(_cursor, "waistCm");
          final int _cursorIndexOfHipsCm = CursorUtil.getColumnIndexOrThrow(_cursor, "hipsCm");
          final int _cursorIndexOfBicepsCm = CursorUtil.getColumnIndexOrThrow(_cursor, "bicepsCm");
          final List<MyAnthropometryEntity> _result = new ArrayList<MyAnthropometryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MyAnthropometryEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final double _tmpWeightKg;
            _tmpWeightKg = _cursor.getDouble(_cursorIndexOfWeightKg);
            final Double _tmpChestCm;
            if (_cursor.isNull(_cursorIndexOfChestCm)) {
              _tmpChestCm = null;
            } else {
              _tmpChestCm = _cursor.getDouble(_cursorIndexOfChestCm);
            }
            final Double _tmpWaistCm;
            if (_cursor.isNull(_cursorIndexOfWaistCm)) {
              _tmpWaistCm = null;
            } else {
              _tmpWaistCm = _cursor.getDouble(_cursorIndexOfWaistCm);
            }
            final Double _tmpHipsCm;
            if (_cursor.isNull(_cursorIndexOfHipsCm)) {
              _tmpHipsCm = null;
            } else {
              _tmpHipsCm = _cursor.getDouble(_cursorIndexOfHipsCm);
            }
            final Double _tmpBicepsCm;
            if (_cursor.isNull(_cursorIndexOfBicepsCm)) {
              _tmpBicepsCm = null;
            } else {
              _tmpBicepsCm = _cursor.getDouble(_cursorIndexOfBicepsCm);
            }
            _item = new MyAnthropometryEntity(_tmpId,_tmpDate,_tmpWeightKg,_tmpChestCm,_tmpWaistCm,_tmpHipsCm,_tmpBicepsCm);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<AthleteSetHistory>> getExerciseHistory(final long exerciseId) {
    final String _sql = "\n"
            + "        SELECT s.date, ws.actualWeightKg as weightKg, ws.actualReps as reps, ws.isCompleted\n"
            + "        FROM my_workout_sets ws\n"
            + "        INNER JOIN my_workout_sessions s ON ws.sessionId = s.id\n"
            + "        WHERE ws.exerciseId = ?\n"
            + "        ORDER BY s.date ASC, ws.setNumber ASC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, exerciseId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"my_workout_sets",
        "my_workout_sessions"}, new Callable<List<AthleteSetHistory>>() {
      @Override
      @NonNull
      public List<AthleteSetHistory> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = 0;
          final int _cursorIndexOfWeightKg = 1;
          final int _cursorIndexOfReps = 2;
          final int _cursorIndexOfIsCompleted = 3;
          final List<AthleteSetHistory> _result = new ArrayList<AthleteSetHistory>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AthleteSetHistory _item;
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final double _tmpWeightKg;
            _tmpWeightKg = _cursor.getDouble(_cursorIndexOfWeightKg);
            final int _tmpReps;
            _tmpReps = _cursor.getInt(_cursorIndexOfReps);
            final boolean _tmpIsCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCompleted);
            _tmpIsCompleted = _tmp != 0;
            _item = new AthleteSetHistory(_tmpDate,_tmpWeightKg,_tmpReps,_tmpIsCompleted);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<AthleteAppSettingsEntity> getSettings() {
    final String _sql = "SELECT * FROM athlete_app_settings WHERE id = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"athlete_app_settings"}, new Callable<AthleteAppSettingsEntity>() {
      @Override
      @Nullable
      public AthleteAppSettingsEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCurrentThemeName = CursorUtil.getColumnIndexOrThrow(_cursor, "currentThemeName");
          final int _cursorIndexOfLanguage = CursorUtil.getColumnIndexOrThrow(_cursor, "language");
          final int _cursorIndexOfGithubToken = CursorUtil.getColumnIndexOrThrow(_cursor, "githubToken");
          final int _cursorIndexOfGithubRepo = CursorUtil.getColumnIndexOrThrow(_cursor, "githubRepo");
          final int _cursorIndexOfCoachGitHubOwner = CursorUtil.getColumnIndexOrThrow(_cursor, "coachGitHubOwner");
          final int _cursorIndexOfAthleteId = CursorUtil.getColumnIndexOrThrow(_cursor, "athleteId");
          final int _cursorIndexOfAutoStartTimer = CursorUtil.getColumnIndexOrThrow(_cursor, "autoStartTimer");
          final int _cursorIndexOfDefaultRestTimeSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "defaultRestTimeSeconds");
          final AthleteAppSettingsEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpCurrentThemeName;
            _tmpCurrentThemeName = _cursor.getString(_cursorIndexOfCurrentThemeName);
            final String _tmpLanguage;
            _tmpLanguage = _cursor.getString(_cursorIndexOfLanguage);
            final String _tmpGithubToken;
            _tmpGithubToken = _cursor.getString(_cursorIndexOfGithubToken);
            final String _tmpGithubRepo;
            _tmpGithubRepo = _cursor.getString(_cursorIndexOfGithubRepo);
            final String _tmpCoachGitHubOwner;
            _tmpCoachGitHubOwner = _cursor.getString(_cursorIndexOfCoachGitHubOwner);
            final long _tmpAthleteId;
            _tmpAthleteId = _cursor.getLong(_cursorIndexOfAthleteId);
            final boolean _tmpAutoStartTimer;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfAutoStartTimer);
            _tmpAutoStartTimer = _tmp != 0;
            final int _tmpDefaultRestTimeSeconds;
            _tmpDefaultRestTimeSeconds = _cursor.getInt(_cursorIndexOfDefaultRestTimeSeconds);
            _result = new AthleteAppSettingsEntity(_tmpId,_tmpCurrentThemeName,_tmpLanguage,_tmpGithubToken,_tmpGithubRepo,_tmpCoachGitHubOwner,_tmpAthleteId,_tmpAutoStartTimer,_tmpDefaultRestTimeSeconds);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
