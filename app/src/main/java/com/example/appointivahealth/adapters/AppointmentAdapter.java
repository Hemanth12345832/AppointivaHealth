package com.example.appointivahealth.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appointivahealth.R;
import com.example.appointivahealth.models.Appointment;
import com.example.appointivahealth.network.ApiClient;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {

    private Context context;
    private List<Appointment> appointmentList;
    private String userRole;

    public AppointmentAdapter(Context context, List<Appointment> appointmentList, String userRole) {
        this.context = context;
        this.appointmentList = appointmentList;
        this.userRole = userRole;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_appointment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Appointment appointment = appointmentList.get(position);

        if ("Patient".equals(userRole)) {
            holder.tvAppmntDoctorPatient.setText("Dr. " + appointment.getDoctorName());
        } else if ("Doctor".equals(userRole)) {
            holder.tvAppmntDoctorPatient.setText("Patient: " + appointment.getPatientName());
        } else {
            holder.tvAppmntDoctorPatient.setText("Dr. " + appointment.getDoctorName() + " - Pt. " + appointment.getPatientName());
        }

        holder.tvAppmntDateTime.setText(appointment.getDate() + " | " + appointment.getTime());
        String displayStatus = appointment.getStatus();
        if ("payment_pending_verification".equals(displayStatus)) {
            displayStatus = "Payment pending. Complete payment to confirm.";
        } else if ("confirmed".equals(displayStatus)) {
            displayStatus = "Confirmed";
        } else if ("payment_failed".equals(displayStatus)) {
            displayStatus = "Payment failed. Please try again.";
        }
        holder.tvAppmntStatus.setText(displayStatus);

        switch (appointment.getStatus()) {
            case "Accepted":
            case "Completed":
            case "confirmed":
                holder.tvAppmntStatus.setTextColor(Color.parseColor("#4CAF50")); // success
                break;
            case "Rejected":
            case "Cancelled":
            case "payment_failed":
                holder.tvAppmntStatus.setTextColor(Color.parseColor("#F44336")); // error
                break;
            case "payment_pending_verification":
                holder.tvAppmntStatus.setTextColor(Color.parseColor("#03A9F4")); // blueish
                break;
            default:
                holder.tvAppmntStatus.setTextColor(Color.parseColor("#FF9800")); // warning (orangeish)
                break;
        }

        if (appointment.getPrescription() != null && !appointment.getPrescription().isEmpty()) {
            holder.tvAppmntPrescription.setVisibility(View.VISIBLE);
            holder.tvAppmntPrescription.setText("Prescription: " + appointment.getPrescription());
        } else {
            holder.tvAppmntPrescription.setVisibility(View.GONE);
        }

        if (appointment.getMessage() != null && !appointment.getMessage().isEmpty()) {
            holder.tvAppmntMessage.setVisibility(View.VISIBLE);
            holder.tvAppmntMessage.setText("Message: " + appointment.getMessage());
        } else {
            holder.tvAppmntMessage.setVisibility(View.GONE);
        }

        holder.layoutActionButtons.setVisibility(View.GONE);
        holder.btnAcceptAppmnt.setVisibility(View.GONE);
        holder.btnRejectAppmnt.setVisibility(View.GONE);
        holder.btnCancelAppmnt.setVisibility(View.GONE);
        holder.btnAddPrescription.setVisibility(View.GONE);
        holder.btnPayAgain.setVisibility(View.GONE);
        holder.btnSubmitComplaint.setVisibility(View.GONE);

        if ("Pending".equals(appointment.getStatus()) || "confirmed".equals(appointment.getStatus())) {
            if ("Doctor".equals(userRole)) {
                holder.layoutActionButtons.setVisibility(View.VISIBLE);
                holder.btnAcceptAppmnt.setVisibility(View.VISIBLE);
                holder.btnRejectAppmnt.setVisibility(View.VISIBLE);
            } else if ("Patient".equals(userRole)) {
                holder.layoutActionButtons.setVisibility(View.VISIBLE);
                holder.btnCancelAppmnt.setVisibility(View.VISIBLE);
            }
        } else if ("Accepted".equals(appointment.getStatus()) && "Doctor".equals(userRole)) {
            holder.layoutActionButtons.setVisibility(View.VISIBLE);
            holder.btnAddPrescription.setVisibility(View.VISIBLE);
        } else if ("payment_failed".equals(appointment.getStatus()) && "Patient".equals(userRole)) {
            holder.layoutActionButtons.setVisibility(View.VISIBLE);
            holder.btnPayAgain.setVisibility(View.VISIBLE);
        } else if ("Completed".equalsIgnoreCase(appointment.getStatus()) && "Patient".equals(userRole)) {
            holder.layoutActionButtons.setVisibility(View.VISIBLE);
            holder.btnSubmitComplaint.setVisibility(View.VISIBLE);
        }

        holder.btnAcceptAppmnt.setOnClickListener(v -> updateStatus(appointment.getId(), "Accepted"));
        holder.btnRejectAppmnt.setOnClickListener(v -> updateStatus(appointment.getId(), "Rejected"));
        holder.btnCancelAppmnt.setOnClickListener(v -> updateStatus(appointment.getId(), "Cancelled"));
        holder.btnAddPrescription.setOnClickListener(v -> showPrescriptionDialog(appointment.getId()));
        
        holder.btnPayAgain.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(context, com.example.appointivahealth.activities.PaymentActivity.class);
            intent.putExtra("doctorId", appointment.getDoctorId());
            intent.putExtra("doctorName", appointment.getDoctorName());
            intent.putExtra("dateStr", appointment.getDate());
            intent.putExtra("timeStr", appointment.getTime());
            intent.putExtra("patientId", appointment.getPatientId());
            intent.putExtra("patientName", appointment.getPatientName());
            intent.putExtra("fee", appointment.getAmount() != null && !appointment.getAmount().isEmpty() ? appointment.getAmount() : "300");
            intent.putExtra("appointmentIdToUpdate", appointment.getId());
            context.startActivity(intent);
        });

        holder.btnSubmitComplaint.setOnClickListener(v -> {
            showComplaintDialog(appointment);
        });

        holder.itemView.setOnClickListener(v -> {
            if ("Patient".equals(userRole)) {
                android.content.Intent intent = new android.content.Intent(context, com.example.appointivahealth.activities.PatientAppointmentDetailsActivity.class);
                intent.putExtra("appointmentId", appointment.getId());
                context.startActivity(intent);
            } else if ("Doctor".equals(userRole)) {
                if ("payment_pending_verification".equals(appointment.getStatus())) {
                    Toast.makeText(context, "Payment is pending. Please wait for confirmation.", Toast.LENGTH_SHORT).show();
                    return;
                }
                android.content.Intent intent = new android.content.Intent(context, com.example.appointivahealth.activities.DoctorAppointmentDetailsActivity.class);
                intent.putExtra("appointmentId", appointment.getId());
                context.startActivity(intent);
            }
        });
    }

    private void updateStatus(String id, String status) {
        showStatusMessageDialog(id, status);
    }

    private void showStatusMessageDialog(String appointmentId, String status) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(status.equals("Accepted") ? "Accept Appointment" : "Reject Appointment");
        builder.setMessage(status.equals("Accepted") ? "Optionally provide your availability (day, date, time) or message:" : "Please provide a reason for rejection:");

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        builder.setView(input);

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String message = input.getText().toString().trim();

            DatabaseReference apptRef = FirebaseDatabase.getInstance().getReference("Appointments").child(appointmentId);
            apptRef.child("status").setValue(status).addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Toast.makeText(context, "Failed to update status", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!message.isEmpty()) {
                    apptRef.child("message").setValue(message);
                }
                if ("Cancelled".equalsIgnoreCase(status)) {
                    // This is what PatientAppointmentDetailsActivity displays.
                    apptRef.child("cancellationMessage").setValue(message);
                }

                Toast.makeText(context, "Status Updated to " + status, Toast.LENGTH_SHORT).show();

                if ("Cancelled".equalsIgnoreCase(status)) {
                    notifyAppointmentCancelled(appointmentId, message);
                }
            });
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void notifyAppointmentCancelled(String appointmentId, String reason) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("appointmentId", appointmentId);
            payload.put("reason", reason);
            payload.put("cancelledByRole", userRole);

            ApiClient.notifyAppointmentCancelled(context, payload, new ApiClient.JsonCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    // Notification failures shouldn't block UX; server returns success/failure.
                }

                @Override
                public void onError(String error) {
                    // Ignore notification errors on the client.
                }
            });
        } catch (JSONException ignored) {
        }
    }

    private void showPrescriptionDialog(String appointmentId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Add Prescription");

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String prescription = input.getText().toString().trim();
            FirebaseDatabase.getInstance().getReference("Appointments").child(appointmentId)
                    .child("prescription").setValue(prescription)
                    .addOnSuccessListener(aVoid -> Toast.makeText(context, "Prescription saved", Toast.LENGTH_SHORT).show());
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showComplaintDialog(Appointment appointment) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Submit Feedback");
        builder.setMessage("Please provide your feedback regarding this appointment:");
        
        final EditText input = new EditText(context);
        input.setHint("Write your feedback here...");
        
        android.widget.FrameLayout container = new android.widget.FrameLayout(context);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 50; params.rightMargin = 50;
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Submit", (dialog, which) -> {
            String complaintText = input.getText().toString().trim();
            if(!complaintText.isEmpty()) {
                submitComplaintToFirebase(appointment, complaintText);
            } else {
                Toast.makeText(context, "Feedback cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void submitComplaintToFirebase(Appointment appointment, String text) {
        com.google.firebase.database.DatabaseReference complaintsRef = FirebaseDatabase.getInstance().getReference("Complaints");
        String complaintId = complaintsRef.push().getKey();
        String patientId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        com.example.appointivahealth.models.Complaint complaint = new com.example.appointivahealth.models.Complaint();
        complaint.setComplaintId(complaintId);
        complaint.setPatientId(patientId);
        complaint.setPatientName(appointment.getPatientName() != null ? appointment.getPatientName() : "Unknown");
        complaint.setDoctorId(appointment.getDoctorId());
        complaint.setDoctorName(appointment.getDoctorName());
        complaint.setAppointmentId(appointment.getId());
        complaint.setTitle("Feedback for Appointment on " + appointment.getDate());
        complaint.setDescription(text);
        complaint.setDate(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(new java.util.Date()));
        complaint.setStatus("Pending");

        if (complaintId != null) {
            complaintsRef.child(complaintId).setValue(complaint).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(context, "Feedback submitted successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Failed to submit feedback", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return appointmentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAppmntDoctorPatient, tvAppmntDateTime, tvAppmntStatus, tvAppmntPrescription, tvAppmntMessage;
        LinearLayout layoutActionButtons;
        MaterialButton btnAcceptAppmnt, btnRejectAppmnt, btnCancelAppmnt, btnAddPrescription, btnPayAgain, btnSubmitComplaint;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAppmntDoctorPatient = itemView.findViewById(R.id.tvAppmntDoctorPatient);
            tvAppmntDateTime = itemView.findViewById(R.id.tvAppmntDateTime);
            tvAppmntStatus = itemView.findViewById(R.id.tvAppmntStatus);
            tvAppmntPrescription = itemView.findViewById(R.id.tvAppmntPrescription);
            tvAppmntMessage = itemView.findViewById(R.id.tvAppmntMessage); // using same id as prescription in layout if not present, but needs its own
            layoutActionButtons = itemView.findViewById(R.id.layoutActionButtons);
            btnAcceptAppmnt = itemView.findViewById(R.id.btnAcceptAppmnt);
            btnRejectAppmnt = itemView.findViewById(R.id.btnRejectAppmnt);
            btnCancelAppmnt = itemView.findViewById(R.id.btnCancelAppmnt);
            btnAddPrescription = itemView.findViewById(R.id.btnAddPrescription);
            btnPayAgain = itemView.findViewById(R.id.btnPayAgain);
            btnSubmitComplaint = itemView.findViewById(R.id.btnSubmitComplaint);
        }
    }
}
