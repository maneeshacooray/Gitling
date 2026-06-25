# Changelog

All notable changes to Gitling are documented here, newest first. This is generated from the
in-app "What's New" history (`app/src/main/java/com/manichord/mgit/whatsnew/WhatsNewContent.kt`)
and the per-release notes under `fastlane/metadata/android/en-US/changelogs/`; update this file
as part of cutting each release (see `docs/agents/release-process.md`).

## 1.0.34 - 2026-06-25
- Added a Blame tab to the file viewer -- see which commit last touched each line

## 1.0.33 - 2026-06-25
- Fixed text overflow on long branch/remote/file names and committer names

## 1.0.32 - 2026-06-24
- Redesigned commit rows to show author and date in a two-line layout

## 1.0.30 - 2026-06-23
- Fixed the Cherry Pick dialog's guide text overflowing past the field border

## 1.0.29 - 2026-06-22
- Fixed a crash tapping a repo in the home-screen widget right after the app had been killed in the background

## 1.0.28 - 2026-06-22
- Added a home-screen widget -- shows your repos and whether each has uncommitted changes, tap one to open it. Add it from Settings or your launcher's widget picker

## 1.0.27 - 2026-06-22
- Housekeeping release -- removes an extra metadata block Google's build tooling adds to the downloadable APK (F-Droid's verification rejects it), no user-facing changes

## 1.0.26 - 2026-06-22
- Housekeeping release -- stops stripping debug symbols from bundled third-party native libraries so builds are byte-identical across machines (needed for F-Droid's reproducible-build verification), no user-facing changes

## 1.0.25 - 2026-06-22
- Housekeeping release -- switches the build's required JDK version to one already available on F-Droid's build servers (fixes a build policy conflict there), no user-facing changes

## 1.0.24 - 2026-06-22
- Housekeeping release -- lets the build system auto-download the exact JDK it needs on machines that don't already have it (fixes a build failure on F-Droid's build servers), no user-facing changes

## 1.0.23 - 2026-06-22
- Housekeeping release -- pins the exact JDK used to build the app so different build machines produce identical APKs (needed for F-Droid's reproducible-build verification), no user-facing changes

## 1.0.22 - 2026-06-22
- Fixed a bug where the app could land on a blank screen after Android relaunches it (e.g. after certain system display/font settings change) -- it now correctly reopens to wherever you were

## 1.0.21 - 2026-06-22
- Fixed a crash opening a repo's Files or Status tab right after Android relaunches the app (e.g. after certain system display/font settings change)

## 1.0.20 - 2026-06-22
- Fixed a bug where cloning a new repo after turning on "Make repos visible to other apps" still used the old private location instead

## 1.0.19 - 2026-06-21
- Added a Settings toggle to make repos visible to other apps (e.g. a file manager) -- still no extra permission needed either way, and switching it moves your existing repos automatically

## 1.0.18 - 2026-06-21
- Gitling no longer requests broad storage access -- repos now live in Gitling's own private folder. If you'd picked a custom storage location before, you'll see a one-time notice and need to re-clone repos that were stored there

## 1.0.17 - 2026-06-21
- Added search to the Files and Commits tabs inside a repo -- find a file anywhere in the repo, or a commit by message, author, or hash

## 1.0.16 - 2026-06-21
- Added search to the repo list -- tap the search icon to filter by name, remote, committer, or commit message

## 1.0.15 - 2026-06-21
- Added a "Check for Updates" button in Settings, and the version row there now shows your actual installed version

## 1.0.14 - 2026-06-21
- Gitling now lets you know when a new version is available on GitHub

## 1.0.13 - 2026-06-21
- Picking a new repo storage location now finds and adds any existing repos already there

## 1.0.12 - 2026-06-21
- Fixed a crash opening Push, Pull, Merge, Rebase, Remove Remote, or Checkout from a commit -- sorry about that one

## 1.0.11 - 2026-06-21
- Fixed a newly-connected GitHub account not showing up until the app was restarted
- The repo list's "+" button no longer says "New repo" -- it covers cloning and importing too

## 1.0.10 - 2026-06-21
- Housekeeping release -- no user-facing changes

## 1.0.9 - 2026-06-21
- Housekeeping release -- no user-facing changes

## 1.0.8 - 2026-06-21
- Fixed the file viewer showing a blank page for most files
- Under-the-hood rewrite: the whole app now runs as a single screen with smooth in-app navigation instead of jumping between separate screens

## 1.0.7 - 2026-06-20
- Commit graph now shows branch, tag, and HEAD labels inline, lazygit-style

## 1.0.6 - 2026-06-20
- Commit graph redesigned to match lazygit's compact terminal style -- denser lanes and one line per commit instead of tall cards

## 1.0.5 - 2026-06-20
- You're looking at it -- Gitling now shows what's new after each update

## 1.0.4 - 2026-06-20
- Status now shows a real per-file list -- stage or unstage individual files instead of just two giant diff buttons
- Added icons throughout the repo menu and the Accounts screen's add button

## 1.0.3 - 2026-06-20
- Every dialog in the app now matches Gitling's own look instead of the system default
- Fixed screens not picking up a theme change made elsewhere until restarted

## 1.0.2 - 2026-06-20
- Refreshed repo list with a clearer "New repo" button and GitHub connect banner
- Started migrating dialogs across the app to match Gitling's brand colors

## 1.0.0 / 1.0.1 - 2026-06-20
- Initial Gitling releases (fork of MGit) -- no per-release notes recorded for these two.
