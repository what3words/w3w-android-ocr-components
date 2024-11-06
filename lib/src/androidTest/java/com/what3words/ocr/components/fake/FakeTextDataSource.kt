package com.what3words.ocr.components.fake

import com.what3words.core.datasource.text.W3WTextDataSource
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.common.W3WResult
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.domain.W3WSuggestion
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WGridSection
import com.what3words.core.types.geometry.W3WRectangle
import com.what3words.core.types.language.W3WLanguage
import com.what3words.core.types.language.W3WProprietaryLanguage
import com.what3words.core.types.options.W3WAutosuggestOptions

class FakeTextDataSource : W3WTextDataSource {

    private var suggested3was: List<W3WSuggestion> = emptyList()

    override fun autosuggest(
        input: String,
        options: W3WAutosuggestOptions?
    ): W3WResult<List<W3WSuggestion>> {
        return if (suggested3was.isNotEmpty()) {
            W3WResult.Success(suggested3was)
        } else {
            W3WResult.Failure(W3WError("Auto suggest failed"))
        }
    }

    override fun availableLanguages(): W3WResult<Set<W3WProprietaryLanguage>> {
        return W3WResult.Success(emptySet())
    }

    override fun convertTo3wa(
        coordinates: W3WCoordinates,
        language: W3WLanguage
    ): W3WResult<W3WAddress> {
        return W3WResult.Failure(W3WError("Not yet implemented"))
    }

    override fun convertToCoordinates(words: String): W3WResult<W3WAddress> {
        return W3WResult.Failure(W3WError("Not yet implemented"))
    }

    override fun gridSection(boundingBox: W3WRectangle): W3WResult<W3WGridSection> {
        return W3WResult.Failure(W3WError("Not yet implemented"))
    }

    override fun isValid3wa(words: String): W3WResult<Boolean> {
        return W3WResult.Failure(W3WError("Not yet implemented"))
    }

    override fun version(version: W3WTextDataSource.Version): String? {
        return null
    }

    fun setSuggested3was(suggested3was: List<W3WSuggestion>) {
        this.suggested3was = suggested3was
    }
}