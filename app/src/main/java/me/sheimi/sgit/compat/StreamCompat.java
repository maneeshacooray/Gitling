package me.sheimi.sgit.compat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Java-8-compatible fallbacks for InputStream methods absent on Android < API 33 / 29.
// JGitCompatPlugin rewrites JGit bytecode at build time to call these instead of the
// native virtual methods, so the app works on minSdk 23 without core library desugaring
// needing to cover these specific APIs.
public final class StreamCompat {
    private static final int BUFFER_SIZE = 8192;

    private StreamCompat() {}

    // Replacement for InputStream.readNBytes(int) — available natively only on API 33+.
    //
    // Reads in bounded BUFFER_SIZE chunks rather than eagerly allocating a single `len`-sized
    // array up front, matching the real JDK implementation -- callers (JGit included) use
    // Integer.MAX_VALUE as a "read everything" idiom, relying on readNBytes to stop at EOF
    // instead of actually allocating a multi-gigabyte buffer.
    public static byte[] readNBytes(InputStream in, int len) throws IOException {
        if (len < 0) throw new IllegalArgumentException("len < 0");
        List<byte[]> chunks = null;
        byte[] result = null;
        int total = 0;
        int remaining = len;
        int n;
        do {
            byte[] buf = new byte[Math.min(remaining, BUFFER_SIZE)];
            int nread = 0;
            while ((n = in.read(buf, nread, buf.length - nread)) > 0) {
                nread += n;
                remaining -= n;
                if (nread == buf.length) break;
            }
            if (nread > 0) {
                byte[] chunk = nread == buf.length ? buf : Arrays.copyOf(buf, nread);
                if (result == null) {
                    result = chunk;
                } else {
                    if (chunks == null) {
                        chunks = new ArrayList<>();
                        chunks.add(result);
                    }
                    chunks.add(chunk);
                }
                total += nread;
            }
        } while (n >= 0 && remaining > 0);

        if (chunks == null) {
            return result == null ? new byte[0] : result;
        }
        byte[] combined = new byte[total];
        int offset = 0;
        for (byte[] chunk : chunks) {
            System.arraycopy(chunk, 0, combined, offset, chunk.length);
            offset += chunk.length;
        }
        return combined;
    }

    // Replacement for InputStream.readNBytes(byte[], int, int) — available natively only on API 33+.
    public static int readNBytes(InputStream in, byte[] b, int off, int len) throws IOException {
        if (off < 0 || len < 0 || off + len > b.length || off + len < 0) {
            throw new IndexOutOfBoundsException();
        }
        int n = 0;
        while (n < len) {
            int count = in.read(b, off + n, len - n);
            if (count < 0) break;
            n += count;
        }
        return n;
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
