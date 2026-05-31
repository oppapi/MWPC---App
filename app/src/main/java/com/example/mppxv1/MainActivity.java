package com.example.mppxv1;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPhone, etPassword, etConfirmPassword;
    private MaterialButton btnRegister;
    private TextView tvLoginLink;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Register button logic
        if (btnRegister != null) {
            btnRegister.setOnClickListener(v -> {
                String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
                String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
                String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
                String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

                if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    Toast.makeText(MainActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null) {
                                    sendEmailVerification(user, name, email, phone);
                                }
                            } else {
                                String errorCode = "";
                                if (task.getException() != null) {
                                    errorCode = task.getException().getClass().getSimpleName();
                                }

                                switch (errorCode) {
                                    case "FirebaseAuthUserCollisionException":
                                        Toast.makeText(MainActivity.this, "Email already in use", Toast.LENGTH_SHORT).show();
                                        break;
                                    case "FirebaseAuthInvalidCredentialsException":
                                        Toast.makeText(MainActivity.this, "Invalid email format", Toast.LENGTH_SHORT).show();
                                        break;
                                    case "FirebaseAuthWeakPasswordException":
                                        Toast.makeText(MainActivity.this, "Password too weak", Toast.LENGTH_SHORT).show();
                                        break;
                                    case "FirebaseNetworkException":
                                        Toast.makeText(MainActivity.this, "No internet connection", Toast.LENGTH_SHORT).show();
                                        break;
                                    default:
                                        Toast.makeText(MainActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
                                        break;
                                }
                            }
                        });
            });
        }

        // Login link logic
        if (tvLoginLink != null) {
            tvLoginLink.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, Login.class);
                startActivity(intent);
            });
        }
    }

    private void sendEmailVerification(FirebaseUser user, String name, String email, String phone) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "Verification email sent. Please check your inbox.", Toast.LENGTH_LONG).show();
                        saveUserToFirestore(user.getUid(), name, email, phone);
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToFirestore(String userId, String name, String email, String phone) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("phone", phone);
        user.put("isVerified", false);

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    mAuth.signOut(); // Sign out until verified
                    startActivity(new Intent(MainActivity.this, Login.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Error saving data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
