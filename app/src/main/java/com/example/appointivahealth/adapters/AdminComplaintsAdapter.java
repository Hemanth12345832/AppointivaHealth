package com.example.appointivahealth.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appointivahealth.R;
import com.example.appointivahealth.models.Complaint;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class AdminComplaintsAdapter extends RecyclerView.Adapter<AdminComplaintsAdapter.ViewHolder> {

    private Context context;
    private List<Complaint> complaintList;

    public AdminComplaintsAdapter(Context context, List<Complaint> complaintList) {
        this.context = context;
        this.complaintList = complaintList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_complaint, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Complaint complaint = complaintList.get(position);

        holder.tvTitle.setText(complaint.getTitle() != null ? complaint.getTitle() : "Complaint");
        holder.tvDate.setText("Date: " + complaint.getDate());
        
        String patientName = complaint.getPatientName() != null ? complaint.getPatientName() : "Unknown";
        String doctorName = complaint.getDoctorName() != null ? complaint.getDoctorName() : "Unknown";
        holder.tvPatientDoctor.setText("Patient: " + patientName + " | Dr. " + doctorName);
        
        holder.tvDesc.setText(complaint.getDescription());
        holder.tvStatus.setText(complaint.getStatus());

        if ("Resolved".equalsIgnoreCase(complaint.getStatus())) {
            holder.tvStatus.setBackgroundColor(Color.parseColor("#2ECC71")); // Green
            holder.btnResolve.setVisibility(View.GONE);
        } else {
            holder.tvStatus.setBackgroundColor(Color.parseColor("#E74C3C")); // Red for Pending/Action Required
            holder.btnResolve.setVisibility(View.VISIBLE);
        }

        holder.btnResolve.setOnClickListener(v -> resolveComplaint(complaint));
        holder.btnDelete.setOnClickListener(v -> deleteComplaint(complaint));
    }

    private void resolveComplaint(Complaint complaint) {
        if (complaint.getComplaintId() != null) {
            FirebaseDatabase.getInstance().getReference("Complaints")
                    .child(complaint.getComplaintId())
                    .child("status")
                    .setValue("Resolved")
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(context, "Marked as Resolved", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Failed to resolve", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void deleteComplaint(Complaint complaint) {
        if (complaint.getComplaintId() != null) {
            FirebaseDatabase.getInstance().getReference("Complaints")
                    .child(complaint.getComplaintId())
                    .removeValue()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(context, "Complaint deleted", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    @Override
    public int getItemCount() {
        return complaintList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvPatientDoctor, tvDesc, tvStatus;
        Button btnResolve, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvComplaintTitle);
            tvDate = itemView.findViewById(R.id.tvComplaintDate);
            tvPatientDoctor = itemView.findViewById(R.id.tvComplaintPatientDoctor);
            tvDesc = itemView.findViewById(R.id.tvComplaintDesc);
            tvStatus = itemView.findViewById(R.id.tvComplaintStatus);
            btnResolve = itemView.findViewById(R.id.btnResolveComplaint);
            btnDelete = itemView.findViewById(R.id.btnDeleteComplaint);
        }
    }
}
