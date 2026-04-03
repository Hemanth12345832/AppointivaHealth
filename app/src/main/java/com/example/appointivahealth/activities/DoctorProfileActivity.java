package com.example.appointivahealth.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appointivahealth.R;
import com.example.appointivahealth.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DoctorProfileActivity extends AppCompatActivity {

    private TextView tvProfileName, tvProfileSpecialization, tvProfilePhone, tvProfileFee, tvProfileAvailableTime;
    private MaterialButton btnBookAppointment, btnEditProfile;
    private User doctor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_profile);

        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileSpecialization = findViewById(R.id.tvProfileSpecialization);
        tvProfilePhone = findViewById(R.id.tvProfilePhone);
        tvProfileFee = findViewById(R.id.tvProfileFee);
        tvProfileAvailableTime = findViewById(R.id.tvProfileAvailableTime);
        btnBookAppointment = findViewById(R.id.btnBookAppointment);
        btnEditProfile = findViewById(R.id.btnEditProfile);

        doctor = (User) getIntent().getSerializableExtra("doctor");

        if (doctor != null) {
            setupProfile(doctor);
            checkUserRoleForBooking();
        } else {
            // Self-view
            loadCurrentUserProfile();
        }

        btnBookAppointment.setOnClickListener(v -> {
            if (doctor != null) {
                Intent intent = new Intent(DoctorProfileActivity.this, BookAppointmentActivity.class);
                intent.putExtra("doctor", doctor);
                startActivity(intent);
            }
        });

        btnEditProfile.setOnClickListener(v -> {
            if (doctor != null) {
                Intent intent = new Intent(DoctorProfileActivity.this, EditDoctorActivity.class);
                intent.putExtra("doctor", doctor);
                startActivity(intent);
            }
        });
    }

    private void setupProfile(User d) {
        doctor = d;
        tvProfileName.setText("Dr. " + d.getName());
        tvProfileSpecialization.setText(d.getSpecialization());
        tvProfilePhone.setText("Phone: " + d.getPhone());
        tvProfileFee.setText("Consultation Fee: ₹" + (d.getFee() != null ? d.getFee() : "N/A"));
        tvProfileAvailableTime.setText("Availability: " + (d.getAvailableTime() != null ? d.getAvailableTime() : "N/A"));

        if (FirebaseAuth.getInstance().getCurrentUser() != null &&
            FirebaseAuth.getInstance().getCurrentUser().getUid().equals(d.getId())) {
            btnBookAppointment.setVisibility(View.GONE);
            btnEditProfile.setVisibility(View.VISIBLE);
        }
    }

    private void loadCurrentUserProfile() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User currentUser = snapshot.getValue(User.class);
                    if (currentUser != null) {
                        currentUser.setId(uid); // Ensure ID is set
                        setupProfile(currentUser);
                        checkUserRoleForBooking();
                    }
                }
            }
            @Override
            public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {
                Toast.makeText(DoctorProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkUserRoleForBooking() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            btnBookAppointment.setVisibility(View.GONE);
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference("Users").child(uid)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String role = snapshot.child("role").getValue(String.class);
                        if (!"Patient".equals(role)) {
                            btnBookAppointment.setVisibility(View.GONE);
                        }
                        if (doctor != null && doctor.getId() != null && doctor.getId().equals(uid)) {
                            btnEditProfile.setVisibility(View.VISIBLE);
                        }
                    }
                }
                @Override
                public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {
                }
            });
    }
}
