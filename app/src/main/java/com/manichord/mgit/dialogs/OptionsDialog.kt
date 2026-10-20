@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.manichord.mgit.dialogs

import androidx.compose.material3.ButtonDefaults
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ListItem
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import me.sheimi.android.activities.SheimiFragmentActivity.onOptionDialogClicked
import com.manichord.mgit.ui.theme.AppTheme

/** Compose replacement for [me.sheimi.android.activities.SheimiFragmentActivity.showOptionsDialog]. */
object OptionsDialog {
    @JvmStatic
    fun show(
        container: ViewGroup,
        title: String,
        options: Array<CharSequence>,
        cancelText: String,
        listeners: Array<onOptionDialogClicked>
    ) {
        val composeView = ComposeView(container.context)
        container.addView(composeView)
        fun close() = container.removeView(composeView)

        composeView.setContent {
            AppTheme {
                AlertDialog(
                    onDismissRequest = { close() },
                    title = { Text(title) },
                    text = {
                        val transparentColors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        Column {
                            options.forEachIndexed { index, option ->
                                ListItem(
                                    headlineContent = { Text(option.toString()) },
                                    colors = transparentColors,
                                    modifier = Modifier.clickable {
                                        close()
                                        listeners[index].onClicked()
                                    }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(shapes = ButtonDefaults.shapes(), onClick = { close() }) { Text(cancelText) }
                    }
                )
            }
        }
    }
}
