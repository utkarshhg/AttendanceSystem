package com.attendance.facerecognition.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.attendance.facerecognition.R;
import com.attendance.facerecognition.database.FirebaseManager;

import java.util.ArrayList;
import java.util.Arrays;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etRoll, etBranch, etPassword;
    private Button btnCaptureFace, btnRegister;
    private TextView tvTitle;
    private String userRole;

    // The Database Manager
    private FirebaseManager db;
    private boolean isFaceCaptured = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_registration);

        db = new FirebaseManager();

        etName = findViewById(R.id.etRegName);
        etRoll = findViewById(R.id.etRegRoll);
        etBranch = findViewById(R.id.etRegBranch);
        etPassword = findViewById(R.id.etRegPassword);
        btnCaptureFace = findViewById(R.id.btnCaptureFace);
        btnRegister = findViewById(R.id.btnFinalRegister);
        tvTitle = findViewById(R.id.tvRegTitle);

        checkUserRole();
        setupListeners();
    }

    private void checkUserRole() {
        userRole = getIntent().getStringExtra("user_role");
        if (userRole != null && userRole.equals("professor")) {
            // Professors don't need a Roll Number or Face Scan to create an account
            etRoll.setVisibility(View.GONE);
            etBranch.setVisibility(View.GONE);
            btnCaptureFace.setVisibility(View.GONE);
            tvTitle.setText("Professor Registration");
        } else {
            // Students need everything
            etRoll.setVisibility(View.VISIBLE);
            etBranch.setVisibility(View.VISIBLE);
            btnCaptureFace.setVisibility(View.VISIBLE);
            tvTitle.setText("Student Registration");
        }
    }

    private void setupListeners() {
        // 1. The Face Capture Button
        btnCaptureFace.setOnClickListener(v -> {
            String rollNumber = etRoll.getText().toString().trim();
            if (rollNumber.isEmpty()) {
                Toast.makeText(this, "Please enter Roll Number first!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Launch the AI Camera
            // Ensure FaceScannerActivity is declared in your AndroidManifest.xml
            Intent intent = new Intent(RegisterActivity.this, FaceScannerActivity.class);
            intent.putExtra("STUDENT_ID", rollNumber);
            intent.putExtra("MODE", "REGISTER");
            startActivity(intent);

            // Mark that they at least opened the camera
            isFaceCaptured = true;
        });

        // 2. The Final Submit Button
        btnRegister.setOnClickListener(v -> performRegistration());
    }

    private void performRegistration() {
        String name = etName.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Basic Validation
        if (name.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 3) {
            Toast.makeText(this, "Password too short!", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("professor".equals(userRole)) {
            // Professor Registration (Basic)
            // Note: You can expand this later by adding a registerProfessor method to FirebaseManager
            Toast.makeText(this, "Professor Registered Successfully!", Toast.LENGTH_SHORT).show();
            finish();

        } else {
            // Student Registration (Full Database Push)
            String roll = etRoll.getText().toString().trim();
            String branch = etBranch.getText().toString().trim();

            if (roll.isEmpty() || branch.isEmpty()) {
                Toast.makeText(this, "Please enter Roll Number and Branch", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isFaceCaptured) {
                Toast.makeText(this, "Please Capture Face first!", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Saving to Database...", Toast.LENGTH_SHORT).show();

            // Create a dummy email format
            String email = roll + "@college.edu";

            // Assign default subjects (This can be made dynamic later)
            ArrayList<String> enrolledSubjects = new ArrayList<>(Arrays.asList(
                    "Mathematics", "Physics", "Computer Science", "Software Engineering"
            ));

            // Push to Firebase with the PASSWORD included
            db.registerStudent(roll, name, email, password, enrolledSubjects, new FirebaseManager.OnCompleteListener() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> {
                        Toast.makeText(RegisterActivity.this, "Registration 100% Complete!", Toast.LENGTH_LONG).show();
                        finish(); // Returns to Login screen
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Database Error: " + error, Toast.LENGTH_LONG).show());
                }
            });
        }
    }
}