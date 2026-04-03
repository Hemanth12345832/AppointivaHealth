package com.example.appointivahealth.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appointivahealth.R;
import com.example.appointivahealth.models.GenericRecord;

import java.util.List;

public class GenericRecordAdapter extends RecyclerView.Adapter<GenericRecordAdapter.RecordViewHolder> {

    private Context context;
    private List<GenericRecord> recordList;
    private boolean isReadOnly = false;

    public GenericRecordAdapter(Context context, List<GenericRecord> recordList) {
        this.context = context;
        this.recordList = recordList;
    }

    public GenericRecordAdapter(Context context, List<GenericRecord> recordList, boolean isReadOnly) {
        this.context = context;
        this.recordList = recordList;
        this.isReadOnly = isReadOnly;
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_generic_record, parent, false);
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        GenericRecord record = recordList.get(position);
        holder.tvTitle.setText(record.title);
        
        if (record.subtitle != null && !record.subtitle.isEmpty()) {
            holder.tvSubtitle.setVisibility(View.VISIBLE);
            holder.tvSubtitle.setText(record.subtitle);
        } else {
            holder.tvSubtitle.setVisibility(View.GONE);
        }

        if (record.date != null && !record.date.isEmpty()) {
            holder.tvDate.setVisibility(View.VISIBLE);
            holder.tvDate.setText(record.date);
        } else {
            holder.tvDate.setVisibility(View.GONE);
        }

        if (record.status != null && !record.status.isEmpty()) {
            holder.tvStatus.setVisibility(View.VISIBLE);
            holder.tvStatus.setText(record.status);
            if (record.status.equalsIgnoreCase("Completed") || record.status.equalsIgnoreCase("Success")) {
                holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
            } else if (record.status.equalsIgnoreCase("Cancelled") || record.status.equalsIgnoreCase("Failed")) {
                holder.tvStatus.setTextColor(Color.parseColor("#F44336"));
            } else {
                holder.tvStatus.setTextColor(Color.parseColor("#FFEB3B")); // Pending/Warning
            }
        } else {
            holder.tvStatus.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (record.appointmentId != null && !record.appointmentId.isEmpty()) {
                android.content.Intent intent = new android.content.Intent(context, com.example.appointivahealth.activities.PatientRecordDetailsActivity.class);
                intent.putExtra("appointmentId", record.appointmentId);
                intent.putExtra("isReadOnly", isReadOnly);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return recordList.size();
    }

    public static class RecordViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle, tvDate, tvStatus;

        public RecordViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvGenericTitle);
            tvSubtitle = itemView.findViewById(R.id.tvGenericSubtitle);
            tvDate = itemView.findViewById(R.id.tvGenericDate);
            tvStatus = itemView.findViewById(R.id.tvGenericStatus);
        }
    }
}
