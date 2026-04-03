package com.example.appointivahealth.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appointivahealth.R;
import com.example.appointivahealth.adapters.DoctorAdapter;
import com.example.appointivahealth.models.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class DoctorListActivity extends AppCompatActivity {

    private RecyclerView rvDoctors;
    private EditText etSearchDoctor;
    private DoctorAdapter adapter;
    private List<User> doctorList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_list);

        rvDoctors = findViewById(R.id.rvDoctors);
        etSearchDoctor = findViewById(R.id.etSearchDoctor);
        rvDoctors.setLayoutManager(new LinearLayoutManager(this));

        doctorList = new ArrayList<>();
        boolean isAdmin = getSharedPreferences("AppointivaPrefs", MODE_PRIVATE).getBoolean("isAdminLoggedIn", false);
        adapter = new DoctorAdapter(this, doctorList, isAdmin);
        rvDoctors.setAdapter(adapter);

        FloatingActionButton fabAddDoctor = findViewById(R.id.fabAddDoctor);
        if (isAdmin) {
            fabAddDoctor.setVisibility(View.VISIBLE);
            fabAddDoctor.setOnClickListener(v -> {
                startActivity(new Intent(DoctorListActivity.this, AddDoctorActivity.class));
            });
        } else {
            fabAddDoctor.setVisibility(View.GONE);
        }

        loadDoctors();

        etSearchDoctor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadDoctors() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
        usersRef.orderByChild("role").equalTo("Doctor").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                doctorList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    User doctor = data.getValue(User.class);
                    if (doctor != null) {
                        doctorList.add(doctor);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DoctorListActivity.this, "Failed to load doctors", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filter(String text) {
        List<User> filteredList = new ArrayList<>();
        for (User doc : doctorList) {
            boolean nameMatches = doc.getName() != null && doc.getName().toLowerCase().contains(text.toLowerCase());
            boolean specMatches = doc.getSpecialization() != null && doc.getSpecialization().toLowerCase().contains(text.toLowerCase());
            
            if (nameMatches || specMatches) {
                filteredList.add(doc);
            }
        }
        adapter.filterList(filteredList);
    }
}
