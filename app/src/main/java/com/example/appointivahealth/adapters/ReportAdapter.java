package com.example.appointivahealth.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appointivahealth.R;

import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

    private Context context;
    private List<String> reportLines;

    public ReportAdapter(Context context, List<String> reportLines) {
        this.context = context;
        this.reportLines = reportLines;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        holder.tvReportLine.setText(reportLines.get(position));
    }

    @Override
    public int getItemCount() {
        return reportLines.size();
    }

    public static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView tvReportLine;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReportLine = itemView.findViewById(R.id.tvReportLine);
        }
    }
}
