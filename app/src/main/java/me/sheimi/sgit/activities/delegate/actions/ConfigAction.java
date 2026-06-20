package me.sheimi.sgit.activities.delegate.actions;

import me.sheimi.sgit.activities.RepoDetailActivity;
import me.sheimi.sgit.database.models.GitConfig;
import me.sheimi.sgit.database.models.Repo;
import me.sheimi.sgit.exception.StopTaskException;
import timber.log.Timber;

/**
 * Action to display configuration for a Repo
 */
public class ConfigAction extends RepoAction {


    public ConfigAction(Repo repo, RepoDetailActivity activity) {
        super(repo, activity);
    }

    @Override
    public void execute() {
        try {
            GitConfig gitConfig = new GitConfig(mRepo);
            com.manichord.mgit.dialogs.RepoConfigDialog.show(
                    mActivity.findViewById(android.R.id.content), gitConfig);
        } catch (StopTaskException e) {
            //FIXME: show error to user
            Timber.e(e);
        }
    }

}
