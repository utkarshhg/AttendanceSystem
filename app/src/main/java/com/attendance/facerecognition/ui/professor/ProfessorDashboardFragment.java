package com.attendance.facerecognition.ui.professor;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.attendance.facerecognition.R;

public class ProfessorDashboardFragment extends Fragment {
    private TextView welcomeText, classText;
    private Button markAttendanceBtn, viewReportsBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_professor_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupListeners();
        loadData();
    }

    private void initViews(View view) {
        welcomeText = view.findViewById(R.id.welcome_text);
        classText = view.findViewById(R.id.class_text);
        markAttendanceBtn = view.findViewById(R.id.mark_attendance_btn);
        viewReportsBtn = view.findViewById(R.id.view_reports_btn);
    }

    private void setupListeners() {
        markAttendanceBtn.setOnClickListener(v -> markAttendance());
        viewReportsBtn.setOnClickListener(v -> viewReports());
    }

    private void loadData() {
        welcomeText.setText("Welcome, Professor!");
        classText.setText("Class: Java Programming (30 students)");
    }

    private void markAttendance() {
        Intent intent = new Intent(getContext(), ProfessorDashboardFragment.class);
        startActivity(intent);
    }

    private void viewReports() {
        // TODO: Implement view reports
    }
}