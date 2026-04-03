package com.example.appointivahealth.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appointivahealth.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class DoctorAvailabilityActivity extends AppCompatActivity {

    private EditText etMon, etTue, etWed, etThu, etFri, etSat, etSun;
    private Button btnSaveAvailability;
    private String doctorId;
    private DatabaseReference availabilityRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_availability);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarAvailability);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        etMon = findViewById(R.id.etMonTime);
        etTue = findViewById(R.id.etTueTime);
        etWed = findViewById(R.id.etWedTime);
        etThu = findViewById(R.id.etThuTime);
        etFri = findViewById(R.id.etFriTime);
        etSat = findViewById(R.id.etSatTime);
        etSun = findViewById(R.id.etSunTime);
        btnSaveAvailability = findViewById(R.id.btnSaveAvailability);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            doctorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            availabilityRef = FirebaseDatabase.getInstance().getReference("Availability").child(doctorId);
            loadExistingAvailability();
        } else {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnSaveAvailability.setOnClickListener(v -> saveAvailability());
    }

    private void loadExistingAvailability() {
        availabilityRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    if (snapshot.hasChild("Monday")) etMon.setText(snapshot.child("Monday").getValue(String.class));
                    if (snapshot.hasChild("Tuesday")) etTue.setText(snapshot.child("Tuesday").getValue(String.class));
                    if (snapshot.hasChild("Wednesday")) etWed.setText(snapshot.child("Wednesday").getValue(String.class));
                    if (snapshot.hasChild("Thursday")) etThu.setText(snapshot.child("Thursday").getValue(String.class));
                    if (snapshot.hasChild("Friday")) etFri.setText(snapshot.child("Friday").getValue(String.class));
                    if (snapshot.hasChild("Saturday")) etSat.setText(snapshot.child("Saturday").getValue(String.class));
                    if (snapshot.hasChild("Sunday")) etSun.setText(snapshot.child("Sunday").getValue(String.class));
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(DoctorAvailabilityActivity.this, "Failed to load availability", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveAvailability() {
        HashMap<String, Object> availMap = new HashMap<>();
        availMap.put("Monday", etMon.getText().toString().trim());
        availMap.put("Tuesday", etTue.getText().toString().trim());
        availMap.put("Wednesday", etWed.getText().toString().trim());
        availMap.put("Thursday", etThu.getText().toString().trim());
        availMap.put("Friday", etFri.getText().toString().trim());
        availMap.put("Saturday", etSat.getText().toString().trim());
        availMap.put("Sunday", etSun.getText().toString().trim());

        // We can also save this to Users/doctorId/availableTime as a concatenated string for quick display
        String concat = "";
        if (!etMon.getText().toString().trim().isEmpty()) concat += "Mon: " + etMon.getText().toString().trim() + " ";
        if (!etTue.getText().toString().trim().isEmpty()) concat += "Tue: " + etTue.getText().toString().trim() + " ";

        final String availableTimeString = concat.trim();

        availabilityRef.setValue(availMap).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (!availableTimeString.isEmpty()) {
                    FirebaseDatabase.getInstance().getReference("Users").child(doctorId)
                            .child("availableTime").setValue(availableTimeString);
                }
                Toast.makeText(DoctorAvailabilityActivity.this, "Availability Saved Successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(DoctorAvailabilityActivity.this, "Failed to save availability", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
