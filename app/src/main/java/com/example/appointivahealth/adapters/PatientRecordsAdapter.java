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
import com.example.appointivahealth.activities.PatientRecordDetailsActivity;
import com.example.appointivahealth.models.User;

import java.util.List;

public class PatientRecordsAdapter extends RecyclerView.Adapter<PatientRecordsAdapter.ViewHolder> {

    private Context context;
    private List<User> patientList;

    public PatientRecordsAdapter(Context context, List<User> patientList) {
        this.context = context;
        this.patientList = patientList;
    }

    public void updateList(List<User> newList) {
        this.patientList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_patient, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User patient = patientList.get(position);
        holder.tvPatientName.setText(patient.getName());
        holder.tvPatientEmail.setText(patient.getEmail());
        
        holder.itemView.setOnClickListener(v -> {
            String currentDoctorId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
            com.google.firebase.database.DatabaseReference recordsRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("MedicalRecords");
            recordsRef.orderByChild("patientId").equalTo(patient.getId()).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        com.example.appointivahealth.models.MedicalRecord latestRecord = null;
                        for (com.google.firebase.database.DataSnapshot ds : snapshot.getChildren()) {
                            com.example.appointivahealth.models.MedicalRecord record = ds.getValue(com.example.appointivahealth.models.MedicalRecord.class);
                            if (record != null && currentDoctorId.equals(record.getDoctorId())) {
                                latestRecord = record;
                            }
                        }
                        if (latestRecord != null) {
                            Intent intent = new Intent(context, com.example.appointivahealth.activities.DoctorPatientDetailsActivity.class);
                            intent.putExtra("recordId", latestRecord.getRecordId());
                            intent.putExtra("appointmentId", latestRecord.getAppointmentId());
                            intent.putExtra("patientId", latestRecord.getPatientId());
                            intent.putExtra("patientName", latestRecord.getPatientName());
                            intent.putExtra("symptoms", latestRecord.getSymptoms());
                            intent.putExtra("date", latestRecord.getDate());
                            intent.putExtra("prescription", latestRecord.getPrescription());
                            context.startActivity(intent);
                        } else {
                            android.widget.Toast.makeText(context, "No medical records found under your care.", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        android.widget.Toast.makeText(context, "No medical records found for this patient.", android.widget.Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {}
            });
        });

        holder.btnViewPatient.setOnClickListener(v -> holder.itemView.performClick());
    }

    @Override
    public int getItemCount() {
        return patientList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPatientName, tvPatientEmail;
        com.google.android.material.button.MaterialButton btnViewPatient;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPatientName = itemView.findViewById(R.id.tvPatientNameLabel);
            tvPatientEmail = itemView.findViewById(R.id.tvPatientPhoneEmail);
            btnViewPatient = itemView.findViewById(R.id.btnViewPatient);
        }
    }
}
