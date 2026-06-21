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
