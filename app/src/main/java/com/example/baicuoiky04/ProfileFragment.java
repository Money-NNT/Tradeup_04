// Dán toàn bộ code này để thay thế file ProfileFragment.java cũ

package com.example.baicuoiky04;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private CircleImageView profileImageView;
    private FloatingActionButton fabChangeImage;
    private TextView textViewDisplayName, textViewRatingValue, textViewTotalTransactions, textViewBio, textViewContactInfo, textViewNoReviews;
    private RatingBar ratingBar;
    private LinearLayout ownerActionsLayout;
    private MaterialButton buttonAdminDashboard, buttonEditProfile, buttonLogout, buttonDeactivate, buttonDelete;
    private MaterialButton buttonPurchaseHistory, buttonSalesHistory, buttonOfferHistory;
    private MaterialButton buttonReportUser;
    private RecyclerView recyclerViewReviews;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseUser currentUser;
    private ListenerRegistration userProfileListener;
    private ReviewAdapter reviewAdapter;
    private List<DataModels.Review> reviewList;

    private String userIdToView;
    private DataModels.User currentUserData;

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;


    public static ProfileFragment newInstance(String userId) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString("USER_ID", userId);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (getArguments() != null) {
            userIdToView = getArguments().getString("USER_ID");
        } else if (currentUser != null) {
            userIdToView = currentUser.getUid();
        }

        initializeLaunchers();
    }

    private void initializeLaunchers() {
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                openImagePicker();
            } else {
                Toast.makeText(getContext(), "Bạn cần cấp quyền để thay đổi ảnh.", Toast.LENGTH_SHORT).show();
            }
        });
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                Uri imageUri = result.getData().getData();
                Glide.with(this).load(imageUri).into(profileImageView);
                compressAndUploadImage(imageUri);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        initViews(view);
        setupRecyclerView();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (userIdToView != null && !userIdToView.isEmpty()) {
            attachUserProfileListener(userIdToView);
            loadUserReviews(userIdToView); // Tải review khi fragment bắt đầu
        } else {
            Toast.makeText(getContext(), "Không có thông tin người dùng", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onStop() {
        super.onStop();
        if (userProfileListener != null) {
            userProfileListener.remove();
        }
    }
    private void attachUserProfileListener(String userId) {
        userProfileListener = db.collection("users").document(userId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed.", error);
                        return;
                    }
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        DataModels.User user = documentSnapshot.toObject(DataModels.User.class);
                        if (user != null) {
                            currentUserData = user;
                            updateUI(user);
                        }
                    }
                });
    }

    private void initViews(View view) {
        profileImageView = view.findViewById(R.id.profileImageView);
        fabChangeImage = view.findViewById(R.id.fabChangeImage);
        textViewDisplayName = view.findViewById(R.id.textViewDisplayName);
        ratingBar = view.findViewById(R.id.ratingBar);
        textViewRatingValue = view.findViewById(R.id.textViewRatingValue);
        textViewTotalTransactions = view.findViewById(R.id.textViewTotalTransactions);
        textViewBio = view.findViewById(R.id.textViewBio);
        textViewContactInfo = view.findViewById(R.id.textViewContactInfo);
        ownerActionsLayout = view.findViewById(R.id.ownerActionsLayout);
        buttonAdminDashboard = view.findViewById(R.id.buttonAdminDashboard);
        buttonEditProfile = view.findViewById(R.id.buttonEditProfile);
        buttonLogout = view.findViewById(R.id.buttonLogout);
        buttonDeactivate = view.findViewById(R.id.buttonDeactivate);
        buttonDelete = view.findViewById(R.id.buttonDelete);
        buttonPurchaseHistory = view.findViewById(R.id.buttonPurchaseHistory);
        buttonSalesHistory = view.findViewById(R.id.buttonSalesHistory);
        buttonOfferHistory = view.findViewById(R.id.buttonOfferHistory);
        buttonReportUser = view.findViewById(R.id.buttonReportUser);
        recyclerViewReviews = view.findViewById(R.id.recyclerViewReviews); // Thêm dòng này
        textViewNoReviews = view.findViewById(R.id.textViewNoReviews);   // Thêm dòng này
    }

    private void setupRecyclerView() {
        reviewList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(getContext(), reviewList);
        recyclerViewReviews.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewReviews.setAdapter(reviewAdapter);
    }

    private void updateUI(DataModels.User user) {
        if (getContext() == null) return;
        textViewDisplayName.setText(user.getDisplayName());
        String bio = user.getBio();
        if (TextUtils.isEmpty(bio)) {
            textViewBio.setVisibility(View.GONE);
        } else {
            textViewBio.setVisibility(View.VISIBLE);
            textViewBio.setText(bio);
        }
        String contactInfo = user.getContactInfo();
        if (TextUtils.isEmpty(contactInfo)) {
            textViewContactInfo.setVisibility(View.GONE);
        } else {
            textViewContactInfo.setVisibility(View.VISIBLE);
            textViewContactInfo.setText(contactInfo);
        }
        ratingBar.setRating((float) user.getAverageRating());
        textViewRatingValue.setText(String.format(Locale.US, "%.1f", user.getAverageRating()));
        textViewTotalTransactions.setText(String.format(Locale.US, "%d giao dịch thành công", user.getTotalTransactions()));

        if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
            Glide.with(this).load(user.getPhotoUrl()).placeholder(R.drawable.ic_profile_placeholder).into(profileImageView);
        } else {
            profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
        }

        if (currentUser != null && currentUser.getUid().equals(user.getUid())) {
            ownerActionsLayout.setVisibility(View.VISIBLE);
            fabChangeImage.setVisibility(View.VISIBLE);
            buttonReportUser.setVisibility(View.GONE);
            setupOwnerActions(user);
        } else {
            ownerActionsLayout.setVisibility(View.GONE);
            fabChangeImage.setVisibility(View.GONE);
            buttonReportUser.setVisibility(View.VISIBLE);
            buttonReportUser.setOnClickListener(v -> showReportDialog());
        }
    }
    private void setupOwnerActions(DataModels.User user) {
        if (getView() == null) return;

        if ("admin".equals(user.getRole())) {
            buttonAdminDashboard.setVisibility(View.VISIBLE);
            buttonAdminDashboard.setOnClickListener(v -> {
                if (getActivity() != null) {
                    startActivity(new Intent(getActivity(), AdminDashboardActivity.class));
                }
            });
        } else {
            buttonAdminDashboard.setVisibility(View.GONE);
        }

        ownerActionsLayout.setVisibility(View.VISIBLE);
        fabChangeImage.setVisibility(View.VISIBLE);

        fabChangeImage.setOnClickListener(v -> checkPermissionAndPickImage());

        buttonEditProfile.setOnClickListener(v -> {
            if (currentUserData == null) {
                Toast.makeText(getContext(), "Đang tải dữ liệu, vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            intent.putExtra("CURRENT_DISPLAY_NAME", currentUserData.getDisplayName());
            intent.putExtra("CURRENT_BIO", currentUserData.getBio());
            intent.putExtra("CURRENT_CONTACT_INFO", currentUserData.getContactInfo());
            startActivity(intent);
        });

        buttonLogout.setOnClickListener(v -> {
            mAuth.signOut();
            goToLogin();
        });

        buttonDeactivate.setOnClickListener(v -> showConfirmationDialog("Hủy kích hoạt", "Bạn có chắc chắn muốn tạm thời vô hiệu hóa tài khoản?", this::deactivateAccount));
        buttonDelete.setOnClickListener(v -> showConfirmationDialog("Xóa tài khoản vĩnh viễn", "Hành động này không thể hoàn tác.", this::deleteAccount));
        buttonPurchaseHistory.setOnClickListener(v -> openHistory(HistoryActivity.TYPE_PURCHASE));
        buttonSalesHistory.setOnClickListener(v -> openHistory(HistoryActivity.TYPE_SALES));
        buttonOfferHistory.setOnClickListener(v -> {
            if (getActivity() != null) {
                startActivity(new Intent(getActivity(), MyOffersActivity.class));
            }
        });
        MaterialButton buttonArchivedListings = getView().findViewById(R.id.buttonArchivedListings);
        buttonArchivedListings.setOnClickListener(v -> {
            if (getActivity() != null) {
                startActivity(new Intent(getActivity(), ArchivedListingsActivity.class));
            }
        });
    }

    private void checkPermissionAndPickImage() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            openImagePicker();
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }
    private void compressAndUploadImage(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), imageUri);
            int targetWidth = 800;
            int targetHeight = (int) (bitmap.getHeight() * ( (float) targetWidth / (float) bitmap.getWidth()));
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
            byte[] data = baos.toByteArray();
            uploadImageToFirebaseStorage(data);
        } catch (IOException e) {
            Log.e(TAG, "Error processing image", e);
        }
    }
    private void uploadImageToFirebaseStorage(byte[] imageData) {
        if (currentUser == null) return;
        final StorageReference fileReference = storage.getReference("profile_pics").child(currentUser.getUid() + "/" + UUID.randomUUID().toString() + ".jpg");
        Toast.makeText(getContext(), "Đang cập nhật ảnh đại diện...", Toast.LENGTH_LONG).show();
        fileReference.putBytes(imageData).addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
            updateAllUserReferences(currentUser.getUid(), uri.toString());
        })).addOnFailureListener(e -> Toast.makeText(getContext(), "Tải ảnh lên thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    private void updateAllUserReferences(String userId, String newPhotoUrl) {
        if (currentUser == null) return;
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().setPhotoUri(Uri.parse(newPhotoUrl)).build();
        currentUser.updateProfile(profileUpdates);
        db.collection("users").document(userId).update("photoUrl", newPhotoUrl)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Cập nhật ảnh đại diện thành công!", Toast.LENGTH_SHORT).show());
    }
    private void openHistory(String historyType) {
        Intent intent = new Intent(getActivity(), HistoryActivity.class);
        intent.putExtra(HistoryActivity.HISTORY_TYPE, historyType);
        startActivity(intent);
    }
    private void deactivateAccount() {
        if (currentUser == null) return;
        db.collection("users").document(currentUser.getUid()).update("accountStatus", "deactivated")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Tài khoản đã được hủy kích hoạt.", Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                    goToLogin();
                });
    }
    private void deleteAccount() {
        if (currentUser == null) return;
        db.collection("users").document(currentUser.getUid()).delete()
                .addOnSuccessListener(aVoid -> currentUser.delete().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Tài khoản đã được xóa vĩnh viễn.", Toast.LENGTH_SHORT).show();
                        goToLogin();
                    } else {
                        Toast.makeText(getContext(), "Lỗi xóa tài khoản, vui lòng đăng nhập lại và thử lại.", Toast.LENGTH_LONG).show();
                    }
                }));
    }
    private void showConfirmationDialog(String title, String message, Runnable onConfirm) {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext()).setTitle(title).setMessage(message)
                .setPositiveButton("Xác nhận", (dialog, which) -> onConfirm.run())
                .setNegativeButton("Hủy", null).show();
    }
    private void goToLogin() {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }
    private void showReportDialog() {
        if (getContext() == null || currentUser == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_report, null);
        builder.setView(dialogView);
        TextView reportTitle = dialogView.findViewById(R.id.textViewReportTitle);
        reportTitle.setText("Báo cáo người dùng: " + currentUserData.getDisplayName());
        final RadioGroup radioGroup = dialogView.findViewById(R.id.radioGroupReasons);
        final TextInputLayout commentLayout = dialogView.findViewById(R.id.textInputLayoutComment);
        final EditText commentEditText = dialogView.findViewById(R.id.editTextComment);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioOther) {
                commentLayout.setVisibility(View.VISIBLE);
            } else {
                commentLayout.setVisibility(View.GONE);
            }
        });
        builder.setPositiveButton("Gửi báo cáo", (dialog, which) -> {
            int selectedId = radioGroup.getCheckedRadioButtonId();
            if (selectedId == -1) return;
            String reason = "";
            if (selectedId == R.id.radioScam) reason = "Lừa đảo";
            else if(selectedId == R.id.radioInappropriate) reason = "Nội dung không phù hợp";
            else if(selectedId == R.id.radioSpam) reason = "Spam";
            else if (selectedId == R.id.radioOther) reason = "Khác";
            String comment = commentEditText.getText().toString().trim();
            submitUserReport(reason, comment);
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }
    private void submitUserReport(String reason, String comment) {
        DataModels.Report report = new DataModels.Report();
        report.setReporterId(currentUser.getUid());
        report.setReportedUserId(userIdToView);
        report.setReason(reason);
        report.setComment(comment);
        db.collection("reports").add(report).addOnSuccessListener(documentReference -> Toast.makeText(getContext(), "Cảm ơn bạn đã gửi báo cáo.", Toast.LENGTH_LONG).show());
    }

    private void loadUserReviews(String userId) {
        if (getContext() == null || TextUtils.isEmpty(userId)) {
            return;
        }

        db.collection("users").document(userId).collection("reviews")
                // KHÔNG CÒN .whereEqualTo("status", "visible") NỮA
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (getContext() == null) return;
                    reviewList.clear();
                    if (queryDocumentSnapshots.isEmpty()) {
                        textViewNoReviews.setVisibility(View.VISIBLE);
                        recyclerViewReviews.setVisibility(View.GONE);
                    } else {
                        textViewNoReviews.setVisibility(View.GONE);
                        recyclerViewReviews.setVisibility(View.VISIBLE);
                        reviewList.addAll(queryDocumentSnapshots.toObjects(DataModels.Review.class));
                    }
                    reviewAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading reviews", e);
                });
    }
    // ==========================================================
}