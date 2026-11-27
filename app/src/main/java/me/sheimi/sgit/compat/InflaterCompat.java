package me.sheimi.sgit.compat;

import java.util.zip.Inflater;

/**
 * Android-compatible replacement for org.eclipse.jgit.lib.InflaterCache.
 *
 * <p>At build time the bytecode patch in app/build.gradle.kts rewrites every
 * {@code InflaterCache.get()} / {@code InflaterCache.release(Inflater)} call inside
 * JGit into a call to the matching method here, so JGit's inflater pool is bypassed
 * entirely.
 *
 * <p>Why: JGit's InflaterCache pools up to four Inflaters. Its release() resets a
 * returned inflater and puts it back in the pool for the next get() to hand out. On
 * Android, closing a java.util.zip.InflaterInputStream ends (closes) the Inflater it
 * wraps even when that Inflater was supplied by the caller rather than created by the
 * stream, so an inflater handed back to the pool can already be closed. A later
 * reset() or inflate() on that pooled, already-closed inflater throws
 * "Inflater has been closed".
 *
 * <p>This is what broke large clones (issue #118): checking out a blob larger than
 * JGit's stream-file threshold streams it through a pooled inflater via
 * DirCacheCheckout.getContent, the stream close poisons the pool, and the next object
 * that borrows that inflater crashes the whole clone. Small repos never cross the
 * threshold, which is why it only showed up on large ones.
 *
 * <p>Bypassing the pool (a fresh inflater per get, ended on release) means no inflater
 * is ever reused after it has been closed. The only cost is a few extra short-lived
 * Inflater allocations, which is negligible next to the I/O of a clone.
 */
public final class InflaterCompat {

    private InflaterCompat() {
    }

    /**
     * Replacement for InflaterCache.get(): always returns a fresh inflater.
     * Matches JGit's own fallback, which constructs {@code new Inflater(false)}
     * (zlib-wrapped, not raw).
     */
    public static Inflater get() {
        return new Inflater(false);
    }

    /**
     * Replacement for InflaterCache.release(): ends the inflater instead of resetting
     * and re-pooling it. Never throws, so releasing an inflater that Android has
     * already closed is a harmless no-op rather than a crash.
     */
    public static void release(Inflater inflater) {
        if (inflater != null) {
            try {
                inflater.end();
            } catch (Throwable ignored) {
                // Already ended or closed; nothing left to free.
            }
        }
    }
}
