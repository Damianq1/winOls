package com.winols.app

import com.winols.app.model.MapDefinition
import java.io.File

/**
 * Sesja robocza użytkownika. Przechowuje aktualnie otwarty projekt, 
 * historię modyfikacji oraz referencję do oryginalnego zrzutu pamięci (do porównań side-by-side).
 */
data class ProjectSession(
    val id: String,
    var binModel: BinModel,
    var originalSnapshot: ByteArray,
    var activeMap: MapDefinition? = null,
    var isDirty: Boolean = false
) {
    fun reloadOriginal() {
        binModel.bufferManager.applyPatch(0, originalSnapshot)
        isDirty = false
    }

    fun markModified() {
        isDirty = true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ProjectSession
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}