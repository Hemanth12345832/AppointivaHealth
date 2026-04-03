package com.example.appointivahealth.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appointivahealth.R;
import com.example.appointivahealth.models.MedicalDocument;

import java.util.List;

public class MedicalDocumentAdapter extends RecyclerView.Adapter<MedicalDocumentAdapter.ViewHolder> {

    private Context context;
    private List<MedicalDocument> documentList;

    public MedicalDocumentAdapter(Context context, List<MedicalDocument> documentList) {
        this.context = context;
        this.documentList = documentList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_medical_document, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MedicalDocument document = documentList.get(position);

        holder.tvDocFileName.setText(document.getFileName());
        holder.tvDocUploadDate.setText("Uploaded: " + document.getUploadDate());

        boolean isImage = document.getFileType() != null && document.getFileType().startsWith("image/");
        holder.ivImagePreview.setVisibility(View.GONE);

        if (isImage) {
            holder.ivDocIcon.setImageResource(android.R.drawable.ic_menu_gallery);
            holder.btnViewDocument.setText("Preview Image");
        } else {
            holder.ivDocIcon.setImageResource(android.R.drawable.ic_menu_agenda);
            holder.btnViewDocument.setText("View PDF");
        }

        holder.btnViewDocument.setOnClickListener(v -> {
            try {
                if (isImage) {
                    holder.ivImagePreview.setVisibility(View.VISIBLE);
                    Glide.with(context)
                            .load(document.getFileUrl())
                            .into(holder.ivImagePreview);
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(document.getFileUrl()), "application/pdf");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    
                    if (intent.resolveActivity(context.getPackageManager()) != null) {
                        context.startActivity(intent);
                    } else {
                        // Fallback purely targeting native web view resolving Cloud storage PDF triggers cleanly 
                        Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(document.getFileUrl()));
                        context.startActivity(webIntent);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, "Failed to route document properly.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return documentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDocIcon, ivImagePreview;
        TextView tvDocFileName, tvDocUploadDate;
        Button btnViewDocument;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDocIcon = itemView.findViewById(R.id.ivDocIcon);
            ivImagePreview = itemView.findViewById(R.id.ivImagePreview);
            tvDocFileName = itemView.findViewById(R.id.tvDocFileName);
            tvDocUploadDate = itemView.findViewById(R.id.tvDocUploadDate);
            btnViewDocument = itemView.findViewById(R.id.btnViewDocument);
        }
    }
}
