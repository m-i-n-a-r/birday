package com.minar.birday.model.VehicleInsurance

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import com.minar.birday.model.EventCode
import java.time.LocalDate

@Entity(
    indices = [Index(
        value = arrayOf("name", "surname", "originalDate"),
        unique = true
    )]
)

data class VehicleInsuranceData(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    @Expose
    val type: String? = EventCode.VEHICLE_INSURANCE.name,
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
    @Expose
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

        other as VehicleInsuranceData

        if (id != other.id) return false
        if (type != other.type) return false
        if (manufacturer_name != other.manufacturer_name) return false
        if (manufacturer_name1 != other.manufacturer_name1) return false
        if (manufacturer_name2 != other.manufacturer_name2) return false
        if (manufacturer_name3 != other.manufacturer_name3) return false
        if (model_name != other.model_name) return false
        if (model_name1 != other.model_name1) return false
        if (model_name2 != other.model_name2) return false
        if (model_name3 != other.model_name3) return false
        if (insurance_provider != other.insurance_provider) return false
        if (originalDate != other.originalDate) return false
        if (!image.contentEquals(other.image)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + manufacturer_name.hashCode()
        result = 31 * result + (model_name.hashCode() ?: 0)
        result = 31 * result + (favorite?.hashCode() ?: 0)
        result = 31 * result + (yearMatter?.hashCode() ?: 0)
        result = 31 * result + originalDate.hashCode()
        result = 31 * result + (notes?.hashCode() ?: 0)
        result = 31 * result + (image?.contentHashCode() ?: 0)
        return result
    }
}