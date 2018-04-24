package com.shahar.userauthentication;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.firebase.client.Firebase;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MAIN_ACTIVITY";
    private static final int RC_SIGN_IN = 1;
    private GoogleApiClient mGoogleSignInClient;
    private CallbackManager mFacebookCallbackManager;
    private EditText mEmail;
    private EditText mPassword;
    private SignInButton mGoogleSignInButton;
    private LoginButton mFacebookSignInButton;
    private Button mSkipButton;
    private FirebaseRemoteConfig mConfig;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private int visibility;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e(TAG, "onCreate() >>");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();
        mSkipButton = (Button)findViewById(R.id.skipButton);
        mSkipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               updateUI();
            }
        });

        Log.w(TAG, "Google sign in failed");
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() != null) {
                    finish();
                    startActivity(new Intent(MainActivity.this,HomeScreenActivity.class ));
                }
            }
        };
        
        remoteConfigInit();
        facebookLoginInit();
        googleSigninInit();
        firebaseAuthenticationInit();

        Log.e(TAG, "onCreate() <<");
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                // ...
            }
        }
        else{
            mFacebookCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void updateUI() {
        finish();
        startActivity(new Intent(MainActivity.this,HomeScreenActivity.class ));
    }

    private void firebaseAuthenticationInit() {
        Log.e(TAG, "firebaseAuthenticationInit() >>");

        mEmail = findViewById(R.id.etEmail);
        mPassword = findViewById(R.id.etPassword);

        Log.e(TAG, "firebaseAuthenticationInit() <<");
    }

    public void onEmailPasswordAuthClick(View V) {

        Log.e(TAG, "onEmailPasswordAuthClick() >>");

        String email = mEmail.getText().toString();
        String pass = mPassword.getText().toString();

        if(validateEmailAndPassword(email, pass)) {

            Task<AuthResult> authResult;

            switch (V.getId()) {
                case R.id.signInButton:
                    //Email / Password sign-in
                    authResult = mAuth.signInWithEmailAndPassword(email, pass);
                    break;
                case R.id.signUpButton:
                    //Email / Password sign-up
                    authResult = mAuth.createUserWithEmailAndPassword(email, pass);
                    break;
                default:
                    return;
            }
            authResult.addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {

                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {

                    Log.e(TAG, "Email/Pass Auth: onComplete() >> " + task.isSuccessful());
                   updateUI();
                    Log.e(TAG, "Email/Pass Auth: onComplete() <<");
                }
            });
        }
        Log.e(TAG, "onEmailPasswordAuthClick() <<");

    }

    private boolean validateEmailAndPassword(String email, String pass) {
        Log.e(TAG, "validateEmailAndPassword() >>");

        boolean isValid = true;


        if(isValid && email.isEmpty())
        {
            DisplayMessage("Please fill your email address.");
            isValid =  false;
        }
        if(isValid && !VerifyEmailAddress(email))
        {
            DisplayMessage("Please fill your email address.");
            isValid =  false;
        }
        if(isValid && pass.isEmpty())
        {
            DisplayMessage("Please fill your password.");
            isValid =  false;
        }
        if(isValid && (pass.length() < 6))
        {
            DisplayMessage("Your password needs to contain at least 6 characters.");
            isValid =  false;
        }

        Log.e(TAG, "CheckIfValidAndSubmit() >>");

        return isValid;
    }

    private void DisplayMessage(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private boolean VerifyEmailAddress(String email) {

        String regExpn = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

        if (email == null) return false;

        Pattern pattern = Pattern.compile(regExpn,Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(email);

        return (matcher.matches());
    }


    private void remoteConfigInit() {

        HashMap<String, Object> defaults = new HashMap<>();
        defaults.put("allow_annoymous_user", false);
        mConfig = FirebaseRemoteConfig.getInstance();
        mConfig.setDefaults(defaults);
        mConfig.fetch(0).addOnCompleteListener(
                this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.e(TAG, "onComplete() Remote Config fetch isSuccessful=>>" + task.isSuccessful());
                        if(task.isSuccessful())
                         mConfig.activateFetched();
                    }
                });
        int visibility =  mConfig.getBoolean("allow_annoymous_user") ? View.VISIBLE : View.GONE;
        mSkipButton.setVisibility(visibility);
    }

    private void facebookLoginInit() {
        Log.e(TAG, "facebookLoginInit() >>");
        mFacebookCallbackManager = CallbackManager.Factory.create();
        mFacebookSignInButton = findViewById(R.id.loginFacebookButton);
        mFacebookSignInButton.setReadPermissions("email", "public_profile");
        mFacebookSignInButton.registerCallback(mFacebookCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.e(TAG, "facebook:onSuccess () >>" + loginResult);
                handleFacebookAccessToken(loginResult.getAccessToken());
                Log.e(TAG, "facebook:onSuccess () <<");
            }

            @Override
            public void onCancel() {
                Log.e(TAG, "facebook:onCancel() >>");
                //
                Log.e(TAG, "facebook:onCancel() <<");
            }

            @Override
            public void onError(FacebookException error) {
                Log.e(TAG, "facebook:onError () >>" + error.getMessage());
                //
                Log.e(TAG, "facebook:onError <<");
            }
        });

        Log.e(TAG, "facebookLoginInit() <<");
    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.e(TAG, "handleFacebookAccessToken () >>" + token.getToken());

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.e(TAG, "Facebook: onComplete() >> " + task.isSuccessful());
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            updateUI();
                        }
                        else{
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();

                        }
                       // goto next
                        Log.e(TAG, "Facebook: onComplete() <<");
                    }
                });

        Log.e(TAG, "handleFacebookAccessToken () <<");

    }

    private void googleSigninInit(){
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = new GoogleApiClient.Builder(getApplicationContext())
                .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Toast.makeText(MainActivity.this,"An error has occurred while singing in google account", Toast.LENGTH_LONG)
                                .show();
                    }
                }).addApi(Auth.GOOGLE_SIGN_IN_API,gso)
                .build();
        mGoogleSignInButton = (SignInButton)findViewById(R.id.loginGoogleButton);
        mGoogleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                googleSignIn();
            }
        });
    }

    private void googleSignIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleSignInClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Log.d(TAG, "signInWithCredential:success");
                            updateUI();
                        } else {
                            // If sign in fails, display a message to the user.
                            //     Log.w(TAG, "signInWithCredential:failure", task.getException());
                            //     Snackbar.make(findViewById(R.id.main_layout), "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
                            //   updat eUI(null);
                        }

                        // ...
                    }
                });
    }

}
