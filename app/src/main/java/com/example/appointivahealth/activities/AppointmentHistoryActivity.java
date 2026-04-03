package com.example.appointivahealth.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appointivahealth.R;
import com.example.appointivahealth.adapters.AppointmentAdapter;
import com.example.appointivahealth.models.Appointment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppointmentHistoryActivity extends AppCompatActivity {

    private RecyclerView rvAppointments;
    private AppointmentAdapter adapter;
    private List<Appointment> appointmentList;
    private String userRole = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_history);

        rvAppointments = findViewById(R.id.rvAppointments);
        rvAppointments.setLayoutManager(new LinearLayoutManager(this));

        appointmentList = new ArrayList<>();
        
        determineUserRoleAndLoad();
    }

    private void determineUserRoleAndLoad() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) {
            userRole = "Admin";
            finishSetup();
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    userRole = snapshot.child("role").getValue(String.class);
                } else {
                    userRole = "Admin";
                }
                finishSetup();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                userRole = "Patient";
                finishSetup();
            }
        });
    }

    private void finishSetup() {
        adapter = new AppointmentAdapter(this, appointmentList, userRole);
        rvAppointments.setAdapter(adapter);
        loadAppointments();
    }

    private void loadAppointments() {
        DatabaseReference apptRef = FirebaseDatabase.getInstance().getReference("Appointments");
        apptRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                appointmentList.clear();
                String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

                for (DataSnapshot data : snapshot.getChildren()) {
                    Appointment appt = data.getValue(Appointment.class);
                    if (appt != null) {
                        if ("Admin".equals(userRole)) {
                            appointmentList.add(appt);
                        } else if ("Doctor".equals(userRole) && uid.equals(appt.getDoctorId())) {
                            appointmentList.add(appt);
                        } else if ("Patient".equals(userRole) && uid.equals(appt.getPatientId())) {
                            appointmentList.add(appt);
                        }
                    }
                }
                Collections.reverse(appointmentList); // newest first usually
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AppointmentHistoryActivity.this, "Failed to load", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
