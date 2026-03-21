package com.attendance.facerecognition.ui.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.attendance.facerecognition.R;
import com.attendance.facerecognition.database.FirebaseManager;

import java.util.List;

public class StudentDashboardFragment extends Fragment {

    // These names must be used exactly below
    private TextView tvStudentName, tvPercentage, tvTotalClasses, tvPresentClasses;
    private Button btnLogout;
    private String studentId;
    private FirebaseManager db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_dashboard, container, false);

        // Initialize UI - Matching your XML IDs exactly
        tvStudentName = view.findViewById(R.id.tvStudentName);
        tvPercentage = view.findViewById(R.id.tvPercentage);
        tvTotalClasses = view.findViewById(R.id.tvTotalClasses);
        tvPresentClasses = view.findViewById(R.id.tvPresentClasses);
        btnLogout = view.findViewById(R.id.btnLogoutStudent);
        btnLogout.setOnClickListener(v -> {
            if (getActivity() != null) {
                // This simulates the 'Back' button, which MainActivity handles
                // to show the login fields again.
                getActivity().onBackPressed();

                Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
            }
        });

        db = new FirebaseManager();

        // Retrieve the Student ID passed from Login
        if (getArguments() != null) {
            studentId = getArguments().getString("STUDENT_ID");
        }

        btnLogout.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        if (studentId != null) {
            fetchStudentData();
        }

        return view;
    }

    private void fetchStudentData() {
        db.getStudentProfile(studentId, new FirebaseManager.OnProfileRetrieved() {
            @Override
            public void onRetrieved(String name, String email, List<String> subjects) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        tvStudentName.setText("Welcome, " + name);
                        tvTotalClasses.setText("20");
                        tvPresentClasses.setText("18");
                        tvPercentage.setText("90%");
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }
}