package me.sheimi.sgit.ssh;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

import org.eclipse.jgit.internal.transport.ssh.jsch.CredentialsProviderUserInfo;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory;
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig.Host;
import org.eclipse.jgit.util.FS;

import java.io.File;

import me.sheimi.sgit.MGitApplication;

/**
 * Custom config for Jsch, including using user-provided private keys
 */
public class SGitSessionFactory extends JschConfigSessionFactory {

    @Override
    protected void configure(Host host, Session session) {
        session.setConfig("StrictHostKeyChecking", "no");
        // keyboard-interactive too: some servers (PAM based, e.g. some Debian setups) only offer
        // that for passwords
        session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");

        // Awful use of App singleton but not really any other way to get hold of a
        // provider that needs
        // to have been initialised with an Android context
        UserInfo userInfo = new CredentialsProviderUserInfo(session, MGitApplication.getJschCredentialsProvider());
        session.setUserInfo(userInfo);
    }

    /**
     * The session {@link #configure} sets up can only answer key passphrases, so a server asking
     * for a password got no answer and the clone failed with "Auth cancel" even after the user
     * entered the username and password (#109). Here JGit hands over the git command's own
     * credentials: answer passwords from those and passphrases as before.
     */
    @Override
    public Session createSession(CredentialsProvider credentialsProvider, FS fs, String user,
            String pass, String host, int port, Host hc) throws JSchException {
        Session session = super.createSession(credentialsProvider, fs, user, pass, host, port, hc);
        session.setUserInfo(new CredentialsProviderUserInfo(session,
                new SshCredentialsProvider(credentialsProvider, MGitApplication.getJschCredentialsProvider())));
        return session;
    }

    @Override
    protected JSch createDefaultJSch(FS fs) throws JSchException {
        JSch jsch = new JSch();
        PrivateKeyUtils.migratePrivateKeys();
        File sshDir = PrivateKeyUtils.getPrivateKeyFolder();
        for (File file : sshDir.listFiles()) {
            KeyPair kpair = KeyPair.load(jsch, file.getAbsolutePath());
            jsch.addIdentity(file.getAbsolutePath());
        }
        return jsch;
    }

}
