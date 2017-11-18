package gr.aueb.emailclient;

import com.sun.mail.util.MailSSLSocketFactory;
import javax.mail.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * I decided to use IMAP instead of POP3, as it is more modern, more robust and better supported.
 * IMAP gives real time feedback, while POP3 does not. It also gives important information such as the sent dates of
 * each email, while in POP3 it was up to the provider to decide whether to set this information.
 * Finally, I always use ssl/tls, because it's best practice and most modern providers refuse unencrypted connections anyway.
 * The decision of the protocol (ssl / tls) is decided by the javax mail API transparently and hence there is not need
 * to set it specifically.
 */
public class EmailConnector {

    private final ConnectionProperties connectionProperties;
    private Store store;

    public EmailConnector(String connectionFilePath) {
        Path connectionPath = Paths.get(connectionFilePath);
        try (BufferedReader br = Files.newBufferedReader(connectionPath)) {
            Properties props = new Properties();
            props.load(br);
            connectionProperties = new ConnectionProperties(props);
        } catch (IOException ioe) {
            System.err.println("Error while reading the connection properties: " + ioe.getMessage());
            throw new RuntimeException(ioe);
        }
    }

    public Folder getInboxFolder() {
        Folder folder;
        try {
            if (store == null) {
                connect();
            }
            folder = store.getFolder("INBOX");
            folder.open(Folder.READ_WRITE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return folder;
    }

    private void doConnect(String protocol, String host, int port) {
        try {
            MailSSLSocketFactory socketFactory = new MailSSLSocketFactory();
            socketFactory.setTrustAllHosts(true);
            Properties prop = new Properties();
            prop.put("mail.pop3s.ssl.socketFactory", socketFactory);
            prop.put("mail.imaps.ssl.socketFactory", socketFactory);
            Session session = Session.getInstance(prop);
            store = session.getStore(protocol);
            store.connect(host,
                    port,
                    connectionProperties.userName,
                    connectionProperties.password);
        } catch (Exception e) {
            // retry without ssl
            try {
                Session session = Session.getInstance(new Properties());
                store = session.getStore(protocol.substring(0, protocol.length() - 1));
                store.connect(host,
                        port,
                        connectionProperties.userName,
                        connectionProperties.password);
            } catch (Exception e2) {
                System.out.println("Failed to connect to server. Error was: " + e2.getMessage());
                store = null;
            }
        }
    }

    private void connect() throws Exception {
        if (connectionProperties.hasValidImapSettings() && connectionProperties.hasValidPopSettings()) {
            System.out.println("Both IMAP and pop3 settings have been defined. Please choose connection protocol.");
            System.out.println("1: POP3");
            System.out.println("2: IMAP");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine();
            switch (input.trim()) {
                case "2":
                    doConnect("imaps", connectionProperties.imapHost, Integer.parseInt(connectionProperties.imapPort));
                    break;
                case "1":
                default:
                    doConnect("pop3s", connectionProperties.popHost, Integer.parseInt(connectionProperties.popPort));
                    break;
            }
        } else if (connectionProperties.hasValidImapSettings()) {
            doConnect("imaps", connectionProperties.imapHost, Integer.parseInt(connectionProperties.imapPort));
        } else if (connectionProperties.hasValidPopSettings()) {
            doConnect("pop3s", connectionProperties.popHost, Integer.parseInt(connectionProperties.popPort));
        } else {
            throw new RuntimeException("No valid connection settings.");
        }
    }

    public class ConnectionProperties {
        private final String popHost;
        private final String popPort;
        private final String imapHost;
        private final String imapPort;
        private final String userName;
        private final String password;

        private ConnectionProperties(Properties properties) {
            popHost = properties.getProperty("pop.Host");
            popPort = properties.getProperty("pop.Port");
            imapHost = properties.getProperty("imap.Host");
            imapPort = properties.getProperty("imap.Port");
            userName = properties.getProperty("pop.Username");
            password = properties.getProperty("pop.Password");
        }

        public boolean hasValidPopSettings() {
            return popHost != null && popPort != null;
        }

        public boolean hasValidImapSettings() {
            return imapHost != null && imapPort != null;
        }
    }
}
