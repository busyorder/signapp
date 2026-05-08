import express from "express";
import mongoose from "mongoose";
import dotenv from "dotenv";
import morgan from "morgan";
import cors from "cors";
import path from "path";
import { fileURLToPath } from "url";
import http from "http";
import { Server } from "socket.io";
import gestureRoutes from "./routes/gestureRoutes.js";

dotenv.config();

// ✅ Fix for ES modules (__dirname not defined)
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const server = http.createServer(app);

// ✅ Initialize Socket.IO with CORS
const io = new Server(server, {
  cors: {
    origin: "*", // Allow Android connection (or specify domain)
    methods: ["GET", "POST"],
  },
});

// ✅ Attach Socket.IO instance to app (for routes to use)
app.set("io", io);

// ✅ Middlewares
app.use(cors());
app.use(express.json({ limit: "20mb" }));
app.use(morgan("dev"));

// ✅ Serve static assets
const assetsPath = path.resolve(process.env.ASSETS_DIR || "./assets");
app.use("/assets", express.static(assetsPath));

// ✅ Routes
app.use("/api/gesture", gestureRoutes);

// ✅ Health check
app.get("/api/health", (req, res) => {
  res.json({
    status: "ok",
    mongo: mongoose.connection.readyState === 1 ? "connected" : "connecting",
    time: new Date(),
  });
});

// ✅ Default route
app.get("/", (req, res) => {
  res.send(`
    <h2>🖐️ Sign Gesture Backend Running ✅</h2>
    <p>MongoDB: ${mongoose.connection.readyState === 1 ? "Connected" : "Connecting..."}</p>
    <p>Socket.IO: Enabled ⚡</p>
    <ul>
      <li><a href="/api/health">/api/health</a> – Server health check</li>
      <li><a href="/api/gesture">/api/gesture</a> – Gesture endpoint</li>
      <li><a href="/api/videos">/api/videos</a> – Gesture videos list</li>
    </ul>
  `);
});

// ✅ Example video route
app.get("/api/videos", (req, res) => {
  const videos = [
    { name: "Hello", url: "/assets/videos/Hello.mp4" },
    { name: "Thank You", url: "/assets/videos/ThankYou.mp4" },
    { name: "Good Morning", url: "/assets/videos/GoodMorning.mp4" },
  ];
  res.json({ videos });
});

// ✅ MongoDB connection
const PORT = process.env.PORT || 4000;
const MONGO_URI = process.env.MONGO_URI;

if (!MONGO_URI) {
  console.error("❌ Missing MONGO_URI in .env file");
  process.exit(1);
}

console.log("Connecting to MongoDB:", MONGO_URI);

mongoose
  .connect(MONGO_URI, { useNewUrlParser: true, useUnifiedTopology: true })
  .then(() => {
    console.log("✅ MongoDB connected successfully");

    // Start server
    server.listen(PORT, () => console.log(`🚀 Server running on port ${PORT}`));
  })
  .catch((err) => {
    console.error("❌ MongoDB connection error:", err.message);
    process.exit(1);
  });

// ✅ Socket.IO event handling
io.on("connection", (socket) => {
  console.log("⚡ Client connected:", socket.id);

  // Example: Handle gesture events
  socket.on("gestureDetected", (data) => {
    console.log("🖐️ Real-time gesture from client:", data);
    io.emit("gestureUpdate", data); // broadcast to all clients
  });

  socket.on("disconnect", () => {
    console.log("🔴 Client disconnected:", socket.id);
  });
});
