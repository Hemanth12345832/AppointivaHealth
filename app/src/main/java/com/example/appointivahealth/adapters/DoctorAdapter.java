package com.example.appointivahealth.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appointivahealth.R;
import com.example.appointivahealth.activities.DoctorProfileActivity;
import com.example.appointivahealth.models.User;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class DoctorAdapter extends RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder> {

    private Context context;
    private List<User> doctorList;
    private boolean isAdmin;

    public DoctorAdapter(Context context, List<User> doctorList, boolean isAdmin) {
        this.context = context;
        this.doctorList = doctorList;
        this.isAdmin = isAdmin;
    }

    public void filterList(List<User> filteredList) {
        doctorList = filteredList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DoctorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_doctor, parent, false);
        return new DoctorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DoctorViewHolder holder, int position) {
        User doctor = doctorList.get(position);
        String docName = doctor.getName() != null ? doctor.getName() : "Unknown";
        holder.tvName.setText("Dr. " + docName);
        
        String special = doctor.getSpecialization() != null ? doctor.getSpecialization() : "General";
        holder.tvSpecialization.setText(special);
        
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

        holder.btnViewProfile.setOnClickListener(v -> {
            Intent intent = new Intent(context, DoctorProfileActivity.class);
            intent.putExtra("doctor", doctor);
            context.startActivity(intent);
        });

        if (isAdmin) {
            holder.btnEditDoctor.setVisibility(View.VISIBLE);
            holder.btnEditDoctor.setOnClickListener(v -> {
                Intent intent = new Intent(context, com.example.appointivahealth.activities.EditDoctorActivity.class);
                intent.putExtra("doctor", doctor);
                context.startActivity(intent);
            });
        } else {
            holder.btnEditDoctor.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return doctorList.size();
    }

    public static class DoctorViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSpecialization;
        MaterialButton btnViewProfile, btnEditDoctor;
        ImageView ivPhoto;

        public DoctorViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvDoctorNameLabel);
            tvSpecialization = itemView.findViewById(R.id.tvDoctorSpecializationLabel);
            btnViewProfile = itemView.findViewById(R.id.btnViewProfile);
            btnEditDoctor = itemView.findViewById(R.id.btnEditDoctor);
            ivPhoto = itemView.findViewById(R.id.ivDoctorPhoto);
        }
    }
}
