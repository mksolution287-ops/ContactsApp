package com.mktech.contactsapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.mktech.contactsapp.data.model.CallLog
import com.mktech.contactsapp.data.model.CallType
import com.mktech.contactsapp.data.model.Contact

class Converters {
    @TypeConverter
    fun fromCallType(value: CallType): String = value.name
    @TypeConverter
    fun toCallType(value: String): CallType = CallType.valueOf(value)
}

@Database(
    entities = [Contact::class, CallLog::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ContactDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun callLogDao(): CallLogDao
}

