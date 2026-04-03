package com.example.appointivahealth.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appointivahealth.R;
import com.example.appointivahealth.models.Appointment;
import com.example.appointivahealth.models.MedicalRecord;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.UUID;

public class PaymentActivity extends AppCompatActivity {

    private LinearLayout layoutBookingSummary, layoutUpiInterface, layoutPaymentStatus;
    private TextView tvSummaryDoctor, tvSummaryDate, tvPaymentStatus, tvTransactionId, tvSummaryFee, tvUpiFee;
    private MaterialButton btnProceedToPay, btnPayNow, btnRetryPayment, btnDone;
    private ProgressBar progressBarPayment;

    private String doctorId, doctorName, dateStr, timeStr, patientId, patientName, fee, appointmentIdToUpdate;
    private String currentAppointmentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        layoutBookingSummary = findViewById(R.id.layoutBookingSummary);
        layoutUpiInterface = findViewById(R.id.layoutUpiInterface);
        layoutPaymentStatus = findViewById(R.id.layoutPaymentStatus);

        tvSummaryDoctor = findViewById(R.id.tvSummaryDoctor);
        tvSummaryDate = findViewById(R.id.tvSummaryDate);
        tvPaymentStatus = findViewById(R.id.tvPaymentStatus);
        tvTransactionId = findViewById(R.id.tvTransactionId);
        tvSummaryFee = findViewById(R.id.tvSummaryFee);
        tvUpiFee = findViewById(R.id.tvUpiFee);

        btnProceedToPay = findViewById(R.id.btnProceedToPay);
        btnPayNow = findViewById(R.id.btnPayNow);
        btnRetryPayment = findViewById(R.id.btnRetryPayment);
        btnDone = findViewById(R.id.btnDone);
        
        progressBarPayment = findViewById(R.id.progressBarPayment);

        Intent intent = getIntent();
        if (intent != null) {
            doctorId = intent.getStringExtra("doctorId");
            doctorName = intent.getStringExtra("doctorName");
            dateStr = intent.getStringExtra("dateStr");
            timeStr = intent.getStringExtra("timeStr");
            patientId = intent.getStringExtra("patientId");
            patientName = intent.getStringExtra("patientName");
            fee = intent.getStringExtra("fee");
            appointmentIdToUpdate = intent.getStringExtra("appointmentIdToUpdate");

            if (fee == null || fee.isEmpty()) {
                fee = "300"; // Fallback just in case
            }

            tvSummaryDoctor.setText("Doctor: Dr. " + doctorName);
            tvSummaryDate.setText("Date & Time: " + dateStr + " at " + timeStr);
            tvSummaryFee.setText("₹" + fee);
            tvUpiFee.setText("₹" + fee);
        }

        btnProceedToPay.setOnClickListener(v -> {
            layoutBookingSummary.setVisibility(View.GONE);
            layoutUpiInterface.setVisibility(View.VISIBLE);
        });

        btnRetryPayment.setOnClickListener(v -> startPaymentFlow());
        btnPayNow.setOnClickListener(v -> startPaymentFlow());
        
        btnDone.setOnClickListener(v -> finish());
    }

    private void startPaymentFlow() {
        layoutUpiInterface.setVisibility(View.GONE);
        layoutPaymentStatus.setVisibility(View.VISIBLE);
        progressBarPayment.setVisibility(View.VISIBLE);
        tvPaymentStatus.setText("Processing Payment...");
        tvPaymentStatus.setTextColor(Color.parseColor("#333333"));
        tvTransactionId.setVisibility(View.GONE);
        btnDone.setVisibility(View.GONE);
        btnRetryPayment.setVisibility(View.GONE);

        btnPayNow.setEnabled(false);

        ensureAppointmentExistsAndResetForPayment(new Runnable() {
            @Override
            public void run() {
                simulateUpiProcessing();
            }
        });
    }

    private void ensureAppointmentExistsAndResetForPayment(Runnable onReady) {
        DatabaseReference apptsRef = FirebaseDatabase.getInstance().getReference("Appointments");

        if (appointmentIdToUpdate != null && !appointmentIdToUpdate.isEmpty()) {
            currentAppointmentId = appointmentIdToUpdate;
            DatabaseReference apptRef = apptsRef.child(currentAppointmentId);
            apptRef.child("status").setValue("payment_pending_verification");
            apptRef.child("paymentStatus").setValue("pending_payment");
            apptRef.child("adminVerified").setValue(false);
            apptRef.child("paymentMode").setValue("UPI");
            apptRef.child("transactionId").setValue("");
            apptRef.child("amount").setValue(fee);
            apptRef.child("message").setValue("");
            apptRef.child("prescription").setValue("No prescription yet");
            onReady.run();
            return;
        }

        String apptId = apptsRef.push().getKey();
        currentAppointmentId = apptId;
        if (apptId == null) {
            Toast.makeText(this, "Failed to create appointment", Toast.LENGTH_SHORT).show();
            btnPayNow.setEnabled(true);
            return;
        }

        Appointment appointment = new Appointment(apptId, patientId, patientName,
                doctorId, doctorName, dateStr, timeStr, "payment_pending_verification");
        appointment.setPaymentStatus("pending_payment");
        appointment.setPaymentMode("UPI");
        appointment.setAdminVerified(false);
        appointment.setTransactionId("");
        appointment.setAmount(fee);
        appointment.setPrescription("No prescription yet");

        apptsRef.child(apptId).setValue(appointment).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                onReady.run();
            } else {
                btnPayNow.setEnabled(true);
                progressBarPayment.setVisibility(View.GONE);
                tvPaymentStatus.setText("Failed to prepare booking for payment.");
                tvPaymentStatus.setTextColor(Color.RED);
                btnRetryPayment.setVisibility(View.VISIBLE);
            }
        });
    }

    private void simulateUpiProcessing() {
        // Simulate network delay for UPI Payment Process
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            String transactionId = "TXN" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            finalizeAppointmentAfterSuccessfulPayment(transactionId);
        }, 2000);
    }

    private void finalizeAppointmentAfterSuccessfulPayment(String transactionId) {
        progressBarPayment.setVisibility(View.GONE);
        tvTransactionId.setVisibility(View.VISIBLE);
        tvTransactionId.setText("Transaction ID: " + transactionId);
        tvPaymentStatus.setText("✓ Payment successful. Sent to Admin for verification.");
        tvPaymentStatus.setTextColor(Color.parseColor("#4CAF50"));
        btnDone.setVisibility(View.VISIBLE);
        btnRetryPayment.setVisibility(View.GONE);

        DatabaseReference apptRef = FirebaseDatabase.getInstance().getReference("Appointments").child(currentAppointmentId);

        apptRef.child("status").setValue("payment_pending_verification");
        apptRef.child("paymentStatus").setValue("paid");
        apptRef.child("adminVerified").setValue(false);
        apptRef.child("paymentMode").setValue("UPI");
        apptRef.child("transactionId").setValue(transactionId);
        apptRef.child("amount").setValue(fee);
        apptRef.child("message").setValue("");
        apptRef.child("prescription").setValue("No prescription yet");

        // Create medical record on successful payment confirmation.
        DatabaseReference recordRef = FirebaseDatabase.getInstance().getReference("MedicalRecords");
        String recordId = recordRef.push().getKey();
        if (recordId == null) {
            Toast.makeText(this, "Failed to create medical record.", Toast.LENGTH_SHORT).show();
            return;
        }

        MedicalRecord record = new MedicalRecord();
        record.setId(recordId);
        record.setPatientId(patientId);
        record.setDoctorId(doctorId);
        record.setAppointmentId(currentAppointmentId);
        record.setPatientName(patientName);
        record.setDate(dateStr);
        record.setDiagnosis("");
        record.setNotes("");
        record.setPrescription("No prescription yet");

        recordRef.child(recordId).setValue(record);
    }
}
