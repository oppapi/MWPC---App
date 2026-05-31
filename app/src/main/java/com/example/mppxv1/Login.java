package com.example.mppxv1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class Login extends AppCompatActivity {

    private FirebaseAuth auth;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextView tvRegisterLink, tvForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
    }

    private void setupListeners() {
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (view, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        btnLogin.setOnClickListener(view -> {
            String email = Objects.requireNonNull(etEmail.getText()).toString().trim();
            String password = Objects.requireNonNull(etPassword.getText()).toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            loginUser(email, password);
        });

        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(view -> {
                String email = Objects.requireNonNull(etEmail.getText()).toString().trim();
                if (email.isEmpty()) {
                    Toast.makeText(this, "Please enter your email to reset password", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                auth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(this, "Reset link sent", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Email not found", Toast.LENGTH_SHORT).show();
                            }
                        });
            });
        }

        if (tvRegisterLink != null) {
            tvRegisterLink.setOnClickListener(view -> {
                startActivity(new Intent(Login.this, MainActivity.class));
                finish();
            });
        }
    }

    private void loginUser(String email, String password) {
        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in...");

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            if (user.isEmailVerified()) {
                                goToHome();
                            } else {
                                auth.signOut();
                                btnLogin.setEnabled(true);
                                btnLogin.setText("Login");
                                Toast.makeText(this, "Please verify your email first.", Toast.LENGTH_LONG).show();
                            }
                        }
                    } else {
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Login");
                        
                        String errorCode = "";
                        if (task.getException() != null) {
                            errorCode = task.getException().getClass().getSimpleName();
                        }
                        
                        switch (errorCode) {
                            case "FirebaseAuthInvalidUserException":
                                Toast.makeText(this, "Account not found", Toast.LENGTH_SHORT).show();
                                break;
                            case "FirebaseAuthInvalidCredentialsException":
                                Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
                                break;
                            case "FirebaseNetworkException":
                                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
                                break;
                            default:
                                Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                });
    }

    private void goToHome() {
        startActivity(new Intent(Login.this, HomeActivity.class));
        finish();
    }
}
