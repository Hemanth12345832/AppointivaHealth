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
import com.example.appointivahealth.activities.AdminPatientDetailsActivity;
import com.example.appointivahealth.models.User;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.PatientViewHolder> {

    private Context context;
    private List<User> patientList;

    public PatientAdapter(Context context, List<User> patientList) {
        this.context = context;
        this.patientList = patientList;
    }

    @NonNull
    @Override
    public PatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_patient, parent, false);
        return new PatientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PatientViewHolder holder, int position) {
        User patient = patientList.get(position);
        holder.tvName.setText(patient.getName());
        holder.tvPhoneEmail.setText((patient.getPhone() != null ? patient.getPhone() : "N/A") + " | " + (patient.getEmail() != null ? patient.getEmail() : "N/A"));

        holder.btnViewPatient.setOnClickListener(v -> {
            Intent intent = new Intent(context, AdminPatientDetailsActivity.class);
            intent.putExtra("patient", patient);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return patientList.size();
    }

    public void filterList(List<User> filteredList) {
        patientList = filteredList;
        notifyDataSetChanged();
    }

    public static class PatientViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhoneEmail;
        MaterialButton btnViewPatient;

        public PatientViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvPatientNameLabel);
            tvPhoneEmail = itemView.findViewById(R.id.tvPatientPhoneEmail);
            btnViewPatient = itemView.findViewById(R.id.btnViewPatient);
        }
    }
}
