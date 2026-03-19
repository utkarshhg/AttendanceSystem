package com.attendance.facerecognition.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.attendance.facerecognition.R;

public class FaceRegistrationActivity extends AppCompatActivity {

    private EditText etName, etRoll, etBranch, etPassword;
    private Button btnCaptureFace, btnRegister;
    private TextView tvTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_registration);

        // 1. Initialize Views
        etName = findViewById(R.id.etRegName);
        etRoll = findViewById(R.id.etRegRoll);
        etBranch = findViewById(R.id.etRegBranch);
        etPassword = findViewById(R.id.etRegPassword);
        btnCaptureFace = findViewById(R.id.btnCaptureFace);
        btnRegister = findViewById(R.id.btnFinalRegister);
        tvTitle = findViewById(R.id.tvRegTitle);

        // 2. Role Check karke UI hide/show karein
        checkUserRole();

        // 3. Click Listeners
        setupListeners();
    }

    private void checkUserRole() {
        // MainActivity se bheja gaya role receive karein
        String role = getIntent().getStringExtra("user_role");

        if (role != null && role.equals("professor")) {
            // Professor ke liye extra fields hide kar do
            etRoll.setVisibility(View.GONE);      // Roll Number gayab
            etBranch.setVisibility(View.GONE);    // Branch gayab
            btnCaptureFace.setVisibility(View.GONE); // Face capture gayab

            tvTitle.setText("Professor Registration");
        } else {
            // Student ke liye sab dikhao
            etRoll.setVisibility(View.VISIBLE);
            etBranch.setVisibility(View.VISIBLE);
            btnCaptureFace.setVisibility(View.VISIBLE);

            tvTitle.setText("Student Registration");
        }
    }

    private void setupListeners() {
        // Capture Face Button
        btnCaptureFace.setOnClickListener(v -> {
            Toast.makeText(this, "Opening Camera for Face Scan...", Toast.LENGTH_SHORT).show();
        });

        // Final Register Button
        btnRegister.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
            } else {
                // Future: Yahan Firebase Manager call hoga
                Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}