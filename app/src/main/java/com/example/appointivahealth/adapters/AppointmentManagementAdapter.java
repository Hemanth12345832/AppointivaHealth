package com.example.appointivahealth.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appointivahealth.R;
import com.example.appointivahealth.models.Appointment;
import com.example.appointivahealth.network.ApiClient;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class AppointmentManagementAdapter extends RecyclerView.Adapter<AppointmentManagementAdapter.ViewHolder> {

    private Context context;
    private List<Appointment> appointmentList;

    public AppointmentManagementAdapter(Context context, List<Appointment> appointmentList) {
        this.context = context;
        this.appointmentList = appointmentList;
    }

    public void updateList(List<Appointment> filteredList) {
        this.appointmentList = filteredList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_appointment_management, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Appointment appointment = appointmentList.get(position);

        holder.tvPatientName.setText(appointment.getPatientName());
        holder.tvDateTime.setText(appointment.getDate() + " | " + appointment.getTime());
        String displayStatus = appointment.getStatus();
        if ("payment_pending_verification".equals(displayStatus)) {
            displayStatus = "Payment pending for " + appointment.getPatientName();
        } else if ("confirmed".equals(displayStatus)) {
            displayStatus = "Payment verified. Appointment confirmed";
        } else if ("payment_failed".equals(displayStatus)) {
            displayStatus = "Payment failed";
        }
        holder.tvStatus.setText(displayStatus);

        // Reset visibility of action buttons
        holder.btnApprove.setVisibility(View.GONE);
        holder.btnSchedule.setVisibility(View.GONE);
        holder.btnComplete.setVisibility(View.GONE);
        holder.btnCancel.setVisibility(View.GONE);

        // Update status pill color
        switch (appointment.getStatus()) {
            case "Pending":
            case "confirmed":
                holder.tvStatus.setBackgroundColor(Color.parseColor("#FFA500")); // Orange
                holder.btnApprove.setVisibility(View.VISIBLE);
                holder.btnCancel.setVisibility(View.VISIBLE);
                break;
            case "Accepted":
                holder.tvStatus.setBackgroundColor(Color.parseColor("#3498DB")); // Blue
                holder.btnSchedule.setVisibility(View.VISIBLE);
                holder.btnCancel.setVisibility(View.VISIBLE);
                break;
            case "Scheduled":
                holder.tvStatus.setBackgroundColor(Color.parseColor("#2980B9")); // Darker Blue
                holder.btnComplete.setVisibility(View.VISIBLE);
                holder.btnCancel.setVisibility(View.VISIBLE);
                break;
            case "Completed":
                holder.tvStatus.setBackgroundColor(Color.parseColor("#2ECC71")); // Green
                break;
            case "Rejected":
            case "Cancelled":
            case "payment_failed":
                holder.tvStatus.setBackgroundColor(Color.parseColor("#E74C3C")); // Red
                break;
            case "payment_pending_verification":
                holder.tvStatus.setBackgroundColor(Color.parseColor("#03A9F4")); // Blueish
                break;
            default:
                holder.tvStatus.setBackgroundColor(Color.GRAY);
        }

        // Action Listeners
        holder.btnApprove.setOnClickListener(v -> updateStatus(appointment, "Accepted"));
        holder.btnSchedule.setOnClickListener(v -> updateStatus(appointment, "Scheduled"));
        holder.btnComplete.setOnClickListener(v -> updateStatus(appointment, "Completed"));
        holder.btnCancel.setOnClickListener(v -> {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
            builder.setTitle("Cancel Appointment");
            builder.setMessage("Please provide a reason or message for the patient regarding this cancellation:");

            final android.widget.EditText input = new android.widget.EditText(context);
            input.setHint("e.g., I have an emergency surgery...");
            
            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(50, 20, 50, 20);
            layout.addView(input);
            builder.setView(layout);

            builder.setPositiveButton("Send & Cancel", (dialog, which) -> {
                String doctorReason = input.getText().toString().trim();
                
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Appointments").child(appointment.getId());
                ref.child("status").setValue("Cancelled");
                // Also save the cancellation message
                ref.child("cancellationMessage").setValue(doctorReason).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        appointment.setStatus("Cancelled");
                        appointment.setCancellationMessage(doctorReason);
                        notifyDataSetChanged();
                        Toast.makeText(context, "Appointment Cancelled", Toast.LENGTH_SHORT).show();
                        
                        sendCustomCancellationMessage(appointment, doctorReason);

                        notifyAppointmentCancelled(appointment.getId(), doctorReason);
                    } else {
                        Toast.makeText(context, "Failed to cancel", Toast.LENGTH_SHORT).show();
                    }
                });
            });
            builder.setNegativeButton("Go Back", (dialog, which) -> dialog.cancel());
            builder.show();
        });

        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(context, com.example.appointivahealth.activities.DoctorAppointmentDetailsActivity.class);
            intent.putExtra("appointmentId", appointment.getId());
            context.startActivity(intent);
        });
    }

    private void updateStatus(Appointment appointment, String newStatus) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Appointments").child(appointment.getId());
        ref.child("status").setValue(newStatus).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                appointment.setStatus(newStatus);
                notifyDataSetChanged();
                Toast.makeText(context, "Appointment " + newStatus, Toast.LENGTH_SHORT).show();
                // We could trigger FCM here to the user
            } else {
                Toast.makeText(context, "Failed to update status", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendCustomCancellationMessage(Appointment appointment, String doctorReason) {
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("Chats").child(appointment.getId());
        String msgId = chatsRef.push().getKey();
        if (msgId != null) {
            String baseMsg = "Your appointment on " + appointment.getDate() + " at " + appointment.getTime() + " has been cancelled.";
            if (!doctorReason.isEmpty()) {
                baseMsg += "\n\nDoctor's Message: " + doctorReason;
            }
            if ("verified".equals(appointment.getPaymentStatus()) || "confirmed".equals(appointment.getStatus())) {
                baseMsg += "\n\nSince your original payment was verified, you can re-book another appointment with me without paying again. The verified payment will be automatically applied when you select a new slot.";
            }

            com.example.appointivahealth.models.ChatMessage chatMessage = new com.example.appointivahealth.models.ChatMessage(
                msgId, appointment.getDoctorId(), "Doctor", baseMsg, System.currentTimeMillis()
            );
            chatsRef.child(msgId).setValue(chatMessage);
        }
    }

    private void notifyAppointmentCancelled(String appointmentId, String reason) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("appointmentId", appointmentId);
            payload.put("reason", reason);
            payload.put("cancelledByRole", "Doctor");

            ApiClient.notifyAppointmentCancelled(context, payload, new ApiClient.JsonCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    // no-op: UX should not depend on notification delivery
                }

                @Override
                public void onError(String error) {
                    // no-op
                }
            });
        } catch (JSONException ignored) {
        }
    }

    @Override
    public int getItemCount() {
        return appointmentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPatientName, tvDateTime, tvStatus;
        Button btnApprove, btnSchedule, btnComplete, btnCancel;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPatientName = itemView.findViewById(R.id.tvPatientNameMgt);
            tvDateTime = itemView.findViewById(R.id.tvDateTimeMgt);
            tvStatus = itemView.findViewById(R.id.tvStatusMgt);

            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnSchedule = itemView.findViewById(R.id.btnSchedule);
            btnComplete = itemView.findViewById(R.id.btnComplete);
            btnCancel = itemView.findViewById(R.id.btnCancel);
        }
    }
}
