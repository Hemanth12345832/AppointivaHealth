package com.example.appointivahealth.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appointivahealth.R;
import com.example.appointivahealth.adapters.PatientAdapter;
import com.example.appointivahealth.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ManagePatientsActivity extends AppCompatActivity {

    private RecyclerView rvUsers;
    private EditText etSearchUser;
    private PatientAdapter adapter;
    private List<User> userList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_patients);

        rvUsers = findViewById(R.id.rvPatients);
        etSearchUser = findViewById(R.id.etSearchPatient);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));

        userList = new ArrayList<>();
        adapter = new PatientAdapter(this, userList);
        rvUsers.setAdapter(adapter);

        loadUsers();

        etSearchUser.addTextChangedListener(new TextWatcher() {
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

    private void loadUsers() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
        usersRef.orderByChild("role").equalTo("Patient").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    User user = data.getValue(User.class);
                    if (user != null) {
                        user.setSpecialization("Patient");
                        userList.add(user);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ManagePatientsActivity.this, "Failed to load patients", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filter(String text) {
        List<User> filteredList = new ArrayList<>();
        for (User user : userList) {
            if (user.getName().toLowerCase().contains(text.toLowerCase()) || 
                (user.getPhone() != null && user.getPhone().contains(text))) {
                filteredList.add(user);
            }
        }
        adapter.filterList(filteredList);
    }
}
