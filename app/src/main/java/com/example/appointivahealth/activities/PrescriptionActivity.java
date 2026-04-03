package com.example.appointivahealth.activities;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appointivahealth.R;
import com.example.appointivahealth.models.MedicalRecord;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PrescriptionActivity extends AppCompatActivity {

    private String appointmentId, patientId, doctorId, patientName;
    private boolean isViewOnly = false;
    private TextView tvPrescriptionPatientName;
    private EditText etPrescriptionContent;
    private Button btnSavePrescription, btnGeneratePDF;
    private MedicalRecord existingRecord;
    private String recordId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prescription);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarPrescription);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        appointmentId = getIntent().getStringExtra("appointmentId");
        patientId = getIntent().getStringExtra("patientId");
        patientName = getIntent().getStringExtra("patientName");
        isViewOnly = getIntent().getBooleanExtra("viewOnly", false);
        
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            doctorId = FirebaseAuth.getInstance().getCurrentUser().getUid(); // Doctor's ID (or Patient's if Patient viewing, handled differently)
        }

        tvPrescriptionPatientName = findViewById(R.id.tvPrescriptionPatientName);
        etPrescriptionContent = findViewById(R.id.etPrescriptionContent);
        btnSavePrescription = findViewById(R.id.btnSavePrescription);
        btnGeneratePDF = findViewById(R.id.btnGeneratePDF);

        if (patientName != null) {
            tvPrescriptionPatientName.setText("Patient: " + patientName);
        }

        if (isViewOnly) {
            btnSavePrescription.setVisibility(View.GONE);
            etPrescriptionContent.setEnabled(false);
            toolbar.setTitle("View Prescription");
        }

        if (appointmentId != null) {
            loadExistingPrescription();
        }

        btnSavePrescription.setOnClickListener(v -> savePrescriptionToFirebase());
        btnGeneratePDF.setOnClickListener(v -> generatePDF());
    }

    private void loadExistingPrescription() {
        DatabaseReference recordsRef = FirebaseDatabase.getInstance().getReference("MedicalRecords");
        Query query = recordsRef.orderByChild("appointmentId").equalTo(appointmentId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        existingRecord = ds.getValue(MedicalRecord.class);
                        recordId = ds.getKey();
                        if (existingRecord != null) {
                            etPrescriptionContent.setText(existingRecord.getPrescription());
                            tvPrescriptionPatientName.setText("Patient: " + existingRecord.getPatientName());
                        }
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void savePrescriptionToFirebase() {
        String prescriptionText = etPrescriptionContent.getText().toString().trim();
        if (prescriptionText.isEmpty()) {
            Toast.makeText(this, "Please enter prescription first", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference recordsRef = FirebaseDatabase.getInstance().getReference("MedicalRecords");
        
        if (recordId == null) {
            recordId = recordsRef.push().getKey();
        }

        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        MedicalRecord record = new MedicalRecord();
        record.setId(recordId);
        record.setPatientId(patientId);
        record.setDoctorId(doctorId);
        record.setAppointmentId(appointmentId);
        record.setPatientName(patientName != null ? patientName : "Unknown");
        record.setDate(currentDate);
        record.setPrescription(prescriptionText);
        record.setDiagnosis("Consultation Diagnosis"); // Generic or extracted if a field existed
        record.setNotes(""); // Blank by default

        if (recordId != null) {
            recordsRef.child(recordId).setValue(record).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(PrescriptionActivity.this, "Prescription saved to Medical Records", Toast.LENGTH_SHORT).show();
                    // Optional: save to appointments node as well for quick access
                    FirebaseDatabase.getInstance().getReference("Appointments").child(appointmentId)
                            .child("prescription").setValue(prescriptionText);
                } else {
                    Toast.makeText(PrescriptionActivity.this, "Failed to save", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void generatePDF() {
        String prescriptionText = etPrescriptionContent.getText().toString().trim();
        if (prescriptionText.isEmpty()) {
            Toast.makeText(this, "Prescription is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument pdfDocument = new PdfDocument();
        Paint myPaint = new Paint();

        PdfDocument.PageInfo myPageInfo = new PdfDocument.PageInfo.Builder(1080, 1920, 1).create();
        PdfDocument.Page myPage = pdfDocument.startPage(myPageInfo);

        Canvas canvas = myPage.getCanvas();
        myPaint.setTextSize(60f);
        myPaint.setFakeBoldText(true);
        myPaint.setColor(Color.DKGRAY);
        canvas.drawText("Appointiva Health - Prescription", 50, 100, myPaint);

        myPaint.setTextSize(40f);
        myPaint.setFakeBoldText(false);
        myPaint.setColor(Color.BLACK);
        
        String pName = existingRecord != null ? existingRecord.getPatientName() : patientName;
        canvas.drawText("Patient: " + (pName != null ? pName : "Unknown"), 50, 200, myPaint);
        
        canvas.drawLine(50, 300, 1030, 300, myPaint);

        myPaint.setTextSize(35f);
        
        int yPosition = 380;
        String[] lines = prescriptionText.split("\n");
        for (String line : lines) {
            canvas.drawText(line, 50, yPosition, myPaint);
            yPosition += 60;
        }

        pdfDocument.finishPage(myPage);

        String pdfFileName = "Prescription_" + System.currentTimeMillis() + ".pdf";
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), pdfFileName);

        try {
            pdfDocument.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "PDF Downloaded: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error generating PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        pdfDocument.close();
    }
}
