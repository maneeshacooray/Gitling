package me.sheimi.sgit.compat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

// Java-8-compatible fallbacks for InputStream methods absent on Android < API 33 / 29.
// JGitCompatPlugin rewrites JGit bytecode at build time to call these instead of the
// native virtual methods, so the app works on minSdk 23 without core library desugaring
// needing to cover these specific APIs.
public final class StreamCompat {
    private static final int BUFFER_SIZE = 8192;

    private StreamCompat() {}

    // Replacement for InputStream.readNBytes(int) — available natively only on API 33+.
    public static byte[] readNBytes(InputStream in, int len) throws IOException {
        if (len < 0) throw new IllegalArgumentException("len < 0");
        byte[] buf = new byte[len];
        int totalRead = 0;
        while (totalRead < len) {
            int n = in.read(buf, totalRead, len - totalRead);
            if (n < 0) break;
            totalRead += n;
        }
        return totalRead == len ? buf : Arrays.copyOf(buf, totalRead);
    }

    // Replacement for InputStream.transferTo(OutputStream) — available natively only on API 29+.
    public static long transferTo(InputStream in, OutputStream out) throws IOException {
        long transferred = 0;
        byte[] buf = new byte[BUFFER_SIZE];
        int read;
        while ((read = in.read(buf, 0, buf.length)) >= 0) {
            out.write(buf, 0, read);
            transferred += read;
        }
        return transferred;
    }
}
