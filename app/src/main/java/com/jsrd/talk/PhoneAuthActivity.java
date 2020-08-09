package com.jsrd.talk;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.FadingCircle;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_auth);

        mAuth = FirebaseAuth.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();


        numberEditTxt = findViewById(R.id.numberEditText);
        otpDescTxt = findViewById(R.id.otpDescriptionTxt);
        otpTextView = findViewById(R.id.otp_view);
        viewAnimator = findViewById(R.id.phoneAuthViewAnimator);
        progressBarOtpLayout = findViewById(R.id.progressBarOtpLayout);


        Sprite fadingCircle = new FadingCircle();
        fadingCircle.setColor(Color.BLUE);
        progressBarOtpLayout.setIndeterminateDrawable(fadingCircle);

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
    }

    @Override
    public void onBackPressed() {
        if (viewAnimator.getDisplayedChild() == 1) {
            viewAnimator.showPrevious();
        } else {
            super.onBackPressed();
        }
    }

    private void checkUserSignIn() {
        if (user != null) {
            Intent mainActivity = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(mainActivity);
            finish();
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
                    if (user != null) {
                        FirebaseUtils ff = new FirebaseUtils(PhoneAuthActivity.this);
                        ff.putUserInfoOnFirestore(user);
                    }
                    progressBarOtpLayout.setVisibility(View.GONE);
                    checkUserSignIn();

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
}