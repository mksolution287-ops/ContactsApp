package com.mktech.contactsapp.data

object PhoneUtils {
    fun normalize(phone: String): String {
        return phone
            .replace("\\s".toRegex(), "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            .removePrefix("+91")
            .removePrefix("0")
    }
}