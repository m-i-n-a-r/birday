package com.minar.birday.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity
data class Birthday(
    @PrimaryKey(autoGenerate = false)
    val id: String,
    val type: String,
    val name: String,
    val surname: String,
    val yearMatter: Boolean? = true,
    val birthDate: LocalDate
)