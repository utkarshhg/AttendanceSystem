package com.attendance.facerecognition.utils;

public class Constants {
    // API & Firebase
    public static final String FIREBASE_STUDENTS = "students";
    public static final String FIREBASE_ATTENDANCE = "attendance";
    public static final String FIREBASE_SUBJECTS = "subjects";
    public static final String FIREBASE_EMBEDDINGS = "face_embeddings";

    // Face Recognition
    public static final float FACE_RECOGNITION_THRESHOLD = 0.6f;
    public static final int EMBEDDING_DIMENSION = 128;
    public static final int MIN_FACES_REQUIRED = 5;

    // Camera
    public static final int CAMERA_PERMISSION_CODE = 100;
    public static final int CAMERA_PREVIEW_WIDTH = 1080;
    public static final int CAMERA_PREVIEW_HEIGHT = 1920;

    // Shared Preferences
    public static final String PREF_NAME = "AttendancePrefs";
    public static final String PREF_USER_ID = "user_id";
    public static final String PREF_USER_NAME = "user_name";
    public static final String PREF_USER_TYPE = "user_type";

    // User Types
    public static final String USER_STUDENT = "student";
    public static final String USER_PROFESSOR = "professor";
    public static final String USER_ADMIN = "admin";

    // UI
    public static final int ANIMATION_DURATION = 300;
    public static final int SNACKBAR_DURATION = 2000;
}