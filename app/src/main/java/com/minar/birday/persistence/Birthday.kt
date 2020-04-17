package com.minar.birday.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.util.*

@Entity
data class Birthday(
    @PrimaryKey(autoGenerate = false)
    val id: UUID,
    val type: String? = "birthday",
    val name: String,
    val surname: String? = "",
    val yearMatter: Boolean? = true,
    val birthDate: LocalDate
)