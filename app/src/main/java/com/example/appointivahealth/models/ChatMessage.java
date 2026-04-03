package com.example.appointivahealth.models;

public class ChatMessage {
    private String messageId;
    private String senderId;
    private String senderRole;
    private String message;
    private long timestamp;
    private String fileUrl;
    private String fileName;
    private String fileType;

    public ChatMessage() {
    }

    public ChatMessage(String messageId, String senderId, String senderRole, String message, long timestamp) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderRole = senderRole;
        this.message = message;
        this.timestamp = timestamp;
    }

    public ChatMessage(String messageId, String senderId, String senderRole, String message, long timestamp, String fileUrl, String fileName, String fileType) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderRole = senderRole;
        this.message = message;
        this.timestamp = timestamp;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.fileType = fileType;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
}
