package me.sheimi.android.activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.utils.StorageUtils;

import java.io.File;
import java.util.Locale;

import me.sheimi.android.avatar.AvatarDownloader;
import me.sheimi.android.utils.BasicFunctions;
import me.sheimi.android.utils.Profile;
import me.sheimi.sgit.R;
import me.sheimi.sgit.dialogs.DummyDialogListener;

public class SheimiFragmentActivity extends AppCompatActivity {

    public static interface OnBackClickListener {
        public boolean onClick();
    }

    private boolean mLastUseDynamicColor;
    private String mLastAppFont;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BasicFunctions.setActiveActivity(this);
        setTheme(getThemeResource());
        updateLocale(Profile.useEnglishLocale(getApplicationContext()));
        mLastUseDynamicColor = Profile.useDynamicColor(getApplicationContext());
        mLastAppFont = Profile.getAppFont(getApplicationContext());

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

    private void updateLocale(boolean useEnglishLocale) {
        final Locale locale;
        if (useEnglishLocale) {
            locale = Locale.ENGLISH;
        } else {
            locale = Locale.getDefault();
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
        if (useDynamicColor != mLastUseDynamicColor || !java.util.Objects.equals(appFont, mLastAppFont)) {
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
