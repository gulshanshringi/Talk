package com.jsrd.talk.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.bumptech.glide.Glide;
import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.FadingCircle;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.jsrd.talk.R;
import com.jsrd.talk.utils.FirebaseUtils;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.concurrent.TimeUnit;

import in.aabhasjindal.otptextview.OTPListener;
import in.aabhasjindal.otptextview.OtpTextView;

public class PhoneAuthActivity extends AppCompatActivity {


    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private EditText numberEditTxt;
    private TextView otpDescTxt;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken Token;
    private OtpTextView otpTextView;
    private ViewAnimator viewAnimator;
    private ProgressBar progressBarOtpLayout;

    private ImageView ivUserImage;
    private EditText etUserName;
    private Button btnSave;
    private ProgressBar pbUserDetails;
    private Uri mImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_auth);

        mAuth = FirebaseAuth.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        checkPermission();

        numberEditTxt = findViewById(R.id.numberEditText);
        otpDescTxt = findViewById(R.id.otpDescriptionTxt);
        otpTextView = findViewById(R.id.otp_view);
        viewAnimator = findViewById(R.id.phoneAuthViewAnimator);
        progressBarOtpLayout = findViewById(R.id.progressBarOtpLayout);
        ivUserImage = findViewById(R.id.ivUserImage);
        etUserName = findViewById(R.id.etUserName);
        btnSave = findViewById(R.id.btnSave);
        pbUserDetails = findViewById(R.id.pbUserDetails);


        Sprite fadingCircle = new FadingCircle();
        fadingCircle.setColor(Color.BLUE);
        progressBarOtpLayout.setIndeterminateDrawable(fadingCircle);
        pbUserDetails.setIndeterminateDrawable(fadingCircle);

        checkUserSignIn();

        numberEditTxt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().length() == 10) {
                    findViewById(R.id.generateOtpBtn).setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                    hideKeyboard();
                } else
                    findViewById(R.id.generateOtpBtn).setBackgroundColor(getResources().getColor(R.color.grey));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        otpTextView.setOtpListener(new OTPListener() {
            @Override
            public void onInteractionListener() {

            }

            @Override
            public void onOTPComplete(String otp) {
                findViewById(R.id.otpVerifyBtn).setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                hideKeyboard();
            }
        });


        ivUserImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getCropedImageAndUpload();
            }
        });


        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadImageToFirestore(mImageUri);

            }
        });

    }

    @Override
    public void onBackPressed() {
        if (viewAnimator.getDisplayedChild() >= 1) {
            viewAnimator.showPrevious();
        } else {
            super.onBackPressed();
        }
    }

    private void checkUserSignIn() {
        if (user != null) {
            if (user.getDisplayName() != null) {
                Intent mainActivity = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(mainActivity);
                finish();
            } else {
                FirebaseAuth.getInstance().signOut();
            }
        }
    }

    public void phoneNumberAuthentication(View v) {
        PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                String code = phoneAuthCredential.getSmsCode();
                // Toast.makeText(PhoneAuthActivity.this, "Succesful"+phoneAuthCredential.getSmsCode(), Toast.LENGTH_SHORT).show();
                //signInWithPhoneAuthCredintial(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Log.i("Fail authentication ", e.toString());
            }

            @Override
            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                mVerificationId = verificationId;
                Token = forceResendingToken;
                viewAnimator.showNext();
            }
        };

        String userNumber = "+91" + numberEditTxt.getText().toString().trim();

        if (userNumber != null) {
            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    userNumber,        // Phone number to verify
                    30,                 // Timeout duration
                    TimeUnit.SECONDS,   // Unit of timeout
                    this,               // Activity (for callback binding)
                    mCallbacks);        // OnVerificationStateChangedCallbacks
        }

        otpDescTxt.setText("Please type the verification code sent to " + userNumber);
    }

    public void verifyPhoneNumberWithCode(View v) {

        String code = otpTextView.getOTP();
        if (code.length() == 6) {
            progressBarOtpLayout.setVisibility(View.VISIBLE);
            PhoneAuthCredential phoneAuthCredential = PhoneAuthProvider.getCredential(mVerificationId, code);

            mAuth.signInWithCredential(phoneAuthCredential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    user = mAuth.getInstance().getCurrentUser();
                    progressBarOtpLayout.setVisibility(View.GONE);
                    viewAnimator.showNext();
                    setUserNameAndImageIfAvailable(user);

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressBarOtpLayout.setVisibility(View.GONE);
                    Toast.makeText(PhoneAuthActivity.this, "Please Enter valid OTP", Toast.LENGTH_SHORT).show();

                }
            });
        }
    }

    private void setUserNameAndImageIfAvailable(FirebaseUser user) {
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

    private void getCropedImageAndUpload() {
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
                setUserProfilePic(resultUri);


            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    private void setUserProfilePic(Uri uri) {
        mImageUri = uri;
        Glide.with(PhoneAuthActivity.this)
                .load(uri)
                .placeholder(R.drawable.ic_profile_icon)
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
                    profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build();
                }

                user.updateProfile(profileUpdates)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    pbUserDetails.setVisibility(View.GONE);
                                    if (user != null) {
                                        FirebaseUtils ff = new FirebaseUtils(PhoneAuthActivity.this);
                                        ff.putUserInfoOnFirestore(user);
                                    }
                                    checkUserSignIn();
                                }
                            }
                        });
            } else {
                Toast.makeText(this, "Name can not be empty", Toast.LENGTH_SHORT).show();
            }
        }

    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = this.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void checkPermission() {
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {
                android.Manifest.permission.READ_CONTACTS,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
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