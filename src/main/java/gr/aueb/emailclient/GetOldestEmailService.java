package gr.aueb.emailclient;

import javax.mail.Folder;
import javax.mail.Message;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class GetOldestEmailService implements EmailService {


    private static final String FILE_ROOT = "C:\\000-POP3-FILES";
    private static final String FILE_TEST_IN_PROGRESS = "WWW_TEST_IN_PROGRESS.txt";
    private static final String FILE_ERROR = "WWW_ERROR_MESSAGE.txt";
    private static final String FILE_BODY_PARTIAL = "WWW_BODY.txt";
    private static final String FILE_BODY_FULL = "WWW_BODY_COMPLETE.txt";

    @Override
    public boolean executeService(Folder inboxFolder) {
        try {
            System.out.println("Getting the oldest email.");
            Files.createDirectories(Paths.get(FILE_ROOT));
            Files.list(Paths.get(FILE_ROOT)).forEach(f -> {
                try {
                    Files.delete(f);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            int totalMessages = inboxFolder.getMessageCount();
            if (totalMessages > 0) {
                EmailMessage oldestEmail = new EmailMessage(inboxFolder.getMessage(1));
                createTestInProgressFile(totalMessages, oldestEmail);
                String emailBody = oldestEmail.getBody();
                createBodyFile(emailBody);
                createFullBodyFile(emailBody);
                createAttachments(oldestEmail);
                System.out.println("Finished getting the oldest email. The results are under " + FILE_ROOT);
            } else {
                System.out.println("There are no messages in inbox.");
            }
        } catch (Exception e) {
            createErrorFile();
            System.err.println("An error has occurred: " + e.getMessage());
        }
        // we never want to expunge the folder (used only for deletions).
        return false;
    }

    private void createAttachments(EmailMessage oldestEmail) throws IOException {
        Path path = Paths.get(FILE_ROOT);
        oldestEmail.getAttachments().forEach(f -> {
            try {
                Files.copy(f.toPath(), path.resolve(f.getName()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void createFullBodyFile(String emailBody) throws IOException {
        Path path = Paths.get(FILE_ROOT, FILE_BODY_FULL);
        Files.deleteIfExists(path);
        try (BufferedWriter bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            bw.write(emailBody);
        }
    }

    private void createBodyFile(String emailBody) throws IOException {
        Path path = Paths.get(FILE_ROOT, FILE_BODY_PARTIAL);
        Files.deleteIfExists(path);
        try (BufferedWriter bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            int maxLength = Math.min(emailBody.length(), 800);
            bw.write(emailBody.substring(0, maxLength));
        }
    }

    private void createTestInProgressFile(int totalMessages, EmailMessage oldestEmail) throws IOException {
        Path path = Paths.get(FILE_ROOT, FILE_TEST_IN_PROGRESS);
        Files.deleteIfExists(path);
        Map<String, String> outputInformation = new LinkedHashMap<>();
        outputInformation.put("totMessagesInInbox", Integer.toString(totalMessages));
        outputInformation.put("curMessageNum", totalMessages == 0 ? "0" : "1");
        outputInformation.put("senderMailX", oldestEmail.getSenderEmail());
        outputInformation.put("senderMailDisplayX", oldestEmail.getSenderPersonal());
        outputInformation.put("senderMailTO", oldestEmail.getRecipientTypeToRecipient().get(Message.RecipientType.TO).stream().collect(Collectors.joining(", ")));
        outputInformation.put("senderMailCC", oldestEmail.getRecipientTypeToRecipient().get(Message.RecipientType.CC).stream().collect(Collectors.joining(", ")));
        outputInformation.put("subjectX", oldestEmail.getSubject());
        outputInformation.put("totFilesAttachedX", Integer.toString(oldestEmail.getAttachments().size()));
        if (oldestEmail.getReceivedDate() != null) {
            outputInformation.put("dateReceivedX", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(oldestEmail.getReceivedDate()));
        } else {
            outputInformation.put("dateReceivedX", "Unknown");
        }
        try (BufferedWriter bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            bw.write(outputInformation.entrySet().stream()
                    .map(entry -> entry.getKey() + " = " + entry.getValue())
                    .collect(Collectors.joining(System.lineSeparator())));
        }
    }

    private void createErrorFile() {
        try {
            if (!Files.exists(Paths.get(FILE_ROOT).resolve(FILE_ERROR)))
                Files.createFile(Paths.get(FILE_ROOT).resolve(FILE_ERROR));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
