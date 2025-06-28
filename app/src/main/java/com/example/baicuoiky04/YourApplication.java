package com.example.baicuoiky04;

import android.app.Application;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class YourApplication extends Application {

    private static final String TAG = "YourApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // Kích hoạt tính năng lưu trữ offline cho Firestore
        // Điều này cho phép ứng dụng của bạn hoạt động ngay cả khi không có mạng
        // và tự động đồng bộ khi có mạng trở lại.
        try {
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build();
            firestore.setFirestoreSettings(settings);
            Log.d(TAG, "Firestore persistence enabled.");
        } catch (Exception e) {
            Log.e(TAG, "Error enabling Firestore persistence", e);
        }
    }
}