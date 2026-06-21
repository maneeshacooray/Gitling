package me.sheimi.sgit.fragments;

import com.manichord.mgit.MainActivity;

import me.sheimi.sgit.activities.RepoDetailActivity;

public abstract class RepoDetailFragment extends BaseFragment {

    /**
     * RepoDetailActivity is no longer a real Activity (see the single-activity rewrite), so it
     * can't be reached by casting the Fragment's hosting context directly (and this can't be
     * named/typed as an override of getRawActivity() since RepoDetailActivity is no longer a
     * SheimiFragmentActivity at all) -- it's looked up via the one real Activity
     * (MainActivity)'s currentRepoDetailHost instead.
     */
    public RepoDetailActivity getRepoDetailActivity() {
        return ((MainActivity) getRawActivity()).getCurrentRepoDetailHost();
    }

}
