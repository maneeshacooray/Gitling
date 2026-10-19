package com.manichord.mgit.ui.components

import android.view.View
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

/**
 * Hosts a legacy [Fragment] inside Compose. Use this instead of reinventing it per-screen --
 * the implementation below is subtle and was the source of a 100%-reproducible crash before
 * being fixed.
 *
 * Only build the (empty, unattached) container in `factory`. Running the fragment transaction
 * synchronously inside factory crashes ("No view found for id ...") because the FrameLayout
 * isn't attached to the window yet at that point, so FragmentManager can't resolve its id --
 * do the transaction in `update` instead, which Compose runs after the view is attached.
 *
 * Compose's Pager also subcomposes pages for measurement/prefetch, which can invoke `update`
 * more than once for the same shared fragment instance against a different container id each
 * time; FragmentManager would still have it tied to a previous, now-discarded container, so
 * detach it first before re-adding so repeated calls stay idempotent.
 */
@Composable
fun FragmentHost(fragmentManager: FragmentManager, fragment: Fragment) {
    AndroidView(
        factory = { context ->
            FrameLayout(context).apply { id = View.generateViewId() }
        },
        update = { view ->
            if (fragmentManager.findFragmentById(view.id) !== fragment) {
                if (fragment.isAdded) {
                    fragmentManager.beginTransaction()
                        .remove(fragment)
                        .commitNowAllowingStateLoss()
                }
                fragmentManager.beginTransaction()
                    .replace(view.id, fragment)
                    .commitNowAllowingStateLoss()
            }
        }
    )
}

/**
 * Keeps [fragment] added to [fragmentManager] without a view, for the lifetime of this
 * composition, and draws its Compose [content] directly here instead of inside an AndroidView.
 * Unlike [FragmentHost], scrolling inside the content then reaches the screen around it (e.g. a
 * collapsing top bar). The fragment still does everything else a fragment does: its git tasks,
 * dialogs and action mode.
 */
@Composable
fun HeadlessFragment(fragmentManager: FragmentManager, fragment: Fragment, tag: String, content: @Composable () -> Unit) {
    androidx.compose.runtime.DisposableEffect(fragment) {
        // A copy restored by the FragmentManager after the Activity was recreated is stale: this
        // composition made its own instance
        fragmentManager.findFragmentByTag(tag)?.takeIf { it !== fragment }?.let {
            fragmentManager.beginTransaction().remove(it).commitNowAllowingStateLoss()
        }
        if (!fragment.isAdded) {
            fragmentManager.beginTransaction().add(fragment, tag).commitNowAllowingStateLoss()
        }
        onDispose {
            if (fragment.isAdded && !fragmentManager.isDestroyed) {
                fragmentManager.beginTransaction().remove(fragment).commitNowAllowingStateLoss()
            }
        }
    }
    content()
}
