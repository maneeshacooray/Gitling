package com.manichord.mgit.whatsnew

/**
 * versionCode here must match the versionCode set in app/build.gradle.kts for that release --
 * add a new entry (with the new versionCode) as part of cutting each release.
 */
data class WhatsNewEntry(
    val versionCode: Int,
    val versionName: String,
    val highlights: List<String>
)

object WhatsNewContent {
    val entries = listOf(
        WhatsNewEntry(
            versionCode = 55,
            versionName = "1.0.55",
            highlights = listOf(
                "Improved deep-link handling for .git URLs"
            )
        ),
        WhatsNewEntry(
            versionCode = 54,
            versionName = "1.0.54",
            highlights = listOf(
                "Fixed a crash opening or initializing a repository on Android 12 and older"
            )
        ),
        WhatsNewEntry(
            versionCode = 53,
            versionName = "1.0.53",
            highlights = listOf(
                "Fixed several rare crashes when reopening a dialog (merge, push, rebase, and others) right after the app restarts",
                "Fixed a crash showing an error dialog if the app was backgrounded while a clone, pull, or push was still running"
            )
        ),
        WhatsNewEntry(
            versionCode = 52,
            versionName = "1.0.52",
            highlights = listOf(
                "Fixed a crash when tapping Clone or Init before typing a repository name",
                "Fixed a crash generating a DSA SSH key with the default key length",
                "Fixed a rare crash pulling right after the app restarts",
                "The Clone/Init sheet now shows validation errors instead of closing silently"
            )
        ),
        WhatsNewEntry(
            versionCode = 51,
            versionName = "1.0.51",
            highlights = listOf(
                "Properly fixed the widget's refresh button crowding the top-right corner — the 1.0.50 fix didn't actually move it"
            )
        ),
        WhatsNewEntry(
            versionCode = 50,
            versionName = "1.0.50",
            highlights = listOf(
                "Fixed the widget's refresh button crowding the top-right corner",
                "Fixed the keyboard covering the Remote URL and Local Path fields when cloning a repository"
            )
        ),
        WhatsNewEntry(
            versionCode = 49,
            versionName = "1.0.49",
            highlights = listOf(
                "Housekeeping release — no functional changes; corrects the What's New notes for 1.0.48, which shipped without them"
            )
        ),
        WhatsNewEntry(
            versionCode = 48,
            versionName = "1.0.48",
            highlights = listOf(
                "Redesigned home screen widget — new Material 3 look with a title bar, always-visible branch and last commit message at every size, and a bigger minimum size for easier reading"
            )
        ),
        WhatsNewEntry(
            versionCode = 47,
            versionName = "1.0.47",
            highlights = listOf(
                "Stage All and Unstage All on the Status screen — bulk-stage or unstage every file in one tap instead of going one by one"
            )
        ),
        WhatsNewEntry(
            versionCode = 46,
            versionName = "1.0.46",
            highlights = listOf(
                "Widget now adapts to its size — shrink it to 1 cell tall for a compact repo list showing just names and status, or keep it taller for the full layout with branch and last commit"
            )
        ),
        WhatsNewEntry(
            versionCode = 45,
            versionName = "1.0.45",
            highlights = listOf(
                "Removed the automatic update banner — no more interruptions on the home screen. Check for updates any time from Settings"
            )
        ),
        WhatsNewEntry(
            versionCode = 44,
            versionName = "1.0.44",
            highlights = listOf(
                "Self-hosted Forgejo and Gitea support — add a Custom account with your instance URL and credentials are applied automatically on clone, fetch, and pull"
            )
        ),
        WhatsNewEntry(
            versionCode = 43,
            versionName = "1.0.43",
            highlights = listOf(
                "Fixed blank File tab after switching to Blame and back — file content now stays visible when returning to the File tab"
            )
        ),
        WhatsNewEntry(
            versionCode = 42,
            versionName = "1.0.42",
            highlights = listOf(
                "fetch and pull in the console — run network sync commands directly from the Console tab without leaving the app"
            )
        ),
        WhatsNewEntry(
            versionCode = 41,
            versionName = "1.0.41",
            highlights = listOf(
                "Redesigned home screen widget — shows branch name, last commit message, and a clean/dirty status indicator for each repo"
            )
        ),
        WhatsNewEntry(
            versionCode = 40,
            versionName = "1.0.40",
            highlights = listOf(
                "Git command console — tap the Console tab inside any repo to run status, log, diff, branch, and stash commands without leaving the app"
            )
        ),
        WhatsNewEntry(
            versionCode = 39,
            versionName = "1.0.39",
            highlights = listOf(
                "Tag repositories with labels and filter the list — long-press a repo, tap Tags, and use the filter chips at the top"
            )
        ),
        WhatsNewEntry(
            versionCode = 38,
            versionName = "1.0.38",
            highlights = listOf(
                "Pin repositories to keep them at the top of the list — long-press a repo and tap Pin"
            )
        ),
        WhatsNewEntry(
            versionCode = 37,
            versionName = "1.0.37",
            highlights = listOf(
                "Fixed clone crash on Android 12 — JGit now works on API 31+",
                "Clone button now always reachable by scrolling the clone sheet"
            )
        ),
        WhatsNewEntry(
            versionCode = 36,
            versionName = "1.0.36",
            highlights = listOf(
                "Added Submodule Update action — init and update submodules from the repo drawer",
                "Fixed Clone recursively checkbox not toggling visually"
            )
        ),
        WhatsNewEntry(
            versionCode = 35,
            versionName = "1.0.35",
            highlights = listOf(
                "Fixed bright splash screen flash on cold launch in dark mode",
                "Added monochrome icon for themed home screens on Android 13+"
            )
        ),
        WhatsNewEntry(
            versionCode = 34,
            versionName = "1.0.34",
            highlights = listOf(
                "Added a Blame tab to the file viewer -- see which commit last touched each line"
            )
        ),
        WhatsNewEntry(
            versionCode = 33,
            versionName = "1.0.33",
            highlights = listOf(
                "Fixed text overflow on long branch/remote/file names and committer names"
            )
        ),
        WhatsNewEntry(
            versionCode = 32,
            versionName = "1.0.32",
            highlights = listOf(
                "Redesigned commit rows to show author and date in a two-line layout"
            )
        ),
        WhatsNewEntry(
            versionCode = 31,
            versionName = "1.0.30",
            highlights = listOf(
                "Fixed the Cherry Pick dialog's guide text overflowing past the field border"
            )
        ),
        WhatsNewEntry(
            versionCode = 30,
            versionName = "1.0.29",
            highlights = listOf(
                "Fixed a crash tapping a repo in the home-screen widget right after the app had been killed in the background"
            )
        ),
        WhatsNewEntry(
            versionCode = 29,
            versionName = "1.0.28",
            highlights = listOf(
                "Added a home-screen widget -- shows your repos and whether each has uncommitted changes, tap one to open it. Add it from Settings or your launcher's widget picker"
            )
        ),
        WhatsNewEntry(
            versionCode = 28,
            versionName = "1.0.27",
            highlights = listOf(
                "Housekeeping release -- removes an extra metadata block Google's build tooling adds to the downloadable APK (F-Droid's verification rejects it), no user-facing changes"
            )
        ),
        WhatsNewEntry(
            versionCode = 27,
            versionName = "1.0.26",
            highlights = listOf(
                "Housekeeping release -- stops stripping debug symbols from bundled third-party native libraries so builds are byte-identical across machines (needed for F-Droid's reproducible-build verification), no user-facing changes"
            )
        ),
        WhatsNewEntry(
            versionCode = 26,
            versionName = "1.0.25",
            highlights = listOf(
                "Housekeeping release -- switches the build's required JDK version to one already available on F-Droid's build servers (fixes a build policy conflict there), no user-facing changes"
            )
        ),
        WhatsNewEntry(
            versionCode = 25,
            versionName = "1.0.24",
            highlights = listOf(
                "Housekeeping release -- lets the build system auto-download the exact JDK it needs on machines that don't already have it (fixes a build failure on F-Droid's build servers), no user-facing changes"
            )
        ),
        WhatsNewEntry(
            versionCode = 24,
            versionName = "1.0.23",
            highlights = listOf(
                "Housekeeping release -- pins the exact JDK used to build the app so different build machines produce identical APKs (needed for F-Droid's reproducible-build verification), no user-facing changes"
            )
        ),
        WhatsNewEntry(
            versionCode = 23,
            versionName = "1.0.22",
            highlights = listOf(
                "Fixed a bug where the app could land on a blank screen after Android relaunches it (e.g. after certain system display/font settings change) -- it now correctly reopens to wherever you were"
            )
        ),
        WhatsNewEntry(
            versionCode = 22,
            versionName = "1.0.21",
            highlights = listOf(
                "Fixed a crash opening a repo's Files or Status tab right after Android relaunches the app (e.g. after certain system display/font settings change)"
            )
        ),
        WhatsNewEntry(
            versionCode = 21,
            versionName = "1.0.20",
            highlights = listOf(
                "Fixed a bug where cloning a new repo after turning on \"Make repos visible to other apps\" still used the old private location instead"
            )
        ),
        WhatsNewEntry(
            versionCode = 20,
            versionName = "1.0.19",
            highlights = listOf(
                "Added a Settings toggle to make repos visible to other apps (e.g. a file manager) -- still no extra permission needed either way, and switching it moves your existing repos automatically"
            )
        ),
        WhatsNewEntry(
            versionCode = 19,
            versionName = "1.0.18",
            highlights = listOf(
                "Gitling no longer requests broad storage access -- repos now live in Gitling's own private folder. If you'd picked a custom storage location before, you'll see a one-time notice and need to re-clone repos that were stored there"
            )
        ),
        WhatsNewEntry(
            versionCode = 18,
            versionName = "1.0.17",
            highlights = listOf(
                "Added search to the Files and Commits tabs inside a repo -- find a file anywhere in the repo, or a commit by message, author, or hash"
            )
        ),
        WhatsNewEntry(
            versionCode = 17,
            versionName = "1.0.16",
            highlights = listOf(
                "Added search to the repo list -- tap the search icon to filter by name, remote, committer, or commit message"
            )
        ),
        WhatsNewEntry(
            versionCode = 16,
            versionName = "1.0.15",
            highlights = listOf(
                "Added a \"Check for Updates\" button in Settings, and the version row there now shows your actual installed version"
            )
        ),
        WhatsNewEntry(
            versionCode = 15,
            versionName = "1.0.14",
            highlights = listOf(
                "Gitling now lets you know when a new version is available on GitHub"
            )
        ),
        WhatsNewEntry(
            versionCode = 14,
            versionName = "1.0.13",
            highlights = listOf(
                "Picking a new repo storage location now finds and adds any existing repos already there"
            )
        ),
        WhatsNewEntry(
            versionCode = 13,
            versionName = "1.0.12",
            highlights = listOf(
                "Fixed a crash opening Push, Pull, Merge, Rebase, Remove Remote, or Checkout from a commit -- sorry about that one"
            )
        ),
        WhatsNewEntry(
            versionCode = 12,
            versionName = "1.0.11",
            highlights = listOf(
                "Fixed a newly-connected GitHub account not showing up until the app was restarted",
                "The repo list's \"+\" button no longer says \"New repo\" -- it covers cloning and importing too"
            )
        ),
        WhatsNewEntry(
            versionCode = 11,
            versionName = "1.0.10",
            highlights = listOf(
                "Housekeeping release -- no user-facing changes"
            )
        ),
        WhatsNewEntry(
            versionCode = 10,
            versionName = "1.0.9",
            highlights = listOf(
                "Housekeeping release -- no user-facing changes"
            )
        ),
        WhatsNewEntry(
            versionCode = 9,
            versionName = "1.0.8",
            highlights = listOf(
                "Fixed the file viewer showing a blank page for most files",
                "Under-the-hood rewrite: the whole app now runs as a single screen with smooth in-app navigation instead of jumping between separate screens"
            )
        ),
        WhatsNewEntry(
            versionCode = 8,
            versionName = "1.0.7",
            highlights = listOf(
                "Commit graph now shows branch, tag, and HEAD labels inline, lazygit-style"
            )
        ),
        WhatsNewEntry(
            versionCode = 7,
            versionName = "1.0.6",
            highlights = listOf(
                "Commit graph redesigned to match lazygit's compact terminal style -- denser lanes and one line per commit instead of tall cards"
            )
        ),
        WhatsNewEntry(
            versionCode = 6,
            versionName = "1.0.5",
            highlights = listOf(
                "You're looking at it -- Gitling now shows what's new after each update"
            )
        ),
        WhatsNewEntry(
            versionCode = 5,
            versionName = "1.0.4",
            highlights = listOf(
                "Status now shows a real per-file list -- stage or unstage individual files instead of just two giant diff buttons",
                "Added icons throughout the repo menu and the Accounts screen's add button"
            )
        ),
        WhatsNewEntry(
            versionCode = 4,
            versionName = "1.0.3",
            highlights = listOf(
                "Every dialog in the app now matches Gitling's own look instead of the system default",
                "Fixed screens not picking up a theme change made elsewhere until restarted"
            )
        ),
        WhatsNewEntry(
            versionCode = 3,
            versionName = "1.0.2",
            highlights = listOf(
                "Refreshed repo list with a clearer \"New repo\" button and GitHub connect banner",
                "Started migrating dialogs across the app to match Gitling's brand colors"
            )
        )
    )
}
