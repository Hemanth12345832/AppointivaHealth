package com.example.appointivahealth.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.appointivahealth.R;
import com.example.appointivahealth.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;

public class AddDoctorActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPassword, etPhone, etSpecialization, etHospital, etExperience, etLicense, etFee, etAvailableTime;
    private ImageView ivProfilePhoto;
    private FloatingActionButton fabUploadPhoto;
    private MaterialButton btnSubmit;
    private ProgressBar progressBar;
    
    private DatabaseReference userRef;
    private Uri imageUri;
    private ActivityResultLauncher<Intent> photoPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_doctor);

        etName = findViewById(R.id.etAddDoctorName);
        etEmail = findViewById(R.id.etAddDoctorEmail);
        etPassword = findViewById(R.id.etAddDoctorPassword);
        etPhone = findViewById(R.id.etAddDoctorPhone);
        etSpecialization = findViewById(R.id.etAddDoctorSpecialization);
        etHospital = findViewById(R.id.etAddDoctorHospital);
        etExperience = findViewById(R.id.etAddDoctorExperience);
        etLicense = findViewById(R.id.etAddDoctorLicense);
        etFee = findViewById(R.id.etAddDoctorFee);
        etAvailableTime = findViewById(R.id.etAddDoctorAvailableTime);
        
        ivProfilePhoto = findViewById(R.id.ivAddDoctorPhoto);
        fabUploadPhoto = findViewById(R.id.fabUploadAddDoctorPhoto);
        btnSubmit = findViewById(R.id.btnAddDoctorSubmit);
        progressBar = findViewById(R.id.progressBarAddDoctor);

        userRef = FirebaseDatabase.getInstance().getReference("Users");

        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        Glide.with(this).load(imageUri).into(ivProfilePhoto);
                    }
                });

        fabUploadPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            photoPickerLauncher.launch(intent);
        });

        btnSubmit.setOnClickListener(v -> addDoctor());
    }

    private void addDoctor() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String specialization = etSpecialization.getText().toString().trim();
        String hospital = etHospital.getText().toString().trim();
        String experience = etExperience.getText().toString().trim();
        String license = etLicense.getText().toString().trim();
        String fee = etFee.getText().toString().trim();
        String availableTime = etAvailableTime.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || 
            TextUtils.isEmpty(specialization) || TextUtils.isEmpty(hospital) || TextUtils.isEmpty(experience)) {
            Toast.makeText(this, "Please fill in all core fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 8) {
            etPassword.setError("Password must be at least 8 characters long");
            return;
        }

        if (imageUri == null) {
            Toast.makeText(this, "Please select a profile photo", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        // We use REST API to prevent logging the Admin out
        createDoctorAuthWithREST(email, password, name, phone, specialization, hospital, experience, license, fee, availableTime);
    }
    
    private void createDoctorAuthWithREST(String email, String pwd, String name, String phone, 
                                          String spec, String hosp, String exp, String lic, 
                                          String fee, String avail) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String apiKey = FirebaseApp.getInstance().getOptions().getApiKey();
                URL url = new URL("https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + apiKey);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String jsonInputString = "{\"email\": \"" + email + "\", \"password\": \"" + pwd + "\", \"returnSecureToken\": true}";

                try(OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);           
                }

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    JSONObject jsonObject = new JSONObject(response.toString());
                    String userId = jsonObject.getString("localId");
                    
                    runOnUiThread(() -> saveDoctorToDatabase(userId, name, email, phone, spec, hosp, exp, lic, fee, avail));
                } else {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
                    StringBuilder errResponse = new StringBuilder();
                    String line;
                    while((line = br.readLine()) != null) {
                        errResponse.append(line);
                    }
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnSubmit.setEnabled(true);
                        Toast.makeText(AddDoctorActivity.this, "Failed to create Auth: " + errResponse.toString(), Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    Toast.makeText(AddDoctorActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void saveDoctorToDatabase(String userId, String name, String email, String phone, 
                                      String spec, String hosp, String exp, String lic, 
                                      String fee, String avail) {
        User user = new User(userId, name, email, "Doctor", phone);
        user.setSpecialization(spec);
        user.setHospitalName(hosp);
        user.setExperience(exp);
        user.setLicenseNumber(lic);
        user.setFee(fee);
        user.setAvailableTime(avail);
        user.setVerified(true);
        user.setLastUpdated(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));

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
        } catch (Exception e) {
            // Proceed without photo if encoding fails
        }

        userRef.child(userId).setValue(user).addOnCompleteListener(dbTask -> {
            progressBar.setVisibility(View.GONE);
            if (dbTask.isSuccessful()) {
                Toast.makeText(AddDoctorActivity.this, "Doctor Added Successfully!", Toast.LENGTH_LONG).show();
                finish(); // Go back to DoctorListActivity without logging admin out!
            } else {
                btnSubmit.setEnabled(true);
                Toast.makeText(AddDoctorActivity.this, "Database Error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
