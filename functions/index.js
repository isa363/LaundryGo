const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendLaundryFinishedNotification = functions.database
    .ref("/machines/{machineId}/history/{historyId}")
    .onCreate(async (snapshot, context) => {
      try {
        const history = snapshot.val();
        if (!history) return null;

        const uid = history.uid || null;
        const aptNumber = history.aptNumber || null;
        const buildingCode = history.buildingCode || null;
        const machineId = history.machineId || context.params.machineId;
        const machineName = history.machineName || "Your machine";

        let userDoc = null;

        if (uid) {
          const userSnap = await admin.firestore()
              .collection("users")
              .doc(uid)
              .get();

          if (userSnap.exists) {
            userDoc = userSnap;
          }
        }

        if (!userDoc && aptNumber && buildingCode) {
          const querySnap = await admin.firestore()
              .collection("users")
              .where("aptNumber", "==", aptNumber)
              .where("buildingCode", "==", buildingCode)
              .limit(1)
              .get();

          if (!querySnap.empty) {
            userDoc = querySnap.docs[0];
          }
        }

        if (!userDoc) {
          console.log("No matching user found for history record");
          return null;
        }

        const userData = userDoc.data();
        const token = userData.fcmToken;

        if (!token) {
          console.log("User has no FCM token");
          return null;
        }

        const title = "LaundryGo!";
        const body = `${machineName} has finished. Your laundry is ready.`;

        const message = {
          token: token,
          notification: {
            title: title,
            body: body,
          },
          data: {
            title: title,
            message: body,
            machineId: String(machineId),
            machineName: String(machineName),
          },
          android: {
            priority: "high",
            notification: {
              channelId: "laundry_channel",
              sound: "default",
            },
          },
          apns: {
            payload: {
              aps: {
                sound: "default",
              },
            },
          },
        };

        const response = await admin.messaging().send(message);
        console.log("Notification sent:", response);
        return null;
      } catch (error) {
        console.error("Error sending notification:", error);
        return null;
      }
    });