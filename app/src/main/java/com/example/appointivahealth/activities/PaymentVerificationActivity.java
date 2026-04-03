package com.example.appointivahealth.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appointivahealth.R;
import com.example.appointivahealth.adapters.PaymentVerificationAdapter;
import com.example.appointivahealth.models.Appointment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PaymentVerificationActivity extends AppCompatActivity {

    private RecyclerView rvPaymentVerifications;
    private PaymentVerificationAdapter adapter;
    private List<Appointment> pendingAppointments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_verification);

        rvPaymentVerifications = findViewById(R.id.rvPaymentVerifications);
        rvPaymentVerifications.setLayoutManager(new LinearLayoutManager(this));

        pendingAppointments = new ArrayList<>();
        adapter = new PaymentVerificationAdapter(this, pendingAppointments);
        rvPaymentVerifications.setAdapter(adapter);

        loadPendingVerifications();
    }

    private void loadPendingVerifications() {
        DatabaseReference apptRef = FirebaseDatabase.getInstance().getReference("Appointments");
        apptRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                pendingAppointments.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Appointment appointment = ds.getValue(Appointment.class);
                    if (appointment != null) {
                        if ("paid".equals(appointment.getPaymentStatus()) && !appointment.isAdminVerified()) {
                            pendingAppointments.add(appointment);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PaymentVerificationActivity.this, "Failed to load verifications", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
