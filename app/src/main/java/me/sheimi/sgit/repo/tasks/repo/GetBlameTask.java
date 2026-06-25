package me.sheimi.sgit.repo.tasks.repo;

import me.sheimi.sgit.database.models.Repo;
import me.sheimi.sgit.exception.StopTaskException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;

public class GetBlameTask extends RepoOpTask {

    private GetBlameCallback mCallback;
    private BlameResult mResult;
    private String mFile;

    public static interface GetBlameCallback {
        public void postBlame(BlameResult result);
    }

    public GetBlameTask(Repo repo, String file, GetBlameCallback callback) {
        super(repo, false);
        mFile = file;
        mCallback = callback;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            mResult = mRepo.getGit().blame().setFilePath(mFile).setFollowFileRenames(true).call();
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

    protected void onPostExecute(Boolean isSuccess) {
        super.onPostExecute(isSuccess);
        if (mCallback != null) {
            mCallback.postBlame(mResult);
        }
    }

}
