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

    //vehicle insurance add event
    @Expose
    val manufacturer_name: String,
    @Expose
    val manufacturer_name1: String,
    @Expose
    val manufacturer_name2: String,
    @Expose
    val manufacturer_name3: String,
    @Expose
    val model_name: String,
    @Expose
    val model_name1: String,
    @Expose
    val model_name2: String,
    @Expose
    val model_name3: String,
    @Expose
    val insurance_provider: String,

    //vehicle insurance renewal add event
    @Expose
    val input1: String,
    @Expose
    val input2: String,
    @Expose
    val input3: String,
    @Expose
    val input4: String,
    @Expose
    val input5: String,
    @Expose
    val input6: String,
    @Expose
    val input7: String,
    @Expose
    val input8: String,
    @Expose
    val input9: String,
    @Expose
    val input10: String,

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