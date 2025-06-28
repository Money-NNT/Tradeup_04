package com.example.baicuoiky04;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private static final int PICK_IMAGE_REQUEST = 1;

    private CircleImageView profileImageView;
    private FloatingActionButton fabChangeImage;
    private TextView textViewDisplayName, textViewRatingValue, textViewTotalTransactions, textViewBio;
    private RatingBar ratingBar;
    private LinearLayout ownerActionsLayout;
    private MaterialButton buttonEditProfile, buttonLogout, buttonDeactivate, buttonDelete;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseUser currentUser;

    private Uri imageUri;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUser = mAuth.getCurrentUser();

        initViews(view);

        if (currentUser != null) {
            loadUserProfile(currentUser.getUid());
            setupOwnerActions();
        } else {
            Log.e(TAG, "Current user is null, redirecting to LoginActivity");
            goToLogin();
        }

        return view;
    }

    private void initViews(View view) {
        profileImageView = view.findViewById(R.id.profileImageView);
        fabChangeImage = view.findViewById(R.id.fabChangeImage);
        textViewDisplayName = view.findViewById(R.id.textViewDisplayName);
        ratingBar = view.findViewById(R.id.ratingBar);
        textViewRatingValue = view.findViewById(R.id.textViewRatingValue);
        textViewTotalTransactions = view.findViewById(R.id.textViewTotalTransactions);
        textViewBio = view.findViewById(R.id.textViewBio);
        ownerActionsLayout = view.findViewById(R.id.ownerActionsLayout);
        buttonEditProfile = view.findViewById(R.id.buttonEditProfile);
        buttonLogout = view.findViewById(R.id.buttonLogout);
        buttonDeactivate = view.findViewById(R.id.buttonDeactivate);
        buttonDelete = view.findViewById(R.id.buttonDelete);
    }

    private void loadUserProfile(String userId) {
        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            DataModels.User user = document.toObject(DataModels.User.class);
                            if (user != null) {
                                Log.d(TAG, "User data: displayName=" + user.getDisplayName() +
                                        ", bio=" + user.getBio() +
                                        ", photoUrl=" + user.getPhotoUrl());

                                String displayName = user.getDisplayName();
                                textViewDisplayName.setText(displayName != null && !displayName.isEmpty() ?
                                        displayName : "Không có tên");

                                String bio = user.getBio();
                                textViewBio.setText(bio == null || bio.isEmpty() ?
                                        "Chưa có giới thiệu." : bio);

                                ratingBar.setRating((float) user.getAverageRating());
                                textViewRatingValue.setText(String.format("%.1f", user.getAverageRating()));
                                textViewTotalTransactions.setText(user.getTotalTransactions() + " giao dịch thành công");

                                if (getContext() != null && user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                                    // Kiểm tra URL có phải là hình ảnh hợp lệ
                                    if (user.getPhotoUrl().startsWith("https://firebasestorage.googleapis.com") ||
                                            user.getPhotoUrl().endsWith(".jpg") || user.getPhotoUrl().endsWith(".png")) {
                                        Glide.with(getContext()).load(user.getPhotoUrl())
                                                .placeholder(R.drawable.ic_person_24) // Sử dụng biểu tượng người dùng
                                                .error(R.drawable.ic_person_24)
                                                .into(profileImageView);
                                    } else {
                                        Log.w(TAG, "Invalid photo URL: " + user.getPhotoUrl());
                                        profileImageView.setImageResource(R.drawable.ic_person_24);
                                    }
                                } else {
                                    Log.d(TAG, "Photo URL is null or empty, setting default image");
                                    profileImageView.setImageResource(R.drawable.ic_person_24);
                                }
                            } else {
                                Log.e(TAG, "User object is null after conversion");
                            }
                        } else {
                            Log.d(TAG, "No such document for userId: " + userId);
                        }
                    } else {
                        Log.e(TAG, "Failed to fetch user data: ", task.getException());
                    }
                });
    }

    private void setupOwnerActions() {
        ownerActionsLayout.setVisibility(View.VISIBLE);

        fabChangeImage.setOnClickListener(v -> openFileChooser());

        // THÊM ĐOẠN CODE NÀY VÀO
        buttonEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            // Truyền dữ liệu hiện tại qua cho màn hình chỉnh sửa
            intent.putExtra("CURRENT_DISPLAY_NAME", textViewDisplayName.getText().toString());
            intent.putExtra("CURRENT_BIO", textViewBio.getText().toString());
            startActivity(intent);
        });

        buttonLogout.setOnClickListener(v -> {
            mAuth.signOut();
            goToLogin();
        });

        buttonDeactivate.setOnClickListener(v -> showConfirmationDialog("Hủy kích hoạt", "Bạn có chắc chắn muốn tạm thời vô hiệu hóa tài khoản? Bạn có thể kích hoạt lại bằng cách đăng nhập.", this::deactivateAccount));

        buttonDelete.setOnClickListener(v -> showConfirmationDialog("Xóa tài khoản vĩnh viễn", "Hành động này không thể hoàn tác. Tất cả dữ liệu của bạn sẽ bị xóa. Bạn có chắc chắn?", this::deleteAccount));
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            profileImageView.setImageURI(imageUri);
            uploadImageToFirebaseStorage();
        }
    }

    private void uploadImageToFirebaseStorage() {
        if (imageUri != null && currentUser != null) {
            StorageReference fileReference = storage.getReference("profile_pics").child(currentUser.getUid() + ".jpg");
            fileReference.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        updateProfileImageUrl(uri);
                        Log.d(TAG, "Image uploaded successfully: " + uri.toString());
                    }))
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to upload image: ", e);
                        Toast.makeText(getContext(), "Lỗi khi tải ảnh lên: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    })
                    .addOnProgressListener(snapshot -> {
                        // Hiển thị tiến trình tải lên nếu cần
                        double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                        Log.d(TAG, "Upload progress: " + progress + "%");
                    });
        } else {
            Log.e(TAG, "Image URI or current user is null");
            Toast.makeText(getContext(), "Không thể tải ảnh lên. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateProfileImageUrl(Uri uri) {
        db.collection("users").document(currentUser.getUid()).update("photoUrl", uri.toString())
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Cập nhật ảnh đại diện thành công!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi khi cập nhật link ảnh.", Toast.LENGTH_SHORT).show());
    }

    private void deactivateAccount() {
        // Logic đơn giản là cập nhật status
        db.collection("users").document(currentUser.getUid()).update("accountStatus", "deactivated")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Tài khoản đã được hủy kích hoạt.", Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                    goToLogin();
                });
    }

    private void deleteAccount() {
        // Xóa document trên Firestore trước
        db.collection("users").document(currentUser.getUid()).delete()
                .addOnSuccessListener(aVoid -> {
                    // Sau đó xóa tài khoản trên Auth
                    currentUser.delete().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Tài khoản đã được xóa vĩnh viễn.", Toast.LENGTH_SHORT).show();
                            goToLogin();
                        }
                    });
                });
    }

    private void showConfirmationDialog(String title, String message, Runnable onConfirm) {
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Xác nhận", (dialog, which) -> onConfirm.run())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void goToLogin() {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }
}