package com.example.appointivahealth.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appointivahealth.R;
import com.example.appointivahealth.models.Appointment;
import com.example.appointivahealth.models.MedicalRecord;
import com.example.appointivahealth.network.ApiClient;
import com.razorpay.Checkout;
import com.razorpay.PaymentData;
import com.razorpay.PaymentResultWithDataListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

public class PaymentActivity extends AppCompatActivity implements PaymentResultWithDataListener {

    private LinearLayout layoutBookingSummary, layoutUpiInterface, layoutPaymentStatus;
    private TextView tvSummaryDoctor, tvSummaryDate, tvPaymentStatus, tvTransactionId, tvSummaryFee, tvUpiFee;
    private MaterialButton btnProceedToPay, btnPayNow, btnRetryPayment, btnDone;
    private ProgressBar progressBarPayment;

    private String doctorId, doctorName, dateStr, timeStr, patientId, patientName, fee, appointmentIdToUpdate;
    private String currentAppointmentId;

    private Checkout checkout;
    private boolean mockMode = false;
    private String pendingOrderId = "";
    private String razorpayKeyIdForCheckout = "";
    private String razorpayCurrency = "INR";
    private long amountPaise = 0L;

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
        checkout = new Checkout();

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

        razorpayCurrency = getString(R.string.razorpay_currency);
        try {
            amountPaise = Math.round(Double.parseDouble(fee) * 100);
        } catch (Exception e) {
            amountPaise = 30000L;
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
        tvPaymentStatus.setText("Creating payment...");
        tvPaymentStatus.setTextColor(Color.parseColor("#333333"));
        tvTransactionId.setVisibility(View.GONE);
        btnDone.setVisibility(View.GONE);
        btnRetryPayment.setVisibility(View.GONE);

        btnPayNow.setEnabled(false);
        mockMode = false;
        pendingOrderId = "";
        razorpayKeyIdForCheckout = "";

        ensureAppointmentExistsAndResetForPayment(new Runnable() {
            @Override
            public void run() {
                createRazorpayOrderAndProceed();
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
            apptRef.child("paymentMode").setValue("RAZORPAY");
            apptRef.child("transactionId").setValue("");
            apptRef.child("orderId").setValue("");
            apptRef.child("paymentId").setValue("");
            apptRef.child("paymentSignature").setValue("");
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
        appointment.setPaymentMode("RAZORPAY");
        appointment.setAdminVerified(false);
        appointment.setTransactionId("");
        appointment.setOrderId("");
        appointment.setPaymentId("");
        appointment.setPaymentSignature("");
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

    private void createRazorpayOrderAndProceed() {
        try {
            JSONObject payload = new JSONObject();
            payload.put("appointmentId", currentAppointmentId);
            payload.put("amount", fee);
            payload.put("currency", razorpayCurrency);
            payload.put("patientId", patientId);
            payload.put("doctorId", doctorId);

            ApiClient.createRazorpayOrder(this, payload, new ApiClient.JsonCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    handleCreateOrderResponse(response);
                }

                @Override
                public void onError(String error) {
                    btnPayNow.setEnabled(true);
                    progressBarPayment.setVisibility(View.GONE);
                    tvPaymentStatus.setText("Failed to create payment order.");
                    tvPaymentStatus.setTextColor(Color.RED);
                    btnRetryPayment.setVisibility(View.VISIBLE);
                    Toast.makeText(PaymentActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (JSONException e) {
            btnPayNow.setEnabled(true);
            progressBarPayment.setVisibility(View.GONE);
            tvPaymentStatus.setText("Invalid request.");
            tvPaymentStatus.setTextColor(Color.RED);
            btnRetryPayment.setVisibility(View.VISIBLE);
        }
    }

    private void handleCreateOrderResponse(JSONObject response) {
        boolean responseMockMode = response.optBoolean("mock", false);
        String orderId = response.optString("orderId", "");
        String keyId = response.optString("keyId", "");

        mockMode = responseMockMode || keyId.isEmpty();
        pendingOrderId = orderId;
        razorpayKeyIdForCheckout = keyId;

        if (pendingOrderId == null || pendingOrderId.isEmpty()) {
            btnPayNow.setEnabled(true);
            progressBarPayment.setVisibility(View.GONE);
            tvPaymentStatus.setText("Invalid order returned from server.");
            tvPaymentStatus.setTextColor(Color.RED);
            btnRetryPayment.setVisibility(View.VISIBLE);
            return;
        }

        progressBarPayment.setVisibility(View.GONE);
        if (mockMode) {
            tvPaymentStatus.setText("Mock payment mode: completing payment...");
            tvPaymentStatus.setTextColor(Color.parseColor("#333333"));
            mockVerifyPayment();
        } else {
            openRazorpayCheckout(orderId, keyId);
        }
    }

    private void openRazorpayCheckout(String orderId, String keyId) {
        try {
            checkout = new Checkout();
            checkout.setKeyID(keyId);

            JSONObject options = new JSONObject();
            options.put("name", "Appointiva Health");
            options.put("description", "Doctor appointment booking payment");
            options.put("order_id", orderId);
            options.put("theme.color", "#0052CC");
            options.put("currency", razorpayCurrency);
            options.put("amount", amountPaise);
            options.put("prefill.name", patientName);

            checkout.open(this, options);
        } catch (Exception e) {
            btnPayNow.setEnabled(true);
            progressBarPayment.setVisibility(View.GONE);
            tvPaymentStatus.setText("Failed to open Razorpay checkout.");
            tvPaymentStatus.setTextColor(Color.RED);
            btnRetryPayment.setVisibility(View.VISIBLE);
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void mockVerifyPayment() {
        try {
            JSONObject payload = new JSONObject();
            payload.put("appointmentId", currentAppointmentId);
            payload.put("orderId", pendingOrderId);
            payload.put("paymentId", "MOCK_PAYMENT_" + System.currentTimeMillis());
            payload.put("signature", "MOCK_SIGNATURE");

            ApiClient.verifyRazorpayPayment(this, payload, new ApiClient.JsonCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    boolean verified = response.optBoolean("verified", false);
                    if (!verified) {
                        paymentFailed("Mock payment verification failed.");
                        return;
                    }

                    String paymentId = response.optString("paymentId", payload.optString("paymentId"));
                    String signature = response.optString("signature", payload.optString("signature"));
                    finalizeAppointmentAfterSuccessfulPayment(paymentId, pendingOrderId, signature);
                }

                @Override
                public void onError(String error) {
                    paymentFailed(error);
                }
            });
        } catch (JSONException e) {
            paymentFailed("Invalid mock verify payload.");
        }
    }

    private void paymentFailed(String reason) {
        progressBarPayment.setVisibility(View.GONE);
        tvPaymentStatus.setText("Payment failed. " + reason);
        tvPaymentStatus.setTextColor(Color.RED);
        btnRetryPayment.setVisibility(View.VISIBLE);
        btnPayNow.setEnabled(true);

        DatabaseReference apptRef = FirebaseDatabase.getInstance().getReference("Appointments").child(currentAppointmentId);
        apptRef.child("status").setValue("payment_failed");
        apptRef.child("paymentStatus").setValue("rejected");
        apptRef.child("payment_status").setValue("rejected");
        apptRef.child("adminVerified").setValue(false);
        apptRef.child("paymentId").setValue("");
        apptRef.child("payment_id").setValue("");
        apptRef.child("paymentSignature").setValue("");
        apptRef.child("payment_signature").setValue("");
        apptRef.child("orderId").setValue(pendingOrderId);
        apptRef.child("order_id").setValue(pendingOrderId);
        apptRef.child("message").setValue(reason);
    }

    @Override
    public void onPaymentSuccess(String razorpayPaymentID, PaymentData paymentData) {
        String pId = razorpayPaymentID;
        String oId = pendingOrderId;
        String sig = "";

        if (paymentData != null) {
            try {
                if (paymentData.getOrderId() != null) oId = paymentData.getOrderId();
                if (paymentData.getPaymentId() != null) pId = paymentData.getPaymentId();
                if (paymentData.getSignature() != null) sig = paymentData.getSignature();
            } catch (Exception ignored) {
            }
        }

        final String finalPaymentId = pId;
        final String finalOrderId = oId;
        final String finalSignature = sig;

        if (finalPaymentId == null || finalPaymentId.isEmpty() || finalOrderId == null || finalOrderId.isEmpty() || finalSignature == null || finalSignature.isEmpty()) {
            paymentFailed("Payment succeeded but signature verification data is missing.");
            return;
        }

        progressBarPayment.setVisibility(View.VISIBLE);
        tvPaymentStatus.setText("Verifying payment...");
        tvPaymentStatus.setTextColor(Color.parseColor("#333333"));

        try {
            JSONObject payload = new JSONObject();
            payload.put("appointmentId", currentAppointmentId);
            payload.put("orderId", finalOrderId);
            payload.put("paymentId", finalPaymentId);
            payload.put("signature", finalSignature);

            ApiClient.verifyRazorpayPayment(this, payload, new ApiClient.JsonCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    boolean verified = response.optBoolean("verified", false);
                    if (!verified) {
                        paymentFailed("Payment verification failed.");
                        return;
                    }

                    String verifiedPaymentId = response.optString("paymentId", finalPaymentId);
                    String verifiedSignature = response.optString("signature", finalSignature);
                    finalizeAppointmentAfterSuccessfulPayment(verifiedPaymentId, finalOrderId, verifiedSignature);
                }

                @Override
                public void onError(String error) {
                    paymentFailed(error);
                }
            });
        } catch (JSONException e) {
            paymentFailed("Invalid verification request.");
        }
    }

    @Override
    public void onPaymentError(int code, String response, PaymentData paymentData) {
        String message = "Error code " + code + (response != null ? (": " + response) : "");
        paymentFailed(message);
    }

    private void finalizeAppointmentAfterSuccessfulPayment(String paymentId, String orderId, String signature) {
        progressBarPayment.setVisibility(View.GONE);
        tvTransactionId.setVisibility(View.VISIBLE);
        tvTransactionId.setText("Payment ID: " + paymentId);
        tvPaymentStatus.setText("✓ Payment received. Sent to Admin for verification.");
        tvPaymentStatus.setTextColor(Color.parseColor("#4CAF50"));
        btnDone.setVisibility(View.VISIBLE);
        btnRetryPayment.setVisibility(View.GONE);

        DatabaseReference apptRef = FirebaseDatabase.getInstance().getReference("Appointments").child(currentAppointmentId);

        apptRef.child("status").setValue("payment_pending_verification");
        apptRef.child("paymentStatus").setValue("paid");
        apptRef.child("payment_status").setValue("paid");
        apptRef.child("adminVerified").setValue(false);
        apptRef.child("paymentMode").setValue("RAZORPAY");
        apptRef.child("transactionId").setValue(paymentId);
        apptRef.child("orderId").setValue(orderId);
        apptRef.child("paymentId").setValue(paymentId);
        apptRef.child("paymentSignature").setValue(signature);
        apptRef.child("payment_id").setValue(paymentId);
        apptRef.child("order_id").setValue(orderId);
        apptRef.child("payment_signature").setValue(signature);
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
