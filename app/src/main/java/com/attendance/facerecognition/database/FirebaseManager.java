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

    // ==================== AUTHENTICATION ====================

    /**
     * Verifies if the student ID and password match the records in Firestore.
     */
    public void verifyStudentLogin(String studentId, String password, OnLoginResult listener) {
        db.collection(Constants.FIREBASE_STUDENTS).document(studentId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String savedPassword = document.getString("password");
                        if (password != null && password.equals(savedPassword)) {
                            listener.onSuccess();
                        } else {
                            listener.onError("Incorrect Password");
                        }
                    } else {
                        listener.onError("Student ID not found");
                    }
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    /**
     * Verifies if the professor ID and password match the records in Firestore.
     */
    public void verifyProfessorLogin(String profId, String password, OnLoginResult listener) {
        // We are using a new collection called "professors"
        db.collection("professors").document(profId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String savedPassword = document.getString("password");
                        if (password != null && password.equals(savedPassword)) {
                            listener.onSuccess();
                        } else {
                            listener.onError("Incorrect Password");
                        }
                    } else {
                        listener.onError("Professor ID not found");
                    }
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    /**
     * Registers a new professor and saves their password to Firestore.
     */
    public void registerProfessor(String profId, String name, String password, OnCompleteListener listener) {
        Map<String, Object> professor = new HashMap<>();
        professor.put("profId", profId);
        professor.put("name", name);
        professor.put("password", password);
        professor.put("registeredAt", new Date());

        db.collection("professors").document(profId)
                .set(professor)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirebaseManager", "Professor registered: " + profId);
                    listener.onSuccess("Professor registered successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseManager", "Error registering professor", e);
                    listener.onError(e.getMessage());
                });
    }

    // ==================== STUDENT OPERATIONS ====================

    /**
     * Registers a new student and SAVES THE PASSWORD to Firestore.
     */
    public void registerStudent(String studentId, String name, String email, String password,
                                List<String> enrolledSubjects,
                                OnCompleteListener listener) {
        Map<String, Object> student = new HashMap<>();
        student.put("studentId", studentId);
        student.put("name", name);
        student.put("email", email);
        student.put("password", password); // Fixed: Saving password automatically
        student.put("enrolledSubjects", enrolledSubjects);
        student.put("registeredAt", new Date());
        student.put("facesRegistered", 0);
        student.put("totalAttendance", 0);

        db.collection(Constants.FIREBASE_STUDENTS).document(studentId)
                .set(student)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Student registered with password: " + studentId);
                    listener.onSuccess("Student registered successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error registering student", e);
                    listener.onError(e.getMessage());
                });
    }

    public void getStudentProfile(String studentId, OnProfileRetrieved listener) {
        db.collection(Constants.FIREBASE_STUDENTS).document(studentId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("name");
                        String email = document.getString("email");
                        List<String> subjects = (List<String>) document.get("enrolledSubjects");
                        listener.onRetrieved(name, email, subjects);
                    } else {
                        listener.onError("Student profile not found");
                    }
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
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

    public void getAllFaceEmbeddings(OnAllEmbeddingsRetrieved listener) {
        db.collection(Constants.FIREBASE_EMBEDDINGS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, float[]> faceBank = new HashMap<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        String studentId = document.getString("studentId");
                        String embeddingStr = document.getString("embeddingVector");
                        if (studentId != null && embeddingStr != null) {
                            faceBank.put(studentId, stringToFloatArray(embeddingStr));
                        }
                    }
                    listener.onRetrieved(faceBank);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    // ==================== CALLBACK INTERFACES ====================

    public interface OnLoginResult {
        void onSuccess();
        void onError(String error);
    }

    public interface OnProfileRetrieved {
        void onRetrieved(String name, String email, List<String> subjects);
        void onError(String error);
    }

    public interface OnAllEmbeddingsRetrieved {
        void onRetrieved(Map<String, float[]> faceBank);
        void onError(String error);
    }

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
    }

    public static class StudentData {
        public String studentId;
        public String name;
        public String email;
        public List<String> enrolledSubjects;
        public long facesRegistered;
        public long totalAttendance;
        public Date registeredAt;
    }
    // ==================== PROFESSOR OPERATIONS ====================

    public void getProfessorProfile(String profId, OnProfessorProfileRetrieved listener) {
        db.collection("professors").document(profId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("name");
                        listener.onRetrieved(name);
                    } else {
                        listener.onError("Professor profile not found");
                    }
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    // Add this interface inside your FirebaseManager class with the other interfaces
    public interface OnProfessorProfileRetrieved {
        void onRetrieved(String name);
        void onError(String error);
    }
    // ==================== HISTORY OPERATIONS ====================

    public void getClassAttendanceHistory(String subjectId, String branch, OnClassAttendanceRetrieved listener) {
        db.collection(Constants.FIREBASE_ATTENDANCE)
                .whereEqualTo("subjectId", subjectId)
                .whereEqualTo("className", branch) // In FaceScanner, we saved branch as className
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ClassAttendanceRecord> records = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ClassAttendanceRecord record = new ClassAttendanceRecord();
                        record.date = doc.getString("date");
                        Long total = doc.getLong("totalPresent");
                        record.totalPresent = total != null ? total.intValue() : 0;
                        record.presentStudents = (List<String>) doc.get("presentStudents");
                        records.add(record);
                    }
                    listener.onRetrieved(records);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    // Add this to your CALLBACK INTERFACES section
    public interface OnClassAttendanceRetrieved {
        void onRetrieved(List<ClassAttendanceRecord> records);
        void onError(String error);
    }

    // Add this to your DATA CLASSES section
    public static class ClassAttendanceRecord {
        public String date;
        public int totalPresent;
        public List<String> presentStudents;
    }
    // ==================== MASTER LIST OPERATION ====================

    public void getAllRegisteredStudents(OnAllStudentsRetrieved listener) {
        db.collection(Constants.FIREBASE_STUDENTS) // Looks at the "students" collection
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> studentIds = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String id = doc.getString("studentId");
                        if (id != null) {
                            studentIds.add(id);
                        }
                    }
                    listener.onRetrieved(studentIds);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    // Add this interface inside your FirebaseManager with your other interfaces!
    public interface OnAllStudentsRetrieved {
        void onRetrieved(List<String> studentIds);
        void onError(String error);
    }
}