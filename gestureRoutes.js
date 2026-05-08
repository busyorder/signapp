import express from "express";
import path from "path";

const router = express.Router();

/**
 * ✅ GET route — used for browser testing
 */
router.get("/", (req, res) => {
  res.send(`
    <h2>🖐️ Gesture API is running!</h2>
    <p>Use <code>POST /api/gesture</code> to send gesture data.</p>
    <pre>{
  "gesture": "hello",
  "confidence": 0.95
}</pre>
  `);
});

/**
 * ✅ POST route — receives gesture + returns video path
 */
router.post("/", (req, res) => {
  const { gesture, confidence } = req.body;
  console.log(`🖐️ Gesture received: ${gesture} (${confidence})`);

  // 🔗 Map gesture to corresponding video file
  const gestureMap = {
    hello: "/assets/videos/Hello.mp4",
    thank_you: "/assets/videos/ThankYou.mp4",
    good_morning: "/assets/videos/GoodMorning.mp4",
  };

  const normalizedGesture = gesture.toLowerCase().replace(" ", "_");
  const videoUrl = gestureMap[normalizedGesture] || null;

  if (!videoUrl) {
    console.warn(`⚠️ No matching video found for gesture: ${gesture}`);
  }

  // ✅ Broadcast gesture + video to connected clients via Socket.IO
  const io = req.app.get("io");
  if (io) {
    io.emit("gestureUpdate", {
      gesture,
      confidence,
      videoUrl,
      time: new Date(),
    });
    console.log(`⚡ Broadcasted gestureUpdate: ${gesture} (${videoUrl || "no video"})`);
  } else {
    console.warn("⚠️ Socket.IO not initialized.");
  }

  // ✅ Respond to client (Android app)
  res.json({
    message: "Gesture received successfully",
    gesture,
    confidence,
    videoUrl,
    time: new Date(),
  });
});

export default router;
