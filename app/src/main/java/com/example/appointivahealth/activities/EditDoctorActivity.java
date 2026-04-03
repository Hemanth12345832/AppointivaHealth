package com.example.appointivahealth.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class EditDoctorActivity extends AppCompatActivity {

    private TextInputEditText etName, etSpecialization, etPhone, etHospital, etExperience, etFee, etAvailableTime;
    private TextView tvLastUpdated;
    private ImageView ivProfilePhoto;
    private FloatingActionButton fabUploadPhoto;
    private MaterialButton btnUpdate, btnDelete;
    private ProgressBar progressBar;
    
    private User doctor;
    private DatabaseReference userRef;
    private Uri imageUri;
    private ActivityResultLauncher<Intent> photoPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_doctor);

        etName = findViewById(R.id.etEditDoctorName);
        etSpecialization = findViewById(R.id.etEditDoctorSpecialization);
        etPhone = findViewById(R.id.etEditDoctorPhone);
        etHospital = findViewById(R.id.etEditDoctorHospital);
        etExperience = findViewById(R.id.etEditDoctorExperience);
        etFee = findViewById(R.id.etEditDoctorFee);
        etAvailableTime = findViewById(R.id.etEditDoctorAvailableTime);
        tvLastUpdated = findViewById(R.id.tvEditDoctorLastUpdated);
        ivProfilePhoto = findViewById(R.id.ivEditDoctorPhoto);
        fabUploadPhoto = findViewById(R.id.fabUploadEditDoctorPhoto);
        btnUpdate = findViewById(R.id.btnUpdateDoctor);
        btnDelete = findViewById(R.id.btnDeleteDoctor);
        progressBar = findViewById(R.id.progressBarEditDoctor);

        doctor = (User) getIntent().getSerializableExtra("doctor");
        
        if (doctor != null) {
            userRef = FirebaseDatabase.getInstance().getReference("Users").child(doctor.getId());
            populateFields();
        } else {
            Toast.makeText(this, "Error loading doctor", Toast.LENGTH_SHORT).show();
            finish();
        }

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

        btnUpdate.setOnClickListener(v -> updateDoctor());
        btnDelete.setOnClickListener(v -> deleteDoctor());
    }

    private void populateFields() {
        etName.setText(doctor.getName() != null ? doctor.getName() : "");
        etSpecialization.setText(doctor.getSpecialization() != null ? doctor.getSpecialization() : "");
        etPhone.setText(doctor.getPhone() != null ? doctor.getPhone() : "");
        etHospital.setText(doctor.getHospitalName() != null ? doctor.getHospitalName() : "");
        etExperience.setText(doctor.getExperience() != null ? doctor.getExperience() : "");
        etFee.setText(doctor.getFee() != null ? doctor.getFee() : "");
        etAvailableTime.setText(doctor.getAvailableTime() != null ? doctor.getAvailableTime() : "");
        
        String lastUpd = doctor.getLastUpdated() != null && !doctor.getLastUpdated().isEmpty() ? doctor.getLastUpdated() : "Never";
        tvLastUpdated.setText("Last Updated: " + lastUpd);

        if (doctor.getProfileImageUrl() != null && !doctor.getProfileImageUrl().isEmpty()) {
            if (doctor.getProfileImageUrl().startsWith("http")) {
                Glide.with(this).load(doctor.getProfileImageUrl()).placeholder(R.mipmap.ic_launcher_round).into(ivProfilePhoto);
            } else {
                try {
                    byte[] imageByteArray = android.util.Base64.decode(doctor.getProfileImageUrl(), android.util.Base64.DEFAULT);
                    Glide.with(this).asBitmap().load(imageByteArray).placeholder(R.mipmap.ic_launcher_round).into(ivProfilePhoto);
                } catch (IllegalArgumentException e) {
                    ivProfilePhoto.setImageResource(R.mipmap.ic_launcher_round);
                }
            }
        }
    }

    private void updateDoctor() {
        String name = etName.getText().toString().trim();
        String spec = etSpecialization.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String hospital = etHospital.getText().toString().trim();
        String exp = etExperience.getText().toString().trim();
        String fee = etFee.getText().toString().trim();
        String avail = etAvailableTime.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(spec)) {
            Toast.makeText(this, "Name and Specialization cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnUpdate.setEnabled(false);

        String currentTimestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());

        userRef.child("name").setValue(name);
        userRef.child("specialization").setValue(spec);
        userRef.child("phone").setValue(phone);
        userRef.child("hospitalName").setValue(hospital);
        userRef.child("experience").setValue(exp);
        userRef.child("fee").setValue(fee);
        userRef.child("availableTime").setValue(avail);
        userRef.child("lastUpdated").setValue(currentTimestamp);

        // Process image if selected
        if (imageUri != null) {
            try {
                java.io.InputStream inputStream = getContentResolver().openInputStream(imageUri);
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                float ratio = Math.min((float) 400 / width, (float) 400 / height);
                if (ratio < 1.0f) {
                    bitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, Math.round(ratio * width), Math.round(ratio * height), true);
                }
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, baos);
                byte[] imageBytes = baos.toByteArray();
                String base64Image = android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT);
                userRef.child("profileImageUrl").setValue(base64Image);
            } catch (Exception e) {
                // Ignore encode error
            }
        }

        // We assume db operations are quick enough to just addlistener to one
        userRef.child("lastUpdated").setValue(currentTimestamp)
            .addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE);
                btnUpdate.setEnabled(true);
                if(task.isSuccessful()){
                    Toast.makeText(EditDoctorActivity.this, "Doctor Updated", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditDoctorActivity.this, "Failed to update", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void deleteDoctor() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Doctor")
            .setMessage("Are you sure you want to delete this doctor? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                progressBar.setVisibility(View.VISIBLE);
                btnDelete.setEnabled(false);
                userRef.removeValue().addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(EditDoctorActivity.this, "Doctor Deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        btnDelete.setEnabled(true);
                        Toast.makeText(EditDoctorActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
