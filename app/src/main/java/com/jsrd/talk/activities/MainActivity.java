package com.jsrd.talk.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.Circle;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import com.jsrd.talk.adapters.ChatListAdapter;
import com.jsrd.talk.fragments.ContactFragmentDialog;
import com.jsrd.talk.R;
import com.jsrd.talk.interfaces.ChatCallBack;
import com.jsrd.talk.model.Chat;
import com.jsrd.talk.model.Message;
import com.jsrd.talk.notification.MyNotificationManager;
import com.jsrd.talk.utils.FirebaseUtils;
import com.jsrd.talk.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jsrd.talk.utils.Utils.getCurrentDateAndTime;

public class MainActivity extends AppCompatActivity {

    private FirebaseUtils firebaseUtils;
    private Toolbar activityMainToolbar;
    private RecyclerView chatListRecyclerView;
    private ProgressBar maProgressBar;
    private ChatListAdapter adapter;
    private boolean isListeningForChats = false;
    private boolean isChatLoadingForFirstTime = true;
    private List<Chat> chats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseUtils = new FirebaseUtils(this);

        //setup toolbar
        setupToolbar();

        //setup recycler view for chat list
        setupRecyclerView();

        setupProgressBar();

        //checkPermission();
        subscribeForNotification();
    }

    @Override
    protected void onResume() {
        super.onResume();
        listenForChatsInRealTime();
        firebaseUtils.updateStatus("online");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!Utils.isAppIsRunning(this)) {
            firebaseUtils.updateStatus(getCurrentDateAndTime());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        firebaseUtils.updateStatus(getCurrentDateAndTime());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_item_main_toolbar, menu);
        return true;
    }

    private void setupToolbar() {
        activityMainToolbar = (Toolbar) findViewById(R.id.activityMainToolbar);
        activityMainToolbar.setTitle(R.string.app_name);
        setSupportActionBar(activityMainToolbar);


        activityMainToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.profile:
                        Intent intent = new Intent(MainActivity.this, CurrentUserProfileActivity.class);
                        startActivity(intent);
                        break;
                    case R.id.logout:
                        FirebaseAuth.getInstance().signOut();
                        Intent phoneAuthIntent = new Intent(MainActivity.this, PhoneAuthActivity.class);
                        startActivity(phoneAuthIntent);
                        finish();
                        break;
                }


                return false;
            }
        });

    }

    private void setupRecyclerView() {
        chatListRecyclerView = findViewById(R.id.chatListRecyclerView);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        chatListRecyclerView.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(chatListRecyclerView.getContext(),
                layoutManager.getOrientation());
        chatListRecyclerView.addItemDecoration(dividerItemDecoration);
    }

    private void setupProgressBar() {
        maProgressBar = findViewById(R.id.maProgressBar);
        Sprite circle = new Circle();
        circle.setColor(Color.BLUE);
        maProgressBar.setIndeterminateDrawable(circle);
    }

    public void openChatWithSelectedUser(String UID, String number) {
        Intent chatIntent = new Intent(MainActivity.this, ChatActivity.class);
        chatIntent.putExtra("UserID", UID);
        chatIntent.putExtra("UserNumber", number);
        startActivity(chatIntent);

    }

    private void listenForChatsInRealTime() {
        if (!isListeningForChats) {
            firebaseUtils.listenForChatsInRealTime(new ChatCallBack() {
                @Override
                public void onComplete(String chatID, List<Message> messageList, List<Chat> chatList) {
                    if (chatList != null) {
                        if (adapter == null) {
                            chats = chatList;
                            adapter = new ChatListAdapter(MainActivity.this, chatList, isChatLoadingForFirstTime);
                            chatListRecyclerView.setAdapter(adapter);
                            maProgressBar.setVisibility(View.GONE);
                            findViewById(R.id.rlChatsLayout).setVisibility(View.VISIBLE);
                            findViewById(R.id.rlNoChatsLayout).setVisibility(View.GONE);
                        } else {
                            adapter.updateChatList(chatList);
                            chats = chatList;
                        }
                    } else {
                        maProgressBar.setVisibility(View.GONE);
                        findViewById(R.id.rlChatsLayout).setVisibility(View.GONE);
                        findViewById(R.id.rlNoChatsLayout).setVisibility(View.VISIBLE);
                        isChatLoadingForFirstTime = false;
                    }
                }
            });
            isListeningForChats = true;
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


}