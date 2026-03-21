package com.attendance.facerecognition.ui;
import com.attendance.facerecognition.database.FirebaseManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.attendance.facerecognition.R;
import com.attendance.facerecognition.ui.professor.ProfessorDashboardFragment;
import com.attendance.facerecognition.ui.student.StudentDashboardFragment;

public class MainActivity extends AppCompatActivity {

    private RadioGroup roleGroup;
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupListeners();
    }

    private void initViews() {
        roleGroup = findViewById(R.id.roleGroup);
        // Using Email field to accept Roll Number as well for students
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> performLogin());

        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            int selectedId = roleGroup.getCheckedRadioButtonId();

            if (selectedId == R.id.radioProfessor) {
                intent.putExtra("user_role", "professor");
            } else {
                intent.putExtra("user_role", "student");
            }
            startActivity(intent);
        });
    }

    private void performLogin() {
        String userId = etEmail.getText().toString().trim().toUpperCase();
        String password = etPassword.getText().toString().trim();

        if (userId.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Clean user ID if they used an email format
        if (userId.contains("@")) userId = userId.split("@")[0];

        FirebaseManager db = new FirebaseManager();
        int selectedId = roleGroup.getCheckedRadioButtonId();

        if (selectedId == R.id.radioStudent) {
            // ACTUAL SECURITY CHECK
            // Inside performLogin()
            String finalUserId = userId;
            db.verifyStudentLogin(userId, password, new FirebaseManager.OnLoginResult() {
                @Override
                public void onSuccess() { // Make sure this is "onSuccess" (no parameters)
                    StudentDashboardFragment studentFrag = new StudentDashboardFragment();
                    Bundle args = new Bundle();
                    args.putString("STUDENT_ID", finalUserId);
                    studentFrag.setArguments(args);
                    loadFragment(studentFrag);
                }

                @Override
                public void onError(String error) { // Make sure this matches "onError"
                    Toast.makeText(MainActivity.this, "Login Failed: " + error, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // Handle Professor login similarly or keep it simple for now
            Toast.makeText(this, "Professor login coming soon!", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadFragment(Fragment fragment) {
        // Hide the login UI elements so the fragment takes over the whole screen cleanly
        findViewById(R.id.roleGroup).setVisibility(View.GONE);
        findViewById(R.id.etEmail).setVisibility(View.GONE);
        findViewById(R.id.etPassword).setVisibility(View.GONE);
        findViewById(R.id.btnLogin).setVisibility(View.GONE);
        findViewById(R.id.tvRegister).setVisibility(View.GONE);
  // Assumes you have a welcome title

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(android.R.id.content, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // If they click back from the dashboard, show the login UI again
        findViewById(R.id.roleGroup).setVisibility(View.VISIBLE);
        findViewById(R.id.etEmail).setVisibility(View.VISIBLE);
        findViewById(R.id.etPassword).setVisibility(View.VISIBLE);
        findViewById(R.id.btnLogin).setVisibility(View.VISIBLE);
        findViewById(R.id.tvRegister).setVisibility(View.VISIBLE);
    }
}