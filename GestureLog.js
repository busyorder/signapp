import mongoose from "mongoose";

const gestureLogSchema = new mongoose.Schema({
  gesture: { type: String, required: true },
  confidence: { type: Number, default: 0 },
  timestamp: { type: Date, default: Date.now }
});

export default mongoose.model("GestureLog", gestureLogSchema);
