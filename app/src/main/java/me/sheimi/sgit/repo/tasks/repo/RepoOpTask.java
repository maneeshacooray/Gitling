package me.sheimi.sgit.repo.tasks.repo;

import me.sheimi.sgit.activities.RepoDetailActivity;
import com.manichord.mgit.MainActivity;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import java.util.Locale;

import com.manichord.mgit.auth.RepoCredentials;
import me.sheimi.android.activities.SheimiFragmentActivity.OnPasswordEntered;
import me.sheimi.android.utils.BasicFunctions;
import me.sheimi.sgit.R;
import me.sheimi.sgit.database.models.Repo;
import me.sheimi.sgit.repo.tasks.SheimiAsyncTask;
import timber.log.Timber;

public abstract class RepoOpTask extends SheimiAsyncTask<Void, String, Boolean> {

    protected Repo mRepo;
    protected boolean mIsTaskAdded;
    private int mSuccessMsg = 0;

    public RepoOpTask(Repo repo) {
        this(repo, true);
    }

    /**
     * @param requiresExclusiveAccess whether this task needs the repo's single-task-at-a-time
     *                                slot (see Repo.addTask/removeTask) -- true for anything that
     *                                writes to the repo (push/pull/commit/checkout/rebase/etc, the
     *                                default), false for read-only tasks like StatusTask/
     *                                GetCommitTask/CommitDiffTask, which would otherwise contend
     *                                with each other (or with repo-open scaffolding) for no real
     *                                safety benefit -- e.g. switching tabs quickly after opening a
     *                                repo could silently drop a tab's data load with only a toast
     *                                ("A task for this repo is already running") as a clue, leaving
     *                                that tab's loading spinner stuck forever.
     */
    public RepoOpTask(Repo repo, boolean requiresExclusiveAccess) {
        mRepo = repo;
        mIsTaskAdded = requiresExclusiveAccess ? repo.addTask(this) : true;
    }

    protected void onPostExecute(Boolean isSuccess) {
        super.onPostExecute(isSuccess);
        mRepo.removeTask(this);
        if (!isSuccess && !isTaskCanceled()) {
            if (mException == null) {
                if (mErrorRes != 0) {
                    // showError takes the title first; passing mErrorRes first put the specific
                    // message in the dialog title and "Error occurred" in its body
                    BasicFunctions.showError(BasicFunctions.getActiveActivity(), getErrorTitleRes(), mErrorRes);
                } else {
                    // No specific message: show the task's error title as the body under the
                    // default title, so the dialog isn't left with an empty body
                    BasicFunctions.showError(BasicFunctions.getActiveActivity(), 0, getErrorTitleRes());
                }
            } else {
                // Title first, like showError. The reversed order showed "Error occurred" twice
                // (as the title and as the first line of the body) and put a specific message
                // such as "Pull failed" in the title; a 0 message is skipped by ErrorDialog
                BasicFunctions.showException(BasicFunctions.getActiveActivity(), mException,
                        getErrorTitleRes(), mErrorRes);
            }
        }
        if (isSuccess && mSuccessMsg != 0) {
            BasicFunctions.getActiveActivity().showToastMessage(mSuccessMsg);
        }        recountSyncCounts();
    }

    /** Any task can change what the repo screen's toolbar counts (changed files, commits to push
     * or pull), including file operations that don't refresh the screen themselves. */
    private void recountSyncCounts() {
        if (!(BasicFunctions.getActiveActivity() instanceof MainActivity)) return;
        RepoDetailActivity host = ((MainActivity) BasicFunctions.getActiveActivity()).getCurrentRepoDetailHost();
        if (host != null && host.getRepo().getID() == mRepo.getID()) {
            host.getViewModel().refreshSyncCounts(mRepo);
        }
    }

    protected void setSuccessMsg(int successMsg) {
        mSuccessMsg = successMsg;
    }

    public void executeTask() {
        if (mIsTaskAdded) {
            super.executeTask();
            return;
        }
        BasicFunctions.getActiveActivity().showToastMessage(
                R.string.error_task_running);
    }

    /** Convenience overload for operations with exactly one remote in play (clone), where
     * mRepo.getRemoteURL() (the repo's own stored URL) already is that remote's URL. Anything
     * that operates against a specific named remote on a repo that can have several (push,
     * pull, fetch) must go through {@link #setCredentials(TransportCommand, String)} instead --
     * see its doc comment for why. */
    protected void setCredentials(TransportCommand command) {
        setCredentials(command, mRepo.getRemoteURL());
    }

    /** The remote URL credentials were last resolved for, so credentials typed into the password
     * prompt after an auth failure are remembered for that remote's host (see RepoRemoteOpTask). */
    protected String mCredentialRemoteUrl;

    /** Credentials are resolved per remote host (connected account first, then this repo's
     * credentials for that host), so a repo with e.g. a GitHub and a GitLab remote sends each
     * host its own credentials (#59, #87). See RepoCredentials for the full lookup order. */
    protected void setCredentials(TransportCommand command, String remoteUrl) {
        mCredentialRemoteUrl = remoteUrl;
        RepoCredentials.Credential credential = RepoCredentials.INSTANCE.resolve(mRepo, remoteUrl);
        if (credential != null) {
            command.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
                    credential.getUsername(), credential.getPassword()));
        } else {
            Timber.d("no CredentialsProvider when no username/password provided");
        }
    }

    protected void handleAuthError(OnPasswordEntered onPassEntered) {
        String msg = mException.getMessage();
        Timber.w("clone Auth error: %s", msg);

        if (msg == null || ((!msg.contains("Auth fail"))
                && (!msg.toLowerCase(Locale.ROOT).contains("auth")))) {
            return;
        }

        String errorInfo = null;
        if (msg.contains("Auth fail")) {
            errorInfo = BasicFunctions.getActiveActivity().getString(
                    R.string.dialog_prompt_for_password_title_auth_fail);
        }
        BasicFunctions.getActiveActivity().promptForPassword(onPassEntered,
                errorInfo);
    }

    class BasicProgressMonitor implements ProgressMonitor {

        private int mTotalWork;
        private int mWorkDone;
        private int mLastProgress;
        private String mTitle;

        @Override
        public void start(int i) {
        }

        @Override
        public void beginTask(String title, int totalWork) {
            mTotalWork = totalWork;
            mWorkDone = 0;
            mLastProgress = 0;
            if (title != null) {
                mTitle = title;
            }
            setProgress();
        }

        @Override
        public void update(int i) {
            mWorkDone += i;
            if (mTotalWork != ProgressMonitor.UNKNOWN && mTotalWork != 0 && mTotalWork - mLastProgress >= 1) {
                setProgress();
                mLastProgress = mWorkDone;
            }
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

        private void setProgress() {
            String msg = mTitle;
            int showedWorkDown = Math.min(mWorkDone, mTotalWork);
            int progress = 0;
            String rightHint = "0/0";
            String leftHint = "0%";
            if (mTotalWork != 0) {
                progress = 100 * showedWorkDown / mTotalWork;
                rightHint = showedWorkDown + "/" + mTotalWork;
                leftHint = progress + "%";
            }
            publishProgress(msg, leftHint, rightHint,
                    Integer.toString(progress));
        }

    }

}
