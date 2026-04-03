package com.example.appointivahealth.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appointivahealth.R;
import com.example.appointivahealth.models.Appointment;
import com.example.appointivahealth.models.MedicalRecord;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class PaymentVerificationAdapter extends RecyclerView.Adapter<PaymentVerificationAdapter.ViewHolder> {

    private Context context;
    private List<Appointment> appointmentList;

    public PaymentVerificationAdapter(Context context, List<Appointment> appointmentList) {
        this.context = context;
        this.appointmentList = appointmentList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_payment_verification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Appointment appointment = appointmentList.get(position);

        holder.tvPaymentPatientName.setText("Patient: " + appointment.getPatientName());
        holder.tvPaymentDoctorName.setText("Doctor: Dr. " + appointment.getDoctorName());
        holder.tvPaymentDate.setText("Date: " + appointment.getDate() + " | " + appointment.getTime());
        holder.tvPaymentTxn.setText("TXN ID: " + appointment.getTransactionId());
        holder.tvPaymentAmount.setText("Amount: ₹" + (appointment.getAmount() != null && !appointment.getAmount().isEmpty() ? appointment.getAmount() : "300"));

        holder.btnVerifyPayment.setOnClickListener(v -> {
            updatePaymentStatus(appointment.getId(), "verified", true, "confirmed");
        });

        holder.btnRejectPayment.setOnClickListener(v -> {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
            builder.setTitle("Reject Payment");
            builder.setMessage("Please provide a reason for rejection:");

            final android.widget.EditText input = new android.widget.EditText(context);
            builder.setView(input);

            builder.setPositiveButton("Submit", (dialog, which) -> {
                String reason = input.getText().toString().trim();
                DatabaseReference apptRef = FirebaseDatabase.getInstance().getReference("Appointments").child(appointment.getId());
                apptRef.child("paymentStatus").setValue("rejected");
                apptRef.child("adminVerified").setValue(false);
                apptRef.child("message").setValue(reason);
                apptRef.child("status").setValue("payment_failed")
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DatabaseReference recRef = FirebaseDatabase.getInstance().getReference("MedicalRecords");
                            recRef.orderByChild("appointmentId").equalTo(appointment.getId())
                                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snap) {
                                        for(com.google.firebase.database.DataSnapshot ds : snap.getChildren()) {
                                            ds.getRef().removeValue();
                                        }
                                        Toast.makeText(context, "Payment rejected & record removed", Toast.LENGTH_SHORT).show();
                                    }
                                    @Override
                                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
                                });
                        } else {
                            Toast.makeText(context, "Failed to update payment status", Toast.LENGTH_SHORT).show();
                        }
                    });
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
        });
    }

    private void updatePaymentStatus(String appointmentId, String paymentStatus, boolean adminVerified, String status) {
        DatabaseReference apptRef = FirebaseDatabase.getInstance().getReference("Appointments").child(appointmentId);
        apptRef.child("paymentStatus").setValue(paymentStatus);
        apptRef.child("adminVerified").setValue(adminVerified);
        apptRef.child("status").setValue(status)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(context, "Payment " + paymentStatus, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Failed to update payment status", Toast.LENGTH_SHORT).show();
                }
            });
    }

    @Override
    public int getItemCount() {
        return appointmentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPaymentPatientName, tvPaymentDoctorName, tvPaymentDate, tvPaymentTxn, tvPaymentAmount;
        MaterialButton btnVerifyPayment, btnRejectPayment;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPaymentPatientName = itemView.findViewById(R.id.tvPaymentPatientName);
            tvPaymentDoctorName = itemView.findViewById(R.id.tvPaymentDoctorName);
            tvPaymentDate = itemView.findViewById(R.id.tvPaymentDate);
            tvPaymentTxn = itemView.findViewById(R.id.tvPaymentTxn);
            tvPaymentAmount = itemView.findViewById(R.id.tvPaymentAmount);
            btnVerifyPayment = itemView.findViewById(R.id.btnVerifyPayment);
            btnRejectPayment = itemView.findViewById(R.id.btnRejectPayment);
        }
    }
}
