package com.what3words.ocr.components.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.what3words.javawrapper.response.Suggestion

class ScanResultState() {
    sealed class State {
        object Idle : State()

        // when new addresses are being loaded
        object Scanning : State()

        // when address selector has available nodes to display to user
        object Detected : State()

        // when address selector should go back to the default empty state
        object Validating : State()

        object Found : State()

        // when address validation process failed
        data class Error(val message: String) : State()
    }

    var foundItems = mutableStateListOf<Suggestion>()
        private set

    var state: State by mutableStateOf(State.Idle)
        private set

    var lastAdded: String? by mutableStateOf(null)
        private set

    fun scanning() {
        state = State.Scanning
    }

    fun detected() {
        state = State.Detected
    }

    fun validating() {
        state = State.Validating
    }

    fun failed(errorMessage: String) {
        state = State.Error(message = errorMessage)
    }

    fun found(res: List<Suggestion>) {
        res.forEach { newFound ->
            if (foundItems.all { it.words != newFound.words }) {
                foundItems.add(0, newFound)
                lastAdded = newFound.words
                state = State.Found
            }
        }
    }

    fun switchToDefault() {
        state = State.Scanning
    }
}