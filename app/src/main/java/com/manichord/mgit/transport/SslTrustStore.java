package com.manichord.mgit.transport;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import me.sheimi.sgit.MGitApplication;

/**
 * Remembers the HTTPS hosts whose (self-signed or otherwise untrusted) certificate the user has
 * explicitly chosen to trust, so git operations to a self-hosted server can succeed (#56).
 *
 * <p>Trust is per host and opt-in: nothing is trusted until the user confirms the "Certificate not
 * trusted" dialog (see CloneTask). {@link MGitHttpConnection} checks this store when opening an
 * HTTPS connection and, for a trusted host, skips certificate verification for that connection.
 * Storing the decision (rather than relying on JGit's in-operation SSL retry) means the connection
 * trusts the certificate from the first handshake, which both avoids replaying a request on an
 * already-used HttpURLConnection and makes later fetch/pull/push to the same host work too.
 */
public final class SslTrustStore {

    private static final String PREF_FILE = "ssl_trusted_hosts";
    private static final String KEY_HOSTS = "hosts";

    private SslTrustStore() {
    }

    public static boolean isTrusted(String host) {
        return host != null && !host.isEmpty() && hosts().contains(host);
    }

    public static void trust(String host) {
        if (host == null || host.isEmpty()) {
            return;
        }
        Set<String> updated = new HashSet<>(hosts());
        if (updated.add(host)) {
            save(updated);
        }
    }

    public static void untrust(String host) {
        if (host == null) {
            return;
        }
        Set<String> updated = new HashSet<>(hosts());
        if (updated.remove(host)) {
            save(updated);
        }
    }

    /** Trusted hosts, sorted, for display in settings. */
    public static Set<String> trustedHosts() {
        return new TreeSet<>(hosts());
    }

    private static Set<String> hosts() {
        return prefs().getStringSet(KEY_HOSTS, Collections.emptySet());
    }

    private static void save(Set<String> hosts) {
        // Store a fresh copy: a Set returned by getStringSet must not be mutated in place.
        prefs().edit().putStringSet(KEY_HOSTS, new HashSet<>(hosts)).apply();
    }

    private static SharedPreferences prefs() {
        return MGitApplication.getContext()
                .getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }
}
