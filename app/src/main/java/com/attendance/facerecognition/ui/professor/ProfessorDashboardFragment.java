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
import com.attendance.facerecognition.database.FirebaseManager;
import com.attendance.facerecognition.ui.FaceScannerActivity;
import com.attendance.facerecognition.ui.ViewHistoryActivity;

import java.util.List;

public class ProfessorDashboardFragment extends Fragment {

    private Spinner spinnerSubject, spinnerBranch;
    private Button btnTakeAttendance, btnLogout, btnViewHistory, btnExport;
    private TextView tvWelcome;

    // Database and ID variables
    private FirebaseManager db;
    private String profId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_professor_dashboard, container, false);

        db = new FirebaseManager();

        initViews(view);
        setupSpinners();
        setupListeners();

        // Catch the Professor ID passed from MainActivity
        if (getArguments() != null) {
            profId = getArguments().getString("PROF_ID");
        }

        // Fetch the name if we have the ID
        if (profId != null) {
            fetchProfessorData();
        }

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

    // The method to fetch and display the name
    private void fetchProfessorData() {
        db.getProfessorProfile(profId, new FirebaseManager.OnProfessorProfileRetrieved() {
            @Override
            public void onRetrieved(String name) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Update the text view with the actual name from Firebase
                        tvWelcome.setText("Welcome, " + name);
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

    private void setupSpinners() {
        String[] subjects = {"Mathematics", "Physics", "Computer Science", "Software Engineering"};
        ArrayAdapter<String> subAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, subjects);
        subAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubject.setAdapter(subAdapter);

        String[] branches = {"CS-A", "CS-B", "IT-A", "Mechanical", "Civil"};
        ArrayAdapter<String> branchAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, branches);
        branchAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBranch.setAdapter(branchAdapter);
    }

    private void setupListeners() {
        btnTakeAttendance.setOnClickListener(v -> {
            // 1. Grab the selected text from the Spinners
            String selectedSub = spinnerSubject.getSelectedItem().toString();
            String selectedBranch = spinnerBranch.getSelectedItem().toString();

            Toast.makeText(getContext(), "Opening Camera for " + selectedSub + " (" + selectedBranch + ")", Toast.LENGTH_SHORT).show();

            // 2. Pass this data to the Face Scanner
            Intent intent = new Intent(getActivity(), FaceScannerActivity.class);
            intent.putExtra("MODE", "ATTENDANCE");
            intent.putExtra("SUBJECT", selectedSub);
            intent.putExtra("BRANCH", selectedBranch);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        btnViewHistory.setOnClickListener(v -> {
            String selectedSub = spinnerSubject.getSelectedItem().toString();
            String selectedBranch = spinnerBranch.getSelectedItem().toString();

            Intent intent = new Intent(getActivity(), ViewHistoryActivity.class);
            intent.putExtra("SUBJECT", selectedSub);
            intent.putExtra("BRANCH", selectedBranch);
            startActivity(intent);
        });

        // NEW: Trigger the CSV Export
        btnExport.setOnClickListener(v -> {
            String selectedSub = spinnerSubject.getSelectedItem().toString();
            String selectedBranch = spinnerBranch.getSelectedItem().toString();
            exportToCSV(selectedSub, selectedBranch);
        });
    }

    // NEW: Generates an Excel-friendly CSV file and saves it to the Downloads folder
    private void exportToCSV(String subject, String branch) {
        Toast.makeText(getContext(), "Generating Spreadsheet...", Toast.LENGTH_SHORT).show();

        db.getClassAttendanceHistory(subject, branch, new FirebaseManager.OnClassAttendanceRetrieved() {
            @Override
            public void onRetrieved(List<FirebaseManager.ClassAttendanceRecord> records) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    if (records.isEmpty()) {
                        Toast.makeText(getContext(), "No data to export!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        // 1. Build the CSV Text
                        StringBuilder csvData = new StringBuilder();
                        csvData.append("Date,Total Present,Student IDs\n"); // Column Headers

                        for (FirebaseManager.ClassAttendanceRecord record : records) {
                            String students = "None";
                            if (record.presentStudents != null && !record.presentStudents.isEmpty()) {
                                // Using semi-colons so it doesn't break the CSV columns
                                students = String.join(";", record.presentStudents);
                            }
                            csvData.append(record.date).append(",")
                                    .append(record.totalPresent).append(",")
                                    .append(students).append("\n");
                        }

                        // 2. Save it to the device's Downloads folder
                        android.content.ContentValues values = new android.content.ContentValues();
                        values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, subject + "_" + branch + "_Attendance.csv");
                        values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/csv");
                        values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS);

                        android.net.Uri uri = requireContext().getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

                        if (uri != null) {
                            java.io.OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri);
                            if (outputStream != null) {
                                outputStream.write(csvData.toString().getBytes());
                                outputStream.close();
                                Toast.makeText(getContext(), "✅ Saved to Downloads folder!", Toast.LENGTH_LONG).show();
                            }
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}