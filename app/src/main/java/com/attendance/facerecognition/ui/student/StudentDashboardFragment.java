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

public class StudentDashboardFragment extends Fragment {

    private Spinner spinnerSubject;
    private TextView tvTotal, tvPresent, tvPercent, tvName, tvSelectedTitle;
    private Button btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Layout attach kar rahe hain
        View view = inflater.inflate(R.layout.fragment_student_dashboard, container, false);

        // Views initialize karein (IDs wahi hain jo humne XML mein di thi)
        tvName = view.findViewById(R.id.tvStudentName);
        spinnerSubject = view.findViewById(R.id.spinnerStudentSubject);
        tvTotal = view.findViewById(R.id.tvTotalClasses);
        tvPresent = view.findViewById(R.id.tvPresentClasses);
        tvPercent = view.findViewById(R.id.tvPercentage);
        tvSelectedTitle = view.findViewById(R.id.tvSelectedSubjectTitle);
        btnLogout = view.findViewById(R.id.btnLogoutStudent);

        // Spinner setup (Subjects ki list)
        setupSubjectSpinner();

        // Logout logic
        btnLogout.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        return view;
    }

    private void setupSubjectSpinner() {
        // List of subjects for student
        String[] subjects = {"Mathematics", "Physics", "Computer Science", "Digital Electronics"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, subjects);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubject.setAdapter(adapter);

        // Spinner Selection Logic
        spinnerSubject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedSubject = subjects[position];
                tvSelectedTitle.setText("Statistics for " + selectedSubject);

                // Demo Data: Yahan baad mein Firebase se real data aayega
                updateAttendanceStats(selectedSubject);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    // Ye function sirf dikhane ke liye hai ki data kaise change hoga
    private void updateAttendanceStats(String subject) {
        if (subject.equals("Mathematics")) {
            tvTotal.setText("40");
            tvPresent.setText("32");
            tvPercent.setText("80%");
        } else if (subject.equals("Physics")) {
            tvTotal.setText("35");
            tvPresent.setText("25");
            tvPercent.setText("71%");
        } else {
            tvTotal.setText("30");
            tvPresent.setText("28");
            tvPercent.setText("93%");
        }
    }
}