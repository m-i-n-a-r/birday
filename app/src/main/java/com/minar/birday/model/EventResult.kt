package com.minar.birday.model

import com.google.gson.annotations.Expose
import java.io.Serializable
import java.time.LocalDate

data class EventResult (
    @Expose
    val id: Int,
    @Expose
    val type: String? = EventCode.BIRTHDAY.name,
    @Expose
    var name: String,
    @Expose
    var surname: String? = "",
    @Expose
    var favorite: Boolean? = false,
    @Expose
    val yearMatter: Boolean? = true,
    @Expose
    var originalDate: LocalDate,
    @Expose
    val nextDate: LocalDate? = null,
    @Expose
    var notes: String? = "",
    val image: ByteArray? = null,
): Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EventResult

        if (id != other.id) return false
        if (type != other.type) return false
        if (name != other.name) return false
        if (surname != other.surname) return false
        if (yearMatter != other.yearMatter) return false
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
        result = 31 * result + (nextDate?.hashCode() ?: 0)
        result = 31 * result + (notes?.hashCode() ?: 0)
        result = 31 * result + (image?.contentHashCode() ?: 0)
        return result
    }
}