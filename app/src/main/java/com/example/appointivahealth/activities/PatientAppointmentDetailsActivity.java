package com.example.appointivahealth.activities;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appointivahealth.R;
import com.example.appointivahealth.models.Appointment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class PatientAppointmentDetailsActivity extends AppCompatActivity {

    private String appointmentId;
    private TextView tvDetailsDoctorName, tvDetailsPatientDateTime, tvDetailsPatientStatus, tvDetailsCancellationMessage;
    private TextView tvDetailsNotes, tvDetailsDiagnosis, tvDetailsPrescription;

    private Appointment appointment;
    private Button btnSubmitComplaint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_appointment_details);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarPatientApptDetails);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        appointmentId = getIntent().getStringExtra("appointmentId");

        tvDetailsDoctorName = findViewById(R.id.tvDetailsDoctorName);
        tvDetailsPatientDateTime = findViewById(R.id.tvDetailsPatientDateTime);
        tvDetailsPatientStatus = findViewById(R.id.tvDetailsPatientStatus);
        tvDetailsCancellationMessage = findViewById(R.id.tvDetailsCancellationMessage);
        tvDetailsNotes = findViewById(R.id.tvDetailsNotes);
        tvDetailsDiagnosis = findViewById(R.id.tvDetailsDiagnosis);
        tvDetailsPrescription = findViewById(R.id.tvDetailsPrescription);
        btnSubmitComplaint = findViewById(R.id.btnSubmitComplaint);

        btnSubmitComplaint.setOnClickListener(v -> showComplaintDialog());

        if (appointmentId != null) {
            loadAppointmentDetails();
            loadMedicalRecord();
        } else {
            Toast.makeText(this, "Error loading appointment", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadMedicalRecord() {
        DatabaseReference recordsRef = FirebaseDatabase.getInstance().getReference("MedicalRecords");
        recordsRef.orderByChild("appointmentId").equalTo(appointmentId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String notes = ds.child("notes").getValue(String.class);
                        String diagnosis = ds.child("diagnosis").getValue(String.class);
                        String prescription = ds.child("prescription").getValue(String.class);

                        tvDetailsNotes.setText("Notes: " + (notes != null && !notes.isEmpty() ? notes : "No notes listed"));
                        tvDetailsDiagnosis.setText("Diagnosis: " + (diagnosis != null && !diagnosis.isEmpty() ? diagnosis : "No diagnosis listed"));
                        tvDetailsPrescription.setText("Prescription: " + (prescription != null && !prescription.isEmpty() ? prescription : "No prescription listed"));
                        return;
                    }
                } else {
                    tvDetailsNotes.setText("Notes: Not available yet");
                    tvDetailsDiagnosis.setText("Diagnosis: Not available yet");
                    tvDetailsPrescription.setText("Prescription: Not available yet");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void loadAppointmentDetails() {
        DatabaseReference apptRef = FirebaseDatabase.getInstance().getReference("Appointments").child(appointmentId);
        apptRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    appointment = snapshot.getValue(Appointment.class);
                    if (appointment != null) {
                        tvDetailsDoctorName.setText("Dr. " + appointment.getDoctorName());
                        tvDetailsPatientDateTime.setText("Date: " + appointment.getDate() + " | Time: " + appointment.getTime());
                        tvDetailsPatientStatus.setText("Status: " + appointment.getStatus());
                        
                        if ("Cancelled".equalsIgnoreCase(appointment.getStatus()) && appointment.getCancellationMessage() != null && !appointment.getCancellationMessage().isEmpty()) {
                            tvDetailsCancellationMessage.setVisibility(View.VISIBLE);
                            tvDetailsCancellationMessage.setText("Doctor's Message: " + appointment.getCancellationMessage());
                        } else {
                            tvDetailsCancellationMessage.setVisibility(View.GONE);
                        }
                        
                        if ("Completed".equalsIgnoreCase(appointment.getStatus())) {
                            btnSubmitComplaint.setVisibility(View.VISIBLE);
                        } else {
                            btnSubmitComplaint.setVisibility(View.GONE);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void showComplaintDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Submit Feedback");
        builder.setMessage("Please provide your feedback regarding this appointment:");
        
        final EditText input = new EditText(this);
        input.setHint("Write your feedback here...");
        
        // Add some padding to the EditText in the dialog
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams params = new  android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 50; params.rightMargin = 50;
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Submit", (dialog, which) -> {
            String complaintText = input.getText().toString().trim();
            if(!complaintText.isEmpty()) {
                submitComplaintToFirebase(complaintText);
            } else {
                Toast.makeText(this, "Feedback cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void submitComplaintToFirebase(String text) {
        DatabaseReference complaintsRef = FirebaseDatabase.getInstance().getReference("Complaints");
        String complaintId = complaintsRef.push().getKey();
        String patientId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        com.example.appointivahealth.models.Complaint complaint = new com.example.appointivahealth.models.Complaint();
        complaint.setComplaintId(complaintId);
        complaint.setPatientId(patientId);
        complaint.setPatientName(appointment.getPatientName() != null ? appointment.getPatientName() : "Unknown");
        complaint.setDoctorId(appointment.getDoctorId());
        complaint.setDoctorName(appointment.getDoctorName());
        complaint.setAppointmentId(appointmentId);
        complaint.setTitle("Feedback for Appointment on " + appointment.getDate());
        complaint.setDescription(text);
        complaint.setDate(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date()));
        complaint.setStatus("Pending");

        if (complaintId != null) {
            complaintsRef.child(complaintId).setValue(complaint).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Feedback submitted successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to submit feedback", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
