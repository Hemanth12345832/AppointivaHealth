package com.example.appointivahealth.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appointivahealth.R;
import com.example.appointivahealth.adapters.PatientRecordsAdapter;
import com.example.appointivahealth.models.Appointment;
import com.example.appointivahealth.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class DoctorPatientRecordsActivity extends AppCompatActivity {

    private RecyclerView rvPatientRecords;
    private EditText etSearchPatientRecord;
    private TextView tvNoPatientsRecords;

    private PatientRecordsAdapter adapter;
    private List<User> patientList;
    private List<User> filteredList;

    private String currentDoctorId;
    private HashSet<String> uniquePatientIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_patient_records);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarPatientRecords);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        rvPatientRecords = findViewById(R.id.rvPatientRecords);
        etSearchPatientRecord = findViewById(R.id.etSearchPatientRecord);
        tvNoPatientsRecords = findViewById(R.id.tvNoPatientsRecords);

        rvPatientRecords.setLayoutManager(new LinearLayoutManager(this));
        patientList = new ArrayList<>();
        filteredList = new ArrayList<>();
        uniquePatientIds = new HashSet<>();

        adapter = new PatientRecordsAdapter(this, filteredList);
        rvPatientRecords.setAdapter(adapter);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentDoctorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            loadTreatedPatients();
        }

        etSearchPatientRecord.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterData(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadTreatedPatients() {
        DatabaseReference appointmentsRef = FirebaseDatabase.getInstance().getReference("Appointments");
        appointmentsRef.orderByChild("doctorId").equalTo(currentDoctorId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                uniquePatientIds.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Appointment appt = ds.getValue(Appointment.class);
                    // Consider all patients that have interacted, or only "Completed". For now everyone to make it easier for doctors to see.
                    if (appt != null && appt.getPatientId() != null) {
                        uniquePatientIds.add(appt.getPatientId());
                    }
                }
                
                if (!uniquePatientIds.isEmpty()) {
                    fetchPatientDetails();
                } else {
                    updateUI();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DoctorPatientRecordsActivity.this, "Error loading records", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchPatientDetails() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                patientList.clear();
                for (String pid : uniquePatientIds) {
                    if (snapshot.hasChild(pid)) {
                        User patient = snapshot.child(pid).getValue(User.class);
                        if (patient != null) {
                            patient.setId(pid);
                            patientList.add(patient);
                        }
                    }
                }
                filterData("");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void filterData(String query) {
        filteredList.clear();
        String lowerQuery = query.toLowerCase().trim();

        for (User u : patientList) {
            if (u.getName() != null && u.getName().toLowerCase().contains(lowerQuery)) {
                filteredList.add(u);
            }
        }

        adapter.updateList(filteredList);
        updateUI();
    }

    private void updateUI() {
        if (filteredList.isEmpty()) {
            tvNoPatientsRecords.setVisibility(View.VISIBLE);
            rvPatientRecords.setVisibility(View.GONE);
        } else {
            tvNoPatientsRecords.setVisibility(View.GONE);
            rvPatientRecords.setVisibility(View.VISIBLE);
        }
    }
}
