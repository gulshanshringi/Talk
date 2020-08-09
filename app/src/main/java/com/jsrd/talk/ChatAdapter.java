package com.jsrd.talk;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jsrd.talk.model.Message;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.jsrd.talk.Utils.getDateTime;

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
            holder.rightTimeTxt.setText(getDateTime(message.getDateTime()));
        } else {
            holder.leftMsgLayout.setVisibility(View.VISIBLE);
            holder.leftMsgTxt.setText(message.getMessage());
            holder.leftTimeTxt.setText(getDateTime(message.getDateTime()));
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
