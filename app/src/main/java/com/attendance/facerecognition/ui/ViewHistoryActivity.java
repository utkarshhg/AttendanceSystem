package com.attendance.facerecognition.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.attendance.facerecognition.R;
import com.attendance.facerecognition.database.FirebaseManager;

import java.util.ArrayList;
import java.util.List;

public class ViewHistoryActivity extends AppCompatActivity {

    private TextView tvTitle;
    private ListView listView;
    private FirebaseManager db;
    private String subject, branch;

    // NEW: Master list of all registered students
    private List<String> allStudents = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_history);

        tvTitle = findViewById(R.id.tvHistoryTitle);
        listView = findViewById(R.id.listViewHistory);
        db = new FirebaseManager();

        subject = getIntent().getStringExtra("SUBJECT");
        branch = getIntent().getStringExtra("BRANCH");

        tvTitle.setText("History: " + subject + " (" + branch + ")");

        // Step 1: Fetch the master list of students first!
        loadStudentsAndHistory();
    }

    private void loadStudentsAndHistory() {
        Toast.makeText(this, "Loading database...", Toast.LENGTH_SHORT).show();

        db.getAllRegisteredStudents(new FirebaseManager.OnAllStudentsRetrieved() {
            @Override
            public void onRetrieved(List<String> studentIds) {
                allStudents = studentIds;

                // Step 2: Now that we know everyone who exists, load the attendance
                loadHistory();
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(ViewHistoryActivity.this, "Failed to load students: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadHistory() {
        db.getClassAttendanceHistory(subject, branch, new FirebaseManager.OnClassAttendanceRetrieved() {
            @Override
            public void onRetrieved(List<FirebaseManager.ClassAttendanceRecord> records) {
                runOnUiThread(() -> {
                    if (records.isEmpty()) {
                        Toast.makeText(ViewHistoryActivity.this, "No attendance records found.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    ArrayList<String> displayList = new ArrayList<>();
                    for (FirebaseManager.ClassAttendanceRecord record : records) {

                        StringBuilder entryBuilder = new StringBuilder();
                        entryBuilder.append("📅 Date: ").append(record.date).append("\n");
                        entryBuilder.append("👥 Total Present: ").append(record.totalPresent).append("\n\n");

                        // 1. Format the PRESENT Roll Numbers
                        if (record.presentStudents != null && !record.presentStudents.isEmpty()) {
                            entryBuilder.append("✅ Present Roll Nos:\n");
                            entryBuilder.append(TextUtils.join(", ", record.presentStudents)).append("\n\n");
                        } else {
                            entryBuilder.append("❌ Present Roll Nos: None\n\n");
                        }

                        // 2. Calculate and format the ABSENT Roll Numbers
                        List<String> absentStudents = new ArrayList<>();
                        for (String studentId : allStudents) {
                            // If the student is NOT in the present list, they are absent
                            if (record.presentStudents == null || !record.presentStudents.contains(studentId)) {
                                absentStudents.add(studentId);
                            }
                        }

                        if (!absentStudents.isEmpty()) {
                            entryBuilder.append("❌ Absent Roll Nos:\n");
                            entryBuilder.append(TextUtils.join(", ", absentStudents));
                        } else {
                            entryBuilder.append("🌟 Absent Roll Nos: None (100% Attendance!)");
                        }

                        // Add bottom spacing between different date records
                        entryBuilder.append("\n\n");

                        displayList.add(entryBuilder.toString());
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            ViewHistoryActivity.this,
                            android.R.layout.simple_list_item_1,
                            displayList
                    );
                    listView.setAdapter(adapter);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(ViewHistoryActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }
}