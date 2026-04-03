package com.example.appointivahealth.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appointivahealth.R;
import com.example.appointivahealth.models.Appointment;
import com.example.appointivahealth.models.User;
import com.example.appointivahealth.network.ApiClient;
import org.json.JSONObject;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class BookAppointmentActivity extends AppCompatActivity {

    private TextView tvBookingDoctorName, tvDoctorAvailability;
    private EditText etBookingDate, etBookingTime;
    private MaterialButton btnConfirmBooking;
    private User doctor;
    private Calendar calendar;
    private HashMap<String, String> availabilityMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_appointment);

        tvBookingDoctorName = findViewById(R.id.tvBookingDoctorName);
        tvDoctorAvailability = findViewById(R.id.tvDoctorAvailability);
        etBookingDate = findViewById(R.id.etBookingDate);
        etBookingTime = findViewById(R.id.etBookingTime);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);

        doctor = (User) getIntent().getSerializableExtra("doctor");
        if (doctor != null) {
            tvBookingDoctorName.setText("Doctor: Dr. " + doctor.getName());
            fetchAvailability();
        }

        calendar = Calendar.getInstance();

        etBookingDate.setOnClickListener(v -> {
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dialog = new DatePickerDialog(this, (view, y, m, d) -> {
                etBookingDate.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d));
            }, year, month, day);
            dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            dialog.show();
        });

        etBookingTime.setOnClickListener(v -> {
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            new TimePickerDialog(this, (view, h, m) -> {
                etBookingTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m));
            }, hour, minute, true).show();
        });

        btnConfirmBooking.setOnClickListener(v -> bookAppointment());
    }

    private void fetchAvailability() {
        DatabaseReference availRef = FirebaseDatabase.getInstance().getReference("Availability").child(doctor.getId());
        availRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    StringBuilder availText = new StringBuilder("Available Times:\n");
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String day = ds.getKey();
                        String time = ds.getValue(String.class);
                        if (time != null && !time.isEmpty()) {
                            availabilityMap.put(day, time);
                            availText.append(day).append(": ").append(time).append("\n");
                        }
                    }
                    if (availabilityMap.isEmpty()) {
                        tvDoctorAvailability.setText("No availability set.");
                    } else {
                        tvDoctorAvailability.setText(availText.toString().trim());
                    }
                } else {
                    tvDoctorAvailability.setText("No availability set.");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvDoctorAvailability.setText("Error loading availability.");
            }
        });
    }

    private void bookAppointment() {
        String dateStr = etBookingDate.getText().toString();
        String timeStr = etBookingTime.getText().toString();

        if (dateStr.isEmpty() || timeStr.isEmpty()) {
            Toast.makeText(this, "Please select Date and Time", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isBookingTimeValid(dateStr, timeStr)) {
            Toast.makeText(this, "Doctor is not available at this time. Please select a time within the available schedule.", Toast.LENGTH_LONG).show();
            return;
        }

        String patientId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(patientId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String patientName = snapshot.child("name").getValue(String.class);
                    
                    DatabaseReference apptsRef = FirebaseDatabase.getInstance().getReference("Appointments");
                    apptsRef.orderByChild("patientId").equalTo(patientId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot apptSnapshot) {
                            boolean hasReusablePayment = false;
                            String reusableApptKey = null;
                            for (DataSnapshot ds : apptSnapshot.getChildren()) {
                                Appointment pastAppt = ds.getValue(Appointment.class);
                                if (pastAppt != null && pastAppt.getDoctorId().equals(doctor.getId())) {
                                    if ("Cancelled".equals(pastAppt.getStatus()) && "verified".equals(pastAppt.getPaymentStatus())) {
                                        hasReusablePayment = true;
                                        reusableApptKey = ds.getKey();
                                        break;
                                    }
                                }
                            }
                            
                            if (hasReusablePayment && reusableApptKey != null) {
                                apptsRef.child(reusableApptKey).child("paymentStatus").setValue("reused");
                                saveAppointmentDirectly(patientId, patientName, dateStr, timeStr);
                            } else {
                                Intent intent = new Intent(BookAppointmentActivity.this, PaymentActivity.class);
                                intent.putExtra("doctorId", doctor.getId());
                                intent.putExtra("doctorName", doctor.getName());
                                intent.putExtra("dateStr", dateStr);
                                intent.putExtra("timeStr", timeStr);
                                intent.putExtra("patientId", patientId);
                                intent.putExtra("patientName", patientName);
                                intent.putExtra("fee", doctor.getFee() != null ? doctor.getFee() : "300");
                                startActivity(intent);
                                finish();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(BookAppointmentActivity.this, "DB Error", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(BookAppointmentActivity.this, "DB Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveAppointmentDirectly(String patientId, String patientName, String dateStr, String timeStr) {
        DatabaseReference apptRef = FirebaseDatabase.getInstance().getReference("Appointments");
        String apptId = apptRef.push().getKey();

        Appointment appointment = new Appointment(apptId, patientId, patientName,
                doctor.getId(), doctor.getName(), dateStr, timeStr, "confirmed");
                
        appointment.setPaymentStatus("verified");
        appointment.setPaymentMode("UPI_REUSED");
        appointment.setAdminVerified(true);
        appointment.setTransactionId("REUSE_" + System.currentTimeMillis());
        appointment.setAmount(doctor.getFee() != null ? doctor.getFee() : "300");
        appointment.setPrescription("No prescription yet");

        if (apptId != null) {
            apptRef.child(apptId).setValue(appointment).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DatabaseReference recordRef = FirebaseDatabase.getInstance().getReference("MedicalRecords");
                    String recordId = recordRef.push().getKey();
                    com.example.appointivahealth.models.MedicalRecord record = new com.example.appointivahealth.models.MedicalRecord();
                    record.setId(recordId);
                    record.setPatientId(patientId);
                    record.setDoctorId(doctor.getId());
                    record.setAppointmentId(apptId);
                    record.setPatientName(patientName);
                    record.setDate(dateStr);
                    record.setDiagnosis("");
                    record.setNotes("");
                    record.setPrescription("No prescription yet");

                    if (recordId != null) {
                        recordRef.child(recordId).setValue(record);
                    }

                    // Send appointment booked notification (email/SMS) for reused payment flow.
                    try {
                        JSONObject payload = new JSONObject();
                        payload.put("appointmentId", apptId);
                        ApiClient.notifyAppointmentBooked(BookAppointmentActivity.this, payload, new ApiClient.JsonCallback() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                // no-op
                            }

                            @Override
                            public void onError(String error) {
                                // no-op
                            }
                        });
                    } catch (Exception ignored) {
                    }

                    Toast.makeText(BookAppointmentActivity.this, "Appointment Booked (Payment Reused)", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(BookAppointmentActivity.this, "Failed to save appointment", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private boolean isBookingTimeValid(String dateStr, String timeStr) {
        if (availabilityMap.isEmpty()) return false;
        
        try {
            SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date dateObj = sdfDate.parse(dateStr);
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
            String dayOfWeek = dayFormat.format(dateObj); // e.g. "Monday"

            if (!availabilityMap.containsKey(dayOfWeek)) return false;
            
            String times = availabilityMap.get(dayOfWeek);
            if (times == null || times.isEmpty()) return false;

            // Simple parser assuming HH:mm-HH:mm or HH:mm - HH:mm
            String[] splitTimes = times.split("-");
            if (splitTimes.length != 2) return true; // Fallback if format is unrecognized like "10-1"

            SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date reqTime = timeFmt.parse(timeStr);
            Date startTime = timeFmt.parse(splitTimes[0].trim());
            Date endTime = timeFmt.parse(splitTimes[1].trim());

            if (reqTime != null && startTime != null && endTime != null) {
                return (reqTime.equals(startTime) || reqTime.after(startTime)) && 
                       (reqTime.equals(endTime) || reqTime.before(endTime));
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return true; // Fallback gracefully if parsing breaks so we don't block bookings totally
        }
        return false;
    }
}
