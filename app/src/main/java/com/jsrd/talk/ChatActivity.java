package com.jsrd.talk;

import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.Circle;
import com.jsrd.talk.interfaces.ChatCallBack;
import com.jsrd.talk.model.Chat;
import com.jsrd.talk.model.Message;
import com.jsrd.talk.notification.APIService;
import com.jsrd.talk.notification.Client;
import com.jsrd.talk.notification.Data;
import com.jsrd.talk.notification.MyResponse;
import com.jsrd.talk.notification.Sender;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;

public class ChatActivity extends AppCompatActivity {

    final private String FCM_API_URL = "https://fcm.googleapis.com/";
    private ProgressBar caProgressBar;
    private RecyclerView chatListRecyclerView;
    private EditText msgEditTxt;
    private FirebaseUtils ff;
    private Toolbar chatActivityToolbar;
    private String receiversUserID, receiversNumber;
    private String currentUserNumber, currentUserUID;
    private String chatId;
    private String token = "/topics/";
    private APIService apiService;
    private Chat chat;
    private boolean isListeningForMessage = false;
    private ImageButton sendBtn;
    private boolean isSetupComplete = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);


        caProgressBar = findViewById(R.id.caProgressBar);
        msgEditTxt = findViewById(R.id.msgEditTxt);
        chatListRecyclerView = findViewById(R.id.chatListRecyclerView);
        chatActivityToolbar = (Toolbar) findViewById(R.id.activityMainToolbar);
        sendBtn = findViewById(R.id.sendBtn);

        receiversUserID = getIntent().getStringExtra("UserID");
        receiversNumber = getIntent().getStringExtra("UserNumber");
        chatId = getIntent().getStringExtra("ChatID");


        //setup toolbar
        chatActivityToolbar.setTitle(Utils.getNameByNumber(this, receiversNumber));
        setSupportActionBar(chatActivityToolbar);
        //add back button on toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        //setup progress bar
        Sprite circle = new Circle();
        circle.setColor(Color.BLUE);
        caProgressBar.setIndeterminateDrawable(circle);

        ff = new FirebaseUtils(this);

        //setup recycler view
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatListRecyclerView.setLayoutManager(layoutManager);


        chat = (Chat) getIntent().getSerializableExtra("Chat");

        if (chat != null) {
            chatId = chat.getChatID();
            if (chat.getMessages() != null) {
                setChatData(chat.getMessages());
            }
        } else if (chatId != null) {
            listenForMessagesInRealTime(chatId);
        }

        token += receiversUserID;

        currentUserNumber = ff.getCurrentUserNumber();
        currentUserUID = ff.getCurrentUserUID();

        apiService = Client.getClient(FCM_API_URL).create(APIService.class);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void setChatData(List<Message> messageList) {
        caProgressBar.setVisibility(View.VISIBLE);
        ChatAdapter adapter = new ChatAdapter(ChatActivity.this, messageList, currentUserUID);
        chatListRecyclerView.setAdapter(adapter);
        caProgressBar.setVisibility(View.GONE);
        chatListRecyclerView.smoothScrollToPosition(messageList.size() - 1);
        if (chatId != null) {
            listenForMessagesInRealTime(chatId);
        }

    }

    public void sendMessage(View view) {
        final String message = msgEditTxt.getText().toString();
        msgEditTxt.setText("");
        if (message.length() > 0) {
            ff.sendMessage(receiversNumber, currentUserNumber, message, chatId, new ChatCallBack() {
                @Override
                public void onComplete(String chatID, List<Message> messageList, List<Chat> chatList) {
                    if (chatID != null) {
                        chatId = chatID;
                        sendNotification(message, chatID);
                        listenForMessagesInRealTime(chatID);
                    }
                }
            });

        } else {
            Toast.makeText(this, "Message can not be empty", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendNotification(String message, String chatID) {
        Data data = new Data(message, currentUserNumber, chatID, currentUserUID);
        Sender sender = new Sender(data, token);

        apiService.sendNotification(sender).enqueue(new Callback<MyResponse>() {
            @Override
            public void onResponse(Call<MyResponse> call, retrofit2.Response<MyResponse> response) {
                if (response.code() == 200) {
                    if (response.body().success == 1) {
                        Log.i("ChatActivity", "Failed");
                    } else {
                        Log.i("ChatActivity", "Success");
                    }
                }
            }

            @Override
            public void onFailure(Call<MyResponse> call, Throwable t) {
                //Toast.makeText(ChatActivity.this, "Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void listenForMessagesInRealTime(String chatID) {
        if (!isListeningForMessage) {
            isListeningForMessage = true;
            FirebaseUtils firebaseUtils = new FirebaseUtils(this);
            firebaseUtils.listenForMessagesInRealTime(chatID, new ChatCallBack() {
                @Override
                public void onComplete(String chatID, List<Message> messageList, List<Chat> chatList) {
                    if (messageList != null) {
                        setChatData(messageList);
                        String currentUserUID = firebaseUtils.getCurrentUserUID();
                        String sender = messageList.get(messageList.size()-1).getSender();
                        if (isSetupComplete && !sender.equals(currentUserUID)) {
                            try {
                                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                                r.play();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        isSetupComplete = true;
                    }
                }
            });
        }
    }
}