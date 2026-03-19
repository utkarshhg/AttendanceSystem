package com.attendance.facerecognition.ui.professor;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.attendance.facerecognition.R;
import com.attendance.facerecognition.ui.FaceRegistrationActivity;

public class ProfessorDashboardFragment extends Fragment {

    private Spinner spinnerSubject, spinnerBranch;
    private Button btnTakeAttendance, btnLogout, btnViewHistory, btnExport;
    private TextView tvWelcome;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Layout attach kar rahe hain
        View view = inflater.inflate(R.layout.fragment_professor_dashboard, container, false);

        // Views initialize karein
        initViews(view);

        // Spinners mein data bharna
        setupSpinners();

        // Click Listeners
        setupListeners();

        return view;
    }

    private void initViews(View view) {
        spinnerSubject = view.findViewById(R.id.spinnerSubject);
        spinnerBranch = view.findViewById(R.id.spinnerBranch);
        btnTakeAttendance = view.findViewById(R.id.btnTakeAttendance);
        btnLogout = view.findViewById(R.id.btnLogoutProf);
        btnViewHistory = view.findViewById(R.id.btnViewHistory);
        btnExport = view.findViewById(R.id.btnExportReport);
        tvWelcome = view.findViewById(R.id.tvProfWelcome);
    }

    private void setupSpinners() {
        // Subject List
        String[] subjects = {"Mathematics", "Physics", "Computer Science", "Software Engineering"};
        ArrayAdapter<String> subAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, subjects);
        subAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubject.setAdapter(subAdapter);

        // Branch List
        String[] branches = {"CS-A", "CS-B", "IT-A", "Mechanical", "Civil"};
        ArrayAdapter<String> branchAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, branches);
        branchAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBranch.setAdapter(branchAdapter);
    }

    private void setupListeners() {
        // Take Attendance Button Click
        btnTakeAttendance.setOnClickListener(v -> {
            String selectedSub = spinnerSubject.getSelectedItem().toString();
            String selectedBranch = spinnerBranch.getSelectedItem().toString();

            Toast.makeText(getContext(), "Starting attendance for " + selectedSub, Toast.LENGTH_SHORT).show();

            // Yahan se hum Camera/ML screen par ja sakte hain
            // Intent intent = new Intent(getActivity(), YourMLActivity.class);
            // startActivity(intent);
        });

        // Logout Button
        btnLogout.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed(); // Wapas login par jane ke liye
            }
        });

        // History aur Export ke liye basic toasts
        btnViewHistory.setOnClickListener(v -> Toast.makeText(getContext(), "Opening History...", Toast.LENGTH_SHORT).show());
        btnExport.setOnClickListener(v -> Toast.makeText(getContext(), "Generating Report...", Toast.LENGTH_SHORT).show());
    }
}