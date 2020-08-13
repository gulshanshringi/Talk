package com.jsrd.talk.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.jsrd.talk.R;
import com.jsrd.talk.activities.ChatActivity;
import com.jsrd.talk.interfaces.ChatCallBack;
import com.jsrd.talk.interfaces.ReceiverCallback;
import com.jsrd.talk.model.Chat;
import com.jsrd.talk.model.Message;
import com.jsrd.talk.notification.MyNotificationManager;
import com.jsrd.talk.utils.FirebaseUtils;
import com.jsrd.talk.utils.Utils;
import com.mikhaellopez.circularimageview.CircularImageView;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;


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

        setUserProfilePic(holder.civUserProfilePic);

    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    class ChatListViewHolder extends RecyclerView.ViewHolder {
        TextView tvPersonName, tvLastMsg, tvLastMsgTime;
        CircularImageView civUserProfilePic;

        public ChatListViewHolder(@NonNull View itemView) {
            super(itemView);

            tvPersonName = itemView.findViewById(R.id.tvPersonName);
            tvLastMsg = itemView.findViewById(R.id.tvLastMsg);
            tvLastMsgTime = itemView.findViewById(R.id.tvLastMsgTime);
            civUserProfilePic = itemView.findViewById(R.id.civUserProfilePic);


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
                    if (Utils.isToday(dateTime)) {
                        tvLastMsgTime.setText(Utils.getTime(dateTime));
                    } else {
                        tvLastMsgTime.setText(Utils.getDate(dateTime));
                    }
                    if (isAllChatsSet && !sender.equals(curentUserUID) && Utils.isAppIsRunning(mContext)) {
                        MyNotificationManager.getInstance(mContext).displayNotification(chat.getReceiversNumber(), message, chat.getReceiversUID(), chat.getChatID());
                    }
                    if (position == chatList.size() - 1) {
                        isAllChatsSet = true;
                    }
                }
            }
        });
    }

    private void setUserProfilePic(CircularImageView civUserProfilePic) {
        firebaseUtils.getReceiversProfilePic(chat.getReceiversNumber(), new ReceiverCallback() {
            @Override
            public void onComplete(String data) {
                if (data != null) {
                    Glide.with(mContext)
                            .load(data)
                            .into(civUserProfilePic);
                }
            }
        });


    }

}
