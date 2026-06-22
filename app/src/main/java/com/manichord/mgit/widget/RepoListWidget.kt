package com.manichord.mgit.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.sheimi.sgit.database.RepoDbManager
import me.sheimi.sgit.database.models.Repo
import timber.log.Timber

data class RepoWidgetEntry(
    val id: Int,
    val displayName: String,
    val lastCommitMsg: String?,
    val isDirty: Boolean
)

class RepoListWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repos = loadRepoEntries(context)
        provideContent {
            RepoWidgetContent(repos = repos)
        }
    }

    companion object {
        // Dirty/clean is computed from local .git state only (same call StatusFragment already
        // uses) -- no network round-trip, safe to run on whatever triggers this (periodic system
        // update or a manual refresh tap).
        suspend fun loadRepoEntries(context: Context): List<RepoWidgetEntry> = withContext(Dispatchers.IO) {
            val repos = Repo.getRepoList(context, RepoDbManager.queryAllRepo())
            repos.map { repo ->
                val isDirty = try {
                    val status = repo.git?.status()?.call()
                    status != null && (status.hasUncommittedChanges() || !status.isClean)
                } catch (e: Exception) {
                    Timber.w(e, "Widget: failed to read status for %s", repo.getDiaplayName())
                    false
                }
                RepoWidgetEntry(
                    id = repo.getID(),
                    displayName = repo.getDiaplayName() ?: "Unknown repository",
                    lastCommitMsg = repo.getLastCommitMsg(),
                    isDirty = isDirty
                )
            }
        }
    }
}
