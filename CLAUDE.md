# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Gitling is a Git client for Android (Gradle project name "MGit"), built with Jetpack Compose and
Material 3 Expressive. It's a fork of [MGit](https://github.com/maks/MGit), itself a continuation
of [SGit](https://github.com/sheimi/SGit). Min SDK 23, target/compile SDK 37. JGit is the
underlying Git implementation (`org.eclipse.jgit`).

`applicationId` (`com.maneeshacooray.gitling`) and `namespace` (`me.sheimi.sgit`) intentionally
differ — the namespace is the inherited legacy package root, the applicationId is the rebrand.
Don't "fix" this mismatch.

## Commands

- Build debug + run unit tests (what CI runs): `./gradlew --no-daemon clean assembleDebug testDebug`
- Run a single test class: `./gradlew testDebug --tests "me.sheimi.sgit.database.models.RepoTest"`
- Build a signed release APK locally requires `-Pspecial -Palias=... -Ppassword=... -Pkeystore=... -Pstore_password=...` (see `.github/workflows/release.yml`); without those properties `assembleRelease` falls back to unsigned.
- Lint: `lint.abortOnError = false` in `app/build.gradle.kts` — lint issues don't fail the build.
- Gradle requires JDK 21 specifically (pinned via `java.toolchain` in `app/build.gradle.kts`) — this is required to match F-Droid's reproducible-build environment, not just a target bytecode level. If the system JDK isn't 21, pass `JAVA_HOME` explicitly pointing at a JDK 21 install rather than changing the toolchain version.

## Further reading

- @docs/agents/architecture.md — the two coexisting codebases (legacy Activity/Fragment layer vs.
  new Compose layer), the `FragmentHost` bridge between them, and commit graph rendering.
- @docs/agents/release-process.md — version bump checklist, the `develop` → `master` workflow, and
  F-Droid's reproducible-build requirements.
