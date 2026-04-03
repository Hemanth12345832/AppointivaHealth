package com.example.appointivahealth.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appointivahealth.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class PatientRecordDetailsActivity extends AppCompatActivity {

    private String patientId, patientName, doctorId;
    private TextView tvPatientNameDetail, tvNoDocuments;
    private EditText etConsultationNotes, etDiagnosis, etPrescriptionText;
    private Button btnSaveRecord, btnGeneratePdf;
    private androidx.recyclerview.widget.RecyclerView rvDocuments;
    private com.example.appointivahealth.adapters.MedicalDocumentAdapter documentAdapter;
    private java.util.List<com.example.appointivahealth.models.MedicalDocument> documentList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_record_details);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarRecordDetails);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        patientId = getIntent().getStringExtra("patientId");
        patientName = getIntent().getStringExtra("patientName");
        
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            doctorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        tvPatientNameDetail = findViewById(R.id.tvPatientNameDetail);
        tvNoDocuments = findViewById(R.id.tvNoDocuments);
        etConsultationNotes = findViewById(R.id.etConsultationNotes);
        etDiagnosis = findViewById(R.id.etDiagnosis);
        etPrescriptionText = findViewById(R.id.etPrescriptionText);
        btnSaveRecord = findViewById(R.id.btnSaveRecord);
        btnGeneratePdf = findViewById(R.id.btnGeneratePdf);
        rvDocuments = findViewById(R.id.rvDocuments);

        rvDocuments.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        documentList = new java.util.ArrayList<>();
        documentAdapter = new com.example.appointivahealth.adapters.MedicalDocumentAdapter(this, documentList);
        rvDocuments.setAdapter(documentAdapter);

        tvPatientNameDetail.setText(patientName != null ? patientName : "Unknown Patient");

        boolean isReadOnly = getIntent().getBooleanExtra("isReadOnly", false);
        if (isReadOnly) {
            etConsultationNotes.setFocusable(false);
            etConsultationNotes.setFocusableInTouchMode(false);
            etDiagnosis.setFocusable(false);
            etDiagnosis.setFocusableInTouchMode(false);
            etPrescriptionText.setFocusable(false);
            etPrescriptionText.setFocusableInTouchMode(false);
            btnSaveRecord.setVisibility(android.view.View.GONE);
        }

        btnSaveRecord.setOnClickListener(v -> saveRecordToFirebase());

        btnGeneratePdf.setOnClickListener(v -> {
            Intent intent = new Intent(PatientRecordDetailsActivity.this, PrescriptionActivity.class);
            intent.putExtra("patientId", patientId);
            intent.putExtra("patientName", patientName);
            intent.putExtra("prescriptionText", etPrescriptionText.getText().toString());
            startActivity(intent);
        });
        
        loadLatestRecord();
    }
    
    // Optional feature: preload latest record
    private void loadLatestRecord() {
        String appointmentIdObj = getIntent().getStringExtra("appointmentId");
        if (appointmentIdObj != null && !appointmentIdObj.isEmpty()) {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("MedicalRecords");
            ref.orderByChild("appointmentId").equalTo(appointmentIdObj).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                    for (com.google.firebase.database.DataSnapshot ds : snapshot.getChildren()) {
                        String notes = ds.child("notes").getValue(String.class);
                        String diagnosis = ds.child("diagnosis").getValue(String.class);
                        String prescription = ds.child("prescription").getValue(String.class);

                        if (notes != null) etConsultationNotes.setText(notes);
                        if (diagnosis != null) etDiagnosis.setText(diagnosis);
                        if (prescription != null) etPrescriptionText.setText(prescription);

                        if (patientName == null || patientName.isEmpty()) {
                            String name = ds.child("patientName").getValue(String.class);
                            if (name != null) tvPatientNameDetail.setText(name);
                        }
                        break;
                    }
                }
                @Override
                public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {}
            });

            loadUploadedDocuments(appointmentIdObj);
        }
    }

    private void loadUploadedDocuments(String appointmentId) {
        DatabaseReference docRef = FirebaseDatabase.getInstance().getReference("Chats").child(appointmentId);
        docRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                documentList.clear();
                if (snapshot.exists()) {
                    for (com.google.firebase.database.DataSnapshot ds : snapshot.getChildren()) {
                        com.example.appointivahealth.models.ChatMessage message = ds.getValue(com.example.appointivahealth.models.ChatMessage.class);
                        if (message != null && message.getFileUrl() != null && !message.getFileUrl().isEmpty()) {
                            com.example.appointivahealth.models.MedicalDocument doc = new com.example.appointivahealth.models.MedicalDocument();
                            doc.setDocumentId(message.getMessageId());
                            doc.setAppointmentId(appointmentId);
                            doc.setFileName(message.getFileName());
                            doc.setFileType(message.getFileType());
                            doc.setFileUrl(message.getFileUrl());
                            doc.setUploadDate(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(message.getTimestamp())));
                            documentList.add(doc);
                        }
                    }
                }

                documentAdapter.notifyDataSetChanged();

                if (documentList.isEmpty()) {
                    tvNoDocuments.setVisibility(android.view.View.VISIBLE);
                    rvDocuments.setVisibility(android.view.View.GONE);
                } else {
                    tvNoDocuments.setVisibility(android.view.View.GONE);
                    rvDocuments.setVisibility(android.view.View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {}
        });
    }

    private void saveRecordToFirebase() {
        String notes = etConsultationNotes.getText().toString().trim();
        String diagnosis = etDiagnosis.getText().toString().trim();
        String rxText = etPrescriptionText.getText().toString().trim();

        if (notes.isEmpty() && diagnosis.isEmpty() && rxText.isEmpty()) {
            Toast.makeText(this, "Please fill in some details to save.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("MedicalRecords");
        String recordId = ref.push().getKey();
        
        String date = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());

        HashMap<String, Object> recordUpdates = new HashMap<>();
        recordUpdates.put("recordId", recordId);
        recordUpdates.put("patientId", patientId);
        recordUpdates.put("doctorId", doctorId);
        recordUpdates.put("notes", notes);
        recordUpdates.put("diagnosis", diagnosis);
        recordUpdates.put("prescription", rxText);
        recordUpdates.put("date", date);
        // MedicalRecord model exists, but we are extending data visually
        recordUpdates.put("title", "Visit on " + date); // Used by patient side to view later

        if (recordId != null) {
            ref.child(recordId).setValue(recordUpdates).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(PatientRecordDetailsActivity.this, "Record saved successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PatientRecordDetailsActivity.this, "Failed to save record", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
