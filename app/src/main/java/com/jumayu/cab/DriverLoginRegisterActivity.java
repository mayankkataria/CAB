package com.jumayu.cab;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.sql.Driver;

public class DriverLoginRegisterActivity extends AppCompatActivity {

    TextView DriverStatus;
    TextView DriverRegisterLink;
    Button DriverLoginButton;
    Button DriverRegisterButton;
    EditText EmailDriver;
    EditText PasswordDriver;
    FirebaseAuth mAuth;
    DatabaseReference DriverDatabaseRef;
    String onlineDriverId;
    ProgressDialog loadingBar;
    DriverMapActivity dma;

    public void findViews(){
        DriverStatus = findViewById(R.id.driver_status);
        DriverRegisterButton = findViewById(R.id.driver_register_btn);
        DriverRegisterLink = findViewById(R.id.driver_register_link);
        DriverLoginButton = findViewById(R.id.driver_login_btn);
        EmailDriver = findViewById(R.id.email_driver);
        PasswordDriver = findViewById(R.id.password_driver);
        dma = new DriverMapActivity();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login_register);
        findViews();
        DriverRegisterButton.setVisibility(View.INVISIBLE);
        DriverRegisterButton.setEnabled(false);
        mAuth=FirebaseAuth.getInstance();

        loadingBar = new ProgressDialog(this);

        DriverRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DriverStatus.setText("Driver Registration");
                DriverLoginButton.setVisibility(View.INVISIBLE);
                DriverRegisterLink.setVisibility(View.INVISIBLE);
                DriverRegisterButton.setVisibility(View.VISIBLE);
                DriverRegisterButton.setEnabled(true);
            }
        });

        DriverRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = EmailDriver.getText().toString();
                String password = PasswordDriver.getText().toString();
                RegisterDriver(email, password);
            }
        });

        DriverLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = EmailDriver.getText().toString();
                String password = PasswordDriver.getText().toString();
                SignInDriver(email, password);
            }
        });
    }

    private void SignInDriver(String email, String password) {

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(DriverLoginRegisterActivity.this, "Please enter email...", Toast.LENGTH_SHORT).show();
        }

        else if(TextUtils.isEmpty(password)) {
            Toast.makeText(DriverLoginRegisterActivity.this, "Please enter password...", Toast.LENGTH_SHORT).show();
        }

        else {
            loadingBar.setTitle("Driver Login");
            loadingBar.setMessage("Logging in...");
            loadingBar.show();
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            if(task.isSuccessful()) {
                                onlineDriverId=mAuth.getCurrentUser().getUid();
                                DriverDatabaseRef= FirebaseDatabase.getInstance().getReference().child("User").child("Driver").child(onlineDriverId);
                                DriverDatabaseRef.setValue(true);
                                Intent driverIntent = new Intent(DriverLoginRegisterActivity.this, DriverMapActivity.class);
                                startActivity(driverIntent);

                                Toast.makeText(DriverLoginRegisterActivity.this, "Login successful...", Toast.LENGTH_SHORT).show();
                                loadingBar.dismiss();
                            }
                            else {
                                Toast.makeText(DriverLoginRegisterActivity.this, "Login unsuccessful...", Toast.LENGTH_SHORT).show();
                                loadingBar.dismiss();
                            }
                        }
                    });
        }
    }

    private void RegisterDriver(String email, String password) {

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(DriverLoginRegisterActivity.this, "Please enter email...", Toast.LENGTH_SHORT).show();
        }

        else if(TextUtils.isEmpty(password)) {
            Toast.makeText(DriverLoginRegisterActivity.this, "Please enter password...", Toast.LENGTH_SHORT).show();
        }

        else {
            loadingBar.setTitle("Driver Registration");
            loadingBar.setMessage("Please wait while we are registering your data...");
            loadingBar.show();
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            if(task.isSuccessful()) {
                                loadingBar.dismiss();
                                Intent intent = getIntent();  /*This will recreate the current activity. */
                                finish();                     //recreate() can also be used to restart activity if api version > 11
                                startActivity(intent);      //Further info: https://stackoverflow.com/questions/1397361/how-do-i-restart-an-android-activity
                                Toast.makeText(DriverLoginRegisterActivity.this, "Registration successful...", Toast.LENGTH_SHORT).show();
                            }
                            else {
                                Toast.makeText(DriverLoginRegisterActivity.this, "Registration unsuccessful...", Toast.LENGTH_SHORT).show();
                                loadingBar.dismiss();
                            }
                        }
                    });
        }
    }
}
