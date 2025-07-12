package com.example.baicuoiky04;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private ImageButton buttonSendMessage;
    private Toolbar toolbar;
    private MessageAdapter messageAdapter;
    private List<DataModels.Message> messageList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private String chatId;
    private String receiverId;
    private String receiverName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        receiverId = getIntent().getStringExtra("receiver_id");
        receiverName = getIntent().getStringExtra("receiver_name");

        if (currentUser == null || receiverId == null || TextUtils.isEmpty(receiverName)) {
            Toast.makeText(this, "Lỗi: Không thể bắt đầu chat.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        getOrCreateChat();

        buttonSendMessage.setOnClickListener(v -> sendMessage());
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(receiverName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSendMessage = findViewById(R.id.buttonSendMessage);
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerViewMessages.setLayoutManager(layoutManager);
        recyclerViewMessages.setAdapter(messageAdapter);
    }

    private void getOrCreateChat() {
        List<String> uids = Arrays.asList(currentUser.getUid(), receiverId);
        Collections.sort(uids);
        chatId = uids.get(0) + "_" + uids.get(1);

        DocumentReference chatRef = db.collection("chats").document(chatId);
        chatRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (!task.getResult().exists()) {
                    createNewChatDocument(chatRef);
                } else {
                    listenForMessages();
                }
            } else {
                Log.e(TAG, "Error checking for chat document", task.getException());
            }
        });
    }

    private void createNewChatDocument(DocumentReference chatRef) {
        final String senderId = currentUser.getUid();
        Task<DocumentSnapshot> senderTask = db.collection("users").document(senderId).get();
        Task<DocumentSnapshot> receiverTask = db.collection("users").document(receiverId).get();

        Tasks.whenAllSuccess(senderTask, receiverTask).addOnSuccessListener(results -> {
            DocumentSnapshot senderDoc = (DocumentSnapshot) results.get(0);
            DocumentSnapshot receiverDoc = (DocumentSnapshot) results.get(1);

            String finalSenderName = senderDoc.exists() ? senderDoc.getString("displayName") : currentUser.getEmail();
            String finalSenderPhoto = senderDoc.exists() ? senderDoc.getString("photoUrl") : "";

            String finalReceiverName = receiverDoc.exists() ? receiverDoc.getString("displayName") : this.receiverName;
            String finalReceiverPhoto = receiverDoc.exists() ? receiverDoc.getString("photoUrl") : "";

            DataModels.Chat newChat = new DataModels.Chat();
            newChat.setParticipants(Arrays.asList(senderId, receiverId));
            newChat.setLastMessage("Bắt đầu cuộc trò chuyện");

            if (senderId.compareTo(receiverId) < 0) {
                newChat.setUser1Id(senderId);
                newChat.setUser1Name(finalSenderName);
                newChat.setUser1Photo(finalSenderPhoto);
                newChat.setUser2Id(receiverId);
                newChat.setUser2Name(finalReceiverName);
                newChat.setUser2Photo(finalReceiverPhoto);
            } else {
                newChat.setUser1Id(receiverId);
                newChat.setUser1Name(finalReceiverName);
                newChat.setUser1Photo(finalReceiverPhoto);
                newChat.setUser2Id(senderId);
                newChat.setUser2Name(finalSenderName);
                newChat.setUser2Photo(finalSenderPhoto);
            }

            chatRef.set(newChat)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Chat document created successfully.");
                        listenForMessages();
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error creating chat document", e));
        });
    }

    private void listenForMessages() {
        db.collection("chats").document(chatId).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }
                    if (snapshots != null) {
                        messageList.clear();
                        messageList.addAll(snapshots.toObjects(DataModels.Message.class));
                        messageAdapter.notifyDataSetChanged();
                        recyclerViewMessages.scrollToPosition(messageList.size() - 1);
                    }
                });
    }

    private void sendMessage() {
        String messageText = editTextMessage.getText().toString().trim();
        if (TextUtils.isEmpty(messageText)) return;

        DataModels.Message message = new DataModels.Message(currentUser.getUid(), messageText);
        WriteBatch batch = db.batch();
        DocumentReference newMessageRef = db.collection("chats").document(chatId).collection("messages").document();
        batch.set(newMessageRef, message);
        DocumentReference chatRef = db.collection("chats").document(chatId);
        batch.update(chatRef, "lastMessage", messageText);
        batch.update(chatRef, "lastMessageTimestamp", FieldValue.serverTimestamp());

        batch.commit().addOnSuccessListener(aVoid -> editTextMessage.setText(""))
                .addOnFailureListener(e -> Toast.makeText(ChatActivity.this, "Gửi tin nhắn thất bại", Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}