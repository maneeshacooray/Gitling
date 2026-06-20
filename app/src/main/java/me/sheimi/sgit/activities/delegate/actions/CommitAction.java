package me.sheimi.sgit.activities.delegate.actions;

import me.sheimi.sgit.activities.RepoDetailActivity;
import me.sheimi.sgit.database.models.Repo;
import me.sheimi.sgit.dialogs.CommitDialog;

public class CommitAction extends RepoAction {

    public CommitAction(Repo repo, RepoDetailActivity activity) {
        super(repo, activity);
    }

    @Override
    public void execute() {
        CommitDialog.show(mActivity.findViewById(android.R.id.content), mRepo, mActivity);
        mActivity.closeOperationDrawer();
    }

}
