package com.attendance.facerecognition;

import android.app.Application;

import com.google.firebase.FirebaseApp;

public class AttendanceApplication extends Application {
    private static AttendanceApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        FirebaseApp.initializeApp(this);
    }

    public static AttendanceApplication getInstance() {
        return instance;
    }
}