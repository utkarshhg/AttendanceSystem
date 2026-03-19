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
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });

        // --- UPDATED REGISTRATION LOGIC START ---
        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FaceRegistrationActivity.class);

                // Check karein ki kounsa role selected hai
                int selectedId = roleGroup.getCheckedRadioButtonId();

                if (selectedId == R.id.radioProfessor) {
                    // Agar Professor radio button select hai
                    intent.putExtra("user_role", "professor");
                } else {
                    // Agar Student radio button select hai ya kuch select nahi hai
                    intent.putExtra("user_role", "student");
                }

                startActivity(intent);
            }
        });
        // --- UPDATED REGISTRATION LOGIC END ---
    }

    private void performLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter all details", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedId = roleGroup.getCheckedRadioButtonId();

        if (selectedId == R.id.radioProfessor) {
            loadFragment(new ProfessorDashboardFragment());
            Toast.makeText(this, "Welcome Professor!", Toast.LENGTH_SHORT).show();
        } else if (selectedId == R.id.radioStudent) {
            loadFragment(new StudentDashboardFragment());
            Toast.makeText(this, "Welcome Student!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(android.R.id.content, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
}