package com.minar.birday.persistence

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(indices = [Index(value = arrayOf("name", "surname", "originalDate"), unique = true)])
data class Event(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val type: String? = "birthday",
    val name: String,
    val surname: String? = "",
    val favorite: Boolean? = false,
    val yearMatter: Boolean? = true,
    val originalDate: LocalDate
)