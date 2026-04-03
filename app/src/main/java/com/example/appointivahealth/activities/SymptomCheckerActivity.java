package com.example.appointivahealth.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appointivahealth.R;
import com.example.appointivahealth.network.ApiClient;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class SymptomCheckerActivity extends AppCompatActivity {

    private LinearLayout llChatContainer;
    private EditText etSymptoms;
    private MaterialButton btnCheckSymptoms;
    private ImageButton btnVoice;
    private DatabaseReference chatRef;
    private static final int VOICE_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptom_checker);

        llChatContainer = findViewById(R.id.llChatContainer);
        etSymptoms = findViewById(R.id.etSymptoms);
        btnCheckSymptoms = findViewById(R.id.btnCheckSymptoms);
        btnVoice = findViewById(R.id.btnVoice);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            chatRef = FirebaseDatabase.getInstance().getReference("PatientSymptomChats").child(currentUser.getUid());
            loadChatHistory();
        } else {
            addBotMessage("Describe your symptoms and I’ll suggest the right doctor type.", false);
        }

        btnVoice.setOnClickListener(v -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your symptoms...");
            try {
                startActivityForResult(intent, VOICE_REQUEST_CODE);
            } catch (Exception e) {
                Toast.makeText(this, "Voice input not supported on this device", Toast.LENGTH_SHORT).show();
            }
        });

        btnCheckSymptoms.setOnClickListener(v -> {
            String symptoms = etSymptoms.getText().toString().trim();
            if (TextUtils.isEmpty(symptoms)) {
                Toast.makeText(this, "Please enter symptoms.", Toast.LENGTH_SHORT).show();
                return;
            }

            addUserMessage(symptoms, true);
            etSymptoms.setText("");
            btnCheckSymptoms.setEnabled(false);
            btnCheckSymptoms.setText("Checking...");

            try {
                JSONObject payload = new JSONObject();
                payload.put("symptoms", symptoms);

                ApiClient.symptomCheck(this, payload, new ApiClient.JsonCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        btnCheckSymptoms.setEnabled(true);
                        btnCheckSymptoms.setText("Check");
                        addBotMessage(formatRecommendations(response), true);
                    }

                    @Override
                    public void onError(String error) {
                        btnCheckSymptoms.setEnabled(true);
                        btnCheckSymptoms.setText("Check");
                        addBotMessage("Sorry, I couldn't process that right now. Please try again.", true);
                        Toast.makeText(SymptomCheckerActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (JSONException e) {
                btnCheckSymptoms.setEnabled(true);
                btnCheckSymptoms.setText("Check");
                addBotMessage("Sorry, something went wrong. Please try again.", true);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                etSymptoms.setText(result.get(0));
            }
        }
    }

    private void loadChatHistory() {
        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                llChatContainer.removeAllViews();
                
                if (!snapshot.exists()) {
                    addBotMessage("Describe your symptoms and I’ll suggest the right doctor type.", true);
                    return;
                }

                for (DataSnapshot msgSnap : snapshot.getChildren()) {
                    Boolean isUserObj = msgSnap.child("isUser").getValue(Boolean.class);
                    String text = msgSnap.child("text").getValue(String.class);
                    
                    boolean isUser = isUserObj != null ? isUserObj : false;
                    if (text != null && !text.isEmpty()) {
                        if (isUser) {
                            addUserMessage(text, false);
                        } else {
                            addBotMessage(text, false);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                addBotMessage("Describe your symptoms and I’ll suggest the right doctor type.", false);
            }
        });
    }

    private void addUserMessage(String text, boolean saveToDb) {
        TextView tv = createBubble(text, true);
        llChatContainer.addView(tv);
        if (saveToDb && chatRef != null) {
            String id = chatRef.push().getKey();
            if (id != null) {
                chatRef.child(id).child("text").setValue(text);
                chatRef.child(id).child("isUser").setValue(true);
                chatRef.child(id).child("timestamp").setValue(System.currentTimeMillis());
            }
        }
    }

    private void addBotMessage(String text, boolean saveToDb) {
        TextView tv = createBubble(text, false);
        llChatContainer.addView(tv);
        if (saveToDb && chatRef != null) {
            String id = chatRef.push().getKey();
            if (id != null) {
                chatRef.child(id).child("text").setValue(text);
                chatRef.child(id).child("isUser").setValue(false);
                chatRef.child(id).child("timestamp").setValue(System.currentTimeMillis());
            }
        }
    }

    private TextView createBubble(String text, boolean fromUser) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(16f);
        tv.setTextColor(fromUser ? Color.WHITE : Color.parseColor("#212121"));
        tv.setPadding(24, 16, 24, 16);

        int bg = fromUser ? Color.parseColor("#0052CC") : Color.parseColor("#EDEDED");
        tv.setBackgroundColor(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.gravity = fromUser ? Gravity.END : Gravity.START;
        lp.setMargins(0, 12, 0, 0);
        tv.setLayoutParams(lp);
        return tv;
    }

    private String formatRecommendations(JSONObject response) {
        if (response.has("message")) {
            return response.optString("message", "No response from AI.");
        }
        
        JSONArray recs = response.optJSONArray("recommendations");
        if (recs == null || recs.length() == 0) {
            String msg = response.optString("message", "No recommendation found.");
            return msg;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Recommended doctor type:");

        int count = Math.min(2, recs.length());
        for (int i = 0; i < count; i++) {
            JSONObject r = recs.optJSONObject(i);
            if (r == null) continue;
            String doctorType = r.optString("doctorType", "General Physician");
            double confidence = r.has("confidence") ? r.optDouble("confidence", -1) : -1;

            sb.append("\n").append(i + 1).append(". ").append(doctorType);
            if (confidence >= 0) {
                sb.append(" (").append((int) Math.round(confidence * 100)).append("%)");
            }
        }

        String tail = response.optString("note", "");
        if (!TextUtils.isEmpty(tail)) sb.append("\n\n").append(tail);
        return sb.toString();
    }
}

