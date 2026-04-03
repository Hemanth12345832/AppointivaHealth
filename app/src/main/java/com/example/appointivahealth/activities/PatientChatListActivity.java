package com.example.appointivahealth.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appointivahealth.R;
import com.example.appointivahealth.adapters.ChatListAdapter;
import com.example.appointivahealth.models.Appointment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PatientChatListActivity extends AppCompatActivity {

    private RecyclerView rvChatList;
    private TextView tvNoChats;
    private ChatListAdapter adapter;
    private List<Appointment> chatList;
    private String currentPatientId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarChatList);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        rvChatList = findViewById(R.id.rvChatList);
        tvNoChats = findViewById(R.id.tvNoChats);

        rvChatList.setLayoutManager(new LinearLayoutManager(this));
        chatList = new ArrayList<>();
        adapter = new ChatListAdapter(this, chatList, false);
        rvChatList.setAdapter(adapter);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentPatientId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            loadChats();
        } else {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadChats() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Appointments");
        ref.orderByChild("patientId").equalTo(currentPatientId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Appointment appt = ds.getValue(Appointment.class);
                    if (appt != null) {
                        String status = appt.getStatus();
                        if ("Accepted".equalsIgnoreCase(status) || "Scheduled".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status) || "Cancelled".equalsIgnoreCase(status)) {
                            chatList.add(appt);
                        }
                    }
                }
                adapter.notifyDataSetChanged();

                if (chatList.isEmpty()) {
                    tvNoChats.setVisibility(View.VISIBLE);
                    rvChatList.setVisibility(View.GONE);
                } else {
                    tvNoChats.setVisibility(View.GONE);
                    rvChatList.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PatientChatListActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
