package me.sheimi.sgit.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.manichord.mgit.repodetail.StatusScreen
import com.manichord.mgit.ui.theme.AppTheme
import me.sheimi.android.activities.SheimiFragmentActivity.OnBackClickListener
import me.sheimi.sgit.activities.CommitDiffActivity
import me.sheimi.sgit.database.models.Repo
import me.sheimi.sgit.repo.tasks.repo.StatusTask

class StatusFragment : RepoDetailFragment() {

    companion object {
        @JvmStatic
        fun newInstance(repo: Repo): StatusFragment {
            val fragment = StatusFragment()
            val bundle = Bundle()
            bundle.putSerializable(Repo.TAG, repo)
            fragment.arguments = bundle
            return fragment
        }
    }

    private var repo: Repo? = null
    private var isLoading by mutableStateOf(true)
    private var statusText by mutableStateOf("")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rawActivity.setStatusFragment(this)

        repo = (arguments?.getSerializable(Repo.TAG) as? Repo)
            ?: (savedInstanceState?.getSerializable(Repo.TAG) as? Repo)
        val repo = repo ?: return ComposeView(requireContext())
        this.repo = repo

        reset()

        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
                    StatusScreen(
                        isLoading = isLoading,
                        statusText = statusText,
                        onUnstagedDiffClick = { showDiff("dircache", "filetree") },
                        onStagedDiffClick = { showDiff("HEAD", "dircache") }
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(Repo.TAG, repo)
    }

    private fun showDiff(oldCommit: String, newCommit: String) {
        val intent = Intent(rawActivity, CommitDiffActivity::class.java)
        intent.putExtra(CommitDiffActivity.OLD_COMMIT, oldCommit)
        intent.putExtra(CommitDiffActivity.NEW_COMMIT, newCommit)
        intent.putExtra(CommitDiffActivity.SHOW_DESCRIPTION, false)
        intent.putExtra(Repo.TAG, repo)
        rawActivity.startActivity(intent)
    }

    override fun reset() {
        val repo = repo ?: return
        isLoading = true
        val task = StatusTask(repo) { result ->
            statusText = result
            isLoading = false
        }
        task.executeTask()
    }

    override fun getOnBackClickListener(): OnBackClickListener? = null
}
