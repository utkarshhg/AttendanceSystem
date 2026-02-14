package com.attendance.facerecognition.database;

import android.util.Log;

import com.attendance.facerecognition.utils.Constants;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FirebaseManager {
    private static final String TAG = "FirebaseManager";
    private FirebaseFirestore db;

    public FirebaseManager() {
        db = FirebaseFirestore.getInstance();
    }

    // ==================== STUDENT OPERATIONS ====================

    public void registerStudent(String studentId, String name, String email,
                                List<String> enrolledSubjects,
                                OnCompleteListener listener) {
        Map<String, Object> student = new HashMap<>();
        student.put("studentId", studentId);
        student.put("name", name);
        student.put("email", email);
        student.put("enrolledSubjects", enrolledSubjects);
        student.put("registeredAt", new Date());
        student.put("facesRegistered", 0);
        student.put("totalAttendance", 0);

        db.collection(Constants.FIREBASE_STUDENTS).document(studentId)
                .set(student)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Student registered: " + studentId);
                    listener.onSuccess("Student registered successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error registering student", e);
                    listener.onError(e.getMessage());
                });
    }

    public void saveFaceEmbedding(String studentId, float[] embedding,
                                  int embeddingIndex, OnCompleteListener listener) {
        Map<String, Object> embeddingData = new HashMap<>();
        embeddingData.put("studentId", studentId);
        embeddingData.put("embeddingIndex", embeddingIndex);
        embeddingData.put("embeddingVector", floatArrayToString(embedding));
        embeddingData.put("savedAt", new Date());

        db.collection(Constants.FIREBASE_EMBEDDINGS)
                .add(embeddingData)
                .addOnSuccessListener(ref -> {
                    updateStudentFaceCount(studentId);
                    listener.onSuccess("Face embedding saved");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving embedding", e);
                    listener.onError(e.getMessage());
                });
    }

    public void getFaceEmbeddings(String studentId,
                                  OnEmbeddingsRetrieved listener) {
        db.collection(Constants.FIREBASE_EMBEDDINGS)
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<float[]> embeddings = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        String embeddingStr = document.getString("embeddingVector");
                        if (embeddingStr != null) {
                            embeddings.add(stringToFloatArray(embeddingStr));
                        }
                    }
                    listener.onRetrieved(embeddings);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error retrieving embeddings", e);
                    listener.onError(e.getMessage());
                });
    }

    // ==================== ATTENDANCE OPERATIONS ====================

    public void markAttendance(String subjectId, List<String> presentStudents,
                               String className, OnCompleteListener listener) {
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String attendanceId = subjectId + "_" + date + "_" + System.currentTimeMillis();

        Map<String, Object> attendance = new HashMap<>();
        attendance.put("attendanceId", attendanceId);
        attendance.put("subjectId", subjectId);
        attendance.put("className", className);
        attendance.put("date", date);
        attendance.put("presentStudents", presentStudents);
        attendance.put("totalPresent", presentStudents.size());
        attendance.put("markedAt", new Date());
        attendance.put("markedBy", "Professor");

        db.collection(Constants.FIREBASE_ATTENDANCE).document(attendanceId)
                .set(attendance)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Attendance marked: " + attendanceId);
                    listener.onSuccess("Attendance marked for " + presentStudents.size() + " students");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error marking attendance", e);
                    listener.onError(e.getMessage());
                });
    }

    public void getStudentAttendance(String studentId, String subjectId,
                                     OnAttendanceRetrieved listener) {
        db.collection(Constants.FIREBASE_ATTENDANCE)
                .whereEqualTo("subjectId", subjectId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    AttendanceRecord record = new AttendanceRecord();
                    record.studentId = studentId;
                    record.subjectId = subjectId;
                    record.presentDays = 0;
                    record.totalClasses = 0;

                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        record.totalClasses++;
                        List<String> presentStudents = (List<String>) document.get("presentStudents");
                        if (presentStudents != null && presentStudents.contains(studentId)) {
                            record.presentDays++;
                        }
                    }

                    record.attendance = record.totalClasses > 0 ?
                            (float) record.presentDays / record.totalClasses * 100 : 0;
                    listener.onRetrieved(record);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error retrieving attendance", e);
                    listener.onError(e.getMessage());
                });
    }

    public void getAllStudents(OnStudentsRetrieved listener) {
        db.collection(Constants.FIREBASE_STUDENTS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<StudentData> students = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        StudentData student = document.toObject(StudentData.class);
                        if (student != null) {
                            students.add(student);
                        }
                    }
                    listener.onRetrieved(students);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error retrieving students", e);
                    listener.onError(e.getMessage());
                });
    }

    // ==================== HELPER METHODS ====================

    private void updateStudentFaceCount(String studentId) {
        db.collection(Constants.FIREBASE_STUDENTS).document(studentId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Long currentCount = (Long) document.get("facesRegistered");
                        db.collection(Constants.FIREBASE_STUDENTS).document(studentId)
                                .update("facesRegistered", (currentCount != null ? currentCount : 0) + 1);
                    }
                });
    }

    private String floatArrayToString(float[] array) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            if (i < array.length - 1) sb.append(",");
        }
        return sb.toString();
    }

    private float[] stringToFloatArray(String str) {
        String[] parts = str.split(",");
        float[] array = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            array[i] = Float.parseFloat(parts[i]);
        }
        return array;
    }

    // ==================== CALLBACK INTERFACES ====================

    public interface OnCompleteListener {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface OnEmbeddingsRetrieved {
        void onRetrieved(List<float[]> embeddings);
        void onError(String error);
    }

    public interface OnAttendanceRetrieved {
        void onRetrieved(AttendanceRecord record);
        void onError(String error);
    }

    public interface OnStudentsRetrieved {
        void onRetrieved(List<StudentData> students);
        void onError(String error);
    }

    // ==================== DATA CLASSES ====================

    public static class AttendanceRecord {
        public String studentId;
        public String subjectId;
        public int presentDays;
        public int totalClasses;
        public float attendance;

        @Override
        public String toString() {
            return "AttendanceRecord{" +
                    "studentId='" + studentId + '\'' +
                    ", presentDays=" + presentDays +
                    ", totalClasses=" + totalClasses +
                    ", attendance=" + attendance + "%" +
                    '}';
        }
    }

    public static class StudentData {
        public String studentId;
        public String name;
        public String email;
        public List<String> enrolledSubjects;
        public long facesRegistered;
        public long totalAttendance;
        public Date registeredAt;

        @Override
        public String toString() {
            return "StudentData{" +
                    "name='" + name + '\'' +
                    ", email='" + email + '\'' +
                    ", facesRegistered=" + facesRegistered +
                    '}';
        }
    }
}