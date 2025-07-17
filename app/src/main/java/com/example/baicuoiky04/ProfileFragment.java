package com.example.baicuoiky04;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
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
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    // Views
    private CircleImageView profileImageView;
    private FloatingActionButton fabChangeImage;
    private TextView textViewDisplayName, textViewRatingValue, textViewTotalTransactions, textViewBio, textViewContactInfo, textViewNoReviews, textViewSeeAllReviews;
    private RatingBar ratingBar;
    private LinearLayout ownerActionsLayout;
    private MaterialButton buttonAdminDashboard, buttonEditProfile, buttonLogout, buttonDeactivate, buttonDelete;
    private MaterialButton buttonPurchaseHistory, buttonSalesHistory, buttonOfferHistory, buttonSavedListings, buttonArchivedListings, buttonBlockedUsers; // <<< THÊM KHAI BÁO
    private MaterialButton buttonReportUser;
    private RecyclerView recyclerViewReviews;

    // Firebase & Data
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ListenerRegistration userProfileListener;
    private ReviewAdapter reviewAdapter;
    private List<DataModels.Review> reviewList;
    private String userIdToView;
    private DataModels.User currentUserData;

    // Others
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ProgressDialog progressDialog;
    private View fragmentView;

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
        currentUser = mAuth.getCurrentUser();

        if (getArguments() != null) {
            userIdToView = getArguments().getString("USER_ID");
        } else if (currentUser != null) {
            userIdToView = currentUser.getUid();
        }

        initializeLaunchers();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.fragment_profile, container, false);
        initViews(fragmentView);
        setupRecyclerView();
        return fragmentView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (userIdToView != null && !userIdToView.isEmpty()) {
            attachUserProfileListener(userIdToView);
            loadUserReviews(userIdToView);
        } else if (getContext() != null) {
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
                if (getContext() != null) {
                    Glide.with(getContext()).load(imageUri).into(profileImageView);
                }
                uploadImageToCloudinary(imageUri);
            }
        });
    }

    private void attachUserProfileListener(String userId) {
        userProfileListener = db.collection("users").document(userId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed.", error);
                        return;
                    }
                    if (isAdded() && documentSnapshot != null && documentSnapshot.exists()) {
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
        buttonSavedListings = view.findViewById(R.id.buttonSavedListings);
        buttonArchivedListings = view.findViewById(R.id.buttonArchivedListings);
        buttonBlockedUsers = view.findViewById(R.id.buttonBlockedUsers); // <<< ÁNH XẠ NÚT MỚI
        buttonReportUser = view.findViewById(R.id.buttonReportUser);
        recyclerViewReviews = view.findViewById(R.id.recyclerViewReviews);
        textViewNoReviews = view.findViewById(R.id.textViewNoReviews);
        textViewSeeAllReviews = view.findViewById(R.id.textViewSeeAllReviews);
    }

    private void setupRecyclerView() {
        reviewList = new ArrayList<>();
        if (getContext() != null) {
            reviewAdapter = new ReviewAdapter(requireContext(), reviewList, this::showReportReviewDialog);
            recyclerViewReviews.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerViewReviews.setAdapter(reviewAdapter);
        }
    }

    private void updateUI(DataModels.User user) {
        if (getContext() == null || !isAdded()) return;
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
            setupOwnerActions();
        } else {
            ownerActionsLayout.setVisibility(View.GONE);
            fabChangeImage.setVisibility(View.GONE);
            buttonReportUser.setVisibility(View.VISIBLE);
            buttonReportUser.setOnClickListener(v -> showReportDialog());
        }
    }

    private void setupOwnerActions() {
        if (fragmentView == null || currentUserData == null) return;

        buttonAdminDashboard.setVisibility("admin".equals(currentUserData.getRole()) ? View.VISIBLE : View.GONE);
        buttonAdminDashboard.setOnClickListener(v -> {
            if (getActivity() != null) startActivity(new Intent(getActivity(), AdminDashboardActivity.class));
        });
        buttonEditProfile.setOnClickListener(v -> {
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
            if (getActivity() != null) startActivity(new Intent(getActivity(), MyOffersActivity.class));
        });
        buttonSavedListings.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragmentFromAnotherFragment(new SavedListingsFragment());
            }
        });
        buttonArchivedListings.setOnClickListener(v -> {
            if (getActivity() != null) startActivity(new Intent(getActivity(), ArchivedListingsActivity.class));
        });

        // <<< THÊM SỰ KIỆN CLICK CHO NÚT MỚI >>>
        buttonBlockedUsers.setOnClickListener(v -> {
            if (getActivity() != null) {
                startActivity(new Intent(getActivity(), BlockedUsersActivity.class));
            }
        });

        fabChangeImage.setOnClickListener(v -> checkPermissionAndPickImage());
    }

    private void checkPermissionAndPickImage() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        if (getContext() != null && ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            openImagePicker();
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        if (getContext() == null || currentUser == null) return;
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Đang cập nhật ảnh đại diện...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        String publicId = "profile_pics/" + currentUser.getUid() + "/" + System.currentTimeMillis();
        MediaManager.get().upload(imageUri)
                .option("public_id", publicId)
                .option("overwrite", true)
                .callback(new UploadCallback() {
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String newPhotoUrl = (String) resultData.get("secure_url");
                        Log.d(TAG, "Tải ảnh lên Cloudinary thành công. URL: " + newPhotoUrl);
                        updateAllUserReferences(newPhotoUrl);
                    }
                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.e(TAG, "Lỗi tải ảnh lên Cloudinary: " + error.getDescription());
                        progressDialog.dismiss();
                        if(getContext() != null) {
                            Toast.makeText(getContext(), "Tải ảnh lên thất bại: " + error.getDescription(), Toast.LENGTH_LONG).show();
                        }
                    }
                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                    @Override
                    public void onStart(String requestId) {}
                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}
                }).dispatch();
    }

    private void updateAllUserReferences(String newPhotoUrl) {
        if (currentUser == null) {
            if(progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
            return;
        }
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setPhotoUri(Uri.parse(newPhotoUrl))
                .build();
        Task<Void> updateAuthTask = currentUser.updateProfile(profileUpdates);
        Task<Void> updateFirestoreTask = db.collection("users").document(currentUser.getUid())
                .update("photoUrl", newPhotoUrl);

        Tasks.whenAllSuccess(updateAuthTask, updateFirestoreTask).addOnSuccessListener(results -> {
            progressDialog.dismiss();
            if (getContext() != null) {
                Toast.makeText(getContext(), "Cập nhật ảnh đại diện thành công!", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            if (getContext() != null) {
                Toast.makeText(getContext(), "Lỗi khi cập nhật hồ sơ.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openHistory(String historyType) {
        if (getActivity() == null) return;
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
        if (getActivity() == null) return;
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }

    private void showReportDialog() {
        if (getContext() == null || currentUser == null || currentUserData == null) return;
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
            else if (selectedId == R.id.radioInappropriate) reason = "Nội dung không phù hợp";
            else if (selectedId == R.id.radioSpam) reason = "Spam";
            else if (selectedId == R.id.radioOther) reason = "Khác";
            String comment = commentEditText.getText().toString().trim();
            submitUserReport(reason, comment);
        });
        builder.setNegativeButton("Hủy", null).create().show();
    }

    private void submitUserReport(String reason, String comment) {
        if (currentUser == null || userIdToView == null) return;
        DataModels.Report report = new DataModels.Report();
        report.setReporterId(currentUser.getUid());
        report.setReportedUserId(userIdToView);
        report.setReason(reason);
        report.setComment(comment);
        db.collection("reports").add(report).addOnSuccessListener(documentReference -> Toast.makeText(getContext(), "Cảm ơn bạn đã gửi báo cáo.", Toast.LENGTH_LONG).show());
    }

    private void loadUserReviews(String userId) {
        if (getContext() == null) return;
        db.collection("users").document(userId).collection("reviews")
                .whereEqualTo("status", "visible")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(4)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (getContext() == null || !isAdded()) return;
                    reviewList.clear();
                    if (queryDocumentSnapshots.isEmpty()) {
                        textViewNoReviews.setVisibility(View.VISIBLE);
                        textViewSeeAllReviews.setVisibility(View.GONE);
                        recyclerViewReviews.setVisibility(View.GONE);
                    } else {
                        textViewNoReviews.setVisibility(View.GONE);
                        recyclerViewReviews.setVisibility(View.VISIBLE);
                        List<DataModels.Review> allFetchedReviews = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            DataModels.Review review = doc.toObject(DataModels.Review.class);
                            review.setId(doc.getId());
                            allFetchedReviews.add(review);
                        }
                        if (allFetchedReviews.size() > 3) {
                            reviewList.addAll(allFetchedReviews.subList(0, 3));
                            textViewSeeAllReviews.setVisibility(View.VISIBLE);
                        } else {
                            reviewList.addAll(allFetchedReviews);
                            textViewSeeAllReviews.setVisibility(View.GONE);
                        }
                    }
                    if(reviewAdapter != null) {
                        reviewAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading reviews", e));
        textViewSeeAllReviews.setOnClickListener(v -> {
            if (currentUserData == null || getActivity() == null) return;
            Intent intent = new Intent(getActivity(), AllReviewsActivity.class);
            intent.putExtra("USER_ID", userIdToView);
            intent.putExtra("USER_NAME", currentUserData.getDisplayName());
            startActivity(intent);
        });
    }

    private void showReportReviewDialog(DataModels.Review review) {
        if (getContext() == null || currentUser == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_report, null);
        builder.setView(dialogView);
        TextView reportTitle = dialogView.findViewById(R.id.textViewReportTitle);
        reportTitle.setText("Báo cáo đánh giá");
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
            if (selectedId == -1) {
                Toast.makeText(getContext(), "Vui lòng chọn lý do báo cáo.", Toast.LENGTH_SHORT).show();
                return;
            }
            String reason = "";
            if (selectedId == R.id.radioScam) reason = "Lừa đảo";
            else if (selectedId == R.id.radioInappropriate) reason = "Nội dung không phù hợp";
            else if (selectedId == R.id.radioSpam) reason = "Spam";
            else if (selectedId == R.id.radioOther) reason = "Khác";
            String comment = commentEditText.getText().toString().trim();
            if (reason.equals("Khác") && TextUtils.isEmpty(comment)) {
                Toast.makeText(getContext(), "Vui lòng nhập bình luận cho lý do 'Khác'.", Toast.LENGTH_SHORT).show();
                return;
            }
            submitReviewReport(review, reason, comment);
        });
        builder.setNegativeButton("Hủy", null).create().show();
    }

    private void submitReviewReport(DataModels.Review review, String reason, String comment) {
        if (currentUser == null) return;
        DataModels.Report report = new DataModels.Report();
        report.setReporterId(currentUser.getUid());
        report.setReportedUserId(review.getReviewerId());
        report.setReportedReviewId(review.getId());
        report.setReason(reason);
        report.setComment(comment);
        db.collection("reports").add(report)
                .addOnSuccessListener(documentReference -> Toast.makeText(getContext(), "Cảm ơn bạn đã gửi báo cáo.", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Gửi báo cáo thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}