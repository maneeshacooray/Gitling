package com.manichord.mgit.transport;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import timber.log.Timber;

/**
 * Best-effort detection of a remote's git object format, used to give a clear message for
 * SHA-256 repositories, which JGit cannot read yet (see eclipse-jgit/jgit#73, Gitling #126).
 *
 * <p>A clone of a SHA-256 repo fails deep in JGit with a cryptic "Short read of block" while
 * parsing the ref advertisement. That message is generic (a truncated or non-git response triggers
 * it too), so rather than guess from it, this probes the remote's smart-HTTP v2 ref advertisement,
 * which contains an explicit {@code object-format=sha256} capability line, and reports SHA-256 only
 * when that marker is actually present. Only HTTP(S) remotes can be probed; anything else returns
 * false.
 */
public final class RemoteObjectFormat {

    private static final String SHA256_MARKER = "object-format=sha256";
    private static final int TIMEOUT_MS = 10_000;
    /** The capability advertisement is at the very start of the response; a few KB is plenty. */
    private static final int MAX_READ = 16 * 1024;

    private RemoteObjectFormat() {
    }

    /**
     * @return true only if {@code remoteUrl} is an HTTP(S) remote whose ref advertisement declares
     *         the SHA-256 object format. Returns false for any other scheme, and on any error, so a
     *         caller can fall back to its normal handling.
     */
    public static boolean isSha256Remote(String remoteUrl) {
        if (remoteUrl == null || remoteUrl.trim().isEmpty()) {
            return false;
        }
        String trimmed = remoteUrl.trim();
        HttpURLConnection conn = null;
        try {
            String scheme = new URI(trimmed).getScheme();
            if (scheme == null) {
                return false;
            }
            scheme = scheme.toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https")) {
                return false;
            }
            String stripped = trimmed.endsWith("/")
                    ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
            URL infoRefs = new URL(stripped + "/info/refs?service=git-upload-pack");
            conn = (HttpURLConnection) infoRefs.openConnection();
            if (conn instanceof HttpsURLConnection
                    && SslTrustStore.isTrusted(infoRefs.getHost())) {
                // The user already trusted this host's certificate for the clone; honor it here too.
                installTrustAll((HttpsURLConnection) conn);
            }
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Git-Protocol", "version=2");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return false;
            }
            try (InputStream in = conn.getInputStream()) {
                return readPrefix(in).contains(SHA256_MARKER);
            }
        } catch (Exception e) {
            Timber.d(e, "SHA-256 remote probe failed for %s", trimmed);
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String readPrefix(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] chunk = new byte[2048];
        int n;
        while (out.size() < MAX_READ && (n = in.read(chunk)) != -1) {
            out.write(chunk, 0, n);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static void installTrustAll(HttpsURLConnection https) throws Exception {
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, new TrustManager[]{new MGitHttpConnection.TrustAllManager()},
                new SecureRandom());
        https.setSSLSocketFactory(ctx.getSocketFactory());
        https.setHostnameVerifier((hostname, session) -> true);
    }
}
