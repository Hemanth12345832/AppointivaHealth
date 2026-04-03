package com.example.appointivahealth.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appointivahealth.R;
import com.example.appointivahealth.adapters.DoctorVerificationAdapter;
import com.example.appointivahealth.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class VerifyDoctorsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DoctorVerificationAdapter verificationAdapter;
    private List<User> pendingDoctors;
    private ProgressBar progressBar;
    private TextView tvNoPendingDoctors;
    private DatabaseReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_doctors);

        recyclerView = findViewById(R.id.recyclerViewVerifyDoctors);
        progressBar = findViewById(R.id.progressBarVerifyDoctors);
        tvNoPendingDoctors = findViewById(R.id.tvNoPendingDoctors);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        pendingDoctors = new ArrayList<>();
        verificationAdapter = new DoctorVerificationAdapter(this, pendingDoctors);
        recyclerView.setAdapter(verificationAdapter);

        reference = FirebaseDatabase.getInstance().getReference("Users");
        
        loadPendingDoctors();
    }

    private void loadPendingDoctors() {
        progressBar.setVisibility(View.VISIBLE);
        reference.orderByChild("role").equalTo("Doctor").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                pendingDoctors.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    User doctor = ds.getValue(User.class);
                    // If not verified, add to list
                    if (doctor != null && !doctor.isVerified()) {
                        pendingDoctors.add(doctor);
                    }
                }
                
                verificationAdapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                
                if (pendingDoctors.isEmpty()) {
                    tvNoPendingDoctors.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    tvNoPendingDoctors.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(VerifyDoctorsActivity.this, "Database Error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
