package com.example.baicuoiky04;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot; // <<< DÒNG IMPORT BỊ THIẾU

import java.util.ArrayList;
import java.util.List;

public class ConversationsFragment extends Fragment {

    private static final String TAG = "ConversationsFragment";

    private RecyclerView recyclerView;
    private ConversationAdapter adapter;
    private List<DataModels.Chat> chatList;
    private ProgressBar progressBar;
    private TextView textViewEmpty;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_conversations, container, false);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        recyclerView = view.findViewById(R.id.recyclerViewConversations);
        progressBar = view.findViewById(R.id.progressBar);
        textViewEmpty = view.findViewById(R.id.textViewEmpty);

        setupRecyclerView();
        loadConversations();

        return view;
    }

    private void setupRecyclerView() {
        chatList = new ArrayList<>();
        adapter = new ConversationAdapter(requireContext(), chatList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadConversations() {
        if (currentUser == null) {
            textViewEmpty.setText("Vui lòng đăng nhập để xem tin nhắn.");
            textViewEmpty.setVisibility(View.VISIBLE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        textViewEmpty.setVisibility(View.GONE);

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(userDoc -> {
                    List<String> blockedIds = new ArrayList<>();
                    if (userDoc.exists() && userDoc.get("blockedUsers") != null) {
                        blockedIds = (List<String>) userDoc.get("blockedUsers");
                    }
                    final List<String> finalBlockedIds = blockedIds;

                    db.collection("chats")
                            .whereArrayContains("participants", currentUser.getUid())
                            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
                            .addSnapshotListener((snapshots, error) -> {
                                if (getContext() == null || !isAdded()) {
                                    return;
                                }
                                progressBar.setVisibility(View.GONE);
                                if (error != null) {
                                    Log.e(TAG, "Lỗi khi tải cuộc trò chuyện: ", error);
                                    textViewEmpty.setText("Lỗi tải tin nhắn.");
                                    textViewEmpty.setVisibility(View.VISIBLE);
                                    return;
                                }

                                if (snapshots != null) {
                                    chatList.clear();
                                    for (QueryDocumentSnapshot doc : snapshots) {
                                        DataModels.Chat chat = doc.toObject(DataModels.Chat.class);

                                        String otherUserId = "";
                                        if (chat.getParticipants() != null) {
                                            for (String id : chat.getParticipants()) {
                                                if (!id.equals(currentUser.getUid())) {
                                                    otherUserId = id;
                                                    break;
                                                }
                                            }
                                        }

                                        if (!finalBlockedIds.contains(otherUserId)) {
                                            chatList.add(chat);
                                        }
                                    }
                                    adapter.notifyDataSetChanged();
                                    textViewEmpty.setVisibility(chatList.isEmpty() ? View.VISIBLE : View.GONE);
                                } else {
                                    textViewEmpty.setVisibility(View.VISIBLE);
                                }
                            });
                });
    }
}