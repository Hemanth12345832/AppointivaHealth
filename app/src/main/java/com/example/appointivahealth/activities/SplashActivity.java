package com.example.appointivahealth.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appointivahealth.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            boolean isAdminLoggedIn = getSharedPreferences("AppointivaPrefs", MODE_PRIVATE).getBoolean("isAdminLoggedIn", false);
            if (isAdminLoggedIn) {
                startActivity(new Intent(SplashActivity.this, AdminPanelActivity.class));
                finish();
                return;
            }

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                checkUserRole(currentUser.getUid());
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish();
            }
        }, 2000);
    }

    private void checkUserRole(String uid) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String role = snapshot.child("role").getValue(String.class);
                    if (role != null) {
                        Intent intent;
                        switch (role) {
                            case "Doctor":
                                intent = new Intent(SplashActivity.this, DoctorDashboardActivity.class);
                                break;
                            case "Admin":
                                intent = new Intent(SplashActivity.this, AdminPanelActivity.class);
                                break;
                            default:
                                intent = new Intent(SplashActivity.this, PatientDashboardActivity.class);
                                break;
                        }
                        startActivity(intent);
                    } else {
                        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                    }
                } else {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                }
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish();
            }
        });
    }
}
