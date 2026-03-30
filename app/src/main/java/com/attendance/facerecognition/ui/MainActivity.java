package com.attendance.facerecognition.ui;

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
import com.attendance.facerecognition.database.FirebaseManager;
import com.attendance.facerecognition.ui.professor.ProfessorDashboardFragment;
import com.attendance.facerecognition.ui.student.StudentDashboardFragment;

public class MainActivity extends AppCompatActivity {

    private RadioGroup roleGroup;
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private FirebaseManager db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new FirebaseManager();
        initViews();
        setupListeners();
    }

    private void initViews() {
        roleGroup = findViewById(R.id.roleGroup);
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

        int selectedId = roleGroup.getCheckedRadioButtonId();
        final String finalUserId = userId;

        if (selectedId == R.id.radioStudent) {
            // STUDENT LOGIN
            db.verifyStudentLogin(userId, password, new FirebaseManager.OnLoginResult() {
                @Override
                public void onSuccess() {
                    StudentDashboardFragment studentFrag = new StudentDashboardFragment();
                    Bundle args = new Bundle();
                    args.putString("STUDENT_ID", finalUserId);
                    studentFrag.setArguments(args);
                    loadFragment(studentFrag);
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(MainActivity.this, "Student Login Failed: " + error, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // PROFESSOR LOGIN
            db.verifyProfessorLogin(userId, password, new FirebaseManager.OnLoginResult() {
                @Override
                public void onSuccess() {
                    ProfessorDashboardFragment profFrag = new ProfessorDashboardFragment();
                    Bundle args = new Bundle();
                    args.putString("PROF_ID", finalUserId);
                    profFrag.setArguments(args);
                    loadFragment(profFrag);
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(MainActivity.this, "Professor Login Failed: " + error, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void loadFragment(Fragment fragment) {
        // Hide the login UI elements
        roleGroup.setVisibility(View.GONE);
        etEmail.setVisibility(View.GONE);
        etPassword.setVisibility(View.GONE);
        btnLogin.setVisibility(View.GONE);
        tvRegister.setVisibility(View.GONE);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        // Replacing the content of the entire activity with the fragment
        fragmentTransaction.replace(android.R.id.content, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            // Show the login UI again when returning from a fragment
            roleGroup.setVisibility(View.VISIBLE);
            etEmail.setVisibility(View.VISIBLE);
            etPassword.setVisibility(View.VISIBLE);
            btnLogin.setVisibility(View.VISIBLE);
            tvRegister.setVisibility(View.VISIBLE);
        } else {
            super.onBackPressed();
        }
    }
}