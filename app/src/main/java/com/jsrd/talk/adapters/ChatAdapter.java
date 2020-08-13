package com.jsrd.talk.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jsrd.talk.R;
import com.jsrd.talk.model.Message;
import com.jsrd.talk.utils.Utils;

import java.util.List;


public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private Context mContext;
    private List<Message> chatList;
    private String currentUserUID;

    public ChatAdapter(Context context, List<Message> chatList, String currentUserUID) {
        mContext = context;
        this.chatList = chatList;
        this.currentUserUID = currentUserUID;
    }


    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.message_item, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Message message = chatList.get(position);

        String sender = message.getSender();

        if (sender.equals(currentUserUID)) {
            holder.rightMsgLayout.setVisibility(View.VISIBLE);
            holder.rightMsgTxt.setText(message.getMessage());
            String dateTime = message.getDateTime();
            if (Utils.isToday(dateTime)) {
                holder.rightTimeTxt.setText(Utils.getTime(dateTime));
            } else {
                holder.rightTimeTxt.setText(Utils.getDate(dateTime));
            }
        } else {
            holder.leftMsgLayout.setVisibility(View.VISIBLE);
            holder.leftMsgTxt.setText(message.getMessage());
            String dateTime = message.getDateTime();
            if (Utils.isToday(dateTime)) {
                holder.leftTimeTxt.setText(Utils.getTime(dateTime));
            } else {
                holder.leftTimeTxt.setText(Utils.getDate(dateTime));
            }
        }
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView leftMsgTxt, rightMsgTxt;
        LinearLayout leftMsgLayout, rightMsgLayout;
        TextView leftTimeTxt, rightTimeTxt;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);

            leftMsgLayout = itemView.findViewById(R.id.leftMsgLayout);
            leftMsgTxt = itemView.findViewById(R.id.leftMsgTxt);
            leftTimeTxt = itemView.findViewById(R.id.leftTimeTxt);

            rightMsgLayout = itemView.findViewById(R.id.rightMsgLayout);
            rightMsgTxt = itemView.findViewById(R.id.rightMsgTxt);
            rightTimeTxt = itemView.findViewById(R.id.rightTimeTxt);
        }
    }
}
