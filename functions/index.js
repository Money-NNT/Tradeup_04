const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

/**
 * Gửi thông báo khi có tin nhắn mới được tạo trong một cuộc trò chuyện.
 */
exports.sendChatNotification = functions.firestore
    .document("chats/{chatId}/messages/{messageId}")
    .onCreate(async (snap, context) => {
      const messageData = snap.data();
      const chatId = context.params.chatId;

      // Lấy thông tin của cuộc trò chuyện
      const chatDoc = await db.collection("chats").doc(chatId).get();
      if (!chatDoc.exists) {
        console.log(`Chat document ${chatId} does not exist.`);
        return null;
      }
      const chatData = chatDoc.data();

      const senderId = messageData.senderId;
      let receiverId = "";
      let senderName = "";

      // Xác định người nhận và tên người gửi từ document chat
      if (chatData.user1Id === senderId) {
        receiverId = chatData.user2Id;
        senderName = chatData.user1Name;
      } else {
        receiverId = chatData.user1Id;
        senderName = chatData.user2Name;
      }

      // Lấy thông tin của người nhận để lấy FCM token
      const receiverDoc = await db.collection("users").doc(receiverId).get();
      if (!receiverDoc.exists) {
        console.log(`Receiver document ${receiverId} does not exist.`);
        return null;
      }
      const receiverData = receiverDoc.data();

      // Kiểm tra xem người nhận có fcmToken không
      if (receiverData.fcmToken) {
        const payload = {
          notification: {
            title: `Tin nhắn mới từ ${senderName}`,
            body: messageData.text || "[Hình ảnh]",
          },
          // Bạn có thể thêm data payload để xử lý khi người dùng nhấn vào thông báo
          // data: {
          //   chatId: chatId,
          //   senderId: senderId,
          // },
        };

        console.log(`Sending notification to token: ${receiverData.fcmToken}`);
        return messaging.sendToDevice(receiverData.fcmToken, payload);
      } else {
        console.log(`Receiver ${receiverId} does not have an FCM token.`);
        return null;
      }
    });

/**
 * Xử lý khi Admin cập nhật một report để cảnh cáo người dùng.
 */
exports.handleAdminAction = functions.firestore
    .document("reports/{reportId}")
    .onUpdate(async (change, context) => {
      const reportAfter = change.after.data();
      const reportBefore = change.before.data();

      // Trường hợp 1: Admin cảnh cáo người dùng
      // Chỉ chạy khi admin cập nhật trạng thái từ một trạng thái khác sang "warned"
      if (reportBefore.status !== "warned" && reportAfter.status === "warned") {
        const userIdToWarn = reportAfter.reportedUserId;
        const userRef = db.collection("users").doc(userIdToWarn);
        const userDoc = await userRef.get();
        const userData = userDoc.data();

        if (!userData) {
          console.log(`User to warn ${userIdToWarn} not found.`);
          return null;
        }

        // Tăng số lần bị cảnh cáo
        const newWarningCount = (userData.warningCount || 0) + 1;
        let messageTitle = `Bạn đã bị cảnh cáo (Lần ${newWarningCount})`;
        let messageBody = `Tài khoản của bạn đã bị cảnh cáo do vi phạm.`;
        if (reportAfter.adminComment) {
          messageBody += ` Lý do: ${reportAfter.adminComment}`;
        }

        // Nếu đủ 3 lần, tự động khóa tài khoản
        if (newWarningCount >= 3) {
          await userRef.update({
            warningCount: newWarningCount,
            accountStatus: "suspended",
          });
          messageTitle = "Tài khoản của bạn đã bị KHÓA";
          messageBody = `Bạn đã bị khóa tài khoản do nhận đủ 3 lần cảnh cáo.`;
        } else {
          await userRef.update({warningCount: newWarningCount});
        }

        // Gửi thông báo đến người dùng bị cảnh cáo
        if (userData.fcmToken) {
          const payload = {
            notification: {
              title: messageTitle,
              body: messageBody,
            },
          };
          console.log(`Sending warning to ${userIdToWarn}`);
          return messaging.sendToDevice(userData.fcmToken, payload);
        }
      }

      // Trường hợp 2: Admin khóa tài khoản trực tiếp (thêm sau nếu cần)
      // ...

      return null;
    });

/**
 * Gửi thông báo khi người bán chấp nhận một lời trả giá.
 */
exports.sendOfferAcceptedNotification = functions.firestore
    .document("listings/{listingId}/offers/{offerId}")
    .onUpdate(async (change, context) => {
      const offerAfter = change.after.data();
      const offerBefore = change.before.data();

      // Chỉ chạy khi trạng thái chuyển sang "accepted"
      if (offerBefore.status !== "accepted" && offerAfter.status === "accepted") {
        const buyerId = offerAfter.buyerId;
        const listingId = context.params.listingId;

        const buyerDoc = await db.collection("users").doc(buyerId).get();
        const listingDoc = await db.collection("listings").doc(listingId).get();

        const buyerData = buyerDoc.data();
        const listingData = listingDoc.data();

        if (buyerData && listingData && buyerData.fcmToken) {
          const payload = {
            notification: {
              title: "Trả giá được chấp nhận!",
              body: `Người bán đã chấp nhận lời trả giá của bạn cho sản phẩm` +
                    ` "${listingData.title}". Hãy vào thanh toán ngay!`,
            },
          };
          console.log(`Sending offer accepted notification to ${buyerId}`);
          return messaging.sendToDevice(buyerData.fcmToken, payload);
        }
      }
      return null;
    });
