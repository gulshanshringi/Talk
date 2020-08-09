package com.jsrd.talk;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.Circle;
import com.google.firebase.messaging.FirebaseMessaging;
import com.jsrd.talk.interfaces.ChatCallBack;
import com.jsrd.talk.interfaces.ProgressSuccessCallBack;
import com.jsrd.talk.interfaces.ReceiverCallback;
import com.jsrd.talk.model.Chat;
import com.jsrd.talk.model.Message;
import com.jsrd.talk.notification.MyNotificationManager;

import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseUtils firebaseUtils;
    private Toolbar activityMainToolbar;
    private RecyclerView chatListRecyclerView;
    private ProgressBar maProgressBar;
    ChatListAdapter adapter;
    private boolean isListeningForChats = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //setup toolbar
        activityMainToolbar = (Toolbar) findViewById(R.id.activityMainToolbar);
        activityMainToolbar.setTitle(R.string.app_name);
        setSupportActionBar(activityMainToolbar);

        firebaseUtils = new FirebaseUtils(this);

        chatListRecyclerView = findViewById(R.id.chatListRecyclerView);
        maProgressBar = findViewById(R.id.maProgressBar);
        chatListRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        Sprite circle = new Circle();
        circle.setColor(Color.BLUE);
        maProgressBar.setIndeterminateDrawable(circle);

        checkPermission();
        subscribeForNotification();
        // getUsersChatList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        listenForChatsInRealTime();
    }

    public void getUsersChatList() {
        maProgressBar.setVisibility(View.VISIBLE);
        String userUID = firebaseUtils.getCurrentUserUID();
        firebaseUtils.getUsersChatList(userUID, new ChatCallBack() {
            @Override
            public void onComplete(String chatID, List<Message> messageList, List<Chat> chatList) {
                if (chatList != null) {
                    adapter = new ChatListAdapter(MainActivity.this, chatList);
                    chatListRecyclerView.setAdapter(adapter);
                    maProgressBar.setVisibility(View.GONE);
                } else {
                    maProgressBar.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "No Chats Found", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void openChatWithSelectedUser(String UID, String number) {
        Intent chatIntent = new Intent(MainActivity.this, ChatActivity.class);
        chatIntent.putExtra("UserID", UID);
        chatIntent.putExtra("UserNumber", number);
        startActivity(chatIntent);

    }

    private void listenForChatsInRealTime() {
        if (!isListeningForChats) {
            isListeningForChats = true;
            firebaseUtils.listenForChatsInRealTime(new ChatCallBack() {
                @Override
                public void onComplete(String chatID, List<Message> messageList, List<Chat> chatList) {
                    if (chatList != null) {
                        adapter = new ChatListAdapter(MainActivity.this, chatList);
                        chatListRecyclerView.setAdapter(adapter);
                        maProgressBar.setVisibility(View.GONE);
                    } else {
                        maProgressBar.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "No Chats Found", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }



    private void subscribeForNotification() {
        firebaseUtils = new FirebaseUtils(this);
        String userUID = firebaseUtils.getCurrentUserUID();
        FirebaseMessaging.getInstance().subscribeToTopic(userUID);
    }

    public void showContactList(View v) {
        ContactFragmentDialog contactFragment = new ContactFragmentDialog();
        contactFragment.setStyle(DialogFragment.STYLE_NO_FRAME, R.style.Theme_AppCompat_Light_NoActionBar);
        contactFragment.show(getSupportFragmentManager(), "ContactFragmentDialog");
    }

    private void checkPermission() {
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {
                android.Manifest.permission.READ_CONTACTS
                //,  android.Manifest.permission.ACCESS_FINE_LOCATION,
                //  android.Manifest.permission.CAMERA
        };

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

}