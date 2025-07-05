// Dán toàn bộ code này để thay thế file ReviewActivity.java

package com.example.baicuoiky04;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

public class ReviewActivity extends AppCompatActivity {

    private static final String TAG = "ReviewActivity";

    private TextView textViewReviewTitle;
    private RatingBar ratingBar;
    private TextInputEditText editTextComment;
    private MaterialButton btnSubmitReview;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private String listingId, userIdToReview, userNameToReview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        listingId = getIntent().getStringExtra("LISTING_ID");
        userIdToReview = getIntent().getStringExtra("USER_ID_TO_REVIEW");
        userNameToReview = getIntent().getStringExtra("USER_NAME_TO_REVIEW");

        if (currentUser == null || listingId == null || userIdToReview == null) {
            Toast.makeText(this, "Lỗi: Thiếu thông tin để đánh giá.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupListeners();
    }

    private void initViews() {
        textViewReviewTitle = findViewById(R.id.textViewReviewTitle);
        ratingBar = findViewById(R.id.ratingBar);
        editTextComment = findViewById(R.id.editTextComment);
        btnSubmitReview = findViewById(R.id.btnSubmitReview);
        progressBar = findViewById(R.id.progressBar);

        textViewReviewTitle.setText("Đánh giá: " + userNameToReview);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Gửi đánh giá");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void setupListeners() {
        btnSubmitReview.setOnClickListener(v -> submitReview());
    }

    private void submitReview() {
        float rating = ratingBar.getRating();
        if (rating == 0) {
            Toast.makeText(this, "Vui lòng chọn số sao đánh giá", Toast.LENGTH_SHORT).show();
            return;
        }
        String comment = editTextComment.getText().toString().trim();

        setLoading(true);

        DataModels.Review newReview = new DataModels.Review();
        newReview.setListingId(listingId);
        newReview.setReviewerId(currentUser.getUid());
        newReview.setReviewerName(currentUser.getDisplayName());
        newReview.setReviewerPhotoUrl(currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : "");
        newReview.setRating(rating);
        newReview.setComment(comment);

        DocumentReference userToReviewRef = db.collection("users").document(userIdToReview);
        DocumentReference newReviewRef = userToReviewRef.collection("reviews").document();

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot userSnapshot = transaction.get(userToReviewRef);

            long totalReviews = userSnapshot.contains("totalReviews") ? userSnapshot.getLong("totalReviews") : 0;
            double currentRating = userSnapshot.contains("averageRating") ? userSnapshot.getDouble("averageRating") : 0.0;

            double newAverageRating = ((currentRating * totalReviews) + rating) / (totalReviews + 1);
            long newTotalReviews = totalReviews + 1;

            transaction.update(userToReviewRef, "averageRating", newAverageRating);
            transaction.update(userToReviewRef, "totalReviews", newTotalReviews);
            transaction.set(newReviewRef, newReview);

            return null;
        }).addOnSuccessListener(aVoid -> {
            setLoading(false);
            Toast.makeText(ReviewActivity.this, "Gửi đánh giá thành công!", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            setLoading(false);
            Log.e(TAG, "Transaction failure.", e);
            Toast.makeText(ReviewActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSubmitReview.setEnabled(!isLoading);
    }
}