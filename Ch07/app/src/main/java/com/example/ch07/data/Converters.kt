package com.example.ch07.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(tags: List<String>?): String {
        return gson.toJson(tags ?: emptyList<String>())
    }

    @TypeConverter
    fun toStringList(data: String?): List<String> {
        if (data.isNullOrBlank()) return emptyList()
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(data, listType) ?: emptyList()
    }
}
