package me.sheimi.sgit.repo.tasks.repo;

import me.sheimi.sgit.R;
import me.sheimi.sgit.database.models.Repo;
import me.sheimi.sgit.exception.StopTaskException;
import me.sheimi.sgit.repo.tasks.SheimiAsyncTask.AsyncTaskPostCallback;

public class RemoveFromStageTask extends RepoOpTask {

    public String mFilePath;
    private AsyncTaskPostCallback mCallback;

    public RemoveFromStageTask(Repo repo, String filePath) {
        this(repo, filePath, null);
    }

    public RemoveFromStageTask(Repo repo, String filePath, AsyncTaskPostCallback callback) {
        super(repo);
        mFilePath = filePath;
        mCallback = callback;
        setSuccessMsg(R.string.success_remove_from_stage);
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        return removeFromStage();
    }

    protected void onPostExecute(Boolean isSuccess) {
        super.onPostExecute(isSuccess);
        if (mCallback != null) {
            mCallback.onPostExecute(isSuccess);
        }
    }

    public boolean removeFromStage() {
        try {
            mRepo.getGit().reset().addPath(mFilePath).call();
        } catch (StopTaskException e) {
            return false;
        } catch (Throwable e) {
            setException(e);
            return false;
        }
        return true;
    }
}
