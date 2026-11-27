package me.sheimi.sgit.repo.tasks.repo;

import androidx.annotation.StringRes;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.NotSupportedException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.transport.URIish;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Locale;

import com.manichord.mgit.transport.RemoteObjectFormat;
import com.manichord.mgit.transport.SslTrustStore;
import me.sheimi.android.activities.SheimiFragmentActivity;
import me.sheimi.android.utils.BasicFunctions;
import me.sheimi.android.utils.Profile;
import me.sheimi.sgit.R;
import me.sheimi.sgit.database.RepoContract;
import me.sheimi.sgit.database.models.Repo;
import me.sheimi.sgit.ssh.SgitTransportCallback;
import timber.log.Timber;

public class CloneTask extends RepoRemoteOpTask {

    /** Depth for a shallow clone: just the latest commit on each branch. */
    public static final int SHALLOW_DEPTH = 1;

    private final AsyncTaskCallback mCallback;
    private final boolean mCloneRecursive;
    private final String mCloneStatusName;
    /** 0 for a full clone, > 0 for a shallow clone of that depth. */
    private final int mDepth;
    /** Set when a full clone ran out of memory, so the user can be offered a shallow retry
     * instead of just a generic error (see {@link #onPostExecute}). */
    private boolean mOfferShallowRetry;
    /** Set when the clone failed because the server's certificate could not be trusted, so the user
     * can be offered a "trust this server" retry instead of a dead-end SSL error (#56). */
    private boolean mOfferSslTrustRetry;

    public CloneTask(Repo repo, boolean cloneRecursive, String statusName, AsyncTaskCallback callback) {
        this(repo, cloneRecursive, statusName, callback, 0, false);
    }

    public CloneTask(Repo repo, boolean cloneRecursive, String statusName, AsyncTaskCallback callback,
            int depth) {
        this(repo, cloneRecursive, statusName, callback, depth, false);
    }

    public CloneTask(Repo repo, boolean cloneRecursive, String statusName, AsyncTaskCallback callback,
            int depth, boolean trustSsl) {
        super(repo);
        mCloneRecursive = cloneRecursive;
        mCloneStatusName = statusName;
        mCallback = callback;
        mDepth = depth;
        mTrustSsl = trustSsl;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // Show the in-progress indicator the instant the clone starts. JGit's first progress
        // callback only arrives after connecting and negotiation, which on a large or slow repo
        // can take several seconds, and without a status the repo card looks idle (as if nothing
        // is happening) the whole time, including while a too-large repo is heading for its
        // out-of-memory. A non-empty status makes the card show the loading indicator right away;
        // the real progress text replaces it once JGit starts reporting.
        SheimiFragmentActivity activity = BasicFunctions.getActiveActivity();
        if (activity != null) {
            mRepo.updateStatus(activity.getString(R.string.cloning));
        }
    }

    @Override
    protected Boolean doInBackground(Void... v) {
        boolean result = cloneRepo();
        if (!result) {
            Timber.e("del repo. clone failed");
            mRepo.deleteRepoSync();
        } else if (mCallback != null) {
            result = mCallback.doInBackground(v) & result;
        }
        return result;
    }

    protected void onPostExecute(Boolean isSuccess) {
        if (!isSuccess && mOfferSslTrustRetry && !isTaskCanceled()) {
            // The server's certificate could not be verified (self-signed / untrusted CA). Instead
            // of a dead-end SSL error, offer to trust this server and retry.
            mRepo.removeTask(this);
            promptSslTrustRetry();
            return;
        }
        if (!isSuccess && mOfferShallowRetry && !isTaskCanceled()) {
            // A full clone ran out of memory. Instead of the base class's generic error, offer to
            // retry as a shallow clone, which downloads far fewer objects and usually fits.
            mRepo.removeTask(this);
            promptShallowRetry();
            return;
        }
        super.onPostExecute(isSuccess);
        if (isTaskCanceled()) {
            return;
        }
        if (isSuccess) {
            mRepo.updateLatestCommitInfo();
            mRepo.updateStatus(RepoContract.REPO_STATUS_NULL);
        }
    }

    private void promptShallowRetry() {
        SheimiFragmentActivity activity =
                (SheimiFragmentActivity) BasicFunctions.getActiveActivity();
        if (activity == null) {
            return;
        }
        activity.showMessageDialog(R.string.dialog_clone_too_large_title,
                activity.getString(R.string.dialog_clone_too_large_msg),
                R.string.label_clone_shallow,
                (dialog, which) -> newTaskWithDepth(SHALLOW_DEPTH).executeTask());
    }

    private void promptSslTrustRetry() {
        SheimiFragmentActivity activity =
                (SheimiFragmentActivity) BasicFunctions.getActiveActivity();
        if (activity == null) {
            return;
        }
        activity.showMessageDialog(R.string.dialog_ssl_untrusted_title,
                activity.getString(R.string.dialog_ssl_untrusted_msg),
                R.string.label_trust_server,
                (dialog, which) -> {
                    trustRemoteHost();
                    newTaskTrustingSsl().executeTask();
                });
    }

    /** Remember the remote's host as user-trusted, so the retry (and later fetch/pull/push) skip
     * certificate verification for it. */
    private void trustRemoteHost() {
        try {
            SslTrustStore.trust(new URIish(mRepo.getRemoteURL()).getHost());
        } catch (URISyntaxException e) {
            Timber.w(e, "Could not parse remote URL to trust its host");
        }
    }

    public boolean cloneRepo() {
        File localRepo = mRepo.getDir();
        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(mRepo.getRemoteURL()).setCloneAllBranches(true)
                .setProgressMonitor(new RepoCloneMonitor())
                .setTransportConfigCallback(new SgitTransportCallback())
                .setDirectory(localRepo)
                .setCloneSubmodules(mCloneRecursive);

        if (mDepth > 0) {
            cloneCommand.setDepth(mDepth);
        }

        setCredentials(cloneCommand);

        try {
            cloneCommand.call();
            Profile.setLastCloneSuccess();
        } catch (InvalidRemoteException e) {
            setError(R.string.error_invalid_remote);
            Profile.setLastCloneFailed(mRepo);
            return false;
        } catch (TransportException e) {
            Profile.setLastCloneFailed(mRepo);
            if (!mTrustSsl && isSslTrustError(e)) {
                // Self-signed or otherwise untrusted certificate. Offer a "trust this server"
                // retry (see onPostExecute) rather than the generic auth/error path (#56).
                setException(e);
                mOfferSslTrustRetry = true;
            } else if (isLikelyObjectFormatError(e)
                    && RemoteObjectFormat.isSha256Remote(mRepo.getRemoteURL())) {
                // JGit can't read SHA-256 repos yet and fails parsing the ref advertisement with a
                // cryptic error ("Short read of block", or "Invalid ref advertisement line" with a
                // 64-hex id). Confirmed via the remote's advertisement, show a clear message instead
                // (#126; eclipse-jgit/jgit#73).
                setError(R.string.error_sha256_unsupported);
            } else {
                setException(e);
                handleAuthError(this);
            }
            return false;
        } catch (GitAPIException e) {
            setException(e, R.string.error_clone_failed);
            return false;
        } catch (JGitInternalException e) {
            // not supported when unsupported git remotehttp://asdgfkas URI
            if (e.getCause() instanceof NotSupportedException) {
                setError(R.string.error_invalid_remote);
            } else {
                setException(e);
            }
            return false;
        } catch (OutOfMemoryError e) {
            // A full clone can exhaust memory on a large repo. Offer a shallow retry rather than
            // a dead-end error; a shallow clone that still OOMs falls back to the plain message.
            if (mDepth <= 0) {
                mOfferShallowRetry = true;
            } else {
                setException(e, R.string.error_out_of_memory);
            }
            Profile.setLastCloneFailed(mRepo);
            return false;
        } catch (Throwable e) {
            setException(e);
            return false;
        }
        return true;
    }

    @Override
    public void cancelTask() {
        super.cancelTask();
        mRepo.deleteRepo();
    }

    @Override
    public RepoRemoteOpTask getNewTask() {
        // An auth retry keeps whatever depth this clone used.
        return newTaskWithDepth(mDepth);
    }

    private CloneTask newTaskWithDepth(int depth) {
        return newTask(depth, mTrustSsl);
    }

    /** Retry keeping this clone's depth but now trusting the server's certificate (#56). */
    private CloneTask newTaskTrustingSsl() {
        return newTask(mDepth, true);
    }

    private CloneTask newTask(int depth, boolean trustSsl) {
        // need to call create repo again as when clone fails (auth error, OOM, or SSL), the repo
        // initially created gets deleted
        String userName = mRepo.getUsername();
        String password = mRepo.getPassword();
        mRepo = Repo.createRepo(mRepo.getLocalPath(), mRepo.getRemoteURL(), mCloneStatusName);
        mRepo.setUsername(userName);
        mRepo.setPassword(password);
        return new CloneTask(mRepo, mCloneRecursive, mCloneStatusName, mCallback, depth, trustSsl);
    }

    @Override
    @StringRes
    public int getErrorTitleRes() {
        return R.string.error_clone_failed;
    }

    public class RepoCloneMonitor implements ProgressMonitor {

        private int mTotalWork;
        private int mWorkDone;
        private int mLastProgress;
        private String mTitle;

        private void publishProgressInner() {
            String status = "";
            String percent = "";
            if (mTitle != null) {
                status = String.format(Locale.getDefault(), "%s ... ", mTitle);
                percent = "0%";
            }
            if (mTotalWork != 0) {
                int p = 100 * mWorkDone / mTotalWork;
                if (p - mLastProgress < 1) {
                    return;
                }
                mLastProgress = p;
                percent = String.format(Locale.getDefault(), "(%d%%)", p);
            }
            mRepo.updateStatus(status + percent);
        }

        @Override
        public void start(int totalTasks) {
            publishProgressInner();
        }

        @Override
        public void beginTask(String title, int totalWork) {
            mTotalWork = totalWork;
            mWorkDone = 0;
            mLastProgress = 0;
            mTitle = title;
            publishProgressInner();
        }

        @Override
        public void update(int i) {
            mWorkDone += i;
            publishProgressInner();
        }

        @Override
        public void endTask() {
        }

        @Override
        public boolean isCancelled() {
            return isTaskCanceled();
        }

        @Override
        public void showDuration(boolean enabled) {
        }

    }

}
