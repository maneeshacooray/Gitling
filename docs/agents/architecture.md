# Architecture: two coexisting codebases

The app is mid-migration from a legacy Java/XML Activity+Fragment app to a Kotlin/Compose
single-activity app. Both live side by side under `app/src/main/java/`:

- **`me/sheimi/sgit/`** (and `me/sheimi/android/`) — the legacy layer: XML-based `Activity`/
  `Fragment` screens, `RepoDbManager`/`Repo` (SQLite-backed repo model), and
  `me.sheimi.sgit.repo.tasks.repo.*` — one class per git operation (`CloneTask`, `PullTask`,
  `RebaseTask`, `CherryPickTask`, `GetCommitGraphTask`, etc.), each a `SheimiAsyncTask` subclass
  (a small custom replacement for Android's deprecated `AsyncTask`, backed by a cached thread
  pool + main-thread `Handler`). This is where actual JGit calls happen.
- **`com/manichord/mgit/`** — the new layer: Compose screens, Material3 theming, and per-screen
  `ViewModel`s (LiveData-based, e.g. `RepoDetailViewModel`, `RepoListViewModel`) that wrap the
  legacy `Repo` model and tasks rather than replacing them.

`MainActivity` (`com.manichord.mgit.MainActivity`) is the single host Activity, using Navigation
Compose — but this is a screen-by-screen migration, not a full rewrite: only some routes
(currently the repo list) are NavHost composables. Other screens are still separate legacy
Activities (e.g. `ViewFileActivity`, `RepoDetailActivity`, `CommitDiffActivity`), started by
Intent from within composables exactly as before. Don't assume a screen is reachable via NavHost
just because the app is "Compose-based" — check whether it's an Activity or a NavHost route.

## Bridging the two layers

`com.manichord.mgit.ui.components.FragmentHost` embeds a legacy `Fragment` inside Compose via
`AndroidView`. This is the mechanism `RepoDetailScreen` uses to host `CommitsFragment`,
`FilesFragment`, and `StatusFragment` as tabs. The implementation is deliberately subtle (see the
doc comment in `FragmentHost.kt`) — the fragment transaction must run in `update`, not `factory`,
because the container isn't attached to the window yet inside `factory`; and a fragment must be
detached before being re-attached to a new container id, because Compose's `Pager` can call
`update` more than once for the same fragment against different container ids during
subcomposition. Don't reimplement this per-screen; reuse `FragmentHost`.

Because `CommitsFragment` is the single shared host for both the main repo's Commits tab and a
file's per-file commit-history tab (`ViewFileActivity`), a change to a composable it hosts (e.g.
`CommitsListContent.kt`'s row layout) automatically applies to both surfaces — no separate
plumbing needed.

## Commit graph rendering

`com.manichord.mgit.repodetail.CommitGraphRenderer` subclasses JGit's package-private
`AbstractPlotRenderer` (the same lane/DAG layout engine EGit uses) to compute branch graph
geometry, then redirects its `draw*` callbacks into a Compose `DrawScope` instead of AWT —
subclassing is the only way to use this engine since the relevant fields are package-private to
`org.eclipse.jgit.revplot`. `GetCommitGraphTask` (full graph, JGit `PlotCommitList`) and the
plain linear `GetCommitTask` (used for per-file history, no real branch topology) are
distinguished by `supportsGraphMode()`.
