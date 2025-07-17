package com.example.baicuoiky04;

import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
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
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private ImageButton buttonSendMessage, buttonAttachImage;
    private Toolbar toolbar;
    private LinearLayout layoutChatbox;
    private TextView textViewBlocked;

    private MessageAdapter messageAdapter;
    private List<DataModels.Message> messageList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private String chatId;
    private String receiverId;
    private String receiverName;

    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        receiverId = getIntent().getStringExtra("receiver_id");
        receiverName = getIntent().getStringExtra("receiver_name");

        if (currentUser == null || receiverId == null || receiverName == null) {
            Toast.makeText(this, "Lỗi: Không thể bắt đầu chat.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeLaunchers();
        initViews();
        setupRecyclerView();
        getOrCreateChat();
    }

    private void initializeLaunchers() {
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        uploadImageToCloudinary(uri);
                    }
                });
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
        buttonAttachImage = findViewById(R.id.button_attach_image);
        layoutChatbox = findViewById(R.id.layout_chatbox);
        textViewBlocked = findViewById(R.id.textViewBlocked);

        buttonSendMessage.setOnClickListener(v -> sendTextMessage());
        buttonAttachImage.setOnClickListener(v -> openImagePicker());
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerViewMessages.setLayoutManager(layoutManager);
        recyclerViewMessages.setAdapter(messageAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (itemId == R.id.action_report_chat) {
            reportChat();
            return true;
        } else if (itemId == R.id.action_block_user) {
            confirmBlockUser();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openImagePicker() {
        pickImageLauncher.launch("image/*");
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang gửi ảnh...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String publicId = "chat_images/" + chatId + "/" + System.currentTimeMillis();

        MediaManager.get().upload(imageUri).option("public_id", publicId)
                .callback(new UploadCallback() {
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        progressDialog.dismiss();
                        String imageUrl = (String) resultData.get("secure_url");
                        sendImageMessage(imageUrl);
                    }
                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        progressDialog.dismiss();
                        Toast.makeText(ChatActivity.this, "Gửi ảnh thất bại: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void sendTextMessage() {
        String messageText = editTextMessage.getText().toString().trim();
        if (TextUtils.isEmpty(messageText)) return;
        DataModels.Message message = new DataModels.Message(currentUser.getUid(), messageText);
        sendMessageToFirestore(message, messageText);
    }

    private void sendImageMessage(String imageUrl) {
        DataModels.Message message = new DataModels.Message();
        message.setSenderId(currentUser.getUid());
        message.setImageUrl(imageUrl);
        sendMessageToFirestore(message, "[Hình ảnh]");
    }

    private void sendMessageToFirestore(DataModels.Message message, String lastMessageText) {
        WriteBatch batch = db.batch();
        DocumentReference newMessageRef = db.collection("chats").document(chatId).collection("messages").document();
        batch.set(newMessageRef, message);
        DocumentReference chatRef = db.collection("chats").document(chatId);
        batch.update(chatRef, "lastMessage", lastMessageText);
        batch.update(chatRef, "lastMessageTimestamp", FieldValue.serverTimestamp());
        batch.commit().addOnSuccessListener(aVoid -> {
            if (message.getText() != null) {
                editTextMessage.setText("");
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Gửi tin nhắn thất bại", Toast.LENGTH_SHORT).show());
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
                checkBlockStatus();
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
            newChat.setStatus("active");

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
                    if (e != null) { Log.w(TAG, "Listen failed.", e); return; }
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
        batch.commit().addOnSuccessListener(aVoid -> editTextMessage.setText("")).addOnFailureListener(e -> Toast.makeText(this, "Gửi tin nhắn thất bại", Toast.LENGTH_SHORT).show());
    }

    private void reportChat() {
        if (currentUser == null || receiverId == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Báo cáo cuộc trò chuyện?")
                .setMessage("Bạn có chắc muốn báo cáo cuộc trò chuyện này với quản trị viên không?")
                .setPositiveButton("Báo cáo", (dialog, which) -> {
                    DataModels.Report report = new DataModels.Report();
                    report.setReporterId(currentUser.getUid());
                    report.setReportedUserId(receiverId);
                    report.setReason("Cuộc trò chuyện không phù hợp");
                    report.setComment("Báo cáo từ cuộc trò chuyện với: " + receiverName + ". Chat ID: " + chatId);
                    db.collection("reports").add(report)
                            .addOnSuccessListener(documentReference -> Toast.makeText(this, "Đã gửi báo cáo.", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Gửi báo cáo thất bại.", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Hủy", null).show();
    }

    private void confirmBlockUser() {
        if (receiverName == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Chặn " + receiverName + "?")
                .setMessage("Bạn sẽ không nhận được tin nhắn từ người này nữa. Bạn có chắc chắn không?")
                .setPositiveButton("Chặn", (dialog, which) -> blockUser())
                .setNegativeButton("Hủy", null).show();
    }

    private void blockUser() {
        if (currentUser == null || receiverId == null) return;
        DocumentReference currentUserRef = db.collection("users").document(currentUser.getUid());
        currentUserRef.update("blockedUsers", FieldValue.arrayUnion(receiverId))
                .addOnSuccessListener(aVoid -> {
                    db.collection("chats").document(chatId).update("status", "blocked");
                    Toast.makeText(this, "Đã chặn người dùng.", Toast.LENGTH_SHORT).show();
                    updateBlockUI(true, "Bạn đã chặn người dùng này. Mở chặn trong Hồ sơ để tiếp tục.");
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Có lỗi xảy ra khi chặn người dùng.", Toast.LENGTH_SHORT).show());
    }

    private void checkBlockStatus() {
        db.collection("users").document(currentUser.getUid()).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists() && snapshot.get("blockedUsers") != null) {
                List<String> blockedUsers = (List<String>) snapshot.get("blockedUsers");
                if (blockedUsers.contains(receiverId)) {
                    updateBlockUI(true, "Bạn đã chặn người dùng này. Mở chặn trong Hồ sơ để tiếp tục.");
                    return;
                }
            }
        });
        db.collection("users").document(receiverId).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists() && snapshot.get("blockedUsers") != null) {
                List<String> blockedUsers = (List<String>) snapshot.get("blockedUsers");
                if (blockedUsers.contains(currentUser.getUid())) {
                    updateBlockUI(true, "Bạn đã bị người dùng này chặn.");
                }
            }
        });
    }

    private void updateBlockUI(boolean isBlocked, String message) {
        if (isBlocked) {
            layoutChatbox.setVisibility(View.GONE);
            textViewBlocked.setVisibility(View.VISIBLE);
            textViewBlocked.setText(message);
        } else {
            layoutChatbox.setVisibility(View.VISIBLE);
            textViewBlocked.setVisibility(View.GONE);
        }
    }
}