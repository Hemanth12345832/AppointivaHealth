package com.example.appointivahealth.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appointivahealth.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private Spinner spinnerLoginRole;
    private MaterialButton btnLogin;
    private TextView tvGoToRegister;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etLoginEmail);
        etPassword = findViewById(R.id.etLoginPassword);
        spinnerLoginRole = findViewById(R.id.spinnerLoginRole);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
        progressBar = findViewById(R.id.progressBarLogin);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.roles_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLoginRole.setAdapter(adapter);

        btnLogin.setOnClickListener(v -> loginUser());

        tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Invalid email format");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }

        String selectedRole = spinnerLoginRole.getSelectedItem().toString();

        if ("Admin".equals(selectedRole)) {
            checkAdminLogin(email, password);
            return;
        }

        if (password.length() < 8) {
            etPassword.setError("Password must be at least 8 characters long");
            return;
        } else if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            etPassword.setError("Password must contain at least 1 special character");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        checkUserRole(mAuth.getCurrentUser().getUid(), selectedRole);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        Toast.makeText(LoginActivity.this, "Please check your email or password", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkAdminLogin(String email, String password) {
        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);
        DatabaseReference adminRef = FirebaseDatabase.getInstance().getReference("Admin");
        adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    // Seed the default admin if it doesn't exist
                    adminRef.child("email").setValue("admin@appointiva.com");
                    adminRef.child("password").setValue("admin123");
                }
                
                adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot2) {
                        progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        String dbEmail = snapshot2.child("email").getValue(String.class);
                        String dbPassword = snapshot2.child("password").getValue(String.class);
                        
                        if (email.equals(dbEmail) && password.equals(dbPassword)) {
                            SharedPreferences prefs = getSharedPreferences("AppointivaPrefs", Context.MODE_PRIVATE);
                            prefs.edit().putBoolean("isAdminLoggedIn", true).apply();
                            startActivity(new Intent(LoginActivity.this, AdminPanelActivity.class));
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Invalid Admin Credentials", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        Toast.makeText(LoginActivity.this, "Database Error", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
            }
        });
    }

    private void checkUserRole(String uid, String selectedRole) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                if (snapshot.exists()) {
                    String role = snapshot.child("role").getValue(String.class);
                    if (!selectedRole.equals(role)) {
                        Toast.makeText(LoginActivity.this, "User role mismatch. You are not a " + selectedRole, Toast.LENGTH_SHORT).show();
                        FirebaseAuth.getInstance().signOut();
                        btnLogin.setEnabled(true);
                        return;
                    }

                    Boolean isVerified = snapshot.child("isVerified").getValue(Boolean.class);
                    if ("Doctor".equals(role) && (isVerified == null || !isVerified)) {
                        Toast.makeText(LoginActivity.this, "Your account is pending verification by an Admin.", Toast.LENGTH_LONG).show();
                        FirebaseAuth.getInstance().signOut();
                        btnLogin.setEnabled(true);
                        return;
                    }
                    
                    Intent intent;
                    if ("Doctor".equals(role)) {
                        intent = new Intent(LoginActivity.this, DoctorDashboardActivity.class);
                    } else if ("Admin".equals(role)) {
                        intent = new Intent(LoginActivity.this, AdminPanelActivity.class);
                    } else {
                        intent = new Intent(LoginActivity.this, PatientDashboardActivity.class);
                    }
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "User data not found.", Toast.LENGTH_SHORT).show();
                    btnLogin.setEnabled(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Database Error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
