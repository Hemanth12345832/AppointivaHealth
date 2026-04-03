package com.example.appointivahealth.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;

import com.example.appointivahealth.R;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PatientDashboardActivity extends AppCompatActivity {

    private TextView tvPatientWelcome;
    private CardView cardFindDoctor, cardMyAppointments, cardSymptomChecker, cardPatientChats, cardLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_dashboard);

        tvPatientWelcome = findViewById(R.id.tvPatientWelcome);
        cardFindDoctor = findViewById(R.id.cardFindDoctor);
        cardMyAppointments = findViewById(R.id.cardMyAppointments);
        cardSymptomChecker = findViewById(R.id.cardSymptomChecker);
        cardPatientChats = findViewById(R.id.cardPatientChats);
        cardLogout = findViewById(R.id.cardLogout);

        loadPatientName();



        cardFindDoctor.setOnClickListener(v -> startActivity(new Intent(PatientDashboardActivity.this, DoctorListActivity.class)));

        cardMyAppointments.setOnClickListener(v -> startActivity(new Intent(PatientDashboardActivity.this, AppointmentHistoryActivity.class)));

        cardSymptomChecker.setOnClickListener(v -> startActivity(new Intent(PatientDashboardActivity.this, SymptomCheckerActivity.class)));

        cardPatientChats.setOnClickListener(v -> startActivity(new Intent(PatientDashboardActivity.this, PatientChatListActivity.class)));

        cardLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(PatientDashboardActivity.this, LoginActivity.class));
            finishAffinity();
        });
    }

    private void loadPatientName() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && snapshot.hasChild("name")) {
                        String name = snapshot.child("name").getValue(String.class);
                        tvPatientWelcome.setText("Welcome, " + name);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(PatientDashboardActivity.this, "Failed to load name", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
