package com.example.appointivahealth.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appointivahealth.R;
import com.example.appointivahealth.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import com.example.appointivahealth.models.Appointment;
import com.example.appointivahealth.models.GenericRecord;
import com.example.appointivahealth.adapters.GenericRecordAdapter;

public class AdminPatientDetailsActivity extends AppCompatActivity {

    private TextView tvName, tvContact, tvDemo, tvRegDate;
    private Spinner spinnerCategory;
    private RecyclerView rvCategoryData;
    private User patient;
    private GenericRecordAdapter adapter;
    private List<GenericRecord> recordList;

    private String[] categories = {"Appointments", "Payments", "Medical Records"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_patient_details);

        tvName = findViewById(R.id.tvDetailsName);
        tvContact = findViewById(R.id.tvDetailsContact);
        tvDemo = findViewById(R.id.tvDetailsDemo);
        tvRegDate = findViewById(R.id.tvDetailsRegDate);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        rvCategoryData = findViewById(R.id.rvCategoryData);
        
        rvCategoryData.setLayoutManager(new LinearLayoutManager(this));
        recordList = new ArrayList<>();
        adapter = new GenericRecordAdapter(this, recordList, true); // Admin is read-only
        rvCategoryData.setAdapter(adapter);

        patient = (User) getIntent().getSerializableExtra("patient");
        if (patient != null) {
            bindPatientData();
        } else {
            Toast.makeText(this, "Error: Patient data missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadCategoryData(categories[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void bindPatientData() {
        tvName.setText(patient.getName());
        tvContact.setText((patient.getPhone() != null && !patient.getPhone().isEmpty() ? patient.getPhone() : "N/A") + " | " + 
                          (patient.getEmail() != null ? patient.getEmail() : "N/A"));
        
        String age = (patient.getAge() != null && !patient.getAge().isEmpty()) ? patient.getAge() : "N/A";
        String gender = (patient.getGender() != null && !patient.getGender().isEmpty()) ? patient.getGender() : "N/A";
        tvDemo.setText("Age: " + age + " | Gender: " + gender);
        
        String reg = (patient.getRegistrationDate() != null && !patient.getRegistrationDate().isEmpty()) ? patient.getRegistrationDate() : "N/A";
        tvRegDate.setText("Registered: " + reg);
    }

    private void loadCategoryData(String category) {
        recordList.clear();
        adapter.notifyDataSetChanged();

        if (category.equals("Appointments")) {
            FirebaseDatabase.getInstance().getReference("Appointments")
                .orderByChild("patientId").equalTo(patient.getId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot data : snapshot.getChildren()) {
                            Appointment appt = data.getValue(Appointment.class);
                            if (appt != null) {
                                recordList.add(new GenericRecord("Dr. " + appt.getDoctorName(), 
                                    "Time: " + appt.getTime(), appt.getDate(), appt.getStatus()));
                            }
                        }
                        if (recordList.isEmpty()) {
                            recordList.add(new GenericRecord("No Appointments Found", "", "", ""));
                        }
                        adapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
        } else if (category.equals("Payments")) {
            FirebaseDatabase.getInstance().getReference("Appointments")
                .orderByChild("patientId").equalTo(patient.getId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot data : snapshot.getChildren()) {
                            Appointment appt = data.getValue(Appointment.class);
                            if (appt != null && appt.getPaymentStatus() != null) {
                                String amount = appt.getAmount() != null && !appt.getAmount().isEmpty() ? appt.getAmount() : "300";
                                String txnId = appt.getTransactionId() != null && !appt.getTransactionId().isEmpty() ? appt.getTransactionId() : "N/A";
                                
                                recordList.add(new GenericRecord("Patient: " + patient.getName() + " | Amt: ₹" + amount, 
                                    "Appt ID: " + appt.getId() + "\nTxn: " + txnId, 
                                    appt.getDate(), appt.getPaymentStatus()));
                            }
                        }
                        if (recordList.isEmpty()) {
                            recordList.add(new GenericRecord("No Payments Found", "", "", ""));
                        }
                        adapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
        } else if (category.equals("Medical Records")) {
            FirebaseDatabase.getInstance().getReference("MedicalRecords")
                .orderByChild("patientId").equalTo(patient.getId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        java.util.HashSet<String> seenAppointments = new java.util.HashSet<>();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            com.example.appointivahealth.models.MedicalRecord mr = data.getValue(com.example.appointivahealth.models.MedicalRecord.class);
                            if (mr != null && mr.getAppointmentId() != null) {
                                if (!seenAppointments.contains(mr.getAppointmentId())) {
                                    seenAppointments.add(mr.getAppointmentId());
                                    GenericRecord gRec = new GenericRecord(mr.getTitle() != null ? mr.getTitle() : "Consultation Notes", 
                                        "Appt ID: " + mr.getAppointmentId(), mr.getDate(), "Viewable");
                                    gRec.appointmentId = mr.getAppointmentId();
                                    gRec.recordId = mr.getId();
                                    recordList.add(gRec);
                                }
                            }
                        }
                        if (recordList.isEmpty()) {
                            recordList.add(new GenericRecord("No Medical Records Found", "", "", ""));
                        }
                        adapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
        }
    }
}
