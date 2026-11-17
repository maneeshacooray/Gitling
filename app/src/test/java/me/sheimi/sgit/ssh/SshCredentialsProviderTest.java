package me.sheimi.sgit.ssh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.Test;

import java.net.URISyntaxException;

/**
 * Covers the auth half of issue #118: a server that only offers the password over
 * keyboard-interactive (PAM setups, rsync.net) failed with "Auth fail for methods
 * publickey,password,keyboard-interactive" even though the user typed a password, because jsch
 * sends a keyboard-interactive prompt as a {@link CredentialItem.StringType} and the delegate
 * {@link UsernamePasswordCredentialsProvider} does not answer those.
 */
public class SshCredentialsProviderTest {

    private static URIish uri() {
        try {
            return new URIish("ssh://user@example.com/repo.git");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /** A keyboard-interactive password prompt (note: not the exact text "Password: ") must be
     * answered with the entered password. */
    @Test
    public void answersKeyboardInteractivePasswordPrompt() {
        CredentialsProvider command =
                new UsernamePasswordCredentialsProvider("user", "s3cret");
        SshCredentialsProvider provider = new SshCredentialsProvider(command, command);

        // rsync.net / PAM phrase their prompt differently from JGit's hard-coded "Password: "
        CredentialItem.StringType prompt =
                new CredentialItem.StringType("Password:", true);

        assertTrue("keyboard-interactive prompt should be answered", provider.get(uri(), prompt));
        assertEquals("s3cret", prompt.getValue());
    }

    /** A key passphrase prompt is still answered from the keys provider, not the password. */
    @Test
    public void stillAnswersKeyPassphraseFromKeys() {
        CredentialsProvider command =
                new UsernamePasswordCredentialsProvider("user", "s3cret");
        CredentialsProvider keys = new CredentialsProvider() {
            @Override public boolean isInteractive() { return false; }
            @Override public boolean supports(CredentialItem... items) { return true; }
            @Override public boolean get(URIish uri, CredentialItem... items) {
                for (CredentialItem item : items) {
                    if (item instanceof CredentialItem.StringType) {
                        ((CredentialItem.StringType) item).setValue("key-pass");
                    }
                }
                return true;
            }
        };
        SshCredentialsProvider provider = new SshCredentialsProvider(command, keys);

        CredentialItem.StringType passphrase =
                new CredentialItem.StringType("Passphrase for /x/id_rsa", true);
        assertTrue(provider.get(uri(), passphrase));
        assertEquals("key-pass", passphrase.getValue());
    }

    /** A plain CredentialItem.Password (the password auth method, #109) still works. */
    @Test
    public void stillAnswersPasswordItem() {
        CredentialsProvider command =
                new UsernamePasswordCredentialsProvider("user", "s3cret");
        SshCredentialsProvider provider = new SshCredentialsProvider(command, command);

        CredentialItem.Password pw = new CredentialItem.Password();
        assertTrue(provider.get(uri(), pw));
        assertEquals("s3cret", new String(pw.getValue()));
    }
}
