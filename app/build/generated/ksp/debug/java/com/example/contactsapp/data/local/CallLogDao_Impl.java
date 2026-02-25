package com.example.contactsapp.data.local;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.example.contactsapp.data.model.CallLog;
import com.example.contactsapp.data.model.CallType;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
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
public final class CallLogDao_Impl implements CallLogDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CallLog> __insertionAdapterOfCallLog;

  private final Converters __converters = new Converters();

  private final SharedSQLiteStatement __preparedStmtOfDeleteCallLog;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllCallLogs;

  public CallLogDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCallLog = new EntityInsertionAdapter<CallLog>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `call_logs` (`id`,`contactName`,`phoneNumber`,`callType`,`timestamp`,`durationSeconds`,`profileImageUri`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CallLog entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getContactName());
        statement.bindString(3, entity.getPhoneNumber());
        final String _tmp = __converters.fromCallType(entity.getCallType());
        statement.bindString(4, _tmp);
        statement.bindLong(5, entity.getTimestamp());
        statement.bindLong(6, entity.getDurationSeconds());
        if (entity.getProfileImageUri() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getProfileImageUri());
        }
      }
    };
    this.__preparedStmtOfDeleteCallLog = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM call_logs WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllCallLogs = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM call_logs";
        return _query;
      }
    };
  }

  @Override
  public Object insertCallLog(final CallLog callLog, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCallLog.insert(callLog);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteCallLog(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteCallLog.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
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
          __preparedStmtOfDeleteCallLog.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllCallLogs(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllCallLogs.acquire();
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
          __preparedStmtOfDeleteAllCallLogs.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CallLog>> getAllCallLogs() {
    final String _sql = "SELECT * FROM call_logs ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"call_logs"}, new Callable<List<CallLog>>() {
      @Override
      @NonNull
      public List<CallLog> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfContactName = CursorUtil.getColumnIndexOrThrow(_cursor, "contactName");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfCallType = CursorUtil.getColumnIndexOrThrow(_cursor, "callType");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSeconds");
          final int _cursorIndexOfProfileImageUri = CursorUtil.getColumnIndexOrThrow(_cursor, "profileImageUri");
          final List<CallLog> _result = new ArrayList<CallLog>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CallLog _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpContactName;
            _tmpContactName = _cursor.getString(_cursorIndexOfContactName);
            final String _tmpPhoneNumber;
            _tmpPhoneNumber = _cursor.getString(_cursorIndexOfPhoneNumber);
            final CallType _tmpCallType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfCallType);
            _tmpCallType = __converters.toCallType(_tmp);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final int _tmpDurationSeconds;
            _tmpDurationSeconds = _cursor.getInt(_cursorIndexOfDurationSeconds);
            final String _tmpProfileImageUri;
            if (_cursor.isNull(_cursorIndexOfProfileImageUri)) {
              _tmpProfileImageUri = null;
            } else {
              _tmpProfileImageUri = _cursor.getString(_cursorIndexOfProfileImageUri);
            }
            _item = new CallLog(_tmpId,_tmpContactName,_tmpPhoneNumber,_tmpCallType,_tmpTimestamp,_tmpDurationSeconds,_tmpProfileImageUri);
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
  public Flow<List<CallLog>> getMissedCalls() {
    final String _sql = "SELECT * FROM call_logs WHERE callType = 'MISSED' ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"call_logs"}, new Callable<List<CallLog>>() {
      @Override
      @NonNull
      public List<CallLog> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfContactName = CursorUtil.getColumnIndexOrThrow(_cursor, "contactName");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfCallType = CursorUtil.getColumnIndexOrThrow(_cursor, "callType");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSeconds");
          final int _cursorIndexOfProfileImageUri = CursorUtil.getColumnIndexOrThrow(_cursor, "profileImageUri");
          final List<CallLog> _result = new ArrayList<CallLog>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CallLog _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpContactName;
            _tmpContactName = _cursor.getString(_cursorIndexOfContactName);
            final String _tmpPhoneNumber;
            _tmpPhoneNumber = _cursor.getString(_cursorIndexOfPhoneNumber);
            final CallType _tmpCallType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfCallType);
            _tmpCallType = __converters.toCallType(_tmp);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final int _tmpDurationSeconds;
            _tmpDurationSeconds = _cursor.getInt(_cursorIndexOfDurationSeconds);
            final String _tmpProfileImageUri;
            if (_cursor.isNull(_cursorIndexOfProfileImageUri)) {
              _tmpProfileImageUri = null;
            } else {
              _tmpProfileImageUri = _cursor.getString(_cursorIndexOfProfileImageUri);
            }
            _item = new CallLog(_tmpId,_tmpContactName,_tmpPhoneNumber,_tmpCallType,_tmpTimestamp,_tmpDurationSeconds,_tmpProfileImageUri);
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
  public Flow<List<CallLog>> getCallLogsForNumber(final String number) {
    final String _sql = "SELECT * FROM call_logs WHERE phoneNumber = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, number);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"call_logs"}, new Callable<List<CallLog>>() {
      @Override
      @NonNull
      public List<CallLog> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfContactName = CursorUtil.getColumnIndexOrThrow(_cursor, "contactName");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfCallType = CursorUtil.getColumnIndexOrThrow(_cursor, "callType");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSeconds");
          final int _cursorIndexOfProfileImageUri = CursorUtil.getColumnIndexOrThrow(_cursor, "profileImageUri");
          final List<CallLog> _result = new ArrayList<CallLog>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CallLog _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpContactName;
            _tmpContactName = _cursor.getString(_cursorIndexOfContactName);
            final String _tmpPhoneNumber;
            _tmpPhoneNumber = _cursor.getString(_cursorIndexOfPhoneNumber);
            final CallType _tmpCallType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfCallType);
            _tmpCallType = __converters.toCallType(_tmp);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final int _tmpDurationSeconds;
            _tmpDurationSeconds = _cursor.getInt(_cursorIndexOfDurationSeconds);
            final String _tmpProfileImageUri;
            if (_cursor.isNull(_cursorIndexOfProfileImageUri)) {
              _tmpProfileImageUri = null;
            } else {
              _tmpProfileImageUri = _cursor.getString(_cursorIndexOfProfileImageUri);
            }
            _item = new CallLog(_tmpId,_tmpContactName,_tmpPhoneNumber,_tmpCallType,_tmpTimestamp,_tmpDurationSeconds,_tmpProfileImageUri);
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
  public Flow<Integer> getMissedCallCount() {
    final String _sql = "SELECT COUNT(*) FROM call_logs WHERE callType = 'MISSED'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"call_logs"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
