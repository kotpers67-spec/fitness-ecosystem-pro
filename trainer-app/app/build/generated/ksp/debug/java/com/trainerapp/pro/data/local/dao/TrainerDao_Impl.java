package com.trainerapp.pro.data.local.dao;

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
import com.trainerapp.pro.data.local.entities.AnthropometryEntity;
import com.trainerapp.pro.data.local.entities.AppSettingsEntity;
import com.trainerapp.pro.data.local.entities.AppointmentEntity;
import com.trainerapp.pro.data.local.entities.ClientEntity;
import com.trainerapp.pro.data.local.entities.ExerciseEntity;
import com.trainerapp.pro.data.local.entities.WorkoutSessionEntity;
import com.trainerapp.pro.data.local.entities.WorkoutSetEntity;
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
public final class TrainerDao_Impl implements TrainerDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ClientEntity> __insertionAdapterOfClientEntity;

  private final EntityInsertionAdapter<ExerciseEntity> __insertionAdapterOfExerciseEntity;

  private final EntityInsertionAdapter<ExerciseEntity> __insertionAdapterOfExerciseEntity_1;

  private final EntityInsertionAdapter<WorkoutSessionEntity> __insertionAdapterOfWorkoutSessionEntity;

  private final EntityInsertionAdapter<WorkoutSetEntity> __insertionAdapterOfWorkoutSetEntity;

  private final EntityInsertionAdapter<AnthropometryEntity> __insertionAdapterOfAnthropometryEntity;

  private final EntityInsertionAdapter<AppSettingsEntity> __insertionAdapterOfAppSettingsEntity;

  private final EntityInsertionAdapter<AppointmentEntity> __insertionAdapterOfAppointmentEntity;

  private final EntityDeletionOrUpdateAdapter<ClientEntity> __deletionAdapterOfClientEntity;

  private final EntityDeletionOrUpdateAdapter<ExerciseEntity> __deletionAdapterOfExerciseEntity;

  private final EntityDeletionOrUpdateAdapter<WorkoutSetEntity> __deletionAdapterOfWorkoutSetEntity;

  private final EntityDeletionOrUpdateAdapter<AppointmentEntity> __deletionAdapterOfAppointmentEntity;

  private final EntityDeletionOrUpdateAdapter<ClientEntity> __updateAdapterOfClientEntity;

  private final EntityDeletionOrUpdateAdapter<ExerciseEntity> __updateAdapterOfExerciseEntity;

  private final EntityDeletionOrUpdateAdapter<WorkoutSessionEntity> __updateAdapterOfWorkoutSessionEntity;

  private final EntityDeletionOrUpdateAdapter<WorkoutSetEntity> __updateAdapterOfWorkoutSetEntity;

  private final EntityDeletionOrUpdateAdapter<AppointmentEntity> __updateAdapterOfAppointmentEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteExerciseFromSession;

  public TrainerDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfClientEntity = new EntityInsertionAdapter<ClientEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `clients` (`id`,`fullName`,`phone`,`goal`,`membershipStatus`,`membershipExpiryDate`,`notes`,`clientUuid`,`pairingCode`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ClientEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getFullName());
        statement.bindString(3, entity.getPhone());
        statement.bindString(4, entity.getGoal());
        statement.bindString(5, entity.getMembershipStatus());
        statement.bindString(6, entity.getMembershipExpiryDate());
        statement.bindString(7, entity.getNotes());
        statement.bindString(8, entity.getClientUuid());
        statement.bindString(9, entity.getPairingCode());
        statement.bindLong(10, entity.getCreatedAt());
      }
    };
    this.__insertionAdapterOfExerciseEntity = new EntityInsertionAdapter<ExerciseEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `exercises` (`id`,`name`,`muscleGroup`,`defaultRestSeconds`,`isCustom`,`description`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ExerciseEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getMuscleGroup());
        statement.bindLong(4, entity.getDefaultRestSeconds());
        final int _tmp = entity.isCustom() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindString(6, entity.getDescription());
      }
    };
    this.__insertionAdapterOfExerciseEntity_1 = new EntityInsertionAdapter<ExerciseEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `exercises` (`id`,`name`,`muscleGroup`,`defaultRestSeconds`,`isCustom`,`description`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ExerciseEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getMuscleGroup());
        statement.bindLong(4, entity.getDefaultRestSeconds());
        final int _tmp = entity.isCustom() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindString(6, entity.getDescription());
      }
    };
    this.__insertionAdapterOfWorkoutSessionEntity = new EntityInsertionAdapter<WorkoutSessionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `workout_sessions` (`id`,`clientId`,`date`,`comments`,`completed`,`isSelfWorkoutAllowed`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WorkoutSessionEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getClientId());
        statement.bindString(3, entity.getDate());
        statement.bindString(4, entity.getComments());
        final int _tmp = entity.getCompleted() ? 1 : 0;
        statement.bindLong(5, _tmp);
        final int _tmp_1 = entity.isSelfWorkoutAllowed() ? 1 : 0;
        statement.bindLong(6, _tmp_1);
      }
    };
    this.__insertionAdapterOfWorkoutSetEntity = new EntityInsertionAdapter<WorkoutSetEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `workout_sets` (`id`,`sessionId`,`exerciseId`,`exerciseOrder`,`setNumber`,`weightKg`,`reps`,`isCompleted`,`rpe`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WorkoutSetEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getSessionId());
        statement.bindLong(3, entity.getExerciseId());
        statement.bindLong(4, entity.getExerciseOrder());
        statement.bindLong(5, entity.getSetNumber());
        statement.bindDouble(6, entity.getWeightKg());
        statement.bindLong(7, entity.getReps());
        final int _tmp = entity.isCompleted() ? 1 : 0;
        statement.bindLong(8, _tmp);
        if (entity.getRpe() == null) {
          statement.bindNull(9);
        } else {
          statement.bindDouble(9, entity.getRpe());
        }
      }
    };
    this.__insertionAdapterOfAnthropometryEntity = new EntityInsertionAdapter<AnthropometryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `anthropometry` (`id`,`clientId`,`date`,`weightKg`,`chestCm`,`waistCm`,`hipsCm`,`bicepsCm`,`thighCm`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AnthropometryEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getClientId());
        statement.bindString(3, entity.getDate());
        statement.bindDouble(4, entity.getWeightKg());
        if (entity.getChestCm() == null) {
          statement.bindNull(5);
        } else {
          statement.bindDouble(5, entity.getChestCm());
        }
        if (entity.getWaistCm() == null) {
          statement.bindNull(6);
        } else {
          statement.bindDouble(6, entity.getWaistCm());
        }
        if (entity.getHipsCm() == null) {
          statement.bindNull(7);
        } else {
          statement.bindDouble(7, entity.getHipsCm());
        }
        if (entity.getBicepsCm() == null) {
          statement.bindNull(8);
        } else {
          statement.bindDouble(8, entity.getBicepsCm());
        }
        if (entity.getThighCm() == null) {
          statement.bindNull(9);
        } else {
          statement.bindDouble(9, entity.getThighCm());
        }
      }
    };
    this.__insertionAdapterOfAppSettingsEntity = new EntityInsertionAdapter<AppSettingsEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `app_settings` (`id`,`currentThemeName`,`currentLayoutStyleName`,`selectedClientId`,`language`,`githubToken`,`githubRepo`,`soundEnabled`,`vibrationEnabled`,`autoStartTimer`,`defaultRestTimeSeconds`,`bleSyncEnabled`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AppSettingsEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getCurrentThemeName());
        statement.bindString(3, entity.getCurrentLayoutStyleName());
        if (entity.getSelectedClientId() == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, entity.getSelectedClientId());
        }
        statement.bindString(5, entity.getLanguage());
        statement.bindString(6, entity.getGithubToken());
        statement.bindString(7, entity.getGithubRepo());
        final int _tmp = entity.getSoundEnabled() ? 1 : 0;
        statement.bindLong(8, _tmp);
        final int _tmp_1 = entity.getVibrationEnabled() ? 1 : 0;
        statement.bindLong(9, _tmp_1);
        final int _tmp_2 = entity.getAutoStartTimer() ? 1 : 0;
        statement.bindLong(10, _tmp_2);
        statement.bindLong(11, entity.getDefaultRestTimeSeconds());
        final int _tmp_3 = entity.getBleSyncEnabled() ? 1 : 0;
        statement.bindLong(12, _tmp_3);
      }
    };
    this.__insertionAdapterOfAppointmentEntity = new EntityInsertionAdapter<AppointmentEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `appointments` (`id`,`clientId`,`dateTime`,`status`,`notes`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AppointmentEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getClientId());
        statement.bindString(3, entity.getDateTime());
        statement.bindString(4, entity.getStatus());
        statement.bindString(5, entity.getNotes());
      }
    };
    this.__deletionAdapterOfClientEntity = new EntityDeletionOrUpdateAdapter<ClientEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `clients` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ClientEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__deletionAdapterOfExerciseEntity = new EntityDeletionOrUpdateAdapter<ExerciseEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `exercises` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ExerciseEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__deletionAdapterOfWorkoutSetEntity = new EntityDeletionOrUpdateAdapter<WorkoutSetEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `workout_sets` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WorkoutSetEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__deletionAdapterOfAppointmentEntity = new EntityDeletionOrUpdateAdapter<AppointmentEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `appointments` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AppointmentEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfClientEntity = new EntityDeletionOrUpdateAdapter<ClientEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `clients` SET `id` = ?,`fullName` = ?,`phone` = ?,`goal` = ?,`membershipStatus` = ?,`membershipExpiryDate` = ?,`notes` = ?,`clientUuid` = ?,`pairingCode` = ?,`createdAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ClientEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getFullName());
        statement.bindString(3, entity.getPhone());
        statement.bindString(4, entity.getGoal());
        statement.bindString(5, entity.getMembershipStatus());
        statement.bindString(6, entity.getMembershipExpiryDate());
        statement.bindString(7, entity.getNotes());
        statement.bindString(8, entity.getClientUuid());
        statement.bindString(9, entity.getPairingCode());
        statement.bindLong(10, entity.getCreatedAt());
        statement.bindLong(11, entity.getId());
      }
    };
    this.__updateAdapterOfExerciseEntity = new EntityDeletionOrUpdateAdapter<ExerciseEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `exercises` SET `id` = ?,`name` = ?,`muscleGroup` = ?,`defaultRestSeconds` = ?,`isCustom` = ?,`description` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ExerciseEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getMuscleGroup());
        statement.bindLong(4, entity.getDefaultRestSeconds());
        final int _tmp = entity.isCustom() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindString(6, entity.getDescription());
        statement.bindLong(7, entity.getId());
      }
    };
    this.__updateAdapterOfWorkoutSessionEntity = new EntityDeletionOrUpdateAdapter<WorkoutSessionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `workout_sessions` SET `id` = ?,`clientId` = ?,`date` = ?,`comments` = ?,`completed` = ?,`isSelfWorkoutAllowed` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WorkoutSessionEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getClientId());
        statement.bindString(3, entity.getDate());
        statement.bindString(4, entity.getComments());
        final int _tmp = entity.getCompleted() ? 1 : 0;
        statement.bindLong(5, _tmp);
        final int _tmp_1 = entity.isSelfWorkoutAllowed() ? 1 : 0;
        statement.bindLong(6, _tmp_1);
        statement.bindLong(7, entity.getId());
      }
    };
    this.__updateAdapterOfWorkoutSetEntity = new EntityDeletionOrUpdateAdapter<WorkoutSetEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `workout_sets` SET `id` = ?,`sessionId` = ?,`exerciseId` = ?,`exerciseOrder` = ?,`setNumber` = ?,`weightKg` = ?,`reps` = ?,`isCompleted` = ?,`rpe` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WorkoutSetEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getSessionId());
        statement.bindLong(3, entity.getExerciseId());
        statement.bindLong(4, entity.getExerciseOrder());
        statement.bindLong(5, entity.getSetNumber());
        statement.bindDouble(6, entity.getWeightKg());
        statement.bindLong(7, entity.getReps());
        final int _tmp = entity.isCompleted() ? 1 : 0;
        statement.bindLong(8, _tmp);
        if (entity.getRpe() == null) {
          statement.bindNull(9);
        } else {
          statement.bindDouble(9, entity.getRpe());
        }
        statement.bindLong(10, entity.getId());
      }
    };
    this.__updateAdapterOfAppointmentEntity = new EntityDeletionOrUpdateAdapter<AppointmentEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `appointments` SET `id` = ?,`clientId` = ?,`dateTime` = ?,`status` = ?,`notes` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AppointmentEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getClientId());
        statement.bindString(3, entity.getDateTime());
        statement.bindString(4, entity.getStatus());
        statement.bindString(5, entity.getNotes());
        statement.bindLong(6, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteExerciseFromSession = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM workout_sets WHERE sessionId = ? AND exerciseId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertClient(final ClientEntity client,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfClientEntity.insertAndReturnId(client);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertExercise(final ExerciseEntity exercise,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfExerciseEntity.insertAndReturnId(exercise);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertExercises(final List<ExerciseEntity> exercises,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfExerciseEntity_1.insert(exercises);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertSession(final WorkoutSessionEntity session,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfWorkoutSessionEntity.insertAndReturnId(session);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertSet(final WorkoutSetEntity workoutSet,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfWorkoutSetEntity.insertAndReturnId(workoutSet);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAnthropometry(final AnthropometryEntity entry,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfAnthropometryEntity.insertAndReturnId(entry);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object saveSettings(final AppSettingsEntity settings,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfAppSettingsEntity.insert(settings);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAppointment(final AppointmentEntity appointment,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfAppointmentEntity.insertAndReturnId(appointment);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteClient(final ClientEntity client,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfClientEntity.handle(client);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteExercise(final ExerciseEntity exercise,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfExerciseEntity.handle(exercise);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteSet(final WorkoutSetEntity workoutSet,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfWorkoutSetEntity.handle(workoutSet);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAppointment(final AppointmentEntity appointment,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfAppointmentEntity.handle(appointment);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateClient(final ClientEntity client,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfClientEntity.handle(client);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateExercise(final ExerciseEntity exercise,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfExerciseEntity.handle(exercise);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateSession(final WorkoutSessionEntity session,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfWorkoutSessionEntity.handle(session);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateSet(final WorkoutSetEntity workoutSet,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfWorkoutSetEntity.handle(workoutSet);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateAppointment(final AppointmentEntity appointment,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfAppointmentEntity.handle(appointment);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteExerciseFromSession(final long sessionId, final long exerciseId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteExerciseFromSession.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, sessionId);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, exerciseId);
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
          __preparedStmtOfDeleteExerciseFromSession.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ClientEntity>> getAllClients() {
    final String _sql = "SELECT * FROM clients ORDER BY fullName ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"clients"}, new Callable<List<ClientEntity>>() {
      @Override
      @NonNull
      public List<ClientEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfFullName = CursorUtil.getColumnIndexOrThrow(_cursor, "fullName");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfGoal = CursorUtil.getColumnIndexOrThrow(_cursor, "goal");
          final int _cursorIndexOfMembershipStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "membershipStatus");
          final int _cursorIndexOfMembershipExpiryDate = CursorUtil.getColumnIndexOrThrow(_cursor, "membershipExpiryDate");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfClientUuid = CursorUtil.getColumnIndexOrThrow(_cursor, "clientUuid");
          final int _cursorIndexOfPairingCode = CursorUtil.getColumnIndexOrThrow(_cursor, "pairingCode");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<ClientEntity> _result = new ArrayList<ClientEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ClientEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpFullName;
            _tmpFullName = _cursor.getString(_cursorIndexOfFullName);
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final String _tmpGoal;
            _tmpGoal = _cursor.getString(_cursorIndexOfGoal);
            final String _tmpMembershipStatus;
            _tmpMembershipStatus = _cursor.getString(_cursorIndexOfMembershipStatus);
            final String _tmpMembershipExpiryDate;
            _tmpMembershipExpiryDate = _cursor.getString(_cursorIndexOfMembershipExpiryDate);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpClientUuid;
            _tmpClientUuid = _cursor.getString(_cursorIndexOfClientUuid);
            final String _tmpPairingCode;
            _tmpPairingCode = _cursor.getString(_cursorIndexOfPairingCode);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new ClientEntity(_tmpId,_tmpFullName,_tmpPhone,_tmpGoal,_tmpMembershipStatus,_tmpMembershipExpiryDate,_tmpNotes,_tmpClientUuid,_tmpPairingCode,_tmpCreatedAt);
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
  public Object getClientById(final long id, final Continuation<? super ClientEntity> $completion) {
    final String _sql = "SELECT * FROM clients WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ClientEntity>() {
      @Override
      @Nullable
      public ClientEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfFullName = CursorUtil.getColumnIndexOrThrow(_cursor, "fullName");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfGoal = CursorUtil.getColumnIndexOrThrow(_cursor, "goal");
          final int _cursorIndexOfMembershipStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "membershipStatus");
          final int _cursorIndexOfMembershipExpiryDate = CursorUtil.getColumnIndexOrThrow(_cursor, "membershipExpiryDate");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfClientUuid = CursorUtil.getColumnIndexOrThrow(_cursor, "clientUuid");
          final int _cursorIndexOfPairingCode = CursorUtil.getColumnIndexOrThrow(_cursor, "pairingCode");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final ClientEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpFullName;
            _tmpFullName = _cursor.getString(_cursorIndexOfFullName);
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final String _tmpGoal;
            _tmpGoal = _cursor.getString(_cursorIndexOfGoal);
            final String _tmpMembershipStatus;
            _tmpMembershipStatus = _cursor.getString(_cursorIndexOfMembershipStatus);
            final String _tmpMembershipExpiryDate;
            _tmpMembershipExpiryDate = _cursor.getString(_cursorIndexOfMembershipExpiryDate);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpClientUuid;
            _tmpClientUuid = _cursor.getString(_cursorIndexOfClientUuid);
            final String _tmpPairingCode;
            _tmpPairingCode = _cursor.getString(_cursorIndexOfPairingCode);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new ClientEntity(_tmpId,_tmpFullName,_tmpPhone,_tmpGoal,_tmpMembershipStatus,_tmpMembershipExpiryDate,_tmpNotes,_tmpClientUuid,_tmpPairingCode,_tmpCreatedAt);
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
  public Object getClientByUuid(final String uuid,
      final Continuation<? super ClientEntity> $completion) {
    final String _sql = "SELECT * FROM clients WHERE clientUuid = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, uuid);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ClientEntity>() {
      @Override
      @Nullable
      public ClientEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfFullName = CursorUtil.getColumnIndexOrThrow(_cursor, "fullName");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfGoal = CursorUtil.getColumnIndexOrThrow(_cursor, "goal");
          final int _cursorIndexOfMembershipStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "membershipStatus");
          final int _cursorIndexOfMembershipExpiryDate = CursorUtil.getColumnIndexOrThrow(_cursor, "membershipExpiryDate");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfClientUuid = CursorUtil.getColumnIndexOrThrow(_cursor, "clientUuid");
          final int _cursorIndexOfPairingCode = CursorUtil.getColumnIndexOrThrow(_cursor, "pairingCode");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final ClientEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpFullName;
            _tmpFullName = _cursor.getString(_cursorIndexOfFullName);
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final String _tmpGoal;
            _tmpGoal = _cursor.getString(_cursorIndexOfGoal);
            final String _tmpMembershipStatus;
            _tmpMembershipStatus = _cursor.getString(_cursorIndexOfMembershipStatus);
            final String _tmpMembershipExpiryDate;
            _tmpMembershipExpiryDate = _cursor.getString(_cursorIndexOfMembershipExpiryDate);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpClientUuid;
            _tmpClientUuid = _cursor.getString(_cursorIndexOfClientUuid);
            final String _tmpPairingCode;
            _tmpPairingCode = _cursor.getString(_cursorIndexOfPairingCode);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new ClientEntity(_tmpId,_tmpFullName,_tmpPhone,_tmpGoal,_tmpMembershipStatus,_tmpMembershipExpiryDate,_tmpNotes,_tmpClientUuid,_tmpPairingCode,_tmpCreatedAt);
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
  public Object getClientByPairingCode(final String code,
      final Continuation<? super ClientEntity> $completion) {
    final String _sql = "SELECT * FROM clients WHERE pairingCode = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, code);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ClientEntity>() {
      @Override
      @Nullable
      public ClientEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfFullName = CursorUtil.getColumnIndexOrThrow(_cursor, "fullName");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfGoal = CursorUtil.getColumnIndexOrThrow(_cursor, "goal");
          final int _cursorIndexOfMembershipStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "membershipStatus");
          final int _cursorIndexOfMembershipExpiryDate = CursorUtil.getColumnIndexOrThrow(_cursor, "membershipExpiryDate");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfClientUuid = CursorUtil.getColumnIndexOrThrow(_cursor, "clientUuid");
          final int _cursorIndexOfPairingCode = CursorUtil.getColumnIndexOrThrow(_cursor, "pairingCode");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final ClientEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpFullName;
            _tmpFullName = _cursor.getString(_cursorIndexOfFullName);
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final String _tmpGoal;
            _tmpGoal = _cursor.getString(_cursorIndexOfGoal);
            final String _tmpMembershipStatus;
            _tmpMembershipStatus = _cursor.getString(_cursorIndexOfMembershipStatus);
            final String _tmpMembershipExpiryDate;
            _tmpMembershipExpiryDate = _cursor.getString(_cursorIndexOfMembershipExpiryDate);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpClientUuid;
            _tmpClientUuid = _cursor.getString(_cursorIndexOfClientUuid);
            final String _tmpPairingCode;
            _tmpPairingCode = _cursor.getString(_cursorIndexOfPairingCode);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new ClientEntity(_tmpId,_tmpFullName,_tmpPhone,_tmpGoal,_tmpMembershipStatus,_tmpMembershipExpiryDate,_tmpNotes,_tmpClientUuid,_tmpPairingCode,_tmpCreatedAt);
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
  public Flow<List<ExerciseEntity>> getAllExercises() {
    final String _sql = "SELECT * FROM exercises ORDER BY muscleGroup ASC, name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"exercises"}, new Callable<List<ExerciseEntity>>() {
      @Override
      @NonNull
      public List<ExerciseEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfMuscleGroup = CursorUtil.getColumnIndexOrThrow(_cursor, "muscleGroup");
          final int _cursorIndexOfDefaultRestSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "defaultRestSeconds");
          final int _cursorIndexOfIsCustom = CursorUtil.getColumnIndexOrThrow(_cursor, "isCustom");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final List<ExerciseEntity> _result = new ArrayList<ExerciseEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ExerciseEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpMuscleGroup;
            _tmpMuscleGroup = _cursor.getString(_cursorIndexOfMuscleGroup);
            final int _tmpDefaultRestSeconds;
            _tmpDefaultRestSeconds = _cursor.getInt(_cursorIndexOfDefaultRestSeconds);
            final boolean _tmpIsCustom;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCustom);
            _tmpIsCustom = _tmp != 0;
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            _item = new ExerciseEntity(_tmpId,_tmpName,_tmpMuscleGroup,_tmpDefaultRestSeconds,_tmpIsCustom,_tmpDescription);
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
  public Object getExerciseById(final long id,
      final Continuation<? super ExerciseEntity> $completion) {
    final String _sql = "SELECT * FROM exercises WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ExerciseEntity>() {
      @Override
      @Nullable
      public ExerciseEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfMuscleGroup = CursorUtil.getColumnIndexOrThrow(_cursor, "muscleGroup");
          final int _cursorIndexOfDefaultRestSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "defaultRestSeconds");
          final int _cursorIndexOfIsCustom = CursorUtil.getColumnIndexOrThrow(_cursor, "isCustom");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final ExerciseEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpMuscleGroup;
            _tmpMuscleGroup = _cursor.getString(_cursorIndexOfMuscleGroup);
            final int _tmpDefaultRestSeconds;
            _tmpDefaultRestSeconds = _cursor.getInt(_cursorIndexOfDefaultRestSeconds);
            final boolean _tmpIsCustom;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCustom);
            _tmpIsCustom = _tmp != 0;
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            _result = new ExerciseEntity(_tmpId,_tmpName,_tmpMuscleGroup,_tmpDefaultRestSeconds,_tmpIsCustom,_tmpDescription);
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
  public Object getSessionByClientAndDate(final long clientId, final String date,
      final Continuation<? super WorkoutSessionEntity> $completion) {
    final String _sql = "SELECT * FROM workout_sessions WHERE clientId = ? AND date = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, clientId);
    _argIndex = 2;
    _statement.bindString(_argIndex, date);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<WorkoutSessionEntity>() {
      @Override
      @Nullable
      public WorkoutSessionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfClientId = CursorUtil.getColumnIndexOrThrow(_cursor, "clientId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfComments = CursorUtil.getColumnIndexOrThrow(_cursor, "comments");
          final int _cursorIndexOfCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "completed");
          final int _cursorIndexOfIsSelfWorkoutAllowed = CursorUtil.getColumnIndexOrThrow(_cursor, "isSelfWorkoutAllowed");
          final WorkoutSessionEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpClientId;
            _tmpClientId = _cursor.getLong(_cursorIndexOfClientId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final String _tmpComments;
            _tmpComments = _cursor.getString(_cursorIndexOfComments);
            final boolean _tmpCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfCompleted);
            _tmpCompleted = _tmp != 0;
            final boolean _tmpIsSelfWorkoutAllowed;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsSelfWorkoutAllowed);
            _tmpIsSelfWorkoutAllowed = _tmp_1 != 0;
            _result = new WorkoutSessionEntity(_tmpId,_tmpClientId,_tmpDate,_tmpComments,_tmpCompleted,_tmpIsSelfWorkoutAllowed);
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
  public Flow<List<WorkoutSessionEntity>> getSessionsForClient(final long clientId) {
    final String _sql = "SELECT * FROM workout_sessions WHERE clientId = ? ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, clientId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"workout_sessions"}, new Callable<List<WorkoutSessionEntity>>() {
      @Override
      @NonNull
      public List<WorkoutSessionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfClientId = CursorUtil.getColumnIndexOrThrow(_cursor, "clientId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfComments = CursorUtil.getColumnIndexOrThrow(_cursor, "comments");
          final int _cursorIndexOfCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "completed");
          final int _cursorIndexOfIsSelfWorkoutAllowed = CursorUtil.getColumnIndexOrThrow(_cursor, "isSelfWorkoutAllowed");
          final List<WorkoutSessionEntity> _result = new ArrayList<WorkoutSessionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WorkoutSessionEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpClientId;
            _tmpClientId = _cursor.getLong(_cursorIndexOfClientId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final String _tmpComments;
            _tmpComments = _cursor.getString(_cursorIndexOfComments);
            final boolean _tmpCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfCompleted);
            _tmpCompleted = _tmp != 0;
            final boolean _tmpIsSelfWorkoutAllowed;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsSelfWorkoutAllowed);
            _tmpIsSelfWorkoutAllowed = _tmp_1 != 0;
            _item = new WorkoutSessionEntity(_tmpId,_tmpClientId,_tmpDate,_tmpComments,_tmpCompleted,_tmpIsSelfWorkoutAllowed);
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
  public Object getLastCompletedSessionBefore(final long clientId, final String currentDate,
      final Continuation<? super WorkoutSessionEntity> $completion) {
    final String _sql = "SELECT * FROM workout_sessions WHERE clientId = ? AND date < ? AND completed = 1 ORDER BY date DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, clientId);
    _argIndex = 2;
    _statement.bindString(_argIndex, currentDate);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<WorkoutSessionEntity>() {
      @Override
      @Nullable
      public WorkoutSessionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfClientId = CursorUtil.getColumnIndexOrThrow(_cursor, "clientId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfComments = CursorUtil.getColumnIndexOrThrow(_cursor, "comments");
          final int _cursorIndexOfCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "completed");
          final int _cursorIndexOfIsSelfWorkoutAllowed = CursorUtil.getColumnIndexOrThrow(_cursor, "isSelfWorkoutAllowed");
          final WorkoutSessionEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpClientId;
            _tmpClientId = _cursor.getLong(_cursorIndexOfClientId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final String _tmpComments;
            _tmpComments = _cursor.getString(_cursorIndexOfComments);
            final boolean _tmpCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfCompleted);
            _tmpCompleted = _tmp != 0;
            final boolean _tmpIsSelfWorkoutAllowed;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsSelfWorkoutAllowed);
            _tmpIsSelfWorkoutAllowed = _tmp_1 != 0;
            _result = new WorkoutSessionEntity(_tmpId,_tmpClientId,_tmpDate,_tmpComments,_tmpCompleted,_tmpIsSelfWorkoutAllowed);
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
  public Flow<List<WorkoutSetEntity>> getSetsForSession(final long sessionId) {
    final String _sql = "SELECT * FROM workout_sets WHERE sessionId = ? ORDER BY exerciseOrder ASC, setNumber ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, sessionId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"workout_sets"}, new Callable<List<WorkoutSetEntity>>() {
      @Override
      @NonNull
      public List<WorkoutSetEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfExerciseId = CursorUtil.getColumnIndexOrThrow(_cursor, "exerciseId");
          final int _cursorIndexOfExerciseOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "exerciseOrder");
          final int _cursorIndexOfSetNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "setNumber");
          final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weightKg");
          final int _cursorIndexOfReps = CursorUtil.getColumnIndexOrThrow(_cursor, "reps");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
          final int _cursorIndexOfRpe = CursorUtil.getColumnIndexOrThrow(_cursor, "rpe");
          final List<WorkoutSetEntity> _result = new ArrayList<WorkoutSetEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WorkoutSetEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpSessionId;
            _tmpSessionId = _cursor.getLong(_cursorIndexOfSessionId);
            final long _tmpExerciseId;
            _tmpExerciseId = _cursor.getLong(_cursorIndexOfExerciseId);
            final int _tmpExerciseOrder;
            _tmpExerciseOrder = _cursor.getInt(_cursorIndexOfExerciseOrder);
            final int _tmpSetNumber;
            _tmpSetNumber = _cursor.getInt(_cursorIndexOfSetNumber);
            final double _tmpWeightKg;
            _tmpWeightKg = _cursor.getDouble(_cursorIndexOfWeightKg);
            final int _tmpReps;
            _tmpReps = _cursor.getInt(_cursorIndexOfReps);
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
            _item = new WorkoutSetEntity(_tmpId,_tmpSessionId,_tmpExerciseId,_tmpExerciseOrder,_tmpSetNumber,_tmpWeightKg,_tmpReps,_tmpIsCompleted,_tmpRpe);
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
      final Continuation<? super List<WorkoutSetEntity>> $completion) {
    final String _sql = "SELECT * FROM workout_sets WHERE sessionId = ? ORDER BY exerciseOrder ASC, setNumber ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, sessionId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<WorkoutSetEntity>>() {
      @Override
      @NonNull
      public List<WorkoutSetEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfExerciseId = CursorUtil.getColumnIndexOrThrow(_cursor, "exerciseId");
          final int _cursorIndexOfExerciseOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "exerciseOrder");
          final int _cursorIndexOfSetNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "setNumber");
          final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weightKg");
          final int _cursorIndexOfReps = CursorUtil.getColumnIndexOrThrow(_cursor, "reps");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
          final int _cursorIndexOfRpe = CursorUtil.getColumnIndexOrThrow(_cursor, "rpe");
          final List<WorkoutSetEntity> _result = new ArrayList<WorkoutSetEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WorkoutSetEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpSessionId;
            _tmpSessionId = _cursor.getLong(_cursorIndexOfSessionId);
            final long _tmpExerciseId;
            _tmpExerciseId = _cursor.getLong(_cursorIndexOfExerciseId);
            final int _tmpExerciseOrder;
            _tmpExerciseOrder = _cursor.getInt(_cursorIndexOfExerciseOrder);
            final int _tmpSetNumber;
            _tmpSetNumber = _cursor.getInt(_cursorIndexOfSetNumber);
            final double _tmpWeightKg;
            _tmpWeightKg = _cursor.getDouble(_cursorIndexOfWeightKg);
            final int _tmpReps;
            _tmpReps = _cursor.getInt(_cursorIndexOfReps);
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
            _item = new WorkoutSetEntity(_tmpId,_tmpSessionId,_tmpExerciseId,_tmpExerciseOrder,_tmpSetNumber,_tmpWeightKg,_tmpReps,_tmpIsCompleted,_tmpRpe);
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
  public Object getLastExerciseSet(final long clientId, final long exerciseId,
      final String currentDate, final Continuation<? super WorkoutSetEntity> $completion) {
    final String _sql = "\n"
            + "        SELECT ws.* FROM workout_sets ws\n"
            + "        INNER JOIN workout_sessions s ON ws.sessionId = s.id\n"
            + "        WHERE s.clientId = ? AND ws.exerciseId = ? AND s.date < ? AND ws.isCompleted = 1\n"
            + "        ORDER BY s.date DESC, ws.setNumber DESC LIMIT 1\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, clientId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, exerciseId);
    _argIndex = 3;
    _statement.bindString(_argIndex, currentDate);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<WorkoutSetEntity>() {
      @Override
      @Nullable
      public WorkoutSetEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfExerciseId = CursorUtil.getColumnIndexOrThrow(_cursor, "exerciseId");
          final int _cursorIndexOfExerciseOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "exerciseOrder");
          final int _cursorIndexOfSetNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "setNumber");
          final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weightKg");
          final int _cursorIndexOfReps = CursorUtil.getColumnIndexOrThrow(_cursor, "reps");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
          final int _cursorIndexOfRpe = CursorUtil.getColumnIndexOrThrow(_cursor, "rpe");
          final WorkoutSetEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpSessionId;
            _tmpSessionId = _cursor.getLong(_cursorIndexOfSessionId);
            final long _tmpExerciseId;
            _tmpExerciseId = _cursor.getLong(_cursorIndexOfExerciseId);
            final int _tmpExerciseOrder;
            _tmpExerciseOrder = _cursor.getInt(_cursorIndexOfExerciseOrder);
            final int _tmpSetNumber;
            _tmpSetNumber = _cursor.getInt(_cursorIndexOfSetNumber);
            final double _tmpWeightKg;
            _tmpWeightKg = _cursor.getDouble(_cursorIndexOfWeightKg);
            final int _tmpReps;
            _tmpReps = _cursor.getInt(_cursorIndexOfReps);
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
            _result = new WorkoutSetEntity(_tmpId,_tmpSessionId,_tmpExerciseId,_tmpExerciseOrder,_tmpSetNumber,_tmpWeightKg,_tmpReps,_tmpIsCompleted,_tmpRpe);
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
  public Flow<List<AnthropometryEntity>> getAnthropometryForClient(final long clientId) {
    final String _sql = "SELECT * FROM anthropometry WHERE clientId = ? ORDER BY date ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, clientId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"anthropometry"}, new Callable<List<AnthropometryEntity>>() {
      @Override
      @NonNull
      public List<AnthropometryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfClientId = CursorUtil.getColumnIndexOrThrow(_cursor, "clientId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weightKg");
          final int _cursorIndexOfChestCm = CursorUtil.getColumnIndexOrThrow(_cursor, "chestCm");
          final int _cursorIndexOfWaistCm = CursorUtil.getColumnIndexOrThrow(_cursor, "waistCm");
          final int _cursorIndexOfHipsCm = CursorUtil.getColumnIndexOrThrow(_cursor, "hipsCm");
          final int _cursorIndexOfBicepsCm = CursorUtil.getColumnIndexOrThrow(_cursor, "bicepsCm");
          final int _cursorIndexOfThighCm = CursorUtil.getColumnIndexOrThrow(_cursor, "thighCm");
          final List<AnthropometryEntity> _result = new ArrayList<AnthropometryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AnthropometryEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpClientId;
            _tmpClientId = _cursor.getLong(_cursorIndexOfClientId);
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
            final Double _tmpThighCm;
            if (_cursor.isNull(_cursorIndexOfThighCm)) {
              _tmpThighCm = null;
            } else {
              _tmpThighCm = _cursor.getDouble(_cursorIndexOfThighCm);
            }
            _item = new AnthropometryEntity(_tmpId,_tmpClientId,_tmpDate,_tmpWeightKg,_tmpChestCm,_tmpWaistCm,_tmpHipsCm,_tmpBicepsCm,_tmpThighCm);
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
  public Flow<List<SetHistoryItem>> getExerciseHistory(final long clientId, final long exerciseId) {
    final String _sql = "\n"
            + "        SELECT s.date, ws.weightKg, ws.reps, ws.isCompleted\n"
            + "        FROM workout_sets ws\n"
            + "        INNER JOIN workout_sessions s ON ws.sessionId = s.id\n"
            + "        WHERE s.clientId = ? AND ws.exerciseId = ?\n"
            + "        ORDER BY s.date ASC, ws.setNumber ASC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, clientId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, exerciseId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"workout_sets",
        "workout_sessions"}, new Callable<List<SetHistoryItem>>() {
      @Override
      @NonNull
      public List<SetHistoryItem> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = 0;
          final int _cursorIndexOfWeightKg = 1;
          final int _cursorIndexOfReps = 2;
          final int _cursorIndexOfIsCompleted = 3;
          final List<SetHistoryItem> _result = new ArrayList<SetHistoryItem>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SetHistoryItem _item;
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
            _item = new SetHistoryItem(_tmpDate,_tmpWeightKg,_tmpReps,_tmpIsCompleted);
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
  public Flow<AppSettingsEntity> getSettings() {
    final String _sql = "SELECT * FROM app_settings WHERE id = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"app_settings"}, new Callable<AppSettingsEntity>() {
      @Override
      @Nullable
      public AppSettingsEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCurrentThemeName = CursorUtil.getColumnIndexOrThrow(_cursor, "currentThemeName");
          final int _cursorIndexOfCurrentLayoutStyleName = CursorUtil.getColumnIndexOrThrow(_cursor, "currentLayoutStyleName");
          final int _cursorIndexOfSelectedClientId = CursorUtil.getColumnIndexOrThrow(_cursor, "selectedClientId");
          final int _cursorIndexOfLanguage = CursorUtil.getColumnIndexOrThrow(_cursor, "language");
          final int _cursorIndexOfGithubToken = CursorUtil.getColumnIndexOrThrow(_cursor, "githubToken");
          final int _cursorIndexOfGithubRepo = CursorUtil.getColumnIndexOrThrow(_cursor, "githubRepo");
          final int _cursorIndexOfSoundEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "soundEnabled");
          final int _cursorIndexOfVibrationEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "vibrationEnabled");
          final int _cursorIndexOfAutoStartTimer = CursorUtil.getColumnIndexOrThrow(_cursor, "autoStartTimer");
          final int _cursorIndexOfDefaultRestTimeSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "defaultRestTimeSeconds");
          final int _cursorIndexOfBleSyncEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "bleSyncEnabled");
          final AppSettingsEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpCurrentThemeName;
            _tmpCurrentThemeName = _cursor.getString(_cursorIndexOfCurrentThemeName);
            final String _tmpCurrentLayoutStyleName;
            _tmpCurrentLayoutStyleName = _cursor.getString(_cursorIndexOfCurrentLayoutStyleName);
            final Long _tmpSelectedClientId;
            if (_cursor.isNull(_cursorIndexOfSelectedClientId)) {
              _tmpSelectedClientId = null;
            } else {
              _tmpSelectedClientId = _cursor.getLong(_cursorIndexOfSelectedClientId);
            }
            final String _tmpLanguage;
            _tmpLanguage = _cursor.getString(_cursorIndexOfLanguage);
            final String _tmpGithubToken;
            _tmpGithubToken = _cursor.getString(_cursorIndexOfGithubToken);
            final String _tmpGithubRepo;
            _tmpGithubRepo = _cursor.getString(_cursorIndexOfGithubRepo);
            final boolean _tmpSoundEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSoundEnabled);
            _tmpSoundEnabled = _tmp != 0;
            final boolean _tmpVibrationEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfVibrationEnabled);
            _tmpVibrationEnabled = _tmp_1 != 0;
            final boolean _tmpAutoStartTimer;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfAutoStartTimer);
            _tmpAutoStartTimer = _tmp_2 != 0;
            final int _tmpDefaultRestTimeSeconds;
            _tmpDefaultRestTimeSeconds = _cursor.getInt(_cursorIndexOfDefaultRestTimeSeconds);
            final boolean _tmpBleSyncEnabled;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfBleSyncEnabled);
            _tmpBleSyncEnabled = _tmp_3 != 0;
            _result = new AppSettingsEntity(_tmpId,_tmpCurrentThemeName,_tmpCurrentLayoutStyleName,_tmpSelectedClientId,_tmpLanguage,_tmpGithubToken,_tmpGithubRepo,_tmpSoundEnabled,_tmpVibrationEnabled,_tmpAutoStartTimer,_tmpDefaultRestTimeSeconds,_tmpBleSyncEnabled);
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
  public Object getAllClientsSync(final Continuation<? super List<ClientEntity>> $completion) {
    final String _sql = "SELECT * FROM clients";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ClientEntity>>() {
      @Override
      @NonNull
      public List<ClientEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfFullName = CursorUtil.getColumnIndexOrThrow(_cursor, "fullName");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfGoal = CursorUtil.getColumnIndexOrThrow(_cursor, "goal");
          final int _cursorIndexOfMembershipStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "membershipStatus");
          final int _cursorIndexOfMembershipExpiryDate = CursorUtil.getColumnIndexOrThrow(_cursor, "membershipExpiryDate");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfClientUuid = CursorUtil.getColumnIndexOrThrow(_cursor, "clientUuid");
          final int _cursorIndexOfPairingCode = CursorUtil.getColumnIndexOrThrow(_cursor, "pairingCode");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<ClientEntity> _result = new ArrayList<ClientEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ClientEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpFullName;
            _tmpFullName = _cursor.getString(_cursorIndexOfFullName);
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final String _tmpGoal;
            _tmpGoal = _cursor.getString(_cursorIndexOfGoal);
            final String _tmpMembershipStatus;
            _tmpMembershipStatus = _cursor.getString(_cursorIndexOfMembershipStatus);
            final String _tmpMembershipExpiryDate;
            _tmpMembershipExpiryDate = _cursor.getString(_cursorIndexOfMembershipExpiryDate);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpClientUuid;
            _tmpClientUuid = _cursor.getString(_cursorIndexOfClientUuid);
            final String _tmpPairingCode;
            _tmpPairingCode = _cursor.getString(_cursorIndexOfPairingCode);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new ClientEntity(_tmpId,_tmpFullName,_tmpPhone,_tmpGoal,_tmpMembershipStatus,_tmpMembershipExpiryDate,_tmpNotes,_tmpClientUuid,_tmpPairingCode,_tmpCreatedAt);
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
  public Object getAllExercisesSync(final Continuation<? super List<ExerciseEntity>> $completion) {
    final String _sql = "SELECT * FROM exercises";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ExerciseEntity>>() {
      @Override
      @NonNull
      public List<ExerciseEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfMuscleGroup = CursorUtil.getColumnIndexOrThrow(_cursor, "muscleGroup");
          final int _cursorIndexOfDefaultRestSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "defaultRestSeconds");
          final int _cursorIndexOfIsCustom = CursorUtil.getColumnIndexOrThrow(_cursor, "isCustom");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final List<ExerciseEntity> _result = new ArrayList<ExerciseEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ExerciseEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpMuscleGroup;
            _tmpMuscleGroup = _cursor.getString(_cursorIndexOfMuscleGroup);
            final int _tmpDefaultRestSeconds;
            _tmpDefaultRestSeconds = _cursor.getInt(_cursorIndexOfDefaultRestSeconds);
            final boolean _tmpIsCustom;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCustom);
            _tmpIsCustom = _tmp != 0;
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            _item = new ExerciseEntity(_tmpId,_tmpName,_tmpMuscleGroup,_tmpDefaultRestSeconds,_tmpIsCustom,_tmpDescription);
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
      final Continuation<? super List<WorkoutSessionEntity>> $completion) {
    final String _sql = "SELECT * FROM workout_sessions";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<WorkoutSessionEntity>>() {
      @Override
      @NonNull
      public List<WorkoutSessionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfClientId = CursorUtil.getColumnIndexOrThrow(_cursor, "clientId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfComments = CursorUtil.getColumnIndexOrThrow(_cursor, "comments");
          final int _cursorIndexOfCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "completed");
          final int _cursorIndexOfIsSelfWorkoutAllowed = CursorUtil.getColumnIndexOrThrow(_cursor, "isSelfWorkoutAllowed");
          final List<WorkoutSessionEntity> _result = new ArrayList<WorkoutSessionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WorkoutSessionEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpClientId;
            _tmpClientId = _cursor.getLong(_cursorIndexOfClientId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final String _tmpComments;
            _tmpComments = _cursor.getString(_cursorIndexOfComments);
            final boolean _tmpCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfCompleted);
            _tmpCompleted = _tmp != 0;
            final boolean _tmpIsSelfWorkoutAllowed;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsSelfWorkoutAllowed);
            _tmpIsSelfWorkoutAllowed = _tmp_1 != 0;
            _item = new WorkoutSessionEntity(_tmpId,_tmpClientId,_tmpDate,_tmpComments,_tmpCompleted,_tmpIsSelfWorkoutAllowed);
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
  public Object getAllSetsSync(final Continuation<? super List<WorkoutSetEntity>> $completion) {
    final String _sql = "SELECT * FROM workout_sets";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<WorkoutSetEntity>>() {
      @Override
      @NonNull
      public List<WorkoutSetEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfExerciseId = CursorUtil.getColumnIndexOrThrow(_cursor, "exerciseId");
          final int _cursorIndexOfExerciseOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "exerciseOrder");
          final int _cursorIndexOfSetNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "setNumber");
          final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weightKg");
          final int _cursorIndexOfReps = CursorUtil.getColumnIndexOrThrow(_cursor, "reps");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
          final int _cursorIndexOfRpe = CursorUtil.getColumnIndexOrThrow(_cursor, "rpe");
          final List<WorkoutSetEntity> _result = new ArrayList<WorkoutSetEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WorkoutSetEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpSessionId;
            _tmpSessionId = _cursor.getLong(_cursorIndexOfSessionId);
            final long _tmpExerciseId;
            _tmpExerciseId = _cursor.getLong(_cursorIndexOfExerciseId);
            final int _tmpExerciseOrder;
            _tmpExerciseOrder = _cursor.getInt(_cursorIndexOfExerciseOrder);
            final int _tmpSetNumber;
            _tmpSetNumber = _cursor.getInt(_cursorIndexOfSetNumber);
            final double _tmpWeightKg;
            _tmpWeightKg = _cursor.getDouble(_cursorIndexOfWeightKg);
            final int _tmpReps;
            _tmpReps = _cursor.getInt(_cursorIndexOfReps);
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
            _item = new WorkoutSetEntity(_tmpId,_tmpSessionId,_tmpExerciseId,_tmpExerciseOrder,_tmpSetNumber,_tmpWeightKg,_tmpReps,_tmpIsCompleted,_tmpRpe);
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
      final Continuation<? super List<AnthropometryEntity>> $completion) {
    final String _sql = "SELECT * FROM anthropometry";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<AnthropometryEntity>>() {
      @Override
      @NonNull
      public List<AnthropometryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfClientId = CursorUtil.getColumnIndexOrThrow(_cursor, "clientId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weightKg");
          final int _cursorIndexOfChestCm = CursorUtil.getColumnIndexOrThrow(_cursor, "chestCm");
          final int _cursorIndexOfWaistCm = CursorUtil.getColumnIndexOrThrow(_cursor, "waistCm");
          final int _cursorIndexOfHipsCm = CursorUtil.getColumnIndexOrThrow(_cursor, "hipsCm");
          final int _cursorIndexOfBicepsCm = CursorUtil.getColumnIndexOrThrow(_cursor, "bicepsCm");
          final int _cursorIndexOfThighCm = CursorUtil.getColumnIndexOrThrow(_cursor, "thighCm");
          final List<AnthropometryEntity> _result = new ArrayList<AnthropometryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AnthropometryEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpClientId;
            _tmpClientId = _cursor.getLong(_cursorIndexOfClientId);
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
            final Double _tmpThighCm;
            if (_cursor.isNull(_cursorIndexOfThighCm)) {
              _tmpThighCm = null;
            } else {
              _tmpThighCm = _cursor.getDouble(_cursorIndexOfThighCm);
            }
            _item = new AnthropometryEntity(_tmpId,_tmpClientId,_tmpDate,_tmpWeightKg,_tmpChestCm,_tmpWaistCm,_tmpHipsCm,_tmpBicepsCm,_tmpThighCm);
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
  public Flow<List<AppointmentEntity>> getAppointmentsForClient(final long clientId) {
    final String _sql = "SELECT * FROM appointments WHERE clientId = ? ORDER BY dateTime ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, clientId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"appointments"}, new Callable<List<AppointmentEntity>>() {
      @Override
      @NonNull
      public List<AppointmentEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfClientId = CursorUtil.getColumnIndexOrThrow(_cursor, "clientId");
          final int _cursorIndexOfDateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "dateTime");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final List<AppointmentEntity> _result = new ArrayList<AppointmentEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AppointmentEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpClientId;
            _tmpClientId = _cursor.getLong(_cursorIndexOfClientId);
            final String _tmpDateTime;
            _tmpDateTime = _cursor.getString(_cursorIndexOfDateTime);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            _item = new AppointmentEntity(_tmpId,_tmpClientId,_tmpDateTime,_tmpStatus,_tmpNotes);
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
  public Object getAppointmentById(final long id,
      final Continuation<? super AppointmentEntity> $completion) {
    final String _sql = "SELECT * FROM appointments WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<AppointmentEntity>() {
      @Override
      @Nullable
      public AppointmentEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfClientId = CursorUtil.getColumnIndexOrThrow(_cursor, "clientId");
          final int _cursorIndexOfDateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "dateTime");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final AppointmentEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpClientId;
            _tmpClientId = _cursor.getLong(_cursorIndexOfClientId);
            final String _tmpDateTime;
            _tmpDateTime = _cursor.getString(_cursorIndexOfDateTime);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            _result = new AppointmentEntity(_tmpId,_tmpClientId,_tmpDateTime,_tmpStatus,_tmpNotes);
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
  public Object getAllAppointmentsSync(
      final Continuation<? super List<AppointmentEntity>> $completion) {
    final String _sql = "SELECT * FROM appointments";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<AppointmentEntity>>() {
      @Override
      @NonNull
      public List<AppointmentEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfClientId = CursorUtil.getColumnIndexOrThrow(_cursor, "clientId");
          final int _cursorIndexOfDateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "dateTime");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final List<AppointmentEntity> _result = new ArrayList<AppointmentEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AppointmentEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpClientId;
            _tmpClientId = _cursor.getLong(_cursorIndexOfClientId);
            final String _tmpDateTime;
            _tmpDateTime = _cursor.getString(_cursorIndexOfDateTime);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            _item = new AppointmentEntity(_tmpId,_tmpClientId,_tmpDateTime,_tmpStatus,_tmpNotes);
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
  public Object getSessionsForClientSync(final long clientId,
      final Continuation<? super List<WorkoutSessionEntity>> $completion) {
    final String _sql = "SELECT * FROM workout_sessions WHERE clientId = ? ORDER BY date ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, clientId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<WorkoutSessionEntity>>() {
      @Override
      @NonNull
      public List<WorkoutSessionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfClientId = CursorUtil.getColumnIndexOrThrow(_cursor, "clientId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfComments = CursorUtil.getColumnIndexOrThrow(_cursor, "comments");
          final int _cursorIndexOfCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "completed");
          final int _cursorIndexOfIsSelfWorkoutAllowed = CursorUtil.getColumnIndexOrThrow(_cursor, "isSelfWorkoutAllowed");
          final List<WorkoutSessionEntity> _result = new ArrayList<WorkoutSessionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WorkoutSessionEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpClientId;
            _tmpClientId = _cursor.getLong(_cursorIndexOfClientId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final String _tmpComments;
            _tmpComments = _cursor.getString(_cursorIndexOfComments);
            final boolean _tmpCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfCompleted);
            _tmpCompleted = _tmp != 0;
            final boolean _tmpIsSelfWorkoutAllowed;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsSelfWorkoutAllowed);
            _tmpIsSelfWorkoutAllowed = _tmp_1 != 0;
            _item = new WorkoutSessionEntity(_tmpId,_tmpClientId,_tmpDate,_tmpComments,_tmpCompleted,_tmpIsSelfWorkoutAllowed);
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
  public Object getAnthropometryForClientSync(final long clientId,
      final Continuation<? super List<AnthropometryEntity>> $completion) {
    final String _sql = "SELECT * FROM anthropometry WHERE clientId = ? ORDER BY date ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, clientId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<AnthropometryEntity>>() {
      @Override
      @NonNull
      public List<AnthropometryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfClientId = CursorUtil.getColumnIndexOrThrow(_cursor, "clientId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weightKg");
          final int _cursorIndexOfChestCm = CursorUtil.getColumnIndexOrThrow(_cursor, "chestCm");
          final int _cursorIndexOfWaistCm = CursorUtil.getColumnIndexOrThrow(_cursor, "waistCm");
          final int _cursorIndexOfHipsCm = CursorUtil.getColumnIndexOrThrow(_cursor, "hipsCm");
          final int _cursorIndexOfBicepsCm = CursorUtil.getColumnIndexOrThrow(_cursor, "bicepsCm");
          final int _cursorIndexOfThighCm = CursorUtil.getColumnIndexOrThrow(_cursor, "thighCm");
          final List<AnthropometryEntity> _result = new ArrayList<AnthropometryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AnthropometryEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpClientId;
            _tmpClientId = _cursor.getLong(_cursorIndexOfClientId);
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
            final Double _tmpThighCm;
            if (_cursor.isNull(_cursorIndexOfThighCm)) {
              _tmpThighCm = null;
            } else {
              _tmpThighCm = _cursor.getDouble(_cursorIndexOfThighCm);
            }
            _item = new AnthropometryEntity(_tmpId,_tmpClientId,_tmpDate,_tmpWeightKg,_tmpChestCm,_tmpWaistCm,_tmpHipsCm,_tmpBicepsCm,_tmpThighCm);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
