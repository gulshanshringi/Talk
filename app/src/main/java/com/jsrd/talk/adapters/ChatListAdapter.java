package com.jsrd.talk.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.jsrd.talk.R;
import com.jsrd.talk.activities.ChatActivity;
import com.jsrd.talk.activities.MainActivity;
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
    private boolean isChatLoadingForFirstTime;
    private String userID;


    public ChatListAdapter(Context context, List<Chat> chatList, boolean isChatLoadingForFirstTime) {
        mContext = context;
        this.chatList = chatList;
        this.isChatLoadingForFirstTime = isChatLoadingForFirstTime;
        firebaseUtils = new FirebaseUtils(mContext);
        curentUserUID = firebaseUtils.getCurrentUserUID();
        userID = firebaseUtils.getCurrentUserUID();
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

        listemForMessageInRealTime(position, chatId, holder.tvLastMsg, holder.tvLastMsgTime, holder.tvUnseenMsg);

        setUserProfilePic(holder.civUserProfilePic);

    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public class ChatListViewHolder extends RecyclerView.ViewHolder {
        TextView tvPersonName, tvLastMsg, tvLastMsgTime;
        public TextView tvUnseenMsg;

        CircularImageView civUserProfilePic;

        public ChatListViewHolder(@NonNull View itemView) {
            super(itemView);

            tvPersonName = itemView.findViewById(R.id.tvPersonName);
            tvLastMsg = itemView.findViewById(R.id.tvLastMsg);
            tvLastMsgTime = itemView.findViewById(R.id.tvLastMsgTime);
            civUserProfilePic = itemView.findViewById(R.id.civUserProfilePic);
            tvUnseenMsg = itemView.findViewById(R.id.tvUnseenMsg);


            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String number = (String) chatList.get(getAdapterPosition()).getReceiversNumber();

                    Intent chatIntent = new Intent(mContext, ChatActivity.class);
                    chatIntent.putExtra("UserNumber", number);
                    chatIntent.putExtra("UserID", chatList.get(getAdapterPosition()).getReceiversUID());
                    chatIntent.putExtra("ChatID", chatList.get(getAdapterPosition()).getChatID());
                    chatIntent.putExtra("Chat", chatList.get(getAdapterPosition()));
                    mContext.startActivity(chatIntent);
                }
            });

        }
    }

    public void updateChatList(List<Chat> chatList) {
        this.chatList = chatList;
        notifyDataSetChanged();
    }

    private void listemForMessageInRealTime(int position, String chatID, TextView tvLastMsg, TextView tvLastMsgTime, TextView tvUnseenMsg) {
        firebaseUtils.listenForMessagesInRealTime(chatID, new ChatCallBack() {
            @Override
            public void onComplete(String chatID, List<Message> messageList, List<Chat> list) {
                if (messageList != null) {
                    String message = messageList.get(messageList.size() - 1).getMessage();
                    String sender = messageList.get(messageList.size() - 1).getSender();
                    String dateTime = messageList.get(messageList.size() - 1).getDateTime();
                    if (getMessageType(message).equalsIgnoreCase("message")) {
                        tvLastMsg.setText(getActualMessage(message));
                    } else if (getMessageType(message).equalsIgnoreCase("image")) {
                        tvLastMsg.setText("Image");
                    }
                    if (Utils.isToday(dateTime)) {
                        tvLastMsgTime.setText(Utils.getTime(dateTime));
                    } else {
                        tvLastMsgTime.setText(Utils.getDate(dateTime));
                    }

                    if (chat.getChatID().equalsIgnoreCase(chatID) && !chat.getDateTime().equalsIgnoreCase(dateTime)) {
                        chat.setDateTime(dateTime);
                        firebaseUtils.updateChatDateTime(chatList);
                    }

//                    if (isAllChatsSet && !sender.equals(curentUserUID) || Utils.isAppIsRunning(mContext) && !isChatLoadingForFirstTime) {
//                        MyNotificationManager.getInstance(mContext).displayNotification(chat.getReceiversNumber(), message, chat.getReceiversUID(), chat.getChatID());
//                    }
                    if (position == chatList.size() - 1) {
                        isAllChatsSet = true;
                    }
                    chat.setMessages(messageList);

                    checkUnseenMessages(tvUnseenMsg, messageList);
                }
            }
        });
    }

    private void checkUnseenMessages(TextView tvUnseenMsg, List<Message> messageList) {
        int unseenMsg = 0;
        for (Message msg : messageList) {
            if (!userID.equalsIgnoreCase(msg.getSender())) {
                if (!msg.isSeen()) {
                    unseenMsg++;
                }
            }
        }
        if (unseenMsg > 0) {
            tvUnseenMsg.setText(String.valueOf(unseenMsg));
            tvUnseenMsg.setVisibility(View.VISIBLE);
        } else {
            tvUnseenMsg.setText(String.valueOf(unseenMsg));
            tvUnseenMsg.setVisibility(View.GONE);
        }
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


    private String getActualMessage(String message) {
        String[] split = message.split("-", 2);
        return split[1];
    }


    private String getMessageType(String message) {
        String[] split = message.split("-", 2);
        // Log.i(TAG, "Spilited String is 1 : " + split[0] + " 2 : " + split[1]);
        return split[0];
    }
}
