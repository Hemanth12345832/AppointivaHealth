package com.example.appointivahealth.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appointivahealth.R;
import com.example.appointivahealth.models.ChatMessage;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_RIGHT = 1;
    private static final int VIEW_TYPE_LEFT = 2;

    private Context context;
    private List<ChatMessage> chatList;
    private String currentUserId;

    public ChatAdapter(Context context, List<ChatMessage> chatList, String currentUserId) {
        this.context = context;
        this.chatList = chatList;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        if (chatList.get(position).getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_RIGHT;
        } else {
            return VIEW_TYPE_LEFT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_RIGHT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_chat_right, parent, false);
            return new RightChatViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_chat_left, parent, false);
            return new LeftChatViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = chatList.get(position);
        
        String time = DateFormat.format("hh:mm a", message.getTimestamp()).toString();

        if (holder.getItemViewType() == VIEW_TYPE_RIGHT) {
            RightChatViewHolder rightHolder = (RightChatViewHolder) holder;
            rightHolder.tvMessage.setText(message.getMessage());
            rightHolder.tvTimestamp.setText(time);
            
            if (message.getFileUrl() != null && !message.getFileUrl().isEmpty()) {
                if (message.getFileType() != null && message.getFileType().contains("image")) {
                    rightHolder.ivAttachment.setVisibility(View.VISIBLE);
                    rightHolder.llPdfAttachment.setVisibility(View.GONE);
                    Glide.with(context).load(message.getFileUrl()).into(rightHolder.ivAttachment);
                } else {
                    rightHolder.ivAttachment.setVisibility(View.GONE);
                    rightHolder.llPdfAttachment.setVisibility(View.VISIBLE);
                    rightHolder.tvPdfName.setText(message.getFileName() != null ? message.getFileName() : "document.pdf");
                }
                
                View.OnClickListener openLink = v -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(message.getFileUrl()));
                    context.startActivity(intent);
                };
                rightHolder.ivAttachment.setOnClickListener(openLink);
                rightHolder.llPdfAttachment.setOnClickListener(openLink);
            } else {
                rightHolder.ivAttachment.setVisibility(View.GONE);
                rightHolder.llPdfAttachment.setVisibility(View.GONE);
            }
        } else {
            LeftChatViewHolder leftHolder = (LeftChatViewHolder) holder;
            leftHolder.tvMessage.setText(message.getMessage());
            leftHolder.tvTimestamp.setText(time);
            
            if (message.getFileUrl() != null && !message.getFileUrl().isEmpty()) {
                if (message.getFileType() != null && message.getFileType().contains("image")) {
                    leftHolder.ivAttachment.setVisibility(View.VISIBLE);
                    leftHolder.llPdfAttachment.setVisibility(View.GONE);
                    Glide.with(context).load(message.getFileUrl()).into(leftHolder.ivAttachment);
                } else {
                    leftHolder.ivAttachment.setVisibility(View.GONE);
                    leftHolder.llPdfAttachment.setVisibility(View.VISIBLE);
                    leftHolder.tvPdfName.setText(message.getFileName() != null ? message.getFileName() : "document.pdf");
                }
                
                View.OnClickListener openLink = v -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(message.getFileUrl()));
                    context.startActivity(intent);
                };
                leftHolder.ivAttachment.setOnClickListener(openLink);
                leftHolder.llPdfAttachment.setOnClickListener(openLink);
            } else {
                leftHolder.ivAttachment.setVisibility(View.GONE);
                leftHolder.llPdfAttachment.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public static class RightChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTimestamp, tvPdfName;
        ImageView ivAttachment;
        LinearLayout llPdfAttachment;

        public RightChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessageRight);
            tvTimestamp = itemView.findViewById(R.id.tvTimestampRight);
            tvPdfName = itemView.findViewById(R.id.tvPdfNameRight);
            ivAttachment = itemView.findViewById(R.id.ivAttachmentRight);
            llPdfAttachment = itemView.findViewById(R.id.llPdfAttachmentRight);
        }
    }

    public static class LeftChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTimestamp, tvPdfName;
        ImageView ivAttachment;
        LinearLayout llPdfAttachment;

        public LeftChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessageLeft);
            tvTimestamp = itemView.findViewById(R.id.tvTimestampLeft);
            tvPdfName = itemView.findViewById(R.id.tvPdfNameLeft);
            ivAttachment = itemView.findViewById(R.id.ivAttachmentLeft);
            llPdfAttachment = itemView.findViewById(R.id.llPdfAttachmentLeft);
        }
    }
}
