package com.example.appointivahealth.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appointivahealth.R;
import com.example.appointivahealth.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class DoctorVerificationAdapter extends RecyclerView.Adapter<DoctorVerificationAdapter.VerifyViewHolder> {

    private Context context;
    private List<User> pendingDoctors;
    private DatabaseReference usersRef;

    public DoctorVerificationAdapter(Context context, List<User> pendingDoctors) {
        this.context = context;
        this.pendingDoctors = pendingDoctors;
        this.usersRef = FirebaseDatabase.getInstance().getReference("Users");
    }

    @NonNull
    @Override
    public VerifyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_verify_doctor, parent, false);
        return new VerifyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VerifyViewHolder holder, int position) {
        User doctor = pendingDoctors.get(position);
        
        holder.tvName.setText("Dr. " + doctor.getName());
        holder.tvSpec.setText(doctor.getSpecialization());
        holder.tvPhone.setText(doctor.getPhone());
        
        holder.tvExperience.setText("Experience: " + doctor.getExperience() + " years");
        holder.tvHospital.setText("Hospital: " + doctor.getHospitalName());
        holder.tvLicense.setText("License Number: " + doctor.getLicenseNumber());

        if (doctor.getProfileImageUrl() != null && !doctor.getProfileImageUrl().isEmpty()) {
            if (doctor.getProfileImageUrl().startsWith("http")) {
                Glide.with(context)
                     .load(doctor.getProfileImageUrl())
                     .placeholder(R.mipmap.ic_launcher_round)
                     .into(holder.ivPhoto);
            } else {
                try {
                    byte[] imageByteArray = android.util.Base64.decode(doctor.getProfileImageUrl(), android.util.Base64.DEFAULT);
                    Glide.with(context)
                         .asBitmap()
                         .load(imageByteArray)
                         .placeholder(R.mipmap.ic_launcher_round)
                         .into(holder.ivPhoto);
                } catch (IllegalArgumentException e) {
                    holder.ivPhoto.setImageResource(R.mipmap.ic_launcher_round);
                }
            }
        } else {
            holder.ivPhoto.setImageResource(R.mipmap.ic_launcher_round);
        }

        holder.btnVerifyAction.setOnClickListener(v -> {
            usersRef.child(doctor.getId()).child("isVerified").setValue(true).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(context, "Doctor " + doctor.getName() + " Verified successfully!", Toast.LENGTH_SHORT).show();
                    pendingDoctors.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, pendingDoctors.size());
                } else {
                    Toast.makeText(context, "Failed to verify doctor.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return pendingDoctors.size();
    }

    public static class VerifyViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSpec, tvPhone, tvExperience, tvHospital, tvLicense;
        ImageView ivPhoto;
        MaterialButton btnVerifyAction;

        public VerifyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvVerifyDoctorName);
            tvSpec = itemView.findViewById(R.id.tvVerifyDoctorSpec);
            tvPhone = itemView.findViewById(R.id.tvVerifyDoctorPhone);
            tvExperience = itemView.findViewById(R.id.tvVerifyDoctorExperience);
            tvHospital = itemView.findViewById(R.id.tvVerifyDoctorHospital);
            tvLicense = itemView.findViewById(R.id.tvVerifyDoctorLicense);
            ivPhoto = itemView.findViewById(R.id.ivVerifyDoctorPhoto);
            btnVerifyAction = itemView.findViewById(R.id.btnVerifyAction);
        }
    }
}
