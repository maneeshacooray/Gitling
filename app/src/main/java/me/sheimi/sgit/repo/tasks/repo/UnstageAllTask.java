package me.sheimi.sgit.repo.tasks.repo;

import me.sheimi.sgit.R;
import me.sheimi.sgit.database.models.Repo;
import me.sheimi.sgit.exception.StopTaskException;
import me.sheimi.sgit.repo.tasks.SheimiAsyncTask.AsyncTaskPostCallback;

public class UnstageAllTask extends RepoOpTask {

    private AsyncTaskPostCallback mCallback;

    public UnstageAllTask(Repo repo, AsyncTaskPostCallback callback) {
        super(repo);
        mCallback = callback;
        setSuccessMsg(R.string.success_remove_from_stage);
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            // Mixed reset with no paths — equivalent to `git reset HEAD`, unstages everything.
            mRepo.getGit().reset().call();
        } catch (StopTaskException e) {
            return false;
        } catch (Throwable e) {
            setException(e);
            return false;
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean isSuccess) {
        super.onPostExecute(isSuccess);
        if (mCallback != null) {
            mCallback.onPostExecute(isSuccess);
        }
    }
}
