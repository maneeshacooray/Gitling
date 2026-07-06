package com.manichord.mgit.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.manichord.mgit.R

@Composable
@GlanceComposable
fun RepoWidgetContent(repos: List<RepoWidgetEntry>) {
    GlanceTheme {
        Scaffold(
            titleBar = {
                TitleBar(
                    title = "Gitling",
                    actions = {
                        CircleIconButton(
                            imageProvider = ImageProvider(R.drawable.ic_sync),
                            contentDescription = "Refresh",
                            onClick = actionRunCallback<RefreshWidgetAction>()
                        )
                    }
                )
            }
        ) {
            if (repos.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No repos cloned yet",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(repos, itemId = { it.id.toLong() }) { repo ->
                        RepoWidgetRow(repo)
                    }
                }
            }
        }
    }
}

@Composable
@GlanceComposable
private fun RepoWidgetRow(repo: RepoWidgetEntry) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(
                actionRunCallback<OpenRepoWidgetAction>(
                    actionParametersOf(RepoIdParamKey to repo.id)
                )
            ),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        // Status dot — tertiary (clean) or error (dirty)
        Box(
            modifier = GlanceModifier
                .size(8.dp)
                .cornerRadius(4.dp)
                .background(
                    if (repo.isDirty) GlanceTheme.colors.error
                    else GlanceTheme.colors.tertiary
                )
        ) {}

        Spacer(modifier = GlanceModifier.width(10.dp))

        Column(modifier = GlanceModifier.defaultWeight()) {
            // Repo name
            Text(
                text = repo.displayName,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                ),
                maxLines = 1
            )

            // Branch + last commit on one muted line — always shown
            val subtitle = listOfNotNull(
                repo.branchName?.takeIf { it.isNotBlank() },
                repo.lastCommitMsg?.takeIf { it.isNotBlank() }
            ).joinToString("  ")

            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}
