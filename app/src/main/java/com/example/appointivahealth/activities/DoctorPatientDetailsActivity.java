package com.example.appointivahealth.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appointivahealth.R;
import com.example.appointivahealth.adapters.MedicalDocumentAdapter;
import com.example.appointivahealth.models.MedicalDocument;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DoctorPatientDetailsActivity extends AppCompatActivity {

    private String recordId, appointmentId, patientId, patientName, symptoms, dateStr, previousPrescription;

    private TextView tvDocPatDetailsName, tvDocPatDetailsDate, tvDocPatDetailsSymptoms, tvDocPatDetailsPrevRx, tvNoDocPatDetailsDocs;
    private RecyclerView rvDocPatDetailsDocs;
    private EditText etDocPatDetailsNotes, etDocPatDetailsDiagnosis, etDocPatDetailsPrescription;
    private Button btnSaveDocPatDetailsRX;

    private MedicalDocumentAdapter adapter;
    private List<MedicalDocument> documentList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_patient_details);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarDocPatDetails);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // Retrieve intent extras
        Intent intent = getIntent();
        recordId = intent.getStringExtra("recordId");
        appointmentId = intent.getStringExtra("appointmentId");
        patientId = intent.getStringExtra("patientId");
        patientName = intent.getStringExtra("patientName");
        symptoms = intent.getStringExtra("symptoms");
        dateStr = intent.getStringExtra("date");
        previousPrescription = intent.getStringExtra("prescription");

        // Initialize UI
        tvDocPatDetailsName = findViewById(R.id.tvDocPatDetailsName);
        tvDocPatDetailsDate = findViewById(R.id.tvDocPatDetailsDate);
        tvDocPatDetailsSymptoms = findViewById(R.id.tvDocPatDetailsSymptoms);
        tvDocPatDetailsPrevRx = findViewById(R.id.tvDocPatDetailsPrevRx);
        tvNoDocPatDetailsDocs = findViewById(R.id.tvNoDocPatDetailsDocs);
        
        rvDocPatDetailsDocs = findViewById(R.id.rvDocPatDetailsDocs);
        etDocPatDetailsNotes = findViewById(R.id.etDocPatDetailsNotes);
        etDocPatDetailsDiagnosis = findViewById(R.id.etDocPatDetailsDiagnosis);
        etDocPatDetailsPrescription = findViewById(R.id.etDocPatDetailsPrescription);
        btnSaveDocPatDetailsRX = findViewById(R.id.btnSaveDocPatDetailsRX);

        // Setup views
        tvDocPatDetailsName.setText("Patient Name: " + (patientName != null ? patientName : "Unknown"));
        tvDocPatDetailsDate.setText("Date: " + (dateStr != null ? dateStr : ""));
        tvDocPatDetailsSymptoms.setText("Symptoms/Reason: " + (symptoms != null && !symptoms.isEmpty() ? symptoms : "None provided"));
        tvDocPatDetailsPrevRx.setText("Current Prescription: " + (previousPrescription != null && !previousPrescription.isEmpty() ? previousPrescription : "None"));

        // Fetch remaining database components
        if (recordId != null && !recordId.isEmpty()) {
            loadRecordDetails();
        }

        // Setup RecyclerView
        rvDocPatDetailsDocs.setLayoutManager(new LinearLayoutManager(this));
        documentList = new ArrayList<>();
        adapter = new MedicalDocumentAdapter(this, documentList);
        rvDocPatDetailsDocs.setAdapter(adapter);

        loadUploadedDocuments();

        btnSaveDocPatDetailsRX.setOnClickListener(v -> savePrescription());
        
        // Ensure recordId exists
        if (recordId == null || recordId.isEmpty()) {
            Toast.makeText(this, "Error: Record ID missing.", Toast.LENGTH_SHORT).show();
            btnSaveDocPatDetailsRX.setEnabled(false);
        }
    }

    private void loadRecordDetails() {
        DatabaseReference recordRef = FirebaseDatabase.getInstance().getReference("MedicalRecords").child(recordId);
        recordRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    if (snapshot.child("notes").exists()) {
                        String fetchedNotes = snapshot.child("notes").getValue(String.class);
                        if (fetchedNotes != null && !fetchedNotes.isEmpty()) {
                            etDocPatDetailsNotes.setText(fetchedNotes);
                        }
                    }
                    if (snapshot.child("diagnosis").exists()) {
                        String fetchedDiag = snapshot.child("diagnosis").getValue(String.class);
                        if (fetchedDiag != null && !fetchedDiag.isEmpty()) {
                            etDocPatDetailsDiagnosis.setText(fetchedDiag);
                        }
                    }
                    if (snapshot.child("prescription").exists()) {
                        String fetchedRx = snapshot.child("prescription").getValue(String.class);
                        if (fetchedRx != null && !fetchedRx.isEmpty() && !fetchedRx.equals("No prescription yet")) {
                            etDocPatDetailsPrescription.setText(fetchedRx);
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadUploadedDocuments() {
        if (appointmentId == null || appointmentId.isEmpty()) {
            tvNoDocPatDetailsDocs.setVisibility(View.VISIBLE);
            rvDocPatDetailsDocs.setVisibility(View.GONE);
            return;
        }

        DatabaseReference docRef = FirebaseDatabase.getInstance().getReference("Chats").child(appointmentId);
        docRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                documentList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        com.example.appointivahealth.models.ChatMessage message = ds.getValue(com.example.appointivahealth.models.ChatMessage.class);
                        if (message != null && message.getFileUrl() != null && !message.getFileUrl().isEmpty()) {
                            MedicalDocument doc = new MedicalDocument();
                            doc.setDocumentId(message.getMessageId());
                            doc.setAppointmentId(appointmentId);
                            doc.setFileName(message.getFileName());
                            doc.setFileType(message.getFileType());
                            doc.setFileUrl(message.getFileUrl());
                            doc.setUploadDate(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(new java.util.Date(message.getTimestamp())));
                            documentList.add(doc);
                        }
                    }
                }

                adapter.notifyDataSetChanged();

                if (documentList.isEmpty()) {
                    tvNoDocPatDetailsDocs.setVisibility(View.VISIBLE);
                    rvDocPatDetailsDocs.setVisibility(View.GONE);
                } else {
                    tvNoDocPatDetailsDocs.setVisibility(View.GONE);
                    rvDocPatDetailsDocs.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DoctorPatientDetailsActivity.this, "Failed to load documents", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void savePrescription() {
        String notes = etDocPatDetailsNotes.getText().toString().trim();
        String diagnosis = etDocPatDetailsDiagnosis.getText().toString().trim();
        String prescription = etDocPatDetailsPrescription.getText().toString().trim();

        if (notes.isEmpty() && diagnosis.isEmpty() && prescription.isEmpty()) {
            Toast.makeText(this, "Please write notes, diagnosis, or prescription", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference recordRef = FirebaseDatabase.getInstance().getReference("MedicalRecords").child(recordId);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("notes", notes);
        updates.put("diagnosis", diagnosis);
        updates.put("prescription", prescription);

        recordRef.updateChildren(updates).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(DoctorPatientDetailsActivity.this, "Saved successfully", Toast.LENGTH_SHORT).show();
                    tvDocPatDetailsPrevRx.setText("Current Prescription: " + prescription);
                    
                    // Note: This updates the data in MedicalRecords. Dashboard views listen to this implicitly.
                } else {
                    Toast.makeText(DoctorPatientDetailsActivity.this, "Failed to save data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
