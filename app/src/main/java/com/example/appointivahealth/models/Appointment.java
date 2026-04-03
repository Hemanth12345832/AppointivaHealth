package com.example.appointivahealth.models;

import java.io.Serializable;

public class Appointment implements Serializable {
    private String id;
    private String patientId;
    private String patientName;
    private String doctorId;
    private String doctorName;
    private String date; // Format: yyyy-MM-dd
    private String time;
    private String status; // "Pending", "Accepted", "Rejected"
    private String prescription; 
    private String message;
    private String paymentStatus; // "pending_payment", "paid", "verified", "rejected"
    private String paymentMode; // "UPI"
    private boolean adminVerified;
    private String transactionId;
    private String amount;
    private String reminderSentAt; // ISO timestamp when 1-hour reminder was sent
    private String cancellationMessage;
    
    public Appointment() {
        // Required for Firebase
    }

    public Appointment(String id, String patientId, String patientName, String doctorId, String doctorName, String date, String time, String status) {
        this.id = id;
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.date = date;
        this.time = time;
        this.status = status;
        this.prescription = "";
        this.message = "";
        this.paymentStatus = "pending_payment";
        this.paymentMode = "";
        this.adminVerified = false;
        this.transactionId = "";
        this.amount = "";
        this.reminderSentAt = "";
        this.cancellationMessage = "";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPrescription() { return prescription; }
    public void setPrescription(String prescription) { this.prescription = prescription; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public boolean isAdminVerified() { return adminVerified; }
    public void setAdminVerified(boolean adminVerified) { this.adminVerified = adminVerified; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }

    public String getReminderSentAt() { return reminderSentAt; }
    public void setReminderSentAt(String reminderSentAt) { this.reminderSentAt = reminderSentAt; }

    public String getCancellationMessage() { return cancellationMessage; }
    public void setCancellationMessage(String cancellationMessage) { this.cancellationMessage = cancellationMessage; }
}
