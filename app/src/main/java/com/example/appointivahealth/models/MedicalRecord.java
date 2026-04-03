package com.example.appointivahealth.models;

import java.io.Serializable;

public class MedicalRecord implements Serializable {
    private String recordId;
    private String patientId;
    private String doctorId;
    private String appointmentId;
    private String patientName;
    private String title;
    private String reportUrl;
    private String date;
    private String prescription;
    private String diagnosis;
    private String notes;
    private String symptoms;

    public MedicalRecord() {}

    public MedicalRecord(String recordId, String patientId, String doctorId, String title, String reportUrl, String date) {
        this.recordId = recordId;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.title = title;
        this.reportUrl = reportUrl;
        this.date = date;
    }

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }
    
    // Alias for setId requested by PrescriptionActivity
    public void setId(String id) { this.recordId = id; }
    public String getId() { return recordId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getReportUrl() { return reportUrl; }
    public void setReportUrl(String reportUrl) { this.reportUrl = reportUrl; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getPrescription() { return prescription; }
    public void setPrescription(String prescription) { this.prescription = prescription; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }
}
