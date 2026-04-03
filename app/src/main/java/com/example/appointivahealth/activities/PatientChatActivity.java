package com.example.appointivahealth.activities;

import android.content.Intent;
import android.os.Bundle;
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

import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import androidx.annotation.Nullable;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class PatientChatActivity extends AppCompatActivity {

    private String appointmentId, doctorId, patientId, doctorName;
    private RecyclerView rvPatientChat;
    private EditText etPatientChatMessage;
    private ImageButton btnPatientSendChat, btnPatientAttachFile;
    private TextView tvChatDoctorName;
    private Button btnViewPrescription;

    private static final int PICK_FILE_REQUEST = 100;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatList;
    private DatabaseReference chatsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_chat);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarPatientChat);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        appointmentId = getIntent().getStringExtra("appointmentId");
        doctorId = getIntent().getStringExtra("doctorId");
        doctorName = getIntent().getStringExtra("doctorName");
        
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            patientId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        rvPatientChat = findViewById(R.id.rvPatientChat);
        etPatientChatMessage = findViewById(R.id.etPatientChatMessage);
        btnPatientSendChat = findViewById(R.id.btnPatientSendChat);
        btnPatientAttachFile = findViewById(R.id.btnPatientAttachFile);
        tvChatDoctorName = findViewById(R.id.tvChatDoctorName);
        btnViewPrescription = findViewById(R.id.btnViewPrescription);

        tvChatDoctorName.setText("Dr. " + (doctorName != null ? doctorName : ""));

        chatList = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, chatList, patientId);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvPatientChat.setLayoutManager(layoutManager);
        rvPatientChat.setAdapter(chatAdapter);

        if (appointmentId != null) {
            chatsRef = FirebaseDatabase.getInstance().getReference("Chats").child(appointmentId);
            checkAppointmentStatus();
            loadMessages();
        } else {
            Toast.makeText(this, "Error: missing appointment ID", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnPatientSendChat.setOnClickListener(v -> sendMessage());
        btnPatientAttachFile.setOnClickListener(v -> openFilePicker());

        btnViewPrescription.setOnClickListener(v -> {
            // Can launch a view-only mode of PrescriptionActivity or PatientRecordDetailsActivity.
            Intent intent = new Intent(PatientChatActivity.this, PrescriptionActivity.class);
            intent.putExtra("appointmentId", appointmentId);
            intent.putExtra("viewOnly", true);
            startActivity(intent);
        });
    }

    private void checkAppointmentStatus() {
        DatabaseReference apptRef = FirebaseDatabase.getInstance().getReference("Appointments").child(appointmentId);
        apptRef.child("status").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.getValue(String.class);
                    if ("Rejected".equalsIgnoreCase(status) || "Cancelled".equalsIgnoreCase(status) || "Pending".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status)) {
                        etPatientChatMessage.setEnabled(false);
                        btnPatientSendChat.setEnabled(false);
                        btnPatientAttachFile.setEnabled(false);
                        etPatientChatMessage.setHint("Chat is disabled for this status.");
                    } else {
                        etPatientChatMessage.setEnabled(true);
                        btnPatientSendChat.setEnabled(true);
                        btnPatientAttachFile.setEnabled(true);
                        etPatientChatMessage.setHint("Type a message...");
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
                rvPatientChat.scrollToPosition(chatList.size() - 1);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void sendMessage() {
        String msgText = etPatientChatMessage.getText().toString().trim();
        if (msgText.isEmpty()) return;

        String msgId = chatsRef.push().getKey();
        long timestamp = System.currentTimeMillis();
        
        ChatMessage chatMessage = new ChatMessage(msgId, patientId, "Patient", msgText, timestamp);
        
        if (msgId != null) {
            chatsRef.child(msgId).setValue(chatMessage).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    etPatientChatMessage.setText("");
                } else {
                    Toast.makeText(PatientChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimetypes = {"image/jpeg", "image/png", "application/pdf"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri fileUri = data.getData();
            processAndUploadFile(fileUri);
        }
    }

    private void processAndUploadFile(Uri fileUri) {
        try {
            String fileName = getFileName(fileUri);
            final String safeName = fileName != null ? fileName : "document_" + System.currentTimeMillis();
            final String mimeType = getContentResolver().getType(fileUri);

            Toast.makeText(this, "Uploading document to Cloud Storage...", Toast.LENGTH_SHORT).show();

            StorageReference storageRef = FirebaseStorage.getInstance().getReference("MedicalDocuments")
                    .child(appointmentId)
                    .child(System.currentTimeMillis() + "_" + safeName);

            storageRef.putFile(fileUri).addOnSuccessListener(taskSnapshot -> {
                storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    saveDocumentMetadata(safeName, mimeType != null ? mimeType : "application/pdf", uri.toString());
                });
            }).addOnFailureListener(e -> {
                Toast.makeText(PatientChatActivity.this, "Upload Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error processing file.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveDocumentMetadata(String fileName, String fileType, String fileUrl) {
        String msgId = chatsRef.push().getKey();
        long timestamp = System.currentTimeMillis();
        
        String msgText = "Sent an attachment.";
        
        ChatMessage chatMessage = new ChatMessage(msgId, patientId, "Patient", msgText, timestamp, fileUrl, fileName, fileType);
        
        if (msgId != null) {
            chatsRef.child(msgId).setValue(chatMessage).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(PatientChatActivity.this, "File sent in chat", Toast.LENGTH_SHORT).show();
                    // Optional: scroll down logic handled by value listener automatically
                } else {
                    Toast.makeText(PatientChatActivity.this, "Failed to send file", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) result = cursor.getString(index);
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}
