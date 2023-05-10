package com.what3words.ocr.components.models

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.os.Parcelable
import android.util.Base64
import com.what3words.javawrapper.response.APIResponse.What3WordsError
import com.what3words.javawrapper.response.Suggestion
import kotlinx.parcelize.Parcelize
import java.io.ByteArrayOutputStream


@Parcelize
data class OcrInfo(
    var boxes: List<RectF> = emptyList(),
    var droppedFrame: Boolean = false
) : Parcelable

@Parcelize
data class OcrScanResult(
    var suggestions: List<Suggestion> = emptyList(),
    var info: OcrInfo? = null,
    var scannedImage: String? = null,
    var error: What3WordsError? = null
) : Parcelable {
    fun isSuccessful(): Boolean {
        return error == null && suggestions.isNotEmpty()
    }
}

