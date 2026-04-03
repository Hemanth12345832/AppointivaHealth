package com.example.appointivahealth.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.Build;
import android.provider.MediaStore;
import java.io.OutputStream;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appointivahealth.R;
import com.example.appointivahealth.adapters.ReportAdapter;
import com.example.appointivahealth.models.Appointment;
import com.example.appointivahealth.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ReportsActivity extends AppCompatActivity {

    private Spinner spinnerReportType;
    private EditText etReportStartDate, etReportEndDate;
    private MaterialButton btnGenerateReport, btnExportReport;
    private RecyclerView rvReports;
    private ReportAdapter adapter;
    private List<String> reportDataList;

    private String[] reportTypes = {"Appointments Report", "Doctors Report", "Patients Report"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        spinnerReportType = findViewById(R.id.spinnerReportType);
        etReportStartDate = findViewById(R.id.etReportStartDate);
        etReportEndDate = findViewById(R.id.etReportEndDate);
        btnGenerateReport = findViewById(R.id.btnGenerateReport);
        btnExportReport = findViewById(R.id.btnExportReport);
        rvReports = findViewById(R.id.rvReports);

        ArrayAdapter<String> spinAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, reportTypes);
        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerReportType.setAdapter(spinAdapter);

        rvReports.setLayoutManager(new LinearLayoutManager(this));
        reportDataList = new ArrayList<>();
        adapter = new ReportAdapter(this, reportDataList);
        rvReports.setAdapter(adapter);

        etReportStartDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                etReportStartDate.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        etReportEndDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                etReportEndDate.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnGenerateReport.setOnClickListener(v -> generateReport());
        btnExportReport.setOnClickListener(v -> exportCSV());
    }

    private void generateReport() {
        String type = spinnerReportType.getSelectedItem().toString();
        String startDateStr = etReportStartDate.getText().toString();
        String endDateStr = etReportEndDate.getText().toString();

        DatabaseReference ref;
        if (type.contains("Appointments")) {
            ref = FirebaseDatabase.getInstance().getReference("Appointments");
        } else {
            ref = FirebaseDatabase.getInstance().getReference("Users");
        }

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                reportDataList.clear();

                if (type.contains("Appointments")) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        Appointment appt = data.getValue(Appointment.class);
                        if (appt != null) {
                            boolean withinRange = true;
                            if (!startDateStr.isEmpty() && appt.getDate().compareTo(startDateStr) < 0) {
                                withinRange = false;
                            }
                            if (!endDateStr.isEmpty() && appt.getDate().compareTo(endDateStr) > 0) {
                                withinRange = false;
                            }
                            
                            if (withinRange) {
                                reportDataList.add(appt.getDate() + " - " + appt.getTime() + " | Doctor: " + appt.getDoctorName() +
                                        " | Patient: " + appt.getPatientName() + " | Status: " + appt.getStatus());
                            }
                        }
                    }
                } else {
                    String roleFilter = type.contains("Doctors") ? "Doctor" : "Patient";
                    for (DataSnapshot data : snapshot.getChildren()) {
                        User user = data.getValue(User.class);
                        // Users don't natively have date filters in this model unless registration date is added,
                        // we ignore date filter for users unless specified.
                        if (user != null && roleFilter.equals(user.getRole())) {
                            reportDataList.add("Name: " + user.getName() + " | Email: " + user.getEmail() + " | Phone: " + user.getPhone());
                        }
                    }
                }
                
                if (reportDataList.isEmpty()) {
                    reportDataList.add("No records found.");
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ReportsActivity.this, "Failed to load report", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void exportCSV() {
        if (reportDataList.isEmpty() || reportDataList.get(0).equals("No records found.")) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "report_" + System.currentTimeMillis() + ".csv";
        boolean success = false;
        Uri fileUri = null;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AppointivaReports");

                fileUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                if (fileUri != null) {
                    try (OutputStream os = resolver.openOutputStream(fileUri)) {
                        for (String line : reportDataList) {
                            os.write((line.replace(" | ", ",") + "\n").getBytes());
                        }
                        success = true;
                    }
                }
            } else {
                File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File dir = new File(downloadDir, "AppointivaReports");
                if (!dir.exists()) dir.mkdirs();

                File file = new File(dir, fileName);
                FileWriter writer = new FileWriter(file);
                for (String line : reportDataList) {
                    writer.append(line.replace(" | ", ",")).append("\n");
                }
                writer.flush();
                writer.close();
                
                success = true;
                fileUri = FileProvider.getUriForFile(this, "com.example.appointivahealth.fileprovider", file);
            }

            if (success) {
                Toast.makeText(this, "Exported Successfully to Downloads/AppointivaReports!", Toast.LENGTH_LONG).show();

                // Intent to share/view the file securely
                if (fileUri != null) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/csv");
                    intent.putExtra(Intent.EXTRA_STREAM, fileUri);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(intent, "Share Report"));
                }
            } else {
                Toast.makeText(this, "Export failed: Unable to create file.", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
