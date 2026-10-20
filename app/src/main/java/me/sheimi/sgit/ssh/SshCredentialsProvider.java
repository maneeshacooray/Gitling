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
            } else if (command != null && command.supports(item)) {
                filled |= command.get(uri, item);
            }
        }
        return filled;
    }

    /** JSch asks for a key's passphrase with the prompt "Passphrase for <key file>". */
    private static boolean isKeyPassphrase(CredentialItem item) {
        return item instanceof CredentialItem.StringType
                && item.getPromptText() != null
                && item.getPromptText().startsWith("Passphrase");
    }
}
