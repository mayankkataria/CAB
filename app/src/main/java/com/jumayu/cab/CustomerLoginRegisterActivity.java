package com.jumayu.cab;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
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

public class CustomerLoginRegisterActivity extends AppCompatActivity {

    TextView CustomerStatus;
    TextView CustomerRegisterLink;
    Button CustomerLoginButton;
    Button CustomerRegisterButton;
    EditText EmailCustomer;
    EditText PasswordCustomer;
    FirebaseAuth mAuth;
    ProgressDialog loadingBar;
    DatabaseReference CustomerDatabaseRef;
    String onlineCustomerId;

    public void findViews(){
        CustomerStatus = findViewById(R.id.customer_status);
        CustomerRegisterButton = findViewById(R.id.customer_register_btn);
        CustomerRegisterLink = findViewById(R.id.customer_register_link);
        CustomerLoginButton = findViewById(R.id.customer_login_btn);
        EmailCustomer = findViewById(R.id.email_customer);
        PasswordCustomer = findViewById(R.id.password_customer);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_login_register);
        findViews();
        CustomerRegisterButton.setVisibility(View.INVISIBLE);
        CustomerRegisterButton.setEnabled(false);
        mAuth=FirebaseAuth.getInstance();

        loadingBar = new ProgressDialog(this);

        CustomerRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CustomerStatus.setText("Customer Registration");
                CustomerLoginButton.setVisibility(View.INVISIBLE);
                CustomerRegisterLink.setVisibility(View.INVISIBLE);
                CustomerRegisterButton.setVisibility(View.VISIBLE);
                CustomerRegisterButton.setEnabled(true);
            }
        });

        CustomerRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = EmailCustomer.getText().toString();
                String password = PasswordCustomer.getText().toString();
                RegisterCustomer(email, password);
            }
        });

        CustomerLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = EmailCustomer.getText().toString();
                String password = PasswordCustomer.getText().toString();
                SignInCustomer(email, password);
            }
        });
    }

    private void SignInCustomer(String email, String password) {

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(CustomerLoginRegisterActivity.this, "Please enter email...", Toast.LENGTH_SHORT).show();
        }

        else if(TextUtils.isEmpty(password)) {
            Toast.makeText(CustomerLoginRegisterActivity.this, "Please enter password...", Toast.LENGTH_SHORT).show();
        }

        else {
            loadingBar.setTitle("Customer Login");
            loadingBar.setMessage("Logging in...");
            loadingBar.show();
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            if(task.isSuccessful()) {
                                onlineCustomerId=mAuth.getCurrentUser().getUid();
                                CustomerDatabaseRef= FirebaseDatabase.getInstance().getReference().child("User").child("Customers").child(onlineCustomerId);
                                CustomerDatabaseRef.setValue(true);
                                Intent customerIntent = new Intent(CustomerLoginRegisterActivity.this, CustomersMapActivity.class);
                                startActivity(customerIntent);
                                Toast.makeText(CustomerLoginRegisterActivity.this, "Login successful...", Toast.LENGTH_SHORT).show();
                                loadingBar.dismiss();
                            }
                            else {
                                Toast.makeText(CustomerLoginRegisterActivity.this, "Login unsuccessful...", Toast.LENGTH_SHORT).show();
                                loadingBar.dismiss();
                            }
                        }
                    });
        }
    }

    private void RegisterCustomer(String email, String password) {

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(CustomerLoginRegisterActivity.this, "Please enter email...", Toast.LENGTH_SHORT).show();
        }

        else if(TextUtils.isEmpty(password)) {
            Toast.makeText(CustomerLoginRegisterActivity.this, "Please enter password...", Toast.LENGTH_SHORT).show();
        }

        else {
            loadingBar.setTitle("Customer Registration");
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
                                Toast.makeText(CustomerLoginRegisterActivity.this, "Registration successful...", Toast.LENGTH_SHORT).show();
                            }
                            else {
                                Toast.makeText(CustomerLoginRegisterActivity.this, "Registration unsuccessful...", Toast.LENGTH_SHORT).show();
                                loadingBar.dismiss();
                            }
                        }
                    });
        }
    }
}
