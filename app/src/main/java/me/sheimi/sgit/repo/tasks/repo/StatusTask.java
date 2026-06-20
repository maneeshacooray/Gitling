package me.sheimi.sgit.repo.tasks.repo;

import me.sheimi.sgit.database.models.Repo;
import me.sheimi.sgit.exception.StopTaskException;

import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;

public class StatusTask extends RepoOpTask {

    public interface GetStatusCallback {
        public void postStatus(Status status);
    }

    private GetStatusCallback mCallback;
    private Status mStatus;

    public StatusTask(Repo repo, GetStatusCallback callback) {
        super(repo, false);
        mCallback = callback;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        return status();
    }

    protected void onPostExecute(Boolean isSuccess) {
        super.onPostExecute(isSuccess);
        if (mCallback != null && isSuccess) {
            mCallback.postStatus(mStatus);
        }
    }

    public void executeTask() {
        super.executeTask();
    }

    private boolean status() {
        try {
            mStatus = mRepo.getGit().status().call();
        } catch (NoWorkTreeException e) {
            setException(e);
            return false;
        } catch (GitAPIException e) {
            setException(e);
            return false;
        } catch (StopTaskException e) {
            return false;
        } catch (Throwable e) {
            setException(e);
            return false;
        }
        return true;
    }

}
