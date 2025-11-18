package com.example.culturalcompass.ui.assistant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.culturalcompass.R;
import com.example.culturalcompass.model.Message;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Message> messages;

    public ChatAdapter(List<Message> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).role;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewType == Message.USER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_user_message, parent, false);
            return new UserHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_assistant_message, parent, false);
            return new AssistantHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message msg = messages.get(position);

        if (holder.getItemViewType() == Message.USER) {
            ((UserHolder) holder).text.setText(msg.text);
        } else {
            ((AssistantHolder) holder).text.setText(msg.text);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class UserHolder extends RecyclerView.ViewHolder {
        TextView text;
        UserHolder(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.text_user);
        }
    }

    static class AssistantHolder extends RecyclerView.ViewHolder {
        TextView text;
        AssistantHolder(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.text_assistant);
        }
    }
}
