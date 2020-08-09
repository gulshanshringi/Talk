package com.jsrd.talk;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jsrd.talk.interfaces.ChatCallBack;
import com.jsrd.talk.model.Chat;
import com.jsrd.talk.model.Message;
import com.jsrd.talk.notification.MyNotificationManager;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import static com.jsrd.talk.Utils.getDateTime;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatListViewHolder> {

    private Context mContext;
    private List<Chat> chatList;
    private FirebaseUtils firebaseUtils;
    private Chat chat;
    private boolean isAllChatsSet = false;
    private String curentUserUID;

    public ChatListAdapter(Context context, List<Chat> chatList) {
        mContext = context;
        this.chatList = chatList;
        firebaseUtils = new FirebaseUtils(mContext);
        curentUserUID = firebaseUtils.getCurrentUserUID();
    }

    @NonNull
    @Override
    public ChatListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.chat_item, parent, false);
        return new ChatListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatListViewHolder holder, int position) {
        chat = chatList.get(position);

        holder.tvPersonName.setText(Utils.getNameByNumber(mContext, chat.getReceiversNumber()));
        String chatId = chat.getChatID();

        listemForMessageInRealTime(position, chatId, holder.tvLastMsg, holder.tvLastMsgTime);
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    class ChatListViewHolder extends RecyclerView.ViewHolder {
        TextView tvPersonName, tvLastMsg, tvLastMsgTime;

        public ChatListViewHolder(@NonNull View itemView) {
            super(itemView);

            tvPersonName = itemView.findViewById(R.id.tvPersonName);
            tvLastMsg = itemView.findViewById(R.id.tvLastMsg);
            tvLastMsgTime = itemView.findViewById(R.id.tvLastMsgTime);


            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String number = (String) chatList.get(getAdapterPosition()).getReceiversNumber();

                    Intent chatIntent = new Intent(mContext, ChatActivity.class);
                    chatIntent.putExtra("UserNumber", number);
                    chatIntent.putExtra("UserID", chatList.get(getAdapterPosition()).getReceiversUID());
                    chatIntent.putExtra("Chat", (Serializable) chatList.get(getAdapterPosition()));
                    mContext.startActivity(chatIntent);
                }
            });

        }
    }

    public void addChatToChatList(Chat chat) {
        chatList.add(chat);
        notifyDataSetChanged();
    }

    private void listemForMessageInRealTime(int position, String chatID, TextView tvLastMsg, TextView tvLastMsgTime) {
        firebaseUtils.listenForMessagesInRealTime(chatID, new ChatCallBack() {
            @Override
            public void onComplete(String chatID, List<Message> messageList, List<Chat> list) {
                if (messageList != null) {
                    chat.setMessages(messageList);
                    String message = messageList.get(messageList.size() - 1).getMessage();
                    String sender = messageList.get(messageList.size() - 1).getSender();
                    String dateTime = messageList.get(messageList.size() - 1).getDateTime();
                    tvLastMsg.setText(message);
                    tvLastMsgTime.setText(getDateTime(dateTime));
                    if (isAllChatsSet && !sender.equals(curentUserUID)) {
                        MyNotificationManager.getInstance(mContext).displayNotification(chat.getReceiversNumber(), message, chat.getReceiversUID(), chat.getChatID());
                    }
                    if (position == chatList.size() - 1) {
                        isAllChatsSet = true;
                    }
                }
            }
        });
    }


}
