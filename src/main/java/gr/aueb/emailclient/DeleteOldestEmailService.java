package gr.aueb.emailclient;

import javax.mail.Flags;
import javax.mail.Folder;

public class DeleteOldestEmailService implements EmailService {

    @Override
    public boolean executeService(Folder inboxFolder) {
        boolean doExpunge = false;
        try {
            System.out.println("Deleting the oldest email.");
            int totalMessages = inboxFolder.getMessageCount();
            if (totalMessages > 0) {
                inboxFolder.getMessage(1).setFlag(Flags.Flag.DELETED, true);
                doExpunge = true;
            }
            System.out.println("Finished deleting the oldest email. The deletion may take some time to display in the actual system.");
        } catch (Exception e) {
            System.err.println("An error has occurred: " + e.getMessage());
        }
        return doExpunge;
    }
}
