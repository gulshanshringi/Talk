package com.jsrd.talk.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.jsrd.talk.R;
import com.jsrd.talk.utils.FirebaseUtils;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

public class CurrentUserProfileActivity extends AppCompatActivity {

    private ImageView ivUserImage;
    private EditText etUserName;
    private FirebaseUser user;
    private Toolbar toolbar;
    private ProgressBar pbUserDetails;
    private Button btnSave;
    private Uri mImageUri = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_user_profile);

        user = FirebaseAuth.getInstance().getCurrentUser();

        ivUserImage = findViewById(R.id.ivUserImage);
        etUserName = findViewById(R.id.etUserName);
        toolbar = findViewById(R.id.toolbarCurrentUserProfileActivity);
        pbUserDetails = findViewById(R.id.pbUserDetails);
        btnSave = findViewById(R.id.btnSave);


        toolbar.setTitle("Profile");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setUserNameAndImageIfAvailable();

        ivUserImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getCropedImage();
            }
        });


        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mImageUri != null) {
                    uploadImageToFirestore(mImageUri);
                } else {
                    saveProfileDetailsToFirebase(null);
                }
            }
        });

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void setUserNameAndImageIfAvailable() {
        if (user != null) {
            if (user.getDisplayName() != null) {
                etUserName.setText(user.getDisplayName());
            }
            Uri imageUri = user.getPhotoUrl();
            if (imageUri != null) {
                setUserProfilePic(imageUri);
            }
        }
    }

    public void getCropedImage() {
        // start picker to get image for cropping and then use the image in cropping activity
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(4, 3)
                .start(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                mImageUri = resultUri;
                setUserProfilePic(resultUri);
                //  uploadImageToFirestore(resultUri);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    private void setUserProfilePic(Uri uri) {
        Glide.with(CurrentUserProfileActivity.this)
                .load(uri)
                .placeholder(R.drawable.ic_user_icon)
                .into(ivUserImage);
    }

    private String getFileExtension(Uri uri) {
        ContentResolver cr = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();

        return mime.getExtensionFromMimeType(cr.getType(uri));
    }

    private void uploadImageToFirestore(Uri imageUri) {
        pbUserDetails.setVisibility(View.VISIBLE);
        ivUserImage.setEnabled(false);
        btnSave.setEnabled(false);
        StorageReference storageReference = FirebaseStorage.getInstance().getReference("ProfileImage");
        StorageReference fileReference = storageReference.child(System.currentTimeMillis() + "." + getFileExtension(imageUri));

        UploadTask uploadTask = fileReference.putFile(imageUri);

        uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if (task.isSuccessful()) {
                    fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            saveProfileDetailsToFirebase(uri);
                        }
                    });
                }
            }
        });
    }

    private void saveProfileDetailsToFirebase(Uri uri) {
        if (user != null) {
            String name = etUserName.getText().toString().trim();
            UserProfileChangeRequest profileUpdates = null;
            if (name.length() > 0) {
                if (uri != null) {
                    profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .setPhotoUri(uri)
                            .build();
                } else {
                    if (!name.equals(user.getDisplayName())) {
                        profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build();
                    } else {
                          onBackPressed();
                    }
                }
                if (profileUpdates != null) {

                    user.updateProfile(profileUpdates)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        if (user != null) {
                                            FirebaseUtils ff = new FirebaseUtils(CurrentUserProfileActivity.this);
                                            ff.putUserInfoOnFirestore(user);
                                            pbUserDetails.setVisibility(View.GONE);
                                            Toast.makeText(CurrentUserProfileActivity.this, "User Profile updated", Toast.LENGTH_SHORT).show();
                                            onBackPressed();
                                        }
                                    }
                                }
                            });
                }else {
                    onBackPressed();
                }
            } else {
                Toast.makeText(this, "Name can not be empty", Toast.LENGTH_SHORT).show();
            }
        }

    }
}