package com.manichord.mgit.ui.components

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect

/*
 * Helpers for TextFieldState-based text fields. The app's fields were migrated off the
 * value/onValueChange overload because Compose only wires drag-and-drop text support into the
 * TextFieldState-based ones (issue #58).
 */

/**
 * Runs [onChange] with the new text after each edit the user makes (typing, paste, drop) --
 * the TextFieldState equivalent of side effects in `onValueChange`. Pass it as a field's
 * `inputTransformation`. Like `onValueChange`, it doesn't fire for text set from code
 * (`setTextAndPlaceCursorAtEnd`, `clearText`), nor for cursor-only moves.
 */
fun onUserTextChange(onChange: (String) -> Unit) = InputTransformation {
    val newText = asCharSequence().toString()
    if (newText != originalText.toString()) onChange(newText)
}

/**
 * A [TextFieldState] that mirrors [value] when the text is owned somewhere else (a ViewModel,
 * or a parent composable), replacing the old `value = ...` binding. Changes to [value] made
 * outside the field are copied in; pair it with [onUserTextChange] to send the user's edits
 * back to the owner.
 *
 * The copy runs in a SideEffect, not a LaunchedEffect: a LaunchedEffect's coroutine can run
 * after the user has typed another character, and would then overwrite the field with the
 * owner's previous value, dropping that character.
 */
@Composable
fun rememberTextFieldStateFor(value: String): TextFieldState {
    val state = rememberTextFieldState(value)
    SideEffect {
        if (state.text.toString() != value) state.setTextAndPlaceCursorAtEnd(value)
    }
    return state
}
