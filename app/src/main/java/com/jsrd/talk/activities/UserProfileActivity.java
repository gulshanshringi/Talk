package com.jsrd.talk.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.jsrd.talk.R;
import com.jsrd.talk.interfaces.ReceiverCallback;
import com.jsrd.talk.utils.FirebaseUtils;
import com.jsrd.talk.utils.Utils;

public class UserProfileActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private String userNumber;
    private ImageView ivProfilePic;
    private FirebaseUtils firebaseUtils;
    private TextView tvUserNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        userNumber = getIntent().getStringExtra("UserNumber");

        firebaseUtils = new FirebaseUtils(this);
        ivProfilePic = findViewById(R.id.ivUserImageUserProfileActivity);

        tvUserNumber = findViewById(R.id.tvUserNumber);

        setupToolbar();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public void goBackToChatActivity(View view) {
        onBackPressed();
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbarUserProfileActivity);
        toolbar.setTitle(Utils.getNameByNumber(this, userNumber));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setupUserProfilePicAndName(userNumber);
    }

    private void setupUserProfilePicAndName(String userNumber) {
        firebaseUtils.getReceiversProfilePic(userNumber, new ReceiverCallback() {
            @Override
            public void onComplete(String data) {
                if (data != null) {
                    Glide.with(UserProfileActivity.this).
                            load(data).
                            placeholder(R.drawable.ic_profile_icon).
                            into(ivProfilePic);
                    tvUserNumber.setText(userNumber);

                }
            }
        });
    }

}