package me.sheimi.android.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;

import me.sheimi.sgit.R;
import me.sheimi.sgit.database.models.Repo;

/**
 * Created by lee on 2015-02-01.
 */
public class Profile {

    private static SharedPreferences sSharedPreference;

    private static boolean sHasLastCloneFail = false;
    private static Repo sLastFailRepo;
    private static int sTheme = -1;

    private static SharedPreferences getProfileSharedPreference(Context context) {
        if (sSharedPreference == null) {
            sSharedPreference = context.getSharedPreferences(
                                    context.getString(R.string.preference_file_key),
                                    Context.MODE_PRIVATE);
        }
        return sSharedPreference;
    }

    public static String getUsername(Context context) {
        String userNamePrefKey = context.getString(R.string.pref_key_git_user_name);
        return getProfileSharedPreference(context).getString(userNamePrefKey, "");
    }

    public static String getEmail(Context context) {
        String userEmailPrefKey = context.getString(R.string.pref_key_git_user_email);
        return getProfileSharedPreference(context).getString(userEmailPrefKey, "");
    }

    public static boolean hasLastCloneFailed() {
        return sHasLastCloneFail;
    }

    public static Repo getLastCloneTryRepo() {
        return sLastFailRepo;
    }

    public static void setLastCloneFailed(Repo repo) {
        sHasLastCloneFail = true;
        sLastFailRepo = repo;
    }

    public static void setLastCloneSuccess() {
        sHasLastCloneFail = false;
    }

    public static synchronized int getTheme(Context context) {
        String themePrefKey = context.getString(R.string.pref_key_use_theme_id);
        SharedPreferences prefs = getProfileSharedPreference(context);
        if (prefs.contains(themePrefKey)) {
            // silly, but Android framework want strings as value array for ListPreference
            return Integer.parseInt(prefs.getString(themePrefKey, "0"));
        }
        // No explicit preference set -- the Settings screen no longer exposes a theme
        // picker (dropped during the Compose migration), so every install was otherwise
        // permanently stuck on "0" (light) here regardless of system dark mode, while the
        // Compose chrome around these legacy Fragment-hosted screens already follows
        // system dark mode via isSystemInDarkTheme(). That mismatch is what made commit
        // messages etc. render in hardcoded-light (black) text on a now-dark background.
        // Default to following the system setting instead so both halves agree.
        int nightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES ? 1 : 0;
    }

    public static int getThemeResource(Context context) {
        final int[] themes = { R.style.AppTheme, R.style.DarkAppTheme };
        return themes[getTheme(context)];
    }

    public static String getCodeMirrorTheme(Context context) {
        final String[] themes = context.getResources().getStringArray(R.array.codemirror_theme_names);
        return themes[getTheme(context)];
    }

    public static int getStyledResource(Context context, int unstyled) {
        TypedArray a = context.getTheme().obtainStyledAttributes(getThemeResource(context), new int[] {unstyled});
        int styled = a.getResourceId(0, 0);
        a.recycle();
        return styled;
    }

    public static boolean useEnglishLocale(Context context) {
        String useEnglishPrefKey = context.getString(R.string.pref_key_use_english);
        return getProfileSharedPreference(context).getBoolean(useEnglishPrefKey, false);
    }

    public static boolean useDynamicColor(Context context) {
        String useDynamicColorPrefKey = context.getString(R.string.pref_key_use_dynamic_color);
        return getProfileSharedPreference(context).getBoolean(useDynamicColorPrefKey, false);
    }

    public static void setUseDynamicColor(Context context, boolean useDynamicColor) {
        String useDynamicColorPrefKey = context.getString(R.string.pref_key_use_dynamic_color);
        getProfileSharedPreference(context).edit().putBoolean(useDynamicColorPrefKey, useDynamicColor).apply();
    }

    public static String getAppFont(Context context) {
        String appFontPrefKey = context.getString(R.string.pref_key_app_font);
        return getProfileSharedPreference(context).getString(appFontPrefKey, null);
    }

    public static void setAppFont(Context context, String fontId) {
        String appFontPrefKey = context.getString(R.string.pref_key_app_font);
        getProfileSharedPreference(context).edit().putString(appFontPrefKey, fontId).apply();
    }

    /**
     * -1 means "never recorded" (fresh install) -- distinct from any real versionCode, which
     * starts at 1, so callers can tell a fresh install apart from an update.
     */
    public static int getLastSeenVersionCode(Context context) {
        String key = context.getString(R.string.pref_key_last_seen_version_code);
        return getProfileSharedPreference(context).getInt(key, -1);
    }

    public static void setLastSeenVersionCode(Context context, int versionCode) {
        String key = context.getString(R.string.pref_key_last_seen_version_code);
        getProfileSharedPreference(context).edit().putInt(key, versionCode).apply();
    }

    /** 0 means "never checked" -- used to throttle UpdateChecker's GitHub API call to once per
     * cooldown window rather than on every app launch. */
    public static long getLastUpdateCheckTime(Context context) {
        String key = context.getString(R.string.pref_key_last_update_check_time);
        return getProfileSharedPreference(context).getLong(key, 0L);
    }

    public static void setLastUpdateCheckTime(Context context, long timeMillis) {
        String key = context.getString(R.string.pref_key_last_update_check_time);
        getProfileSharedPreference(context).edit().putLong(key, timeMillis).apply();
    }

    /** The version the user dismissed the update banner for, so it doesn't reappear every
     * launch -- but does reappear if a newer version ships after the dismissal. */
    public static String getDismissedUpdateVersion(Context context) {
        String key = context.getString(R.string.pref_key_dismissed_update_version);
        return getProfileSharedPreference(context).getString(key, "");
    }

    public static void setDismissedUpdateVersion(Context context, String version) {
        String key = context.getString(R.string.pref_key_dismissed_update_version);
        getProfileSharedPreference(context).edit().putString(key, version).apply();
    }
}
