package com.example.appointivahealth.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

import android.widget.LinearLayout;
import android.widget.ImageView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.net.Uri;
import com.bumptech.glide.Glide;
import com.example.appointivahealth.R;
import com.example.appointivahealth.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etName, etPhone, etEmail, etPassword, etSpecialization, etExperience, etHospitalName, etLicenseNumber, etFee, etAvailableTime;
    private RadioGroup rgGender;
    private LinearLayout layoutSpecialization;
    private Spinner spinnerRole;
    private MaterialButton btnRegister, btnUploadPhoto;
    private ImageView ivProfilePhotoPreview;
    private TextView tvGoToLogin;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private DatabaseReference reference;
    private Uri imageUri;
    private ActivityResultLauncher<Intent> photoPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        reference = FirebaseDatabase.getInstance().getReference("Users");

        etName = findViewById(R.id.etRegisterName);
        etPhone = findViewById(R.id.etRegisterPhone);
        etEmail = findViewById(R.id.etRegisterEmail);
        etPassword = findViewById(R.id.etRegisterPassword);
        etSpecialization = findViewById(R.id.etSpecialization);
        etExperience = findViewById(R.id.etExperience);
        etHospitalName = findViewById(R.id.etHospitalName);
        etLicenseNumber = findViewById(R.id.etLicenseNumber);
        etFee = findViewById(R.id.etRegisterFee);
        etAvailableTime = findViewById(R.id.etRegisterAvailableTime);
        rgGender = findViewById(R.id.rgGender);
        layoutSpecialization = findViewById(R.id.layoutSpecialization);
        spinnerRole = findViewById(R.id.spinnerRole);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);
        progressBar = findViewById(R.id.progressBarRegister);
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto);
        ivProfilePhotoPreview = findViewById(R.id.ivProfilePhotoPreview);

        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        Glide.with(this).load(imageUri).into(ivProfilePhotoPreview);
                    }
                });

        btnUploadPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            photoPickerLauncher.launch(intent);
        });

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.register_roles_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);

        spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 1) { // Doctor selected
                    layoutSpecialization.setVisibility(View.VISIBLE);
                } else {
                    layoutSpecialization.setVisibility(View.GONE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnRegister.setOnClickListener(v -> registerUser());

        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String role = spinnerRole.getSelectedItem().toString();
        String specialization = etSpecialization.getText().toString().trim();
        String experience = "";
        String hospitalName = "";
        String licenseNumber = "";
        String gender = "";
        int selectedGenderId = rgGender.getCheckedRadioButtonId();
        if (selectedGenderId != -1) {
            RadioButton rbSelected = findViewById(selectedGenderId);
            gender = rbSelected.getText().toString();
        }

        if (TextUtils.isEmpty(name)) { etName.setError("Name required"); return; }
        if (TextUtils.isEmpty(phone)) { etPhone.setError("Phone required"); return; }
        if (TextUtils.isEmpty(email)) { 
            etEmail.setError("Email required"); 
            return; 
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Invalid email format");
            return;
        }

        if (TextUtils.isEmpty(password)) { 
            etPassword.setError("Password required"); 
            return; 
        } else if (password.length() < 8) {
            etPassword.setError("Password must be at least 8 characters long");
            return;
        } else if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            etPassword.setError("Password must contain at least 1 special character");
            return;
        }

        if (TextUtils.isEmpty(gender)) {
            Toast.makeText(this, "Please select a Gender", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("Doctor".equals(role)) {
            experience = etExperience.getText().toString().trim();
            hospitalName = etHospitalName.getText().toString().trim();
            licenseNumber = etLicenseNumber.getText().toString().trim();
            String fee = etFee.getText().toString().trim();
            String availableTime = etAvailableTime.getText().toString().trim();
            
            if (TextUtils.isEmpty(specialization)) { etSpecialization.setError("Specialization required"); return; }
            if (TextUtils.isEmpty(experience)) { etExperience.setError("Experience required"); return; }
            if (TextUtils.isEmpty(hospitalName)) { etHospitalName.setError("Hospital required"); return; }
            if (TextUtils.isEmpty(licenseNumber)) { etLicenseNumber.setError("License required"); return; }
            if (imageUri == null) {
                Toast.makeText(this, "Profile Photo is required for Doctors", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        String finalExperience = experience;
        String finalHospitalName = hospitalName;
        String finalLicenseNumber = licenseNumber;
        String finalFee = etFee.getText().toString().trim();
        String finalAvailableTime = etAvailableTime.getText().toString().trim();
        String finalGender = gender;

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        User user = new User(userId, name, email, role, phone);
                        user.setGender(finalGender);
                        user.setRegistrationDate(new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date()));
                        
                        if ("Doctor".equals(role)) {
                            user.setSpecialization(specialization);
                            user.setExperience(finalExperience);
                            user.setHospitalName(finalHospitalName);
                            user.setLicenseNumber(finalLicenseNumber);
                            user.setFee(finalFee);
                            user.setAvailableTime(finalAvailableTime);
                            user.setLastUpdated(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
                            user.setVerified(false);

                            try {
                                java.io.InputStream inputStream = getContentResolver().openInputStream(imageUri);
                                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
                                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

                                // Scale down bitmap to save Realtime DB space
                                int width = bitmap.getWidth();
                                int height = bitmap.getHeight();
                                float ratio = Math.min((float) 400 / width, (float) 400 / height);
                                if (ratio < 1.0f) {
                                    bitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, Math.round(ratio * width), Math.round(ratio * height), true);
                                }
                                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, baos);
                                byte[] imageBytes = baos.toByteArray();
                                String base64Image = android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT);
                                
                                user.setProfileImageUrl(base64Image);
                                saveUserToDatabase(userId, user, true);
                            } catch (Exception e) {
                                progressBar.setVisibility(View.GONE);
                                btnRegister.setEnabled(true);
                                Toast.makeText(RegisterActivity.this, "Failed to process photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            user.setVerified(true);
                            saveUserToDatabase(userId, user, false);
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);
                        Toast.makeText(RegisterActivity.this, "Auth Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToDatabase(String userId, User user, boolean isDoctor) {
        reference.child(userId).setValue(user).addOnCompleteListener(dbTask -> {
            progressBar.setVisibility(View.GONE);
            if (dbTask.isSuccessful()) {
                if (isDoctor) {
                    Toast.makeText(RegisterActivity.this, "Registration Successful. Pending Admin Verification.", Toast.LENGTH_LONG).show();
                    mAuth.signOut(); // Log out from Auth because they shouldn't enter app yet
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                } else {
                    Toast.makeText(RegisterActivity.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(RegisterActivity.this, PatientDashboardActivity.class));
                }
                finish();
            } else {
                btnRegister.setEnabled(true);
                Toast.makeText(RegisterActivity.this, "DB Error: " + dbTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
