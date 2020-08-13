package com.jsrd.talk.activities;

import android.content.Intent;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.Circle;
import com.jsrd.talk.adapters.ChatAdapter;
import com.jsrd.talk.R;
import com.jsrd.talk.interfaces.ChatCallBack;
import com.jsrd.talk.interfaces.StatusCallBack;
import com.jsrd.talk.model.Chat;
import com.jsrd.talk.model.Message;
import com.jsrd.talk.notification.APIService;
import com.jsrd.talk.notification.Client;
import com.jsrd.talk.notification.Data;
import com.jsrd.talk.notification.MyResponse;
import com.jsrd.talk.notification.Sender;
import com.jsrd.talk.utils.FirebaseUtils;
import com.jsrd.talk.utils.Utils;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;

import static com.jsrd.talk.utils.Utils.getCurrentDateAndTime;
import static com.jsrd.talk.utils.Utils.getDate;
import static com.jsrd.talk.utils.Utils.getNameByNumber;
import static com.jsrd.talk.utils.Utils.isToday;

public class ChatActivity extends AppCompatActivity {

    final private String FCM_API_URL = "https://fcm.googleapis.com/";
    private ProgressBar caProgressBar;
    private RecyclerView chatListRecyclerView;
    private EditText etMsg;
    private FirebaseUtils firebaseUtils;
    private Toolbar chatActivityToolbar;
    private String receiversUserID, receiversNumber;
    private String currentUserNumber, currentUserID;
    private String chatId;
    public static String chatingWith;
    private String token = "/topics/";
    private APIService apiService;
    private Chat chat;
    private boolean isListeningForMessage = false;
    private ImageButton sendBtn;
    private boolean isSetupComplete = false;
    private TextView tvToolbarTitle, tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);


        caProgressBar = findViewById(R.id.caProgressBar);
        etMsg = findViewById(R.id.etMsg);
        chatListRecyclerView = findViewById(R.id.chatListRecyclerView);
        chatActivityToolbar = (Toolbar) findViewById(R.id.activityMainToolbar);
        sendBtn = findViewById(R.id.sendBtn);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        tvStatus = findViewById(R.id.tvStatus);

        receiversUserID = getIntent().getStringExtra("UserID");
        receiversNumber = getIntent().getStringExtra("UserNumber");
        chatId = getIntent().getStringExtra("ChatID");


        //setup toolbar
        chatActivityToolbar.setTitle("");
        setSupportActionBar(chatActivityToolbar);
        //add back button on toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        tvToolbarTitle.setText(getNameByNumber(this, receiversNumber));

        //setup progress bar
        Sprite circle = new Circle();
        circle.setColor(Color.BLUE);
        caProgressBar.setIndeterminateDrawable(circle);

        firebaseUtils = new FirebaseUtils(this);

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

        currentUserNumber = firebaseUtils.getCurrentUserNumber();
        currentUserID = firebaseUtils.getCurrentUserUID();

        listenForStatusInRealTime(receiversUserID);

        apiService = Client.getClient(FCM_API_URL).create(APIService.class);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        chatingWith = null;
        if (!Utils.isAppIsRunning(this)) {
            firebaseUtils.updateStatus(getCurrentDateAndTime());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        chatingWith = receiversNumber;
        firebaseUtils.updateStatus("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        chatingWith = null;
    }

    public void openUserProfileActivity(View view) {
        Intent intent = new Intent(ChatActivity.this, UserProfileActivity.class);
        intent.putExtra("UserNumber", receiversNumber);
        startActivity(intent);
    }

    private void listenForStatusInRealTime(String userId) {
        firebaseUtils.listenForStatusInRealTime(userId, new StatusCallBack() {
            @Override
            public void onStatusUpdate(String status) {
                if (status != null) {
                    if (status.equals("online")) {
                        tvStatus.setText(status);
                    } else {
                        if (isToday(status)) {
                            tvStatus.setText("last seen today at " + Utils.getTime(status));
                        } else {
                            tvStatus.setText("last seen " + getDate(status) + " at " + Utils.getTime(status));
                        }
                    }
                }
            }
        });
    }

    private void setChatData(List<Message> messageList) {
        caProgressBar.setVisibility(View.VISIBLE);
        ChatAdapter adapter = new ChatAdapter(ChatActivity.this, messageList, currentUserID);
        chatListRecyclerView.setAdapter(adapter);
        caProgressBar.setVisibility(View.GONE);
        chatListRecyclerView.smoothScrollToPosition(messageList.size() - 1);
        if (chatId != null) {
            listenForMessagesInRealTime(chatId);
        }

    }

    public void sendMessage(View view) {
        final String message = etMsg.getText().toString();
        etMsg.setText("");
        if (message.length() > 0) {
            firebaseUtils.sendMessage(receiversNumber, currentUserNumber, message, chatId, new ChatCallBack() {
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
        Data data = new Data(message, currentUserNumber, chatID, currentUserID);
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
                        String sender = messageList.get(messageList.size() - 1).getSender();
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