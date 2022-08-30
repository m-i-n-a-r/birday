package com.minar.birday.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import java.time.LocalDate

@Entity(
    indices = [Index(
        value = arrayOf("name", "surname", "originalDate"),
        unique = true
    )]
)
data class Event(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    @Expose
    val type: String? = EventCode.BIRTHDAY.name,
    @Expose
    val name: String,
    @Expose
    val surname: String? = "",
    val favorite: Boolean? = false,
    @Expose
    val yearMatter: Boolean? = true,
    @Expose
    val originalDate: LocalDate,
    @Expose
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
        if (!image.contentEquals(other.image)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + name.hashCode()
        result = 31 * result + (surname?.hashCode() ?: 0)
        result = 31 * result + (favorite?.hashCode() ?: 0)
        result = 31 * result + (yearMatter?.hashCode() ?: 0)
        result = 31 * result + originalDate.hashCode()
        result = 31 * result + (notes?.hashCode() ?: 0)
        result = 31 * result + (image?.contentHashCode() ?: 0)
        return result
    }
}