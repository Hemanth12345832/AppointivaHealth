package com.example.appointivahealth.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appointivahealth.R;
import com.example.appointivahealth.adapters.AdminComplaintsAdapter;
import com.example.appointivahealth.models.Complaint;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminComplaintsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdminComplaintsAdapter adapter;
    private List<Complaint> complaintList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_complaints);

        Toolbar toolbar = findViewById(R.id.toolbarAdminComplaints);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        recyclerView = findViewById(R.id.rvAdminComplaints);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        complaintList = new ArrayList<>();
        adapter = new AdminComplaintsAdapter(this, complaintList);
        recyclerView.setAdapter(adapter);

        loadComplaints();
    }

    private void loadComplaints() {
        DatabaseReference complaintsRef = FirebaseDatabase.getInstance().getReference("Complaints");
        complaintsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                complaintList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Complaint complaint = ds.getValue(Complaint.class);
                    if (complaint != null) {
                        complaintList.add(complaint);
                    }
                }
                Collections.reverse(complaintList); // Newest first
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminComplaintsActivity.this, "Failed to load complaints", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
