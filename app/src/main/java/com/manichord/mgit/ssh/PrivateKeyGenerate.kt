package com.manichord.mgit.ssh

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import com.jcraft.jsch.JSch
import com.jcraft.jsch.KeyPair
import com.manichord.mgit.MainActivity
import com.manichord.mgit.ui.theme.AppTheme
import me.sheimi.android.views.SheimiDialogFragment
import me.sheimi.sgit.R
import me.sheimi.sgit.ssh.PrivateKeyUtils
import org.acra.ktx.sendWithAcra
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

class PrivateKeyGenerate : SheimiDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val activity = (requireActivity() as MainActivity).currentPrivateKeyManageHost!!

        return ComposeView(requireContext()).apply {
            setContent {
                var filename by remember { mutableStateOf("") }
                var keySize by remember { mutableStateOf("4096") }
                var isDsa by remember { mutableStateOf(false) }
                var errorRes by remember { mutableStateOf<Int?>(null) }

                AppTheme {
                    AlertDialog(
                        onDismissRequest = { dismiss() },
                        title = { Text(stringResource(R.string.label_dialog_generate_key)) },
                        text = {
                            Column {
                                OutlinedTextField(
                                    value = filename,
                                    onValueChange = {
                                        filename = it
                                        errorRes = null
                                    },
                                    label = { Text(stringResource(R.string.label_new_file_name)) },
                                    singleLine = true,
                                    isError = errorRes != null,
                                    supportingText = errorRes?.let { res -> { Text(stringResource(res)) } }
                                )
                                OutlinedTextField(
                                    value = keySize,
                                    onValueChange = { keySize = it },
                                    label = { Text(stringResource(R.string.label_key_size)) },
                                    singleLine = true
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = !isDsa,
                                        onClick = { isDsa = false }
                                    )
                                    Text(
                                        "RSA",
                                        modifier = Modifier.selectable(selected = !isDsa, onClick = { isDsa = false })
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = isDsa,
                                        onClick = { isDsa = true }
                                    )
                                    Text(
                                        "DSA",
                                        modifier = Modifier.selectable(selected = isDsa, onClick = { isDsa = true })
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val newFilename = filename.trim()
                                val size = keySize.toIntOrNull() ?: 0
                                when {
                                    newFilename.isEmpty() -> {
                                        errorRes = R.string.alert_new_filename_required
                                    }
                                    newFilename.contains("/") -> {
                                        errorRes = R.string.alert_filename_format
                                    }
                                    size < 1024 -> {
                                        errorRes = R.string.alert_too_short_key_size
                                    }
                                    size > 16384 -> {
                                        errorRes = R.string.alert_too_long_key_size
                                    }
                                    File(PrivateKeyUtils.getPrivateKeyFolder(), newFilename).exists() -> {
                                        errorRes = R.string.alert_key_exists
                                    }
                                    else -> {
                                        generateKey(newFilename, size, isDsa)
                                        activity.refreshList()
                                        dismiss()
                                    }
                                }
                            }) {
                                Text(stringResource(R.string.label_generate_key))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { dismiss() }) {
                                Text(stringResource(R.string.label_cancel))
                            }
                        }
                    )
                }
            }
        }
    }

    private fun generateKey(filename: String, keySize: Int, isDsa: Boolean) {
        val type = if (isDsa) KeyPair.DSA else KeyPair.RSA
        val newKey = File(PrivateKeyUtils.getPrivateKeyFolder(), filename)
        val newPubKey = File(PrivateKeyUtils.getPublicKeyFolder(), filename)
        try {
            val jsch = JSch()
            val kpair = KeyPair.genKeyPair(jsch, type, keySize)
            kpair.writePrivateKey(FileOutputStream(newKey))
            kpair.writePublicKey(FileOutputStream(newPubKey), "mgit")
            kpair.dispose()
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate SSH key")
            RuntimeException("Failed to generate SSH key", e).sendWithAcra()
            newKey.delete()
            newPubKey.delete()
        }
    }
}
