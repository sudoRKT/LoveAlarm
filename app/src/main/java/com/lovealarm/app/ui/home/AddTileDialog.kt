package com.lovealarm.app.ui.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import com.lovealarm.app.data.MAX_TILE_TEXT_LENGTH

/**
 * Asks for the text of a new custom tile. Any text, emoji or symbols are fine.
 * [onAdd] receives the trimmed text; the caller closes the dialog.
 */
@Composable
fun AddTileDialog(
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val trimmed = text.trim()
    val length = trimmed.length
    val tooLong = length > MAX_TILE_TEXT_LENGTH
    val canAdd = trimmed.isNotEmpty() && !tooLong

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New tile") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("What should it say?") },
                singleLine = false,
                maxLines = 3,
                isError = tooLong,
                supportingText = {
                    Text(
                        text = "$length/$MAX_TILE_TEXT_LENGTH",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(trimmed) },
                enabled = canAdd,
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
