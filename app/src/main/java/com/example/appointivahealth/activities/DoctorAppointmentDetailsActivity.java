package com.example.appointivahealth.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.appointivahealth.R;
import com.example.appointivahealth.models.Appointment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DoctorAppointmentDetailsActivity extends AppCompatActivity {

    private String appointmentId;
    private TextView tvDetailsPatientName, tvDetailsDateTime, tvDetailsStatus, tvRestrictedMessage;
    private CardView cardPatientInfo;

    private Appointment appointment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_appointment_details);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarDoctorApptDetails);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        appointmentId = getIntent().getStringExtra("appointmentId");

        tvDetailsPatientName = findViewById(R.id.tvDetailsPatientName);
        tvDetailsDateTime = findViewById(R.id.tvDetailsDateTime);
        tvDetailsStatus = findViewById(R.id.tvDetailsStatus);
        tvRestrictedMessage = findViewById(R.id.tvRestrictedMessage);
        cardPatientInfo = findViewById(R.id.cardPatientInfo);

        if (appointmentId != null) {
            loadAppointmentDetails();
        } else {
            Toast.makeText(this, "Error loading appointment", Toast.LENGTH_SHORT).show();
            finish();
        }


    }

    private void loadAppointmentDetails() {
        DatabaseReference apptRef = FirebaseDatabase.getInstance().getReference("Appointments").child(appointmentId);
        apptRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    appointment = snapshot.getValue(Appointment.class);
                    if (appointment != null) {
                        tvDetailsPatientName.setText("Patient: " + appointment.getPatientName());
                        tvDetailsDateTime.setText("Date: " + appointment.getDate() + " | Time: " + appointment.getTime());
                        tvDetailsStatus.setText("Status: " + appointment.getStatus());
                        
                        boolean isVerified = "verified".equals(appointment.getPaymentStatus()) || 
                                             "confirmed".equals(appointment.getStatus()) ||
                                             "Accepted".equals(appointment.getStatus()) ||
                                             "Scheduled".equals(appointment.getStatus()) ||
                                             "Completed".equalsIgnoreCase(appointment.getStatus());

                        if (isVerified) {
                            tvRestrictedMessage.setVisibility(View.GONE);
                            cardPatientInfo.setVisibility(View.VISIBLE);
                        } else if ("payment_failed".equals(appointment.getStatus()) || "rejected".equals(appointment.getPaymentStatus())) {
                            tvRestrictedMessage.setVisibility(View.VISIBLE);
                            tvRestrictedMessage.setText("Payment Rejected by Admin. Patient details restricted.");
                            cardPatientInfo.setVisibility(View.GONE);
                        } else {
                            tvRestrictedMessage.setVisibility(View.VISIBLE);
                            tvRestrictedMessage.setText("Patient details are restricted pending Admin Payment Verification.");
                            cardPatientInfo.setVisibility(View.GONE);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }
}
