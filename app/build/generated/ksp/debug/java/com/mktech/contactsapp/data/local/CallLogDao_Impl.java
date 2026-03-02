package com.mktech.contactsapp.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.mktech.contactsapp.data.model.CallLog;
import com.mktech.contactsapp.data.model.CallType;
import com.mktech.contactsapp.data.model.Contact;
import com.mktech.contactsapp.data.model.ResolvedCallLog;
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

  private final SharedSQLiteStatement __preparedStmtOfUpdateContactNameByPhone;

  public CallLogDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCallLog = new EntityInsertionAdapter<CallLog>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `call_logs` (`id`,`contactName`,`phoneNumber`,`callType`,`timestamp`,`durationSeconds`,`profileImageUri`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
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
    this.__preparedStmtOfUpdateContactNameByPhone = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE call_logs SET contactName = ? WHERE phoneNumber = ?";
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
  public Object updateContactNameByPhone(final String phone, final String newName,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateContactNameByPhone.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, newName);
        _argIndex = 2;
        _stmt.bindString(_argIndex, phone);
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
          __preparedStmtOfUpdateContactNameByPhone.release(_stmt);
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
  public Object getCallLogById(final long id, final Continuation<? super CallLog> $completion) {
    final String _sql = "SELECT * FROM call_logs WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CallLog>() {
      @Override
      @Nullable
      public CallLog call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfContactName = CursorUtil.getColumnIndexOrThrow(_cursor, "contactName");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfCallType = CursorUtil.getColumnIndexOrThrow(_cursor, "callType");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSeconds");
          final int _cursorIndexOfProfileImageUri = CursorUtil.getColumnIndexOrThrow(_cursor, "profileImageUri");
          final CallLog _result;
          if (_cursor.moveToFirst()) {
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
            _result = new CallLog(_tmpId,_tmpContactName,_tmpPhoneNumber,_tmpCallType,_tmpTimestamp,_tmpDurationSeconds,_tmpProfileImageUri);
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

  @Override
  public Object getContactByDeviceId(final String deviceId,
      final Continuation<? super Contact> $completion) {
    final String _sql = "SELECT * FROM contacts WHERE deviceContactId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, deviceId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Contact>() {
      @Override
      @Nullable
      public Contact call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
          final int _cursorIndexOfProfileImageUri = CursorUtil.getColumnIndexOrThrow(_cursor, "profileImageUri");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfDeviceContactId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceContactId");
          final int _cursorIndexOfLastUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "last_updated_at");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final Contact _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPhoneNumber;
            _tmpPhoneNumber = _cursor.getString(_cursorIndexOfPhoneNumber);
            final String _tmpEmail;
            _tmpEmail = _cursor.getString(_cursorIndexOfEmail);
            final String _tmpProfileImageUri;
            if (_cursor.isNull(_cursorIndexOfProfileImageUri)) {
              _tmpProfileImageUri = null;
            } else {
              _tmpProfileImageUri = _cursor.getString(_cursorIndexOfProfileImageUri);
            }
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final String _tmpDeviceContactId;
            if (_cursor.isNull(_cursorIndexOfDeviceContactId)) {
              _tmpDeviceContactId = null;
            } else {
              _tmpDeviceContactId = _cursor.getString(_cursorIndexOfDeviceContactId);
            }
            final long _tmpLastUpdatedAt;
            _tmpLastUpdatedAt = _cursor.getLong(_cursorIndexOfLastUpdatedAt);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new Contact(_tmpId,_tmpName,_tmpPhoneNumber,_tmpEmail,_tmpProfileImageUri,_tmpIsFavorite,_tmpDeviceContactId,_tmpLastUpdatedAt,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Flow<List<ResolvedCallLog>> getAllResolvedCallLogs() {
    final String _sql = "\n"
            + "    SELECT \n"
            + "        cl.id,\n"
            + "        cl.phoneNumber,\n"
            + "        cl.callType,\n"
            + "        cl.timestamp,\n"
            + "        cl.durationSeconds,\n"
            + "        cl.profileImageUri,\n"
            + "        COALESCE(\n"
            + "            (SELECT c.name FROM contacts c \n"
            + "             WHERE REPLACE(REPLACE(REPLACE(REPLACE(c.phoneNumber,' ',''),'-',''),'(',''),')','')\n"
            + "                 = REPLACE(REPLACE(REPLACE(REPLACE(cl.phoneNumber,' ',''),'-',''),'(',''),')','')\n"
            + "             LIMIT 1),\n"
            + "            cl.contactName\n"
            + "        ) AS contactName\n"
            + "    FROM call_logs cl\n"
            + "    ORDER BY cl.timestamp DESC\n";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"contacts",
        "call_logs"}, new Callable<List<ResolvedCallLog>>() {
      @Override
      @NonNull
      public List<ResolvedCallLog> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = 0;
          final int _cursorIndexOfPhoneNumber = 1;
          final int _cursorIndexOfCallType = 2;
          final int _cursorIndexOfTimestamp = 3;
          final int _cursorIndexOfDurationSeconds = 4;
          final int _cursorIndexOfProfileImageUri = 5;
          final int _cursorIndexOfContactName = 6;
          final List<ResolvedCallLog> _result = new ArrayList<ResolvedCallLog>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ResolvedCallLog _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
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
            final String _tmpContactName;
            _tmpContactName = _cursor.getString(_cursorIndexOfContactName);
            _item = new ResolvedCallLog(_tmpId,_tmpPhoneNumber,_tmpCallType,_tmpTimestamp,_tmpDurationSeconds,_tmpProfileImageUri,_tmpContactName);
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
  public Flow<List<ResolvedCallLog>> getMissedResolvedCalls() {
    final String _sql = "\n"
            + "    SELECT \n"
            + "        cl.id,\n"
            + "        cl.phoneNumber,\n"
            + "        cl.callType,\n"
            + "        cl.timestamp,\n"
            + "        cl.durationSeconds,\n"
            + "        cl.profileImageUri,\n"
            + "        COALESCE(\n"
            + "            (SELECT c.name FROM contacts c \n"
            + "             WHERE REPLACE(REPLACE(REPLACE(REPLACE(c.phoneNumber,' ',''),'-',''),'(',''),')','')\n"
            + "                 = REPLACE(REPLACE(REPLACE(REPLACE(cl.phoneNumber,' ',''),'-',''),'(',''),')','')\n"
            + "             LIMIT 1),\n"
            + "            cl.contactName\n"
            + "        ) AS contactName\n"
            + "    FROM call_logs cl\n"
            + "    WHERE cl.callType = 'MISSED'\n"
            + "    ORDER BY cl.timestamp DESC\n";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"contacts",
        "call_logs"}, new Callable<List<ResolvedCallLog>>() {
      @Override
      @NonNull
      public List<ResolvedCallLog> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = 0;
          final int _cursorIndexOfPhoneNumber = 1;
          final int _cursorIndexOfCallType = 2;
          final int _cursorIndexOfTimestamp = 3;
          final int _cursorIndexOfDurationSeconds = 4;
          final int _cursorIndexOfProfileImageUri = 5;
          final int _cursorIndexOfContactName = 6;
          final List<ResolvedCallLog> _result = new ArrayList<ResolvedCallLog>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ResolvedCallLog _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
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
            final String _tmpContactName;
            _tmpContactName = _cursor.getString(_cursorIndexOfContactName);
            _item = new ResolvedCallLog(_tmpId,_tmpPhoneNumber,_tmpCallType,_tmpTimestamp,_tmpDurationSeconds,_tmpProfileImageUri,_tmpContactName);
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
