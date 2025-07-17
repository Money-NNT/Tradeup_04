package com.example.baicuoiky04;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class BlockedUsersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private BlockedUserAdapter adapter;
    private List<DataModels.User> blockedUserList;
    private ProgressBar progressBar;
    private TextView textViewEmpty;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked_users);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        initViews();
        setupRecyclerView();
        loadBlockedUsers();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewBlockedUsers);
        progressBar = findViewById(R.id.progressBar);
        textViewEmpty = findViewById(R.id.textViewEmpty);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Người dùng đã chặn");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        blockedUserList = new ArrayList<>();
        adapter = new BlockedUserAdapter(this, blockedUserList, user -> unblockUser(user));
        recyclerView.setAdapter(adapter);
    }

    private void loadBlockedUsers() {
        if (currentUser == null) return;
        progressBar.setVisibility(View.VISIBLE);

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.get("blockedUsers") != null) {
                        List<String> blockedIds = (List<String>) documentSnapshot.get("blockedUsers");
                        if (blockedIds.isEmpty()) {
                            progressBar.setVisibility(View.GONE);
                            textViewEmpty.setVisibility(View.VISIBLE);
                            return;
                        }
                        fetchUserDetails(blockedIds);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        textViewEmpty.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void fetchUserDetails(List<String> ids) {
        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String id : ids) {
            tasks.add(db.collection("users").document(id).get());
        }
        Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
            blockedUserList.clear();
            for (Object snapshot : results) {
                DataModels.User user = ((DocumentSnapshot) snapshot).toObject(DataModels.User.class);
                if (user != null) {
                    blockedUserList.add(user);
                }
            }
            adapter.notifyDataSetChanged();
            progressBar.setVisibility(View.GONE);
        });
    }

    private void unblockUser(DataModels.User userToUnblock) {
        DocumentReference currentUserRef = db.collection("users").document(currentUser.getUid());
        currentUserRef.update("blockedUsers", FieldValue.arrayRemove(userToUnblock.getUid()))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã mở chặn " + userToUnblock.getDisplayName(), Toast.LENGTH_SHORT).show();
                    // Tải lại danh sách sau khi mở chặn
                    loadBlockedUsers();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Có lỗi xảy ra.", Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}