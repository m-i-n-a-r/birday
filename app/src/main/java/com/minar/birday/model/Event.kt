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
    val manufacturerName: String? ="",
    val manufacturerName1: String? ="",
    val manufacturerName2: String? ="",
    val manufacturerName3: String? ="",
    val modelName: String? ="",
    val modelName1: String? ="",
    val modelName2: String? ="",
    val modelName3: String? ="",
    val insuranceProvider: String? ="",
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
        if (manufacturerName != other.manufacturerName) return false
        if (manufacturerName1 != other.manufacturerName1) return false
        if (manufacturerName2 != other.manufacturerName2) return false
        if (manufacturerName3 != other.manufacturerName3) return false
        if (modelName != other.modelName) return false
        if (modelName1 != other.modelName1) return false
        if (modelName2 != other.modelName2) return false
        if (modelName3 != other.modelName3) return false

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
        result = 31 * result + (manufacturerName?.hashCode() ?: 0)
        result = 31 * result + (manufacturerName1?.hashCode() ?: 0)
        result = 31 * result + (manufacturerName2?.hashCode() ?: 0)
        result = 31 * result + (manufacturerName3?.hashCode() ?: 0)

        result = 31 * result + (modelName.hashCode() ?: 0)
        result = 31 * result + (modelName1.hashCode() ?: 0)
        result = 31 * result + (modelName2.hashCode() ?: 0)
        result = 31 * result + (modelName3.hashCode() ?: 0)
        result = 31 * result + (insuranceProvider.hashCode() ?: 0)

        //vehicle insurance renewal add event
        result = 31 * result + (input1.hashCode() ?: 0)
        result = 31 * result + (input2.hashCode() ?: 0)
        result = 31 * result + (input3.hashCode() ?: 0)
        result = 31 * result + (input4.hashCode() ?: 0)
        result = 31 * result + (input5.hashCode() ?: 0)
        result = 31 * result + (input6.hashCode() ?: 0)
        result = 31 * result + (input7.hashCode() ?: 0)
        result = 31 * result + (input8.hashCode() ?: 0)
        result = 31 * result + (input9.hashCode() ?: 0)
        result = 31 * result + (input10.hashCode() ?: 0)
        return result
    }
}