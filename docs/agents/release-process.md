# Release process

## Branch model

Two long-lived branches, both pushed to `origin` and both protected by GitHub branch-protection
rules ("Changes must be made through a pull request" / "Cannot change this locked branch") that
get bypassed as repo admin rather than satisfied via an actual PR — that's an accepted tradeoff
for a single-maintainer repo, not an oversight.

- **`develop`** — where all feature/fix commits land first.
- **`master`** — only ever updated by merging `develop` into it with `--no-ff` (never by
  committing directly to `master`, and never by fast-forwarding). `master` is what tags get cut
  from and what F-Droid's `fdroiddata` metadata points at.

Merge commit message convention (used for every `develop` → `master` merge, release or not):

```
Merge branch 'develop' into master: <short description of what this merge contains>
```

## Standard merge (no release involved)

For a fix/feature that doesn't need its own version bump yet (e.g. several small fixes landing
together before the next cut):

```bash
git checkout develop
git add <files>
git commit -m "<type>: <description>"
git checkout master
git merge --no-ff develop -m "Merge branch 'develop' into master: <short description>"
git push origin develop master
```

Always verify the build compiles before pushing (see "JDK requirement" below) — `master` has no
CI gate of its own beyond the tag-triggered release workflow, so a broken commit on `master`
isn't caught automatically.

## Cutting a release

A release is a version bump + tag on top of whatever's already been merged to `master`. Do the
version bump itself as its own commit on `develop` first, then merge to `master` like any other
change — **never commit the version bump directly to `master`** (see "Pitfalls" below for why
this matters more than it sounds like it should).

1. **Bump `versionCode`/`versionName`** in `app/build.gradle.kts` (`defaultConfig` block).
   `versionCode` increments by 1; `versionName` follows the app's `1.0.x` scheme.
2. **Add a `WhatsNewEntry`** at the top of the `entries` list in
   `app/src/main/java/com/manichord/mgit/whatsnew/WhatsNewContent.kt` — `versionCode` must match
   step 1 exactly. Keep `highlights` short and user-facing (this renders in-app).
3. **Add a changelog file** at `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`
   (plain text, one or a few sentences — F-Droid and other fastlane-style stores display this
   verbatim).
4. **Verify the build compiles** (see "JDK requirement" below) before committing.
5. **Commit on `develop`**, merge to `master`, tag, push all three in one go:

```bash
git checkout develop
git add app/build.gradle.kts \
        app/src/main/java/com/manichord/mgit/whatsnew/WhatsNewContent.kt \
        fastlane/metadata/android/en-US/changelogs/<versionCode>.txt
git commit -m "chore: bump version to v<versionName>"
git checkout master
git merge --no-ff develop -m "Merge branch 'develop' into master: v<versionName>"
git tag -a v<versionName> -m "v<versionName>"
git push origin develop master v<versionName>
```

6. **Confirm both branches match** after pushing — `git log --oneline -1 develop` and
   `master` should show the same `versionCode`/`versionName` in `app/build.gradle.kts`. This is
   exactly the check that catches the drift bug described below.

### JDK requirement

Gradle needs JDK 21 specifically (see `CLAUDE.md`'s Commands section for why). If the shell's
default `JAVA_HOME` isn't 21, pass it explicitly rather than touching the toolchain config:

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.11/libexec/openjdk.jdk/Contents/Home \
  ./gradlew compileDebugKotlin --console=plain -q
```

(Path will vary by machine — find it with `ls /opt/homebrew/Cellar/openjdk@21/*/libexec/*.jdk/Contents/Home`
or equivalent.)

## What happens after pushing the tag

- **GitHub**: `.github/workflows/release.yml` triggers on any tag matching `v[0-9]+.[0-9]+.[0-9]+`,
  builds a signed release APK (using repo secrets for the keystore), and publishes it as a GitHub
  Release with auto-generated release notes. This is fast (a few minutes) — check
  `gh run list --workflow=release.yml` or the Actions tab if you want to confirm it succeeded.
- **F-Droid**: builds from `metadata/` in F-Droid's own separate `fdroiddata` repository, not from
  anything here — there is nothing to push or trigger on this side. F-Droid's own infrastructure
  polls for new tags and queues a build, which can lag the GitHub release by anywhere from
  several hours to a few days. Reproducible-build verification there is sensitive to JDK version
  (pinned to 21 for exactly this reason) and native library stripping —
  `app/build.gradle.kts`'s `packaging.jniLibs` disables stripping for prebuilt `.so` deps (e.g.
  Conscrypt) because the NDK's `llvm-strip` version isn't pinned and varies across build
  machines, which previously broke byte-for-byte reproducibility.

## Pitfalls (things that have actually gone wrong)

- **Committing the version bump directly to `master`.** It's tempting to do this as a "quick"
  step right before tagging, but it means `develop` never gets the bump and silently drifts out
  of sync — the next time something is merged from `develop` to `master`, the merge can
  reintroduce the *old* `versionCode`/`versionName` as a conflict, or worse, merge cleanly with
  stale values if only one of the two fields conflicts. This already happened once: `develop`
  ended up with a stale `versionName` for an entire release cycle because the bump commit only
  ever existed on `master`. If this happens, fix it with
  `git cherry-pick <bump-commit-sha>` onto `develop` (resolve the version-line conflict by taking
  the newer values) and push — don't just let it slide, since it'll resurface at the next merge.
- **Forgetting to push after tagging.** `git tag` is purely local; the GitHub Actions release
  workflow only fires once the tag is pushed (`git push origin <tag>`, or include it in the same
  push as the branches as shown above).
- **Assuming F-Droid availability means the latest tag is live.** F-Droid being reachable / the
  app existing in their index doesn't mean your newest tag has built yet — check the actual
  reported version (e.g. via `f-droid.org/api/v1/packages/<applicationId>` or the monitor at
  `monitor.f-droid.org/builds/build`) rather than assuming.
