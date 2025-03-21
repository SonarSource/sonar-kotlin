package androidx.compose.foundation.text

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType

// Partial stubbing of the primary constructor
class KeyboardOptions(
    val capitalization: KeyboardCapitalization = KeyboardCapitalization.Unspecified,
    val autoCorrectEnabled: Boolean? = null,
    val keyboardType: KeyboardType = KeyboardType.Unspecified,
    // ...
) {
    companion object {
        val Default = KeyboardOptions()
    }

    // Partial stubbing of parameters
    fun copy(
        capitalization: KeyboardCapitalization = this.capitalization,
        autoCorrectEnabled: Boolean? = this.autoCorrectEnabled,
        keyboardType: KeyboardType = this.keyboardType,
        // ...
    ) : KeyboardOptions {
        throw Exception("Stub!")
    }
}
