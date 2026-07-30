package me.sheimi.sgit.repo.tasks.repo;

import java.io.File;

import me.sheimi.android.activities.SheimiFragmentActivity;
import me.sheimi.android.utils.BasicFunctions;
import me.sheimi.android.utils.FsUtils;
import me.sheimi.sgit.R;
import me.sheimi.sgit.database.models.Repo;
import me.sheimi.sgit.exception.StopTaskException;
import me.sheimi.sgit.repo.tasks.SheimiAsyncTask.AsyncTaskPostCallback;

public class AddToStageTask extends RepoOpTask {

    public String mFilePattern;
    private AsyncTaskPostCallback mCallback;
    private final boolean mIsMediaPermissionRetry;

    public AddToStageTask(Repo repo, String filepattern) {
        this(repo, filepattern, null);
    }

    public AddToStageTask(Repo repo, String filepattern, AsyncTaskPostCallback callback) {
        this(repo, filepattern, callback, false);
    }

    /** @param isMediaPermissionRetry true only for the single retry instance
     * retryAfterMediaPermission constructs -- caps that retry at one attempt (see there for why
     * a hard cap, not just checking the permission again, is required). */
    private AddToStageTask(Repo repo, String filepattern, AsyncTaskPostCallback callback,
                           boolean isMediaPermissionRetry) {
        super(repo);
        mFilePattern = filepattern;
        mCallback = callback;
        mIsMediaPermissionRetry = isMediaPermissionRetry;
        setSuccessMsg(R.string.success_add_to_stage);
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        return addToStage();
    }

    protected void onPostExecute(Boolean isSuccess) {
        super.onPostExecute(isSuccess);
        if (mCallback != null) {
            mCallback.onPostExecute(isSuccess);
        }
    }

    public boolean addToStage() {
        try {
            mRepo.getGit().add().addFilepattern(mFilePattern).call();
        } catch (StopTaskException e) {
            return false;
        } catch (Throwable e) {
            File eaccesFile = FsUtils.findEaccesFile(e);
            if (eaccesFile == null) {
                setException(e);
            } else if (mIsMediaPermissionRetry || !retryAfterMediaPermission(e, eaccesFile)) {
                setException(FsUtils.wrapEaccesFile(e, eaccesFile));
            }
            // else: a permission prompt is in flight and will silently retry via a fresh task on
            // its own if granted -- this attempt's own error dialog is suppressed below so it
            // doesn't flash up alongside (or after) a retry that's about to succeed.
            return false;
        }
        return true;
    }

    /** See FsUtils.findEaccesFile -- asks for the runtime permission the EACCES was actually
     * about, then (if granted) reruns this task from scratch, mirroring how RepoRemoteOpTask
     * retries after an auth prompt. Returns whether a request was actually kicked off (false if
     * there's no active activity to ask from, in which case the caller shows the normal error
     * dialog instead).
     *
     * <p>The retry is capped at exactly one attempt (see mIsMediaPermissionRetry) rather than
     * re-checking the permission indefinitely -- confirmed on-device that after certain photo
     * picker choices (e.g. "Don't select more" without picking this specific file), Android
     * reports READ_MEDIA_IMAGES as granted via checkSelfPermission while MediaProvider's own
     * per-file check still denies this exact file, which would otherwise retry forever. */
    private boolean retryAfterMediaPermission(final Throwable cause, final File eaccesFile) {
        SheimiFragmentActivity activity = BasicFunctions.getActiveActivity();
        if (activity == null) {
            return false;
        }
        // Suppresses this attempt's own error dialog (see RepoOpTask#onPostExecute) -- its
        // outcome is superseded by the fresh task the permission callback below starts, or by
        // the error dialog onDenied() raises directly if the user declines.
        cancelTask();
        // Frees the repo's exclusive-task slot right now rather than waiting for this task's own
        // onPostExecute (which normally does this) to get posted and run -- when the permission
        // is already granted, requestMediaImagesPermission below calls onGranted() immediately,
        // still nested inside this very call stack, and the new AddToStageTask it constructs
        // would otherwise find the slot still held by this one and refuse to start ("A task for
        // this repo is already running").
        mRepo.removeTask(this);
        activity.requestMediaImagesPermission(new SheimiFragmentActivity.OnPermissionResult() {
            @Override
            public void onGranted() {
                new AddToStageTask(mRepo, mFilePattern, mCallback, true).executeTask();
            }

            @Override
            public void onDenied() {
                SheimiFragmentActivity activity = BasicFunctions.getActiveActivity();
                if (activity != null) {
                    BasicFunctions.showException(activity, FsUtils.wrapEaccesFile(cause, eaccesFile),
                            0, R.string.dialog_error_title);
                }
            }
        });
        return true;
    }
}
