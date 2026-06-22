package com.manichord.mgit.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
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

@Composable
fun RepoWidgetContent(repos: List<RepoWidgetEntry>) {
    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(8.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = "Gitling",
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    text = "⟳", // refresh glyph -- no icon asset needed for a single tap target
                    style = TextStyle(color = GlanceTheme.colors.primary, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier
                        .padding(4.dp)
                        .clickable(actionRunCallback<RefreshWidgetAction>())
                )
            }
            Spacer(modifier = GlanceModifier.size(4.dp))
            if (repos.isEmpty()) {
                Text(
                    text = "No repos cloned yet",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
                )
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
private fun RepoWidgetRow(repo: RepoWidgetEntry) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(
                actionRunCallback<OpenRepoWidgetAction>(
                    actionParametersOf(RepoIdParamKey to repo.id)
                )
            ),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .size(8.dp)
                .background(if (repo.isDirty) GlanceTheme.colors.error else GlanceTheme.colors.primary)
        ) {}
        Spacer(modifier = GlanceModifier.width(8.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = repo.displayName,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Medium),
                maxLines = 1
            )
            if (!repo.lastCommitMsg.isNullOrBlank()) {
                Text(
                    text = repo.lastCommitMsg,
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                    maxLines = 1
                )
            }
        }
    }
}
