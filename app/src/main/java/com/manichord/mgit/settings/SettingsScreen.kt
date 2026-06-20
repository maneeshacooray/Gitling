package com.manichord.mgit.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.sheimi.sgit.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onManageAccountsClick: () -> Unit,
    onManageSshKeysClick: () -> Unit,
    onFeedbackClick: () -> Unit,
    onRepoRootClick: () -> Unit
) {
    val repoRoot by viewModel.repoRoot.observeAsState("")
    val useEnglish by viewModel.useEnglish.observeAsState(false)
    val gitUserName by viewModel.gitUserName.observeAsState("")
    val gitUserEmail by viewModel.gitUserEmail.observeAsState("")
    val useGravatar by viewModel.useGravatar.observeAsState(true)
    val useDynamicColor by viewModel.useDynamicColor.observeAsState(false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SettingsCategory(title = stringResource(R.string.pref_category_title_general))

            SettingsClickableItem(
                title = stringResource(R.string.preference_repo_location),
                summary = repoRoot.ifEmpty { "Default" },
                icon = Icons.Default.Folder,
                onClick = onRepoRootClick
            )

            SettingsSwitchItem(
                title = stringResource(R.string.preference_eng_lang),
                summary = stringResource(R.string.preference_eng_lang_summary),
                checked = useEnglish,
                onCheckedChange = { viewModel.setUseEnglish(it) },
                icon = Icons.Default.Language
            )

            SettingsSwitchItem(
                title = stringResource(R.string.preference_use_dynamic_color),
                summary = stringResource(R.string.preference_use_dynamic_color_summary),
                checked = useDynamicColor,
                onCheckedChange = { viewModel.setUseDynamicColor(it) },
                icon = Icons.Default.Palette
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsCategory(title = stringResource(R.string.pref_category_title_git_profile))

            SettingsClickableItem(
                title = "Managed Accounts",
                summary = "Save tokens for GitHub, GitLab, etc.",
                icon = Icons.Default.AccountCircle,
                onClick = onManageAccountsClick
            )

            SettingsEditTextItem(
                title = stringResource(R.string.preference_git_user_name),
                value = gitUserName,
                onValueChange = { viewModel.setGitUserName(it) },
                icon = Icons.Default.Person
            )

            SettingsEditTextItem(
                title = stringResource(R.string.preference_git_user_email),
                value = gitUserEmail,
                onValueChange = { viewModel.setGitUserEmail(it) },
                icon = Icons.Default.Email
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsCategory(title = stringResource(R.string.pref_category_title_security))

            SettingsClickableItem(
                title = stringResource(R.string.preference_manage_ssh_keys),
                summary = stringResource(R.string.preference_manage_ssh_keys_summary),
                icon = Icons.Default.Key,
                onClick = onManageSshKeysClick
            )

            SettingsSwitchItem(
                title = stringResource(R.string.preference_use_gravatar),
                summary = stringResource(R.string.preference_use_gravatar_summary),
                checked = useGravatar,
                onCheckedChange = { viewModel.setUseGravatar(it) },
                icon = Icons.Default.Face
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsCategory(title = stringResource(R.string.pref_category_title_about))

            SettingsClickableItem(
                title = stringResource(R.string.preference_send_feedback),
                summary = stringResource(R.string.preference_send_feedback_summary),
                icon = Icons.Default.Feedback,
                onClick = onFeedbackClick
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.preference_app_version)) },
                supportingContent = { Text("Modernized v2.0") },
                leadingContent = { Icon(Icons.Default.Info, null) }
            )
        }
    }
}

@Composable
fun SettingsCategory(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun SettingsClickableItem(title: String, summary: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        leadingContent = { Icon(icon, null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun SettingsSwitchItem(title: String, summary: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        leadingContent = { Icon(icon, null) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}

@Composable
fun SettingsEditTextItem(title: String, value: String, onValueChange: (String) -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    var showDialog by remember { mutableStateOf(false) }
    var tempValue by remember { mutableStateOf(value) }

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(value.ifEmpty { "Not set" }) },
        leadingContent = { Icon(icon, null) },
        modifier = Modifier.clickable {
            tempValue = value
            showDialog = true
        }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                OutlinedTextField(
                    value = tempValue,
                    onValueChange = { tempValue = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onValueChange(tempValue)
                    showDialog = false
                }) {
                    Text(stringResource(R.string.label_done))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.label_cancel))
                }
            }
        )
    }
}
