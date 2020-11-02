package com.minar.birday.model

import androidx.room.ColumnInfo
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
    val originalDate: LocalDate,
    val notes: String? = "",
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val image: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Event

        if (id != other.id) return false
        if (type != other.type) return false
        if (name != other.name) return false
        if (surname != other.surname) return false
        if (originalDate != other.originalDate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + name.hashCode()
        result = 31 * result + (surname?.hashCode() ?: 0)
        result = 31 * result + originalDate.hashCode()
        return result
    }
}