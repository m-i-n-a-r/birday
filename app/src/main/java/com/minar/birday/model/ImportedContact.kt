package com.minar.birday.model

// Used to hold an imported contact in contacts importer
data class ImportedContact(
    val id: String,
    val completeName: String,
    val birthday: String,
    val image: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImportedContact

        if (completeName != other.completeName) return false
        if (birthday != other.birthday) return false
        if (!image.contentEquals(other.image)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = completeName.hashCode()
        result = 31 * result + birthday.hashCode()
        result = 31 * result + image.contentHashCode()
        return result
    }
}
