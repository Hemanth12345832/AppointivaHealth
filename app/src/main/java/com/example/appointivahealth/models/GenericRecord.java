package com.example.appointivahealth.models;

public class GenericRecord {
    public String title;
    public String subtitle;
    public String date;
    public String status;
    public String recordId;
    public String appointmentId;

    public GenericRecord(String title, String subtitle, String date, String status) {
        this.title = title;
        this.subtitle = subtitle;
        this.date = date;
        this.status = status;
    }
}
