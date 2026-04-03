package com.example.appointivahealth.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.appointivahealth.R;
import com.example.appointivahealth.models.Appointment;
import com.example.appointivahealth.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.appointivahealth.adapters.DashboardPatientAdapter;
import com.example.appointivahealth.models.MedicalRecord;

public class DoctorDashboardActivity extends AppCompatActivity {

    private TextView tvDoctorWelcome, tvTodayAppointments, tvUpcomingAppointments, tvTotalPatients, tvEarnings;
    private CardView cardManageAppointments, cardPatientRecords, cardAvailability, cardChatMessages, cardLogoutDoctor;
    
    private String doctorFee = "0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_dashboard);

        tvDoctorWelcome = findViewById(R.id.tvDoctorWelcome);
        tvTodayAppointments = findViewById(R.id.tvTodayAppointments);
        tvUpcomingAppointments = findViewById(R.id.tvUpcomingAppointments);
        tvTotalPatients = findViewById(R.id.tvTotalPatients);
        tvEarnings = findViewById(R.id.tvEarnings);

        cardManageAppointments = findViewById(R.id.cardManageAppointments);
        cardPatientRecords = findViewById(R.id.cardPatientRecords);
        cardAvailability = findViewById(R.id.cardAvailability);
        cardChatMessages = findViewById(R.id.cardChatMessages);
        cardLogoutDoctor = findViewById(R.id.cardLogoutDoctor);

        loadDoctorData();

        cardManageAppointments.setOnClickListener(v -> startActivity(new Intent(DoctorDashboardActivity.this, AppointmentManagementActivity.class)));
        cardPatientRecords.setOnClickListener(v -> startActivity(new Intent(DoctorDashboardActivity.this, DoctorPatientRecordsActivity.class)));
        cardAvailability.setOnClickListener(v -> startActivity(new Intent(DoctorDashboardActivity.this, DoctorAvailabilityActivity.class)));
        cardChatMessages.setOnClickListener(v -> startActivity(new Intent(DoctorDashboardActivity.this, DoctorChatListActivity.class)));

        cardLogoutDoctor.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(DoctorDashboardActivity.this, LoginActivity.class));
            finishAffinity();
        });
    }

    private void loadDoctorData() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            tvDoctorWelcome.setText("Dr. " + user.getName());
                            if (user.getFee() != null && !user.getFee().isEmpty()) {
                                doctorFee = user.getFee();
                            }
                            // Only load appointments once we have the fee to calculate earnings correctly
                            loadStatistics(uid);
                        }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(DoctorDashboardActivity.this, "Failed to load doctor data", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadStatistics(String doctorId) {
        DatabaseReference apptRef = FirebaseDatabase.getInstance().getReference("Appointments");
        apptRef.orderByChild("doctorId").equalTo(doctorId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int todayCount = 0;
                int upcomingCount = 0;
                double totalEarnings = 0;
                HashSet<String> uniquePatients = new HashSet<>();

                String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Appointment appointment = ds.getValue(Appointment.class);
                    if (appointment != null) {
                        String status = appointment.getStatus();
                        String dateStr = appointment.getDate(); // e.g., 20/03/2026

                        if (status != null && !status.equalsIgnoreCase("Rejected") && !status.equalsIgnoreCase("Cancelled") && !status.equalsIgnoreCase("payment_failed")) {
                            uniquePatients.add(appointment.getPatientId());

                            if (status.equalsIgnoreCase("Completed")) {
                                try {
                                    totalEarnings += Double.parseDouble(doctorFee);
                                } catch (NumberFormatException e) {
                                    Log.e("DoctorDashboard", "Invalid fee format", e);
                                }
                            }

                            if (dateStr != null && (status.equalsIgnoreCase("Pending") || status.equalsIgnoreCase("Accepted") || status.equalsIgnoreCase("Scheduled") || status.equalsIgnoreCase("confirmed") || status.equalsIgnoreCase("Completed"))) {
                                try {
                                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                    Date apptDate = sdf.parse(dateStr);
                                    Date todayMidnight = sdf.parse(todayStr); 
                                    
                                    if (apptDate != null && todayMidnight != null) {
                                        if (apptDate.equals(todayMidnight)) {
                                            todayCount++;
                                        } else if (apptDate.after(todayMidnight)) {
                                            upcomingCount++;
                                        }
                                    }
                                } catch (java.text.ParseException e) {
                                    Log.e("DoctorDashboard", "Date parse error", e);
                                    if (dateStr.equals(todayStr)) {
                                        todayCount++;
                                    } else {
                                        upcomingCount++;
                                    }
                                }
                            }
                        }
                    }
                } // Closes the for loop over snapshot.getChildren()
                
                tvTodayAppointments.setText(String.valueOf(todayCount));
                tvUpcomingAppointments.setText(String.valueOf(upcomingCount));
                tvTotalPatients.setText(String.valueOf(uniquePatients.size()));
                tvEarnings.setText("₹" + String.format(Locale.getDefault(), "%.0f", totalEarnings));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("DoctorDashboard", "Failed to load statistics: " + error.getMessage());
            }
        });
    }


}
