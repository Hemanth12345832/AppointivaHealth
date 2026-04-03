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
import com.example.appointivahealth.activities.DoctorChatActivity;
import com.example.appointivahealth.activities.PatientChatActivity;
import com.example.appointivahealth.models.Appointment;

import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {

    private Context context;
    private List<Appointment> chatList;
    private boolean isDoctor;

    public ChatListAdapter(Context context, List<Appointment> chatList, boolean isDoctor) {
        this.context = context;
        this.chatList = chatList;
        this.isDoctor = isDoctor;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Appointment appt = chatList.get(position);

        if (isDoctor) {
            holder.tvChatListName.setText(appt.getPatientName());
            holder.tvChatListStatusInfo.setText("Appt Date: " + appt.getDate() + " | " + appt.getStatus());
        } else {
            holder.tvChatListName.setText("Dr. " + appt.getDoctorName());
            holder.tvChatListStatusInfo.setText("Date: " + appt.getDate() + " | " + appt.getStatus());
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent;
            if (isDoctor) {
                intent = new Intent(context, DoctorChatActivity.class);
                intent.putExtra("patientId", appt.getPatientId());
                intent.putExtra("patientName", appt.getPatientName());
            } else {
                intent = new Intent(context, PatientChatActivity.class);
                intent.putExtra("doctorId", appt.getDoctorId());
                intent.putExtra("doctorName", appt.getDoctorName());
            }
            intent.putExtra("appointmentId", appt.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvChatListName, tvChatListStatusInfo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChatListName = itemView.findViewById(R.id.tvChatListName);
            tvChatListStatusInfo = itemView.findViewById(R.id.tvChatListStatusInfo);
        }
    }
}
