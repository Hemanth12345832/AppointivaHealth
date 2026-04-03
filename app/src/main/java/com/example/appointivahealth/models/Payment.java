package com.example.appointivahealth.models;

import java.io.Serializable;

public class Payment implements Serializable {
    private String paymentId;
    private String patientId;
    private String doctorId;
    private String amount;
    private String transactionId;
    private String date;
    private String status;

    public Payment() {}

    public Payment(String paymentId, String patientId, String doctorId, String amount, String transactionId, String date, String status) {
        this.paymentId = paymentId;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.amount = amount;
        this.transactionId = transactionId;
        this.date = date;
        this.status = status;
    }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
