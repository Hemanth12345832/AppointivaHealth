package com.example.appointivahealth.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.appointivahealth.R;
import com.google.firebase.auth.FirebaseAuth;

public class AdminPanelActivity extends AppCompatActivity {

    private CardView cardManageDoctors, cardManagePatients, cardAllAppointments, cardReports, cardLogoutAdmin, cardVerifyPayments, cardComplaints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_panel);

        cardManageDoctors = findViewById(R.id.cardManageDoctors);
        cardManagePatients = findViewById(R.id.cardManagePatients);
        cardAllAppointments = findViewById(R.id.cardAllAppointments);
        cardReports = findViewById(R.id.cardReports);
        cardLogoutAdmin = findViewById(R.id.cardLogoutAdmin);
        cardVerifyPayments = findViewById(R.id.cardVerifyPayments);
        cardComplaints = findViewById(R.id.cardComplaints);

        cardManageDoctors.setOnClickListener(v -> {
            startActivity(new Intent(AdminPanelActivity.this, DoctorListActivity.class));
        });
        
        CardView cardVerifyDoctors = findViewById(R.id.cardVerifyDoctors);
        cardVerifyDoctors.setOnClickListener(v -> {
            startActivity(new Intent(AdminPanelActivity.this, VerifyDoctorsActivity.class));
        });



        cardManagePatients.setOnClickListener(v -> {
            startActivity(new Intent(AdminPanelActivity.this, ManagePatientsActivity.class));
        });

        cardAllAppointments.setOnClickListener(v -> {
            startActivity(new Intent(AdminPanelActivity.this, AppointmentHistoryActivity.class));
        });

        cardReports.setOnClickListener(v -> {
            startActivity(new Intent(AdminPanelActivity.this, ReportsActivity.class));
        });

        cardVerifyPayments.setOnClickListener(v -> {
            startActivity(new Intent(AdminPanelActivity.this, PaymentVerificationActivity.class));
        });

        cardComplaints.setOnClickListener(v -> {
            startActivity(new Intent(AdminPanelActivity.this, AdminComplaintsActivity.class));
        });

        cardLogoutAdmin.setOnClickListener(v -> {
            getSharedPreferences("AppointivaPrefs", MODE_PRIVATE).edit().putBoolean("isAdminLoggedIn", false).apply();
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(AdminPanelActivity.this, LoginActivity.class));
            finishAffinity();
        });
    }
}
