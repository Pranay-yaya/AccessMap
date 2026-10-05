package com.accessmap.app.database;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
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
public final class HazardDao_Impl implements HazardDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<HazardEntity> __insertionAdapterOfHazardEntity;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<HazardEntity> __deletionAdapterOfHazardEntity;

  private final EntityDeletionOrUpdateAdapter<HazardEntity> __updateAdapterOfHazardEntity;

  private final SharedSQLiteStatement __preparedStmtOfMarkResolved;

  public HazardDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfHazardEntity = new EntityInsertionAdapter<HazardEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `hazards` (`id`,`latitude`,`longitude`,`type`,`description`,`timestamp`,`isResolved`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HazardEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindDouble(2, entity.getLatitude());
        statement.bindDouble(3, entity.getLongitude());
        final String _tmp = __converters.fromHazardType(entity.getType());
        statement.bindString(4, _tmp);
        statement.bindString(5, entity.getDescription());
        statement.bindLong(6, entity.getTimestamp());
        final int _tmp_1 = entity.isResolved() ? 1 : 0;
        statement.bindLong(7, _tmp_1);
      }
    };
    this.__deletionAdapterOfHazardEntity = new EntityDeletionOrUpdateAdapter<HazardEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `hazards` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HazardEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfHazardEntity = new EntityDeletionOrUpdateAdapter<HazardEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `hazards` SET `id` = ?,`latitude` = ?,`longitude` = ?,`type` = ?,`description` = ?,`timestamp` = ?,`isResolved` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HazardEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindDouble(2, entity.getLatitude());
        statement.bindDouble(3, entity.getLongitude());
        final String _tmp = __converters.fromHazardType(entity.getType());
        statement.bindString(4, _tmp);
        statement.bindString(5, entity.getDescription());
        statement.bindLong(6, entity.getTimestamp());
        final int _tmp_1 = entity.isResolved() ? 1 : 0;
        statement.bindLong(7, _tmp_1);
        statement.bindLong(8, entity.getId());
      }
    };
    this.__preparedStmtOfMarkResolved = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE hazards SET isResolved = 1 WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final HazardEntity hazard, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfHazardEntity.insertAndReturnId(hazard);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final HazardEntity hazard, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfHazardEntity.handle(hazard);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final HazardEntity hazard, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfHazardEntity.handle(hazard);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object markResolved(final long hazardId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkResolved.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, hazardId);
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
          __preparedStmtOfMarkResolved.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<HazardEntity>> getActiveHazards() {
    final String _sql = "SELECT * FROM hazards WHERE isResolved = 0 ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"hazards"}, new Callable<List<HazardEntity>>() {
      @Override
      @NonNull
      public List<HazardEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfIsResolved = CursorUtil.getColumnIndexOrThrow(_cursor, "isResolved");
          final List<HazardEntity> _result = new ArrayList<HazardEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HazardEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final HazardType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __converters.toHazardType(_tmp);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final boolean _tmpIsResolved;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsResolved);
            _tmpIsResolved = _tmp_1 != 0;
            _item = new HazardEntity(_tmpId,_tmpLatitude,_tmpLongitude,_tmpType,_tmpDescription,_tmpTimestamp,_tmpIsResolved);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
