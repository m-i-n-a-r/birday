package com.minar.birday.utilities

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// This class helps gson to serialize and deserialize a LocalDate
class LocalDateJsonSerializer : TypeAdapter<LocalDate>() {

    // Write a date to JSON, using a standard format
    override fun write(out: JsonWriter, value: LocalDate) {
        out.value(DateTimeFormatter.ISO_LOCAL_DATE.format(value))
    }

    // Read a date from JSON. If invalid, return the actual date
    override fun read(input: JsonReader): LocalDate {
        return try {
            LocalDate.parse(input.nextString())
        } catch (e: Exception) {
            LocalDate.now()
        }
    }
}