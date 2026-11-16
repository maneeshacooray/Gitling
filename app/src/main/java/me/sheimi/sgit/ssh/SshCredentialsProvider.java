package me.sheimi.sgit.ssh;

import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;

/**
 * What an SSH session asks for, answered from the right place: passphrases for the app's
 * private keys from the saved passphrases ({@code keys}), and everything else (the password for
 * password or keyboard-interactive login) from the git command's own credentials, i.e. the
 * username and password entered in the "Needs Authentication" dialog ({@code command}).
 */
class SshCredentialsProvider extends CredentialsProvider {

    private final CredentialsProvider command;
    private final CredentialsProvider keys;

    SshCredentialsProvider(CredentialsProvider command, CredentialsProvider keys) {
        this.command = command;
        this.keys = keys;
    }

    @Override
    public boolean isInteractive() {
        return false;
    }

    @Override
    public boolean supports(CredentialItem... items) {
        return true;
    }

    @Override
    public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
        boolean filled = false;
        for (CredentialItem item : items) {
            if (isKeyPassphrase(item)) {
                filled |= keys.get(uri, item);
            } else if (item instanceof CredentialItem.StringType) {
                // A keyboard-interactive prompt: jsch delivers the server's own prompt text (e.g.
                // "Password:", or a PAM prompt) as a StringType. UsernamePasswordCredentialsProvider
                // only answers a StringType whose text is exactly "Password: ", so servers that
                // offer the password over keyboard-interactive and phrase the prompt differently
                // (rsync.net, many PAM setups) got no answer and the clone failed with "Auth fail
                // for methods ...,keyboard-interactive" even after the password was entered (#118).
                // Answer such a prompt with the entered password.
                filled |= fillFromPassword(uri, (CredentialItem.StringType) item);
            } else if (command != null && command.supports(item)) {
                filled |= command.get(uri, item);
            }
        }
        return filled;
    }

    /** Answers a keyboard-interactive prompt with the password the user entered (held by the git
     * command's own credentials provider). */
    private boolean fillFromPassword(URIish uri, CredentialItem.StringType item) {
        if (command == null) {
            return false;
        }
        CredentialItem.Password password = new CredentialItem.Password();
        try {
            if (command.get(uri, password) && password.getValue() != null) {
                item.setValue(new String(password.getValue()));
                return true;
            }
            return false;
        } finally {
            password.clear();
        }
    }

    /** JSch asks for a key's passphrase with the prompt "Passphrase for <key file>". */
    private static boolean isKeyPassphrase(CredentialItem item) {
        return item instanceof CredentialItem.StringType
                && item.getPromptText() != null
                && item.getPromptText().startsWith("Passphrase");
    }
}
