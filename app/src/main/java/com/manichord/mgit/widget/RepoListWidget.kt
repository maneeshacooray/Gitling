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
    val branchName: String?,
    val lastCommitMsg: String?,
    val isDirty: Boolean,
    val isPinned: Boolean
)

class RepoListWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repos = loadRepoEntries(context)
        provideContent {
            RepoWidgetContent(repos = repos)
        }
    }

    companion object {
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
                val branch = try { repo.getBranchName().removePrefix("refs/heads/") } catch (_: Exception) { null }
                RepoWidgetEntry(
                    id = repo.getID(),
                    displayName = repo.getDiaplayName() ?: "Unknown",
                    branchName = branch,
                    lastCommitMsg = repo.getLastCommitMsg(),
                    isDirty = isDirty,
                    isPinned = repo.isPinned
                )
            }.sortedWith(compareByDescending<RepoWidgetEntry> { it.isPinned }.thenBy { it.displayName })
        }
    }
}
