package gr.aueb.emailclient;

import org.jsoup.Jsoup;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMultipart;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class EmailMessage {

    private final Address sender;
    private final String subject;
    private final Date receivedDate;
    private final Map<Message.RecipientType, List<String>> recipientTypeToRecipient;
    private final List<File> attachments;
    private final List<String> stringBodyParts;
    private final List<String> htmlBodyParts;

    public EmailMessage(Message message) {
        try {
            subject = message.getSubject();
            sender = message.getFrom()[0];
            receivedDate = message.getReceivedDate();
            recipientTypeToRecipient = resolveRecipients(message);
            attachments = new LinkedList<>();
            stringBodyParts = new LinkedList<>();
            htmlBodyParts = new LinkedList<>();
            parseMessage(message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getSenderEmail() {
        if (sender instanceof InternetAddress) {
            return ((InternetAddress) sender).getAddress();
        }
        return sender.toString();
    }

    public String getSenderPersonal() {
        if (sender instanceof InternetAddress) {
            return ((InternetAddress) sender).getPersonal();
        }
        return "";
    }

    public String getSubject() {
        return subject;
    }

    public Map<Message.RecipientType, List<String>> getRecipientTypeToRecipient() {
        return recipientTypeToRecipient;
    }

    public List<File> getAttachments() {
        return attachments;
    }

    public Date getReceivedDate() {
        return receivedDate;
    }

    /**
     * Returns the plain text of the message. If the message contained plain text then this is returned.
     * If it didn't contain plain text, but it contained html, the html is returned after it is parsed to plain text.
     * @return The plain text of the message.
     */
    public String getBody() {
        String body = stringBodyParts.stream()
                    .collect(Collectors.joining(System.lineSeparator()))
                    .trim();
        if (body.isEmpty()) {
            body = htmlBodyParts.stream()
                    .collect(Collectors.joining(System.lineSeparator()))
                    .trim();
        }
        return body;
    }

    private Map<Message.RecipientType, List<String>> resolveRecipients(Message message) throws MessagingException {
        final Map<Message.RecipientType, List<String>> recipients = new HashMap<>();
        List<Message.RecipientType> types = Arrays.asList(Message.RecipientType.TO, Message.RecipientType.CC, Message.RecipientType.BCC);
        for (Message.RecipientType type : types) {
            List<String> listOfRecipientsForCurrentType = new LinkedList<>();
            recipients.put(type, listOfRecipientsForCurrentType);
            // for each type (CC, TO, BCC) extract from the Message, the recipients
            Address[] addresses = message.getRecipients(type); // java implementation that returns the recipients only for the given type
            if (addresses != null) {
                listOfRecipientsForCurrentType.addAll(
                        Arrays.stream(addresses)
                                .map(Address::toString)
                                .collect(Collectors.toList())
                );
            }
        }
        return recipients;
    }

    private void parseMessage(Message message) throws Exception {
        if (message.isMimeType("text/plain")) {
            stringBodyParts.add(message.getContent().toString());
        } else if (message.isMimeType("multipart/*")) {
            handleMimeMultipartMessage((MimeMultipart) message.getContent());
        }
    }

    private void handleMimeMultipartMessage(MimeMultipart mimeMultipart) throws Exception {
        for (int i = 0; i < mimeMultipart.getCount(); i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                stringBodyParts.add(bodyPart.getContent().toString());
            } else if (bodyPart.isMimeType("text/html")) {
                htmlBodyParts.add(Jsoup.parse((String) bodyPart.getContent()).text());
            } else if(Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) &&
                    bodyPart.getFileName() != null && !bodyPart.getFileName().isEmpty()) {
                InputStream is = bodyPart.getInputStream();
                if (!Files.exists(Paths.get("attachments"))) {
                    Files.createDirectory(Paths.get("attachments"));
                }
                File f = new File("attachments/" + bodyPart.getFileName());
                FileOutputStream fos = new FileOutputStream(f);
                byte[] buf = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buf)) != -1) {
                    fos.write(buf, 0, bytesRead);
                }
                fos.close();
                attachments.add(f);
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                handleMimeMultipartMessage((MimeMultipart) bodyPart.getContent());
            }
        }
    }
}
