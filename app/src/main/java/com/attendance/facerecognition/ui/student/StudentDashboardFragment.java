package com.attendance.facerecognition.ui.student;

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

public class StudentDashboardFragment extends Fragment {
    private TextView welcomeText, attendanceText;
    private Button registerFaceBtn, viewAttendanceBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_dashboard, container, false);
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
        attendanceText = view.findViewById(R.id.attendance_text);
        registerFaceBtn = view.findViewById(R.id.register_face_btn);
        viewAttendanceBtn = view.findViewById(R.id.view_attendance_btn);
    }

    private void setupListeners() {
        registerFaceBtn.setOnClickListener(v -> registerFace());
        viewAttendanceBtn.setOnClickListener(v -> viewAttendance());
    }

    private void loadData() {
        welcomeText.setText("Welcome, Student!");
        attendanceText.setText("Attendance: 85.5%");
    }

    private void registerFace() {
        Intent intent = new Intent(getContext(), StudentDashboardFragment.class);
        startActivity(intent);
    }

    private void viewAttendance() {
        // TODO: Implement view attendance
    }
}