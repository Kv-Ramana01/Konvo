package com.example.konvo.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

@Composable
fun rememberKeyboardHider(): () -> Unit {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    return {
        focusManager.clearFocus()
        keyboardController?.hide()
    }
}