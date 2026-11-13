package me.sheimi.android.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.manichord.mgit.documents.BlockedFileRepair;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.utils.StorageUtils;

import java.io.File;
import java.util.Locale;

import me.sheimi.android.avatar.AvatarDownloader;
import me.sheimi.android.utils.BasicFunctions;
import me.sheimi.android.utils.FsUtils;
import me.sheimi.android.utils.Profile;
import me.sheimi.sgit.R;
import me.sheimi.sgit.database.models.Repo;
import me.sheimi.sgit.dialogs.DummyDialogListener;

public class SheimiFragmentActivity extends AppCompatActivity {

    public static interface OnBackClickListener {
        public boolean onClick();
    }

    private boolean mLastUseDynamicColor;
    private String mLastAppFont;
    private String mLastAppLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BasicFunctions.setActiveActivity(this);
        setTheme(getThemeResource());
        updateLocale(Profile.getAppLanguage(getApplicationContext()));
        mLastUseDynamicColor = Profile.useDynamicColor(getApplicationContext());
        mLastAppFont = Profile.getAppFont(getApplicationContext());
        mLastAppLanguage = Profile.getAppLanguage(getApplicationContext());

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    protected int getThemeResource() {
        return Profile.getThemeResource(getApplicationContext());
    }

    private void updateLocale(String languageTag) {
        final Locale locale;
        if (languageTag == null || languageTag.isEmpty()) {
            locale = Locale.getDefault();
        } else {
            locale = Locale.forLanguageTag(languageTag);
        }
        final Resources r = getResources();
        final DisplayMetrics dm = r.getDisplayMetrics();
        final Configuration c = r.getConfiguration();
        if (c.locale == null || !c.locale.equals(locale)) {
            c.locale = locale;
            r.updateConfiguration(c, dm);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        BasicFunctions.setActiveActivity(this);

        // Compose screens read theme preferences (dynamic color, app font) once at
        // composition time via AppTheme's default parameters -- they're not reactive, so an
        // Activity left alive in the back stack while the user changes a theme setting
        // elsewhere (e.g. in Settings) won't pick up the change on its own. Recreating here
        // is the standard fix: cheap, and only triggers when something actually changed.
        boolean useDynamicColor = Profile.useDynamicColor(getApplicationContext());
        String appFont = Profile.getAppFont(getApplicationContext());
        String appLanguage = Profile.getAppLanguage(getApplicationContext());
        if (useDynamicColor != mLastUseDynamicColor || !java.util.Objects.equals(appFont, mLastAppFont)
                || !java.util.Objects.equals(appLanguage, mLastAppLanguage)) {
            recreate();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mImageLoader != null && mImageLoader.isInited()) {
            mImageLoader.destroy();
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event);
    }

    /* View Utils Start */
    public void showToastMessage(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SheimiFragmentActivity.this, msg,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    public void showToastMessage(int resId) {
        showToastMessage(getString(resId));
    }

    public void showMessageDialog(int title, int msg, int positiveBtn,
            DialogInterface.OnClickListener positiveListener) {
        showMessageDialog(title, getString(msg), positiveBtn,
                R.string.label_cancel, positiveListener,
                new DummyDialogListener());
    }

    public void showMessageDialog(int title, String msg, int positiveBtn,
            DialogInterface.OnClickListener positiveListener) {
        showMessageDialog(title, msg, positiveBtn, R.string.label_cancel,
                positiveListener, new DummyDialogListener());
    }

    public void showMessageDialog(int title, String msg, int positiveBtn,
            int negativeBtn, DialogInterface.OnClickListener positiveListener,
            DialogInterface.OnClickListener negativeListener) {
        com.manichord.mgit.dialogs.MessageDialog.show(
                findViewById(android.R.id.content),
                getString(title), msg,
                getString(positiveBtn), getString(negativeBtn),
                positiveListener, negativeListener);
    }

    public void showMessageDialog(int title, String msg) {
        com.manichord.mgit.dialogs.MessageDialog.showSingleButton(
                findViewById(android.R.id.content),
                getString(title), msg,
                getString(R.string.label_ok), new DummyDialogListener());
    }

    public void showOptionsDialog(int title, final int option_names,
            final onOptionDialogClicked[] option_listeners) {
        CharSequence[] options_values = getResources().getStringArray(option_names);
        showOptionsDialog(title, options_values, option_listeners);
    }

    public void showOptionsDialog(int title, CharSequence[] option_values,
            final onOptionDialogClicked[] option_listeners) {
        com.manichord.mgit.dialogs.OptionsDialog.show(
                findViewById(android.R.id.content),
                getString(title), option_values,
                getString(R.string.label_cancel), option_listeners);
    }

    public void showEditTextDialog(int title, int hint, int positiveBtn,
            final OnEditTextDialogClicked positiveListener) {
        showEditTextDialog(title, hint, positiveBtn, positiveListener, 0);
    }

    /** @param helperText resource id for short guidance shown below the field (wraps normally),
     * or 0 for none -- use this instead of stuffing long text into the hint/label, which is
     * rendered as a single-line floating label by Material3 and overflows the field's border
     * if it doesn't fit on one line when focused. */
    public void showEditTextDialog(int title, int hint, int positiveBtn,
            final OnEditTextDialogClicked positiveListener, int helperText) {
        com.manichord.mgit.dialogs.EditTextDialog.show(
                findViewById(android.R.id.content),
                getString(title), getString(hint),
                getString(positiveBtn), getString(R.string.label_cancel),
                positiveListener,
                helperText == 0 ? null : getString(helperText));
    }

    public void promptForPassword(OnPasswordEntered onPasswordEntered,
            int errorId) {
        promptForPassword(onPasswordEntered, getString(errorId));
    }

    public void promptForPassword(final OnPasswordEntered onPasswordEntered,
            final String errorInfo) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                promptForPasswordInner(onPasswordEntered, errorInfo);
            }
        });
    }

    private void promptForPasswordInner(
            final OnPasswordEntered onPasswordEntered, String errorInfo) {
        String title = errorInfo != null ? errorInfo
                : getString(R.string.dialog_prompt_for_password_title);
        ViewGroup container = findViewById(android.R.id.content);
        com.manichord.mgit.dialogs.PasswordPromptDialog.show(
                container, title, onPasswordEntered);
    }

    public static interface onOptionDialogClicked {
        void onClicked();
    }

    public static interface OnEditTextDialogClicked {
        void onClicked(String text);
    }

    /**
     * Callback interface to receive credentials entered via UI by the user after
     * being prompted
     * in the UI in order to connect to a remote repo
     */
    public static interface OnPasswordEntered {

        /**
         * Handle retrying a Remote Repo task after user supplies requested credentials
         *
         * @param username
         * @param password
         * @param savePassword
         */
        void onClicked(String username, String password, boolean savePassword);

        void onCanceled();
    }

    /**
     * Callback for {@link #requestMediaFileAccess}, mirroring the {@link OnPasswordEntered}
     * pattern used for auth retries -- the caller's failed task is already done by the time this
     * fires (the permission dialog is async), so onGranted() is expected to start a fresh task
     * rather than resume the old one.
     */
    public static interface OnPermissionResult {
        void onGranted();

        void onDenied();
    }

    private OnPermissionResult mPendingMediaPermissionCallback;

    private final ActivityResultLauncher<String> mMediaPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                OnPermissionResult callback = mPendingMediaPermissionCallback;
                mPendingMediaPermissionCallback = null;
                if (callback == null) {
                    return;
                }
                if (granted) {
                    callback.onGranted();
                } else {
                    callback.onDenied();
                }
            });

    /**
     * Makes {@code blockedFile} (and any other file under {@code repairDir} Android won't let
     * Gitling read) readable, then calls back -- see FsUtils.findEaccesFile for the
     * scoped-storage quirk this works around. Below API 33 that's the READ_EXTERNAL_STORAGE
     * runtime permission. On API 33+ the equivalent permission (READ_MEDIA_IMAGES) isn't allowed
     * by Google Play's policy for this use, so instead the user is offered a "Fix access" button
     * that gets one-time folder access through the system picker and replaces each blocked file
     * with Gitling's own copy (see BlockedFileRepair); with access already given, that runs
     * without asking. Safe to call from a background thread; all UI runs on the UI thread.
     */
    public void requestMediaFileAccess(final File repairDir, final File blockedFile,
                                       final OnPermissionResult callback) {
        if (Build.VERSION.SDK_INT >= 33) {
            fixBlockedFiles(repairDir, blockedFile, callback);
            return;
        }
        final String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            callback.onGranted();
            return;
        }
        mPendingMediaPermissionCallback = callback;
        runOnUiThread(() -> mMediaPermissionLauncher.launch(permission));
    }

    private OnPermissionResult mPendingRepairCallback;
    private File mPendingRepairDir;
    private File mPendingBlockedFile;

    private final ActivityResultLauncher<Uri> mRepairFolderLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), treeUri -> {
                OnPermissionResult callback = mPendingRepairCallback;
                File repairDir = mPendingRepairDir;
                File blockedFile = mPendingBlockedFile;
                mPendingRepairCallback = null;
                mPendingRepairDir = null;
                mPendingBlockedFile = null;
                if (callback == null) {
                    return;
                }
                if (treeUri == null) {
                    callback.onDenied();
                    return;
                }
                if (!BlockedFileRepair.covers(treeUri, blockedFile)) {
                    // Picked some other folder: explain and let them pick again
                    showMessageDialog(R.string.dialog_fix_file_access_title,
                            getString(R.string.dialog_fix_file_access_wrong_folder, blockedFile.getName()),
                            R.string.label_try_again, R.string.label_cancel,
                            (dialog, which) -> launchRepairFolderPicker(repairDir, blockedFile, callback),
                            (dialog, which) -> callback.onDenied());
                    return;
                }
                BlockedFileRepair.persistGrant(this, treeUri);
                runRepair(treeUri, repairDir, callback);
            });

    private void fixBlockedFiles(final File repairDir, final File blockedFile,
                                 final OnPermissionResult callback) {
        Uri grant = BlockedFileRepair.findGrant(this, blockedFile);
        if (grant != null) {
            runRepair(grant, repairDir, callback);
            return;
        }
        runOnUiThread(() -> showMessageDialog(R.string.dialog_fix_file_access_title,
                getString(R.string.dialog_fix_file_access_message, blockedFile.getName()),
                R.string.label_fix_file_access, R.string.label_cancel,
                (dialog, which) -> launchRepairFolderPicker(repairDir, blockedFile, callback),
                (dialog, which) -> callback.onDenied()));
    }

    /** Opens the picker on the shared repos root when the file is under it, so one grant covers
     * every repo there, and on {@code repairDir} otherwise. */
    private void launchRepairFolderPicker(File repairDir, File blockedFile, OnPermissionResult callback) {
        File sharedRoot = FsUtils.getMediaDir(Repo.REPO_DIR, false);
        boolean underSharedRoot = blockedFile.getAbsolutePath()
                .startsWith(sharedRoot.getAbsolutePath() + File.separator);
        mPendingRepairCallback = callback;
        mPendingRepairDir = repairDir;
        mPendingBlockedFile = blockedFile;
        mRepairFolderLauncher.launch(BlockedFileRepair.initialPickerUri(
                underSharedRoot ? sharedRoot : repairDir));
    }

    private void runRepair(final Uri treeUri, final File repairDir, final OnPermissionResult callback) {
        new Thread(() -> {
            BlockedFileRepair.Result result = BlockedFileRepair.repair(this, treeUri, repairDir);
            runOnUiThread(() -> {
                // Nothing found blocked any more also retries; the retry itself is capped at one
                if (result.failed.isEmpty()) {
                    callback.onGranted();
                } else {
                    callback.onDenied();
                }
            });
        }).start();
    }

    /* View Utils End */

    /* Switch Activity Animation Start */
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        forwardTransition();
    }

    public void finish() {
        super.finish();
        backTransition();
    }

    public void rawfinish() {
        super.finish();
    }

    public void forwardTransition() {
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.m3_open_enter, R.anim.m3_open_exit);
        } else {
            overridePendingTransition(R.anim.m3_open_enter, R.anim.m3_open_exit);
        }
    }

    public void backTransition() {
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, R.anim.m3_close_enter, R.anim.m3_close_exit);
        } else {
            overridePendingTransition(R.anim.m3_close_enter, R.anim.m3_close_exit);
        }
    }

    /* Switch Activity Animation End */

    /* ImageCache Start */

    private static final int SIZE = 100 << 20;
    private ImageLoader mImageLoader;

    private void setupImageLoader() {
        DisplayImageOptions mDisplayOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .showImageForEmptyUri(
                        VectorDrawableCompat.create(getResources(), R.drawable.ic_default_author, getTheme()))
                .showImageOnFail(VectorDrawableCompat.create(getResources(), R.drawable.ic_default_author, getTheme()))
                .build();
        File cacheDir = StorageUtils.getCacheDirectory(this);
        ImageLoaderConfiguration configuration = new ImageLoaderConfiguration.Builder(this)
                .defaultDisplayImageOptions(mDisplayOptions)
                .diskCache(new UnlimitedDiskCache(cacheDir))
                .diskCacheSize(SIZE)
                .imageDownloader(new AvatarDownloader(this))
                .build();

        mImageLoader = ImageLoader.getInstance();
        mImageLoader.init(configuration);
    }

    public ImageLoader getImageLoader() {
        if (mImageLoader == null || !mImageLoader.isInited()) {
            setupImageLoader();
        }
        return mImageLoader;
    }
    /* ImageCache End */
}
