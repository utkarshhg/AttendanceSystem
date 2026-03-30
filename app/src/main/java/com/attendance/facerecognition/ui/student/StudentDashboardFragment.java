package com.attendance.facerecognition.ui.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.attendance.facerecognition.R;
import com.attendance.facerecognition.database.FirebaseManager;

import java.util.List;
import java.util.Locale;

public class StudentDashboardFragment extends Fragment {

    private TextView tvStudentName, tvPercentage, tvTotalClasses, tvPresentClasses;
    private Spinner spinnerSubject;
    private Button btnLogout;

    private String studentId;
    private FirebaseManager db;
    private List<String> enrolledSubjects;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_dashboard, container, false);

        // Initialize UI
        tvStudentName = view.findViewById(R.id.tvStudentName);
        tvPercentage = view.findViewById(R.id.tvPercentage);
        tvTotalClasses = view.findViewById(R.id.tvTotalClasses);
        tvPresentClasses = view.findViewById(R.id.tvPresentClasses);
        spinnerSubject = view.findViewById(R.id.spinnerStudentSubject);
        btnLogout = view.findViewById(R.id.btnLogoutStudent);

        db = new FirebaseManager();

        // Retrieve the Student ID passed from MainActivity
        if (getArguments() != null) {
            studentId = getArguments().getString("STUDENT_ID");
        }

        btnLogout.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
                Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
            }
        });

        if (studentId != null) {
            fetchStudentProfile();
        }

        return view;
    }

    private void fetchStudentProfile() {
        db.getStudentProfile(studentId, new FirebaseManager.OnProfileRetrieved() {
            @Override
            public void onRetrieved(String name, String email, List<String> subjects) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        tvStudentName.setText("Welcome, " + name);
                        enrolledSubjects = subjects;
                        setupSubjectSpinner();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Error loading profile: " + error, Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void setupSubjectSpinner() {
        if (enrolledSubjects == null || enrolledSubjects.isEmpty()) return;

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, enrolledSubjects);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubject.setAdapter(adapter);

        // Listen for when the student changes the subject in the dropdown
        spinnerSubject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedSubject = enrolledSubjects.get(position);
                fetchAttendanceForSubject(selectedSubject);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void fetchAttendanceForSubject(String subjectId) {
        // Show loading state temporarily
        tvTotalClasses.setText("...");
        tvPresentClasses.setText("...");
        tvPercentage.setText("...");

        db.getStudentAttendance(studentId, subjectId, new FirebaseManager.OnAttendanceRetrieved() {
            @Override
            public void onRetrieved(FirebaseManager.AttendanceRecord record) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        tvTotalClasses.setText(String.valueOf(record.totalClasses));
                        tvPresentClasses.setText(String.valueOf(record.presentDays));

                        // Format the percentage beautifully (e.g., "85.5%")
                        tvPercentage.setText(String.format(Locale.US, "%.1f%%", record.attendance));
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Error fetching attendance", Toast.LENGTH_SHORT).show();
                        tvTotalClasses.setText("0");
                        tvPresentClasses.setText("0");
                        tvPercentage.setText("0.0%");
                    });
                }
            }
        });
    }
}