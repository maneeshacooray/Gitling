package me.sheimi.sgit.fragments

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.database.DataSetObserver
import android.os.Bundle
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.manichord.mgit.MainActivity
import com.manichord.mgit.repodetail.CommitRowState
import com.manichord.mgit.repodetail.CommitsListContent
import com.manichord.mgit.ui.theme.AppTheme
import me.sheimi.android.activities.SheimiFragmentActivity.OnBackClickListener
import me.sheimi.sgit.R
import me.sheimi.sgit.adapters.CommitsListAdapter
import me.sheimi.sgit.database.models.Repo
import me.sheimi.sgit.dialogs.CheckoutDialog

class CommitsFragment : BaseFragment(), ActionMode.Callback {

    companion object {
        private const val IS_ACTION_MODE = "is action mode"
        private const val CHOSEN_ITEM = "chosen item"
        private const val FILE = "commit_file"

        @JvmStatic
        fun newInstance(repo: Repo, file: String?): CommitsFragment {
            val fragment = CommitsFragment()
            val bundle = Bundle()
            bundle.putSerializable(Repo.TAG, repo)
            file?.let { bundle.putString(FILE, it) }
            fragment.arguments = bundle
            return fragment
        }
    }

    private var commitsListAdapter: CommitsListAdapter? = null
    private var actionMode: ActionMode? = null
    private val chosenItems: MutableSet<Int> = mutableSetOf()
    private var repo: Repo? = null
    private var file: String? = null
    private lateinit var clipboard: ClipboardManager

    private var rows by mutableStateOf<List<CommitRowState>>(emptyList())
    private var allBranches by mutableStateOf(false)

    private fun refreshRowsFromAdapter() {
        val adapter = commitsListAdapter ?: return
        rows = (0 until adapter.count).map { i ->
            if (adapter.isProgressBar(i)) {
                CommitRowState.Loading
            } else {
                CommitRowState.Item(
                    position = i,
                    commit = adapter.getItem(i),
                    selected = chosenItems.contains(i)
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (rawActivity as? MainActivity)?.currentRepoDetailHost?.setCommitsFragment(this)

        val bundle = arguments ?: return ComposeView(requireContext())
        val repo = bundle.getSerializable(Repo.TAG) as? Repo ?: return ComposeView(requireContext())
        this.repo = repo
        file = bundle.getString(FILE)
        clipboard = rawActivity.getSystemService(Activity.CLIPBOARD_SERVICE) as ClipboardManager

        val adapter = CommitsListAdapter(rawActivity, chosenItems, repo, file)
        adapter.registerDataSetObserver(object : DataSetObserver() {
            override fun onChanged() = refreshRowsFromAdapter()
            override fun onInvalidated() = refreshRowsFromAdapter()
        })
        commitsListAdapter = adapter
        allBranches = adapter.isAllBranches
        reset()

        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
                    CommitsListContent(
                        rows = rows,
                        onItemClick = ::onItemClicked,
                        onItemLongClick = ::onItemLongClicked,
                        showBranchScopeToggle = adapter.supportsGraphMode(),
                        allBranches = allBranches,
                        onAllBranchesChange = { value ->
                            allBranches = value
                            commitsListAdapter?.setAllBranches(value)
                        }
                    )
                }
            }
        }
    }

    private fun onItemClicked(position: Int) {
        if (actionMode == null) {
            val newCommit = commitsListAdapter?.getItem(position) ?: return
            showDiff(null, null, newCommit.name, true)
            return
        }
        chooseItem(position)
    }

    private fun onItemLongClicked(position: Int) {
        if (actionMode == null) {
            enterDiffActionMode()
        }
        chooseItem(position)
    }

    fun setFilter(query: String?) {
        commitsListAdapter?.setFilter(query)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState == null) return
        val isActionMode = savedInstanceState.getBoolean(IS_ACTION_MODE)
        if (isActionMode) {
            val itemsInt = savedInstanceState.getIntegerArrayList(CHOSEN_ITEM)
            actionMode = rawActivity.startActionMode(this)
            itemsInt?.let { chosenItems.addAll(it) }
            commitsListAdapter?.notifyDataSetChanged()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(IS_ACTION_MODE, actionMode != null)
        outState.putIntegerArrayList(CHOSEN_ITEM, ArrayList(chosenItems))
    }

    override fun getOnBackClickListener(): OnBackClickListener? = null

    override fun reset() {
        commitsListAdapter?.resetCommit()
    }

    fun enterDiffActionMode() {
        actionMode = rawActivity.startActionMode(this)
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        val inflater: MenuInflater = mode.menuInflater
        inflater.inflate(R.menu.action_mode_commit_diff, menu)
        mode.setTitle(R.string.action_mode_diff)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = true

    private fun showDiff(actionMode: ActionMode?, oldCommit: String?, newCommit: String, showDescription: Boolean) {
        actionMode?.finish()
        (rawActivity as MainActivity).openCommitDiff(oldCommit, newCommit, showDescription, repo)
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        val adapter = commitsListAdapter ?: return false
        when (item.itemId) {
            R.id.action_mode_diff -> {
                val items = chosenItems.toTypedArray()
                if (items.isEmpty()) {
                    showToastMessage(R.string.alert_no_items_selected)
                    return true
                }
                val item1 = items[0]
                val item2: Int
                if (items.size == 1) {
                    item2 = item1 + 1
                    if (item2 == adapter.count) {
                        showToastMessage(R.string.alert_no_older_commits)
                        return true
                    }
                } else {
                    item2 = items[1]
                }
                val smaller = minOf(item1, item2)
                val larger = maxOf(item1, item2)
                val oldCommit = adapter.getItem(larger).name
                val newCommit = adapter.getItem(smaller).name
                showDiff(mode, oldCommit, newCommit, false)
                return true
            }
            R.id.action_mode_copy_commit -> {
                if (chosenItems.size != 1) {
                    showToastMessage(R.string.alert_you_must_choose_one_commit_to_copy)
                    return true
                }
                val commit = adapter.getItem(chosenItems.iterator().next()).name
                clipboard.setPrimaryClip(ClipData.newPlainText("commit_to_copy", commit))
                showToastMessage(R.string.msg_commit_str_has_copied)
                mode.finish()
                return true
            }
            R.id.action_mode_checkout -> {
                val commit = adapter.getItem(chosenItems.iterator().next()).name
                val pathArg = Bundle()
                pathArg.putString(CheckoutDialog.BASE_COMMIT, commit)
                pathArg.putSerializable(Repo.TAG, repo)
                mode.finish()
                val ckd = CheckoutDialog()
                ckd.arguments = pathArg
                ckd.show(parentFragmentManager, "rename-dialog")
            }
        }
        return false
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        actionMode = null
        chosenItems.clear()
        commitsListAdapter?.notifyDataSetChanged()
    }

    private fun chooseItem(position: Int) {
        val adapter = commitsListAdapter ?: return
        if (adapter.isProgressBar(position)) return
        if (chosenItems.contains(position)) {
            chosenItems.remove(position)
            adapter.notifyDataSetChanged()
            return
        }
        if (chosenItems.size >= 2) {
            showToastMessage(R.string.alert_choose_two_items)
            return
        }
        chosenItems.add(position)
        adapter.notifyDataSetChanged()
    }
}
