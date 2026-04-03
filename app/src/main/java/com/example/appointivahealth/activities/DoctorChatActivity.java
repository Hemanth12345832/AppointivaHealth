package com.example.appointivahealth.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appointivahealth.R;
import com.example.appointivahealth.adapters.ChatAdapter;
import com.example.appointivahealth.models.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class DoctorChatActivity extends AppCompatActivity {

    private String appointmentId, patientId, doctorId, patientName;
    private RecyclerView rvDoctorChat;
    private EditText etDoctorChatMessage;
    private ImageButton btnDoctorSendChat;
    private TextView tvChatPatientName;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatList;
    private DatabaseReference chatsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_chat);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarDoctorChat);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        appointmentId = getIntent().getStringExtra("appointmentId");
        patientId = getIntent().getStringExtra("patientId");
        patientName = getIntent().getStringExtra("patientName");
        
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            doctorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        rvDoctorChat = findViewById(R.id.rvDoctorChat);
        etDoctorChatMessage = findViewById(R.id.etDoctorChatMessage);
        btnDoctorSendChat = findViewById(R.id.btnDoctorSendChat);
        tvChatPatientName = findViewById(R.id.tvChatPatientName);

        tvChatPatientName.setText(patientName != null ? patientName : "Patient Chat");
        
        chatList = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, chatList, doctorId);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvDoctorChat.setLayoutManager(layoutManager);
        rvDoctorChat.setAdapter(chatAdapter);

        if (appointmentId != null) {
            chatsRef = FirebaseDatabase.getInstance().getReference("Chats").child(appointmentId);
            checkAppointmentStatus();
            loadMessages();
        } else {
            Toast.makeText(this, "Error: missing appointment ID", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnDoctorSendChat.setOnClickListener(v -> sendMessage());
    }

    private void checkAppointmentStatus() {
        DatabaseReference apptRef = FirebaseDatabase.getInstance().getReference("Appointments").child(appointmentId);
        apptRef.child("status").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.getValue(String.class);
                    if ("Rejected".equalsIgnoreCase(status) || "Cancelled".equalsIgnoreCase(status) || "Pending".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status)) {
                        etDoctorChatMessage.setEnabled(false);
                        btnDoctorSendChat.setEnabled(false);
                        etDoctorChatMessage.setHint("Chat is disabled for this status.");
                    } else {
                        etDoctorChatMessage.setEnabled(true);
                        btnDoctorSendChat.setEnabled(true);
                        etDoctorChatMessage.setHint("Type a message...");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void loadMessages() {
        chatsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ChatMessage msg = ds.getValue(ChatMessage.class);
                    if (msg != null) {
                        chatList.add(msg);
                    }
                }
                chatAdapter.notifyDataSetChanged();
                rvDoctorChat.scrollToPosition(chatList.size() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void sendMessage() {
        String msgText = etDoctorChatMessage.getText().toString().trim();
        if (msgText.isEmpty()) return;

        String msgId = chatsRef.push().getKey();
        long timestamp = System.currentTimeMillis();
        
        ChatMessage chatMessage = new ChatMessage(msgId, doctorId, "Doctor", msgText, timestamp);
        
        if (msgId != null) {
            chatsRef.child(msgId).setValue(chatMessage).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    etDoctorChatMessage.setText("");
                } else {
                    Toast.makeText(DoctorChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
