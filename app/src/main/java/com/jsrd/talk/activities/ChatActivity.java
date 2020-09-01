package com.jsrd.talk.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.Circle;
import com.jsrd.talk.adapters.ChatAdapter;
import com.jsrd.talk.R;
import com.jsrd.talk.interfaces.ChatCallBack;
import com.jsrd.talk.interfaces.ImageUploadCallBack;
import com.jsrd.talk.interfaces.StatusCallBack;
import com.jsrd.talk.model.Chat;
import com.jsrd.talk.model.Message;
import com.jsrd.talk.notification.APIService;
import com.jsrd.talk.notification.Client;
import com.jsrd.talk.notification.Data;
import com.jsrd.talk.notification.MyResponse;
import com.jsrd.talk.notification.Sender;
import com.jsrd.talk.utils.FirebaseUtils;
import com.jsrd.talk.utils.ImageResizer;
import com.jsrd.talk.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;

import static com.jsrd.talk.utils.Utils.getCurrentDateAndTime;
import static com.jsrd.talk.utils.Utils.getDate;
import static com.jsrd.talk.utils.Utils.getNameByNumber;
import static com.jsrd.talk.utils.Utils.isToday;

public class ChatActivity extends AppCompatActivity {

    private final String TAG = "ChatActivity";
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
    private List<Message> messages;
    private ChatAdapter adapter;

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
        chatingWith = receiversNumber;

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

        token += receiversUserID;

        currentUserNumber = firebaseUtils.getCurrentUserNumber();
        currentUserID = firebaseUtils.getCurrentUserUID();

        apiService = Client.getClient(FCM_API_URL).create(APIService.class);

        if (chat != null) {
            chatId = chat.getChatID();
            if (chat.getMessages() != null) {
                setChatData(chat.getMessages());
            }else {
                listenForMessagesInRealTime(chatId);
            }
        } else if (chatId != null) {
            listenForMessagesInRealTime(chatId);
        }

        listenForStatusInRealTime(receiversUserID);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        chatingWith = null;
        finish();
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
        adapter = new ChatAdapter(ChatActivity.this, messageList, currentUserID, chatId);
        chatListRecyclerView.setAdapter(adapter);
        caProgressBar.setVisibility(View.GONE);
        chatListRecyclerView.smoothScrollToPosition(messageList.size() - 1);
        if (chatId != null) {
            listenForMessagesInRealTime(chatId);
        }

    }

    public void sendMessage(View view) {
        final String message = "Message-" + etMsg.getText().toString();
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

    private void sendMedia(String url, String type) {
        String message = type + "-" + url;
        if (message.length() > 0) {
            Log.i(TAG, "Sending media URL with message");
            firebaseUtils.sendMessage(receiversNumber, currentUserNumber, message, chatId, new ChatCallBack() {
                @Override
                public void onComplete(String chatID, List<Message> messageList, List<Chat> chatList) {
                    if (chatID != null) {
                        Log.i(TAG, "media sent succesfully");
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
//                        if (isSetupComplete && !sender.equals(currentUserUID) && messages.size() < messageList.size()) {
//                            try {
//                                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//                                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
//                                r.play();
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
//                        }
                        isSetupComplete = true;
                        messages = messageList;
                    }
                }
            });
        }
    }

    public void accessImagesAndVideos(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select File"), 101);


//        Intent gallary = new Intent();
//        gallary.setAction(Intent.ACTION_PICK);
//        gallary.setType("image/* video/*");
//        gallary.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivityForResult(gallary, 101);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            Log.i(TAG, "Image Selected From Gallary");
            String msg = "upload-" + selectedImage.toString();
            String userID = firebaseUtils.getCurrentUserUID();
            String dateTime = Utils.getCurrentDateAndTime();
            Message message = new Message(msg, userID, dateTime);
            if (adapter != null) {
                messages.add(message);
                adapter.updateMessages(messages);
                chatListRecyclerView.smoothScrollToPosition(messages.size() - 1);
            } else {
                messages = new ArrayList<>();
                messages.add(message);
                setChatData(messages);
            }
            try {
                Bitmap fullSizeImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                uploadImageToFirebase(fullSizeImage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void uploadImageToFirebase(Bitmap fullSizeImage) {

        Bitmap reducedImage = ImageResizer.reduceBitmapSize(fullSizeImage, 360000);
        Uri imageUri = getImageUri(reducedImage);

        firebaseUtils.uploadImageToFirestore(imageUri, new ImageUploadCallBack() {
            @Override
            public void onImageUpload(Uri uri) {
                Log.i(TAG, "Image download Uri received");
                sendMedia(uri.toString(), "Image");
            }
        });
    }

    public Uri getImageUri(Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(this.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }
}