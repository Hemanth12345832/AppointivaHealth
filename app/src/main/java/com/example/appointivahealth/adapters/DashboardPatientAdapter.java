package com.example.appointivahealth.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appointivahealth.R;
import com.example.appointivahealth.models.MedicalRecord;

import java.util.List;

public class DashboardPatientAdapter extends RecyclerView.Adapter<DashboardPatientAdapter.ViewHolder> {

    private Context context;
    private List<MedicalRecord> recordList;

    public DashboardPatientAdapter(Context context, List<MedicalRecord> recordList) {
        this.context = context;
        this.recordList = recordList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_dashboard_patient, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MedicalRecord record = recordList.get(position);

        String name = record.getPatientName();
        holder.tvDashPatientName.setText(name != null && !name.trim().isEmpty() ? name : "Unknown Patient");
        if (name == null || name.trim().isEmpty() || name.equalsIgnoreCase("Unknown") || name.equalsIgnoreCase("Unknown Patient")) {
            if (record.getPatientId() != null) {
                com.google.firebase.database.FirebaseDatabase.getInstance().getReference("Users")
                    .child(record.getPatientId()).child("name")
                    .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String realName = snapshot.getValue(String.class);
                                holder.tvDashPatientName.setText(realName != null ? realName : "Unknown Patient");
                                record.setPatientName(realName);
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
                    });
            }
        }

        holder.tvDashPatientDate.setText(record.getDate() != null ? record.getDate() : "");

        String rawSymptoms = record.getSymptoms();
        String extractedSymptom = "Not specified";
        if (rawSymptoms != null && !rawSymptoms.trim().isEmpty()) {
            String lower = rawSymptoms.toLowerCase();
            if (lower.contains("stomach pain") || lower.contains("stomach ache")) extractedSymptom = "Stomach pain";
            else if (lower.contains("headache") || lower.contains("head ache")) extractedSymptom = "Headache";
            else if (lower.contains("fever")) extractedSymptom = "Fever";
            else if (lower.contains("chest pain")) extractedSymptom = "Chest pain";
            else extractedSymptom = rawSymptoms; 
        }
        holder.tvDashPatientSymptoms.setText("Symptoms/Reason: " + extractedSymptom);

        String prescription = record.getPrescription();
        if (prescription == null || prescription.trim().isEmpty()) {
            prescription = "No prescription yet";
        }
        holder.tvDashPatientPrescription.setText("Prescription: " + prescription);

        // Fetch appointment status dynamically
        if (record.getAppointmentId() != null) {
            com.google.firebase.database.FirebaseDatabase.getInstance().getReference("Appointments")
                .child(record.getAppointmentId())
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String status = snapshot.child("status").getValue(String.class);
                            if ("payment_pending_verification".equals(status)) {
                                holder.tvDashPatientSymptoms.setText("Patient " + record.getPatientName() + "'s payment is yet to be verified by admin");
                                holder.tvDashPatientSymptoms.setTextColor(android.graphics.Color.parseColor("#E65100")); 
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
                });
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, com.example.appointivahealth.activities.DoctorPatientDetailsActivity.class);
            intent.putExtra("recordId", record.getRecordId());
            intent.putExtra("appointmentId", record.getAppointmentId());
            intent.putExtra("patientId", record.getPatientId());
            intent.putExtra("patientName", record.getPatientName());
            intent.putExtra("symptoms", record.getSymptoms());
            intent.putExtra("date", record.getDate());
            intent.putExtra("prescription", record.getPrescription());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return Math.min(recordList.size(), 10); // Display top 10 recent
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDashPatientName, tvDashPatientDate, tvDashPatientSymptoms, tvDashPatientPrescription;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDashPatientName = itemView.findViewById(R.id.tvDashPatientName);
            tvDashPatientDate = itemView.findViewById(R.id.tvDashPatientDate);
            tvDashPatientSymptoms = itemView.findViewById(R.id.tvDashPatientSymptoms);
            tvDashPatientPrescription = itemView.findViewById(R.id.tvDashPatientPrescription);
        }
    }
}
