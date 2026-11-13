@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.manichord.mgit.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.manichord.mgit.ui.theme.FontOption
import com.manichord.mgit.update.UpdateCheckResult
import me.sheimi.sgit.BuildConfig
import me.sheimi.sgit.R
import me.sheimi.sgit.database.models.Repo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onManageAccountsClick: () -> Unit,
    onManageSshKeysClick: () -> Unit,
    onFeedbackClick: () -> Unit,
    onViewReleaseClick: (String) -> Unit = {},
    onAddWidgetClick: () -> Unit = {}
) {
    val repoRoot by viewModel.repoRoot.observeAsState(Repo.getDefaultRepoRootDir().absolutePath)
    val useSharedMediaStorage by viewModel.useSharedMediaStorage.observeAsState(false)
    val movingRepoStorage by viewModel.movingRepoStorage.observeAsState(false)
    val useEnglish by viewModel.useEnglish.observeAsState(false)
    val gitUserName by viewModel.gitUserName.observeAsState("")
    val gitUserEmail by viewModel.gitUserEmail.observeAsState("")
    val useGravatar by viewModel.useGravatar.observeAsState(true)
    val useDynamicColor by viewModel.useDynamicColor.observeAsState(false)
    val fontOption by viewModel.fontOption.observeAsState(FontOption.DEFAULT)
    val checkingForUpdate by viewModel.checkingForUpdate.observeAsState(false)
    val updateCheckResult by viewModel.updateCheckResult.observeAsState(null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_settings)) },
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
            .padding(bottom = 24.dp)
        ) {
            SettingsCategory(title = stringResource(R.string.pref_category_title_general))
            SettingsGroup(
                listOf(
                    {
                        ListItem(
                            colors = LocalSettingsItemColors.current ?: ListItemDefaults.colors(),
                            headlineContent = { Text(stringResource(R.string.preference_repo_location)) },
                            supportingContent = { Text(repoRoot) },
                            leadingContent = { Icon(Icons.Default.Folder, null) }
                        )
                    },
                    {
                        SettingsSwitchItem(
                            title = stringResource(R.string.preference_use_shared_media_storage),
                            summary = if (movingRepoStorage) {
                                "Moving repos..."
                            } else {
                                stringResource(R.string.preference_use_shared_media_storage_summary)
                            },
                            checked = useSharedMediaStorage,
                            onCheckedChange = { viewModel.setUseSharedMediaStorage(it) },
                            icon = Icons.Default.FolderShared,
                            enabled = !movingRepoStorage
                        )
                    },
                    {
                        SettingsSwitchItem(
                            title = stringResource(R.string.preference_eng_lang),
                            summary = stringResource(R.string.preference_eng_lang_summary),
                            checked = useEnglish,
                            onCheckedChange = { viewModel.setUseEnglish(it) },
                            icon = Icons.Default.Language
                        )
                    },
                    {
                        SettingsSwitchItem(
                            title = stringResource(R.string.preference_use_dynamic_color),
                            summary = stringResource(R.string.preference_use_dynamic_color_summary),
                            checked = useDynamicColor,
                            onCheckedChange = { viewModel.setUseDynamicColor(it) },
                            icon = Icons.Default.Palette
                        )
                    },
                    {
                        SettingsFontItem(
                            selected = fontOption,
                            onSelect = { viewModel.setFontOption(it) }
                        )
                    },
                    {
                        SettingsClickableItem(
                            title = stringResource(R.string.preference_add_widget),
                            summary = stringResource(R.string.preference_add_widget_summary),
                            icon = Icons.Default.Widgets,
                            onClick = onAddWidgetClick
                        )
                    }
                )
            )

            SettingsCategory(title = stringResource(R.string.pref_category_title_git_profile))
            SettingsGroup(
                listOf(
                    {
                        SettingsClickableItem(
                            title = "Managed Accounts",
                            summary = "Save tokens for GitHub, GitLab, etc.",
                            icon = Icons.Default.AccountCircle,
                            onClick = onManageAccountsClick
                        )
                    },
                    {
                        SettingsEditTextItem(
                            title = stringResource(R.string.preference_git_user_name),
                            value = gitUserName,
                            onValueChange = { viewModel.setGitUserName(it) },
                            icon = Icons.Default.Person
                        )
                    },
                    {
                        SettingsEditTextItem(
                            title = stringResource(R.string.preference_git_user_email),
                            value = gitUserEmail,
                            onValueChange = { viewModel.setGitUserEmail(it) },
                            icon = Icons.Default.Email
                        )
                    }
                )
            )

            SettingsCategory(title = stringResource(R.string.pref_category_title_security))
            SettingsGroup(
                listOf(
                    {
                        SettingsClickableItem(
                            title = stringResource(R.string.preference_manage_ssh_keys),
                            summary = stringResource(R.string.preference_manage_ssh_keys_summary),
                            icon = Icons.Default.Key,
                            onClick = onManageSshKeysClick
                        )
                    },
                    {
                        SettingsSwitchItem(
                            title = stringResource(R.string.preference_use_gravatar),
                            summary = stringResource(R.string.preference_use_gravatar_summary),
                            checked = useGravatar,
                            onCheckedChange = { viewModel.setUseGravatar(it) },
                            icon = Icons.Default.Face
                        )
                    }
                )
            )

            SettingsCategory(title = stringResource(R.string.pref_category_title_about))
            SettingsGroup(
                listOf(
                    {
                        SettingsClickableItem(
                            title = stringResource(R.string.preference_send_feedback),
                            summary = stringResource(R.string.preference_send_feedback_summary),
                            icon = Icons.Default.Feedback,
                            onClick = onFeedbackClick
                        )
                    },
                    {
                        ListItem(
                            colors = LocalSettingsItemColors.current ?: ListItemDefaults.colors(),
                            headlineContent = { Text(stringResource(R.string.preference_app_version)) },
                            supportingContent = { Text(BuildConfig.VERSION_NAME) },
                            leadingContent = { Icon(Icons.Default.Info, null) }
                        )
                    },
                    {
                        SettingsUpdateCheckItem(
                            checking = checkingForUpdate,
                            result = updateCheckResult,
                            onCheckClick = { viewModel.checkForUpdate() },
                            onViewReleaseClick = onViewReleaseClick
                        )
                    }
                )
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
        colors = LocalSettingsItemColors.current ?: ListItemDefaults.colors(),
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        leadingContent = { Icon(icon, null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun SettingsSwitchItem(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true
) {
    ListItem(
        colors = LocalSettingsItemColors.current ?: ListItemDefaults.colors(),
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        leadingContent = { Icon(icon, null) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        },
        modifier = Modifier.clickable(enabled = enabled) { onCheckedChange(!checked) }
    )
}

@Composable
fun SettingsUpdateCheckItem(
    checking: Boolean,
    result: UpdateCheckResult?,
    onCheckClick: () -> Unit,
    onViewReleaseClick: (String) -> Unit
) {
    ListItem(
        colors = LocalSettingsItemColors.current ?: ListItemDefaults.colors(),
        headlineContent = { Text("Check for Updates") },
        supportingContent = {
            Text(
                when {
                    checking -> "Checking..."
                    result is UpdateCheckResult.UpdateAvailable -> "Version ${result.versionName} is available"
                    result is UpdateCheckResult.UpToDate -> "You're on the latest version"
                    result is UpdateCheckResult.Failed -> "Couldn't check for updates -- check your connection"
                    else -> "Tap to check"
                }
            )
        },
        leadingContent = { Icon(Icons.Default.Refresh, null) },
        trailingContent = {
            when {
                checking -> LoadingIndicator(modifier = Modifier.size(24.dp))
                result is UpdateCheckResult.UpdateAvailable -> {
                    TextButton(shapes = ButtonDefaults.shapes(), onClick = { onViewReleaseClick(result.releaseUrl) }) { Text("View") }
                }
            }
        },
        modifier = Modifier.clickable(enabled = !checking, onClick = onCheckClick)
    )
}

@Composable
fun SettingsFontItem(selected: FontOption, onSelect: (FontOption) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    ListItem(

        colors = LocalSettingsItemColors.current ?: ListItemDefaults.colors(),
        headlineContent = { Text(stringResource(R.string.preference_app_font)) },
        supportingContent = { Text(selected.displayName) },
        leadingContent = { Icon(Icons.Default.TextFields, null) },
        modifier = Modifier.clickable { showDialog = true }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.preference_app_font)) },
            text = {
                Column {
                    FontOption.entries.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(option)
                                    showDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(selected = option == selected, onClick = {
                                onSelect(option)
                                showDialog = false
                            })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(option.displayName, fontFamily = option.fontFamily)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(shapes = ButtonDefaults.shapes(), onClick = { showDialog = false }) {
                    Text(stringResource(R.string.label_cancel))
                }
            }
        )
    }
}

@Composable
fun SettingsEditTextItem(title: String, value: String, onValueChange: (String) -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    var showDialog by remember { mutableStateOf(false) }
    val tempValue = rememberTextFieldState(value)

    ListItem(

        colors = LocalSettingsItemColors.current ?: ListItemDefaults.colors(),
        headlineContent = { Text(title) },
        supportingContent = { Text(value.ifEmpty { "Not set" }) },
        leadingContent = { Icon(icon, null) },
        modifier = Modifier.clickable {
            tempValue.setTextAndPlaceCursorAtEnd(value)
            showDialog = true
        }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                OutlinedTextField(
                    state = tempValue,
                    lineLimits = TextFieldLineLimits.SingleLine,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(shapes = ButtonDefaults.shapes(), onClick = {
                    onValueChange(tempValue.text.toString())
                    showDialog = false
                }) {
                    Text(stringResource(R.string.label_done))
                }
            },
            dismissButton = {
                TextButton(shapes = ButtonDefaults.shapes(), onClick = { showDialog = false }) {
                    Text(stringResource(R.string.label_cancel))
                }
            }
        )
    }
}

@Composable
fun SettingsGroup(items: List<@Composable () -> Unit>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items.forEachIndexed { index, item ->
            val restingShape = settingsItemShape(index, items.size)
            var pressed by remember { mutableStateOf(false) }
            val topStart by animateDpAsState(
                targetValue = if (pressed) 24.dp else restingShape.topStart,
                animationSpec = tween(120),
                label = "topStart"
            )
            val topEnd by animateDpAsState(
                targetValue = if (pressed) 24.dp else restingShape.topEnd,
                animationSpec = tween(120),
                label = "topEnd"
            )
            val bottomEnd by animateDpAsState(
                targetValue = if (pressed) 24.dp else restingShape.bottomEnd,
                animationSpec = tween(120),
                label = "bottomEnd"
            )
            val bottomStart by animateDpAsState(
                targetValue = if (pressed) 24.dp else restingShape.bottomStart,
                animationSpec = tween(120),
                label = "bottomStart"
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart),
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            pressed = true
                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.changes.none { it.pressed }) break
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.first()
                                    if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) break
                                }
                            } finally {
                                pressed = false
                            }
                        }
                    }
            ) {
                CompositionLocalProvider(LocalSettingsItemColors provides ListItemDefaults.colors(containerColor = Color.Transparent)) {
                    Column { item() }
                }
            }
        }
    }
}

private fun settingsItemShape(index: Int, count: Int): RoundedCornerShape {
    val outer = 24.dp
    val inner = 4.dp
    if (count <= 1) return RoundedCornerShape(outer)
    val top = if (index == 0) outer else inner
    val bottom = if (index == count - 1) outer else inner
    return RoundedCornerShape(top, top, bottom, bottom)
}

val LocalSettingsItemColors = staticCompositionLocalOf<ListItemColors?> { null }
