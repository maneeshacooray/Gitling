# Release process

Releases are tag-triggered (`.github/workflows/release.yml`, tags matching `v[0-9]+.[0-9]+.[0-9]+`)
and build a signed release APK published as a GitHub Release. A release also needs:

- `versionCode`/`versionName` bumped in `app/build.gradle.kts`
- A new entry in `com.manichord.mgit.whatsnew.WhatsNewContent`
- A changelog file at `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` (F-Droid and
  fastlane-style stores read this)

The repo uses a `develop` → `master` workflow: land feature work on `develop`, then
`git merge --no-ff develop` into `master` (see recent merge commits for the message convention:
`Merge branch 'develop' into master: <short description>`).

F-Droid builds from `metadata/` in F-Droid's own `fdroiddata` repo, not from anything in this
repo, and reproducible-build verification is sensitive to JDK version (pinned to 21, see
the Commands section in `CLAUDE.md`) and native library stripping — `app/build.gradle.kts`'s
`packaging.jniLibs` disables stripping for prebuilt `.so` deps (e.g. Conscrypt) because the NDK's
`llvm-strip` version isn't pinned and varies across build machines, breaking byte-for-byte
reproducibility.
