package com.minar.birday.model

import java.io.Serializable
import java.time.LocalDate

data class EventResult (
    val id: Int,
    val type: String? = EventCode.BIRTHDAY.name,
    var name: String,
    var surname: String? = "",
    var favorite: Boolean? = false,
    val yearMatter: Boolean? = true,
    var originalDate: LocalDate,
    val nextDate: LocalDate? = null,
    var notes: String? = "",
    val image: ByteArray? = null,
    //vehicle insurance add event
    val manufacturer_name: String? ="",
    val manufacturer_name1: String? ="",
    val manufacturer_name2: String? ="",
    val manufacturer_name3: String? ="",
    val model_name: String? ="",
    val model_name1: String? ="",
    val model_name2: String? ="",
    val model_name3: String? ="",
    val insurance_provider: String? ="",
    //vehicle insurance renewal add event
    val input1: String? ="",
    val input2: String? ="",
    val input3: String? ="",
    val input4: String? ="",
    val input5: String? ="",
    val input6: String? ="",
    val input7: String? ="",
    val input8: String? ="",
    val input9: String? ="",
    val input10: String? ="",

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

        //vehicle insurance add event
        if (manufacturer_name != other.manufacturer_name) return false
        if (manufacturer_name1 != other.manufacturer_name1) return false
        if (manufacturer_name2 != other.manufacturer_name2) return false
        if (manufacturer_name3 != other.manufacturer_name3) return false
        if (model_name != other.model_name) return false
        if (model_name1 != other.model_name1) return false
        if (model_name2 != other.model_name2) return false
        if (model_name3 != other.model_name3) return false

        //vehicle insurance renewal add event
        if (input1 != other.input1) return false
        if (input2 != other.input2) return false
        if (input3 != other.input3) return false
        if (input4 != other.input4) return false
        if (input5 != other.input5) return false
        if (input6 != other.input6) return false
        if (input7 != other.input7) return false
        if (input8 != other.input8) return false
        if (input9 != other.input9) return false
        if (input10 != other.input10) return false

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

        //vehicle insurance add event
        result = 31 * result + manufacturer_name.hashCode()
        result = 31 * result + (model_name.hashCode() ?: 0)

        //vehicle insurance renewal add event
        result = 31 * result + (input1.hashCode() ?: 0)

        return result
    }
}