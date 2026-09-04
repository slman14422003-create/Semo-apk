const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

/**
 * تُطلَق تلقائياً عند إنشاء رسالة جديدة في مجموعة "messages"، وترسل إشعار
 * Push إلى كل المستخدمين الآخرين الذين لديهم fcmToken محفوظ، باستثناء المُرسِل.
 */
exports.onNewMessage = onDocumentCreated("messages/{messageId}", async (event) => {
  const message = event.data?.data();
  if (!message || message.isDeleted) return;

  const usersSnapshot = await db.collection("users").get();
  const tokens = [];
  usersSnapshot.forEach((doc) => {
    const user = doc.data();
    if (user.uid !== message.senderUid && user.fcmToken) {
      tokens.push(user.fcmToken);
    }
  });
  if (tokens.length === 0) return;

  const bodyText = message.type === "STICKER" ? "أرسل ستيكر" : (message.text || "").slice(0, 120);

  await admin.messaging().sendEachForMulticast({
    tokens,
    notification: {
      title: `${message.senderAvatarEmoji || ""} ${message.senderUsername}`.trim(),
      body: bodyText,
    },
    data: {
      messageId: event.params.messageId,
    },
  });
});
