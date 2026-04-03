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
import com.example.appointivahealth.adapters.MedicalDocumentAdapter;
import com.example.appointivahealth.models.MedicalDocument;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class DocumentListActivity extends AppCompatActivity {

    private String appointmentId;
    private RecyclerView rvDocumentList;
    private TextView tvNoDocuments;
    
    private MedicalDocumentAdapter adapter;
    private List<MedicalDocument> documentList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_list);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarDocumentList);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        appointmentId = getIntent().getStringExtra("appointmentId");

        rvDocumentList = findViewById(R.id.rvDocumentList);
        tvNoDocuments = findViewById(R.id.tvNoDocuments);
        
        rvDocumentList.setLayoutManager(new LinearLayoutManager(this));
        documentList = new ArrayList<>();
        adapter = new MedicalDocumentAdapter(this, documentList);
        rvDocumentList.setAdapter(adapter);

        if (appointmentId != null && !appointmentId.isEmpty()) {
            loadDocuments();
        } else {
            Toast.makeText(this, "Error: Invalid Appointment context", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadDocuments() {
        DatabaseReference docRef = FirebaseDatabase.getInstance().getReference("MedicalDocuments").child(appointmentId);
        docRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                documentList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        MedicalDocument doc = ds.getValue(MedicalDocument.class);
                        if (doc != null) {
                            documentList.add(doc);
                        }
                    }
                }
                
                adapter.notifyDataSetChanged();
                
                if (documentList.isEmpty()) {
                    tvNoDocuments.setVisibility(View.VISIBLE);
                    rvDocumentList.setVisibility(View.GONE);
                } else {
                    tvNoDocuments.setVisibility(View.GONE);
                    rvDocumentList.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DocumentListActivity.this, "Failed to load documents", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
