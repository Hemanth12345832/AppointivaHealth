package com.example.appointivahealth.activities;

import android.app.DatePickerDialog;
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
import com.example.appointivahealth.adapters.AppointmentManagementAdapter;
import com.example.appointivahealth.models.Appointment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AppointmentManagementActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AppointmentManagementAdapter adapter;
    private List<Appointment> appointmentList;
    private List<Appointment> filteredList;

    private EditText etSearchPatient, etFilterDate;
    private TextView tvNoAppointments;

    private String currentDoctorId;
    private Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_management);

        recyclerView = findViewById(R.id.rvAppointmentManagement);
        etSearchPatient = findViewById(R.id.etSearchPatient);
        etFilterDate = findViewById(R.id.etFilterDate);
        tvNoAppointments = findViewById(R.id.tvNoAppointments);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        appointmentList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new AppointmentManagementAdapter(this, filteredList);
        recyclerView.setAdapter(adapter);

        calendar = Calendar.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentDoctorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            loadAppointments();
        } else {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Search text listener
        etSearchPatient.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterData();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Date filter listener
        etFilterDate.setOnClickListener(v -> showDatePicker());
        
        // Double tap or long click to clear date filter could be useful, or clear icon.
        // For simplicity, long click to clear
        etFilterDate.setOnLongClickListener(v -> {
            etFilterDate.setText("");
            filterData();
            return true;
        });

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarAppointmentMgt);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void loadAppointments() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Appointments");
        ref.orderByChild("doctorId").equalTo(currentDoctorId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                appointmentList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Appointment appt = ds.getValue(Appointment.class);
                    if (appt != null) {
                        if ("payment_failed".equals(appt.getStatus())) {
                            continue; // Requirement: don't show rejected payments
                        }
                        appointmentList.add(appt);
                    }
                }
                filterData();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AppointmentManagementActivity.this, "Failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterData() {
        String searchQuery = etSearchPatient.getText().toString().toLowerCase().trim();
        String dateQuery = etFilterDate.getText().toString().trim();

        filteredList.clear();

        for (Appointment appt : appointmentList) {
            boolean matchesName = appt.getPatientName() != null && appt.getPatientName().toLowerCase().contains(searchQuery);
            boolean matchesDate = dateQuery.isEmpty() || (appt.getDate() != null && appt.getDate().equals(dateQuery));

            if (matchesName && matchesDate) {
                filteredList.add(appt);
            }
        }

        // Sort by date/time (simple sort pending items first or by date descending)
        // For now just refresh
        adapter.updateList(filteredList);

        if (filteredList.isEmpty()) {
            tvNoAppointments.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvNoAppointments.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showDatePicker() {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, yearSelected, monthOfYear, dayOfMonth) -> {
                    // Format: yyyy-MM-dd to match BookAppointmentActivity mapping
                    String formattedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", yearSelected, monthOfYear + 1, dayOfMonth);
                    etFilterDate.setText(formattedDate);
                    filterData();
                }, year, month, day);
        datePickerDialog.show();
    }
}
