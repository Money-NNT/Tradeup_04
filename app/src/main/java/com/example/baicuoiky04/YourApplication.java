package com.example.baicuoiky04;

import android.app.Application;
import android.util.Log;

import com.cloudinary.android.MediaManager; // Thêm import
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.HashMap; // Thêm import
import java.util.Map; // Thêm import

public class YourApplication extends Application {
    private static final String TAG = "YourApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // Khởi tạo Firestore (giữ nguyên)
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

        // Khởi tạo Cloudinary
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME);
            config.put("api_key", BuildConfig.CLOUDINARY_API_KEY);
            config.put("api_secret", BuildConfig.CLOUDINARY_API_SECRET);
            MediaManager.init(this, config);
            Log.d(TAG, "Cloudinary initialized successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Cloudinary", e);
        }
    }
}