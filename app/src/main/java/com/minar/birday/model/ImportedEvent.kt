package com.minar.birday.model

// Used to hold an imported contact in contacts importer
data class ImportedEvent(
    val id: String,
    val completeName: String,
    val eventDate: String,
    val image: ByteArray? = null,
    val eventType: String = EventCode.BIRTHDAY.name,
    val customLabel: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImportedEvent

        if (completeName != other.completeName) return false
        if (eventDate != other.eventDate) return false
        if (eventType != other.eventType) return false
        if (!image.contentEquals(other.image)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = completeName.hashCode()
        result = 31 * result + eventDate.hashCode()
        result = 31 * result + image.contentHashCode()
        return result
    }
}
