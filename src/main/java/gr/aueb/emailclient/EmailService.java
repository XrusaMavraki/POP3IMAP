package gr.aueb.emailclient;

import javax.mail.Folder;

public interface EmailService {
    boolean executeService(Folder inboxFolder);
}
