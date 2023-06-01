package com.what3words.ocr.components.models

import android.os.Parcelable
import com.what3words.javawrapper.response.APIResponse.What3WordsError
import com.what3words.javawrapper.response.Suggestion
import kotlinx.parcelize.Parcelize

@Parcelize
data class OcrScanResult(
    val suggestions: List<Suggestion> = emptyList(),
    val error: What3WordsError? = null
) : Parcelable {
    fun isSuccessful(): Boolean {
        return error == null && suggestions.isNotEmpty()
    }
}

