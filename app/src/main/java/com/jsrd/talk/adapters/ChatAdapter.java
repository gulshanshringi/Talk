package com.jsrd.talk.adapters;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.FadingCircle;
import com.jsrd.talk.R;
import com.jsrd.talk.interfaces.ChatCallBack;
import com.jsrd.talk.interfaces.ImageUploadCallBack;
import com.jsrd.talk.model.Chat;
import com.jsrd.talk.model.Message;
import com.jsrd.talk.utils.FirebaseUtils;
import com.jsrd.talk.utils.Utils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final String TAG = "ChatAdapter";
    private Context mContext;
    private List<Message> messageList;
    private String currentUserUID;
    private String chatId;
    private Boolean isNewMessage = false;
    private FirebaseUtils firebaseUtils;

    public ChatAdapter(Context context, List<Message> messageList, String currentUserUID, String chatId) {
        mContext = context;
        this.messageList = messageList;
        this.currentUserUID = currentUserUID;
        this.chatId = chatId;
        firebaseUtils = new FirebaseUtils(mContext);
    }


    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.message_item, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Message message = messageList.get(position);

        String sender = message.getSender();

        if (sender.equals(currentUserUID)) {
            holder.rightMsgLayout.setVisibility(View.VISIBLE);
            String messageType = getMessageType(message.getMessage());
            if (messageType.equalsIgnoreCase("image")) {
                String imageURL = getActualMessage(message.getMessage());
                holder.pbRightIV.setVisibility(View.VISIBLE);
                Glide.with(mContext).
                        load(imageURL).fitCenter().placeholder(R.drawable.ic_image).
                        into(holder.rightImageView);
                holder.rightMsgTxt.setVisibility(View.GONE);
                holder.rightMsgLayout.setVisibility(View.VISIBLE);
                holder.rightImageView.setVisibility(View.VISIBLE);
                holder.pbRightIV.setVisibility(View.GONE);
            } else if (messageType.equalsIgnoreCase("message")) {
                String msg = getActualMessage(message.getMessage());
                holder.rightMsgTxt.setText(msg);
                holder.rightMsgTxt.setVisibility(View.VISIBLE);
                holder.rightImageView.setVisibility(View.GONE);
                holder.rightMsgLayout.setVisibility(View.VISIBLE);
                holder.pbRightIV.setVisibility(View.GONE);
            } else {
                String imageURL = getActualMessage(message.getMessage());
                holder.pbRightIV.setVisibility(View.VISIBLE);
                Glide.with(mContext).
                        load(imageURL).fitCenter().
                        placeholder(R.drawable.ic_image).
                        into(holder.rightImageView);
                holder.rightMsgTxt.setVisibility(View.GONE);
                holder.rightMsgLayout.setVisibility(View.VISIBLE);
                holder.rightImageView.setVisibility(View.VISIBLE);
            }
            String dateTime = message.getDateTime();
            if (Utils.isToday(dateTime)) {
                holder.rightTimeTxt.setText(Utils.getTime(dateTime));
            } else {
                holder.rightTimeTxt.setText(Utils.getDate(dateTime));
            }
            if (message.isSeen()) {
                holder.seenIndicatorTxt.setVisibility(View.VISIBLE);
            }

        } else {
            String messageType = getMessageType(message.getMessage());
            if (messageType.equalsIgnoreCase("image")) {
                String imageURL = getActualMessage(message.getMessage());
                Glide.with(mContext).
                        load(imageURL).fitCenter().
                        into(holder.leftImageView);
                holder.leftMsgTxt.setVisibility(View.GONE);
                holder.leftMsgLayout.setVisibility(View.VISIBLE);
                holder.leftImageView.setVisibility(View.VISIBLE);
            } else {
                String msg = getActualMessage(message.getMessage());
                holder.rightMsgTxt.setText(msg);
                holder.leftMsgTxt.setVisibility(View.VISIBLE);
                holder.leftMsgLayout.setVisibility(View.VISIBLE);
                holder.leftImageView.setVisibility(View.GONE);
            }
            String dateTime = message.getDateTime();
            if (Utils.isToday(dateTime)) {
                holder.leftTimeTxt.setText(Utils.getTime(dateTime));
            } else {
                holder.leftTimeTxt.setText(Utils.getDate(dateTime));
            }
        }
        if ((messageList.size() - 1) == position) {
            markedNewMessageesAsSeen();
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView leftMsgTxt, rightMsgTxt;
        LinearLayout leftMsgLayout, rightMsgLayout;
        TextView leftTimeTxt, rightTimeTxt, seenIndicatorTxt;
        ImageView rightImageView, leftImageView;
        ProgressBar pbLeftIV, pbRightIV;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);

            leftMsgLayout = itemView.findViewById(R.id.leftMsgLayout);
            leftMsgTxt = itemView.findViewById(R.id.leftMsgTxt);
            leftTimeTxt = itemView.findViewById(R.id.leftTimeTxt);
            leftImageView = itemView.findViewById(R.id.leftImageView);
            pbLeftIV = itemView.findViewById(R.id.pbLeftIV);

            rightMsgLayout = itemView.findViewById(R.id.rightMsgLayout);
            rightMsgTxt = itemView.findViewById(R.id.rightMsgTxt);
            rightTimeTxt = itemView.findViewById(R.id.rightTimeTxt);
            seenIndicatorTxt = itemView.findViewById(R.id.seenIndicatorTxt);
            rightImageView = itemView.findViewById(R.id.rightImageView);
            pbRightIV = itemView.findViewById(R.id.pbRightIV);

            Sprite fadingCircle = new FadingCircle();
            fadingCircle.setColor(Color.BLUE);
            pbLeftIV.setIndeterminateDrawable(fadingCircle);
            pbRightIV.setIndeterminateDrawable(fadingCircle);
        }
    }

    public void updateMessages(List<Message> messageList) {
        this.messageList = messageList;
        notifyDataSetChanged();
    }

    private void markedNewMessageesAsSeen() {
        if (chatId != null) {
            isNewMessage = false;
            for (Message msg : messageList) {
                if (!msg.getSender().equals(currentUserUID)) {
                    if (!msg.isSeen()) {
                        msg.setSeen(true);
                        isNewMessage = true;
                    }
                }
            }
            if (isNewMessage) {
                FirebaseUtils firebaseUtils = new FirebaseUtils(mContext);
                firebaseUtils.markedMessagesAsSeen(messageList, chatId);
            }
        }
    }

    private String getMessageType(String message) {
        String[] split = message.split("-", 2);
        // Log.i(TAG, "Spilited String is 1 : " + split[0] + " 2 : " + split[1]);
        return split[0];
    }

    private String getActualMessage(String message) {
        String[] split = message.split("-", 2);
        Log.i(TAG, "Spilited String is 1 : " + split[0] + " 2 : " + split[1]);
        return split[1];
    }
}
