package me.sheimi.sgit.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import com.manichord.mgit.ui.theme.AppTheme
import me.sheimi.android.utils.CodeGuesser
import me.sheimi.android.views.SheimiDialogFragment
import me.sheimi.sgit.R
import me.sheimi.sgit.activities.ViewFileActivity

class ChooseLanguageDialog : SheimiDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val activity = requireActivity() as ViewFileActivity
        val langs = CodeGuesser.getLanguageList()

        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
                    AlertDialog(
                        onDismissRequest = { dismiss() },
                        title = { Text(stringResource(R.string.dialog_choose_language_title)) },
                        text = {
                            val transparentColors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            Column {
                                langs.forEach { lang ->
                                    ListItem(
                                        headlineContent = { Text(lang) },
                                        colors = transparentColors,
                                        modifier = Modifier.clickable {
                                            activity.setLanguage(CodeGuesser.getLanguageTag(lang))
                                            dismiss()
                                        }
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { dismiss() }) {
                                Text(stringResource(R.string.label_cancel))
                            }
                        }
                    )
                }
            }
        }
    }
}
