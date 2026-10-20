@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.manichord.mgit.repodetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import me.sheimi.sgit.R

/** What the toolbar asks the repo screen to do. */
class RepoToolbarActions(
    val pull: (force: Boolean) -> Unit,
    val push: (pushAll: Boolean, force: Boolean) -> Unit,
    val choosePullRemote: () -> Unit,
    val choosePushRemote: () -> Unit,
    val commit: () -> Unit
)

private enum class ForceConfirm { PULL, PUSH }

/**
 * The everyday git actions one tap away instead of in the operations drawer: Pull and Push as split
 * buttons (one tap pulls from or pushes to origin; the arrow opens the other options) and Commit.
 * Everything else stays in the drawer, opened from the top bar. Force pull and force push ask
 * first, since they can overwrite work.
 *
 * Each button shows how much it has waiting ([counts]): commits to pull, commits to push, changed
 * files to commit. A count only appears when it's above zero. On a narrow screen or with a large
 * text size the counts can make the toolbar wider than the screen; then they're dropped and
 * screen readers still hear them.
 */
@Composable
fun RepoToolbar(
    actions: RepoToolbarActions,
    counts: RepoDetailViewModel.SyncCounts?,
    modifier: Modifier = Modifier
) {
    var confirm by remember { mutableStateOf<ForceConfirm?>(null) }

    val onForcePull = { confirm = ForceConfirm.PULL }
    val onForcePush = { confirm = ForceConfirm.PUSH }
    SubcomposeLayout(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) { constraints ->
        // Measure with counts at their natural width; without them if that doesn't fit
        val natural = constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity)
        var placeables: List<Placeable> = subcompose(true) {
            ToolbarContent(actions, counts, showCounts = true, onForcePull, onForcePush)
        }.map { it.measure(natural) }
        if (placeables.maxOf { it.width } > constraints.maxWidth) {
            placeables = subcompose(false) {
                ToolbarContent(actions, counts, showCounts = false, onForcePull, onForcePush)
            }.map { it.measure(constraints.copy(minWidth = 0)) }
        }
        val height = placeables.maxOf { it.height }
        layout(constraints.maxWidth, height) {
            placeables.forEach { it.place((constraints.maxWidth - it.width) / 2, 0) }
        }
    }

    confirm?.let { which ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = {
                Text(stringResource(if (which == ForceConfirm.PULL) R.string.menu_force_pull else R.string.menu_force_push))
            },
            text = {
                Text(stringResource(if (which == ForceConfirm.PULL) R.string.dialog_force_pull_msg else R.string.dialog_force_push_msg))
            },
            confirmButton = {
                TextButton(shapes = ButtonDefaults.shapes(), onClick = {
                    confirm = null
                    if (which == ForceConfirm.PULL) actions.pull(true) else actions.push(false, true)
                }) {
                    Text(stringResource(if (which == ForceConfirm.PULL) R.string.menu_force_pull else R.string.menu_force_push))
                }
            },
            dismissButton = {
                TextButton(shapes = ButtonDefaults.shapes(), onClick = { confirm = null }) {
                    Text(stringResource(R.string.label_cancel))
                }
            }
        )
    }
}

/** A count as shown on a button: big numbers don't need to be exact to say "a lot". */
private fun countText(count: Int) = if (count > 99) "99+" else count.toString()

@Composable
private fun ToolbarContent(
    actions: RepoToolbarActions,
    counts: RepoDetailViewModel.SyncCounts?,
    showCounts: Boolean,
    onForcePull: () -> Unit,
    onForcePush: () -> Unit
) {
    val behind = counts?.behind ?: 0
    val ahead = counts?.ahead ?: 0
    val changes = counts?.changes ?: 0
    HorizontalFloatingToolbar(
        expanded = true,
        // Tighter than the defaults so the counts fit a typical phone
        contentPadding = PaddingValues(6.dp),
        content = {
            SplitAction(
                icon = Icons.Default.CloudDownload,
                label = if (behind > 0) {
                    pluralStringResource(R.plurals.toolbar_pull_count, behind, behind)
                } else {
                    stringResource(R.string.label_pull)
                },
                count = if (showCounts && behind > 0) countText(behind) else null,
                optionsLabel = stringResource(R.string.label_pull_options),
                onClick = { actions.pull(false) },
                options = listOf(
                    stringResource(R.string.menu_force_pull) to onForcePull,
                    stringResource(R.string.menu_choose_remote) to actions.choosePullRemote
                )
            )
            val commitLabel = if (changes > 0) {
                pluralStringResource(R.plurals.toolbar_commit_count, changes, changes)
            } else {
                stringResource(R.string.label_commit)
            }
            if (showCounts && changes > 0) {
                Button(
                    onClick = actions.commit,
                    shapes = ButtonDefaults.shapes(),
                    contentPadding = PaddingValues(start = 14.dp, end = 10.dp),
                    modifier = Modifier.semantics { contentDescription = commitLabel }
                ) {
                    Icon(Icons.Default.Commit, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(8.dp))
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.onPrimary) {
                        Text(
                            countText(changes),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 24.dp)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            } else {
                FilledIconButton(onClick = actions.commit, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Default.Commit, contentDescription = commitLabel)
                }
            }
            SplitAction(
                icon = Icons.Default.CloudUpload,
                label = if (ahead > 0) {
                    pluralStringResource(R.plurals.toolbar_push_count, ahead, ahead)
                } else {
                    stringResource(R.string.label_push)
                },
                count = if (showCounts && ahead > 0) countText(ahead) else null,
                optionsLabel = stringResource(R.string.label_push_options),
                onClick = { actions.push(false, false) },
                options = listOf(
                    stringResource(R.string.menu_push_all) to { actions.push(true, false) },
                    stringResource(R.string.menu_force_push) to onForcePush,
                    stringResource(R.string.menu_choose_remote) to actions.choosePushRemote
                )
            )
        }
    )
}

/**
 * A split button: the main action on the left, a menu of options behind the arrow on the right.
 * [label] is what screen readers hear for the main action, [count] the number shown next to its
 * icon, if any.
 */
@Composable
private fun SplitAction(
    icon: ImageVector,
    label: String,
    count: String?,
    optionsLabel: String,
    onClick: () -> Unit,
    options: List<Pair<String, () -> Unit>>
) {
    var expanded by remember { mutableStateOf(false) }
    SplitButtonLayout(
        leadingButton = {
            SplitButtonDefaults.TonalLeadingButton(
                onClick = onClick,
                contentPadding = if (count != null) PaddingValues(start = 14.dp, end = 12.dp) else PaddingValues(start = 16.dp, end = 12.dp),
                modifier = Modifier.semantics { contentDescription = label }
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize))
                if (count != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(count, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        },
        trailingButton = {
            Box {
                SplitButtonDefaults.TonalTrailingButton(
                    checked = expanded,
                    onCheckedChange = { expanded = it },
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    modifier = Modifier.semantics { contentDescription = optionsLabel }
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier
                            .size(SplitButtonDefaults.TrailingIconSize)
                            .rotate(if (expanded) 180f else 0f)
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { (text, action) ->
                        DropdownMenuItem(
                            text = { Text(text) },
                            onClick = {
                                expanded = false
                                action()
                            }
                        )
                    }
                }
            }
        }
    )
}
