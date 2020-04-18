package com.minar.birday.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity
data class Event(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val type: String? = "birthday",
    val name: String,
    val surname: String? = "",
    val yearMatter: Boolean? = true,
    val originalDate: LocalDate
)