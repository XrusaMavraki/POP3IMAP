package gr.aueb.emailclient;

import javax.mail.Folder;

public class DeleteOldestEmailMain {

    private static final String CONNECTION_FILE_PATH = "C:\\POP3ini.txt";

    public static void main(String[] args) throws Exception {
        EmailConnector emailConnector = new EmailConnector(CONNECTION_FILE_PATH);
        Folder inbox = emailConnector.getInboxFolder();
        EmailService emailService = new DeleteOldestEmailService();
        boolean doExpunge = emailService.executeService(inbox);
        inbox.close(doExpunge);
    }
}
