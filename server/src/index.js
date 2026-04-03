const express = require("express");
const cors = require("cors");
const Razorpay = require("razorpay");
const crypto = require("crypto");

const config = require("./config");
const { getDb } = require("./firebaseAdmin");
const notifications = require("./notifications");
const { recommendDoctors } = require("./symptomRules");

const app = express();
app.use(cors());
app.use(express.json({ limit: "1mb" }));

const razorpay = config.isRazorpayConfigured
  ? new Razorpay({ key_id: config.RAZORPAY_KEY_ID, key_secret: config.RAZORPAY_KEY_SECRET })
  : null;

function requireString(value) {
  return typeof value === "string" && value.trim().length > 0 ? value.trim() : "";
}

async function updateAppointmentPaymentFields(appointmentId, fields) {
  const db = getDb();
  if (!db || !appointmentId) return { mockDb: true };
  const ref = db.ref(`Appointments/${appointmentId}`);
  await ref.update(fields);
  return { mockDb: false };
}

app.get("/health", (req, res) => res.json({ ok: true }));

// 1) Create Razorpay order
app.post("/api/payment/create-order", async (req, res) => {
  try {
    const appointmentId = requireString(req.body?.appointmentId);
    const amount = req.body?.amount;
    const currency = requireString(req.body?.currency) || "INR";

    if (!appointmentId) return res.status(400).json({ error: "appointmentId is required" });

    const amountNum = Number(amount);
    const amountPaise = Number.isFinite(amountNum) ? Math.round(amountNum * 100) : 0;

    // If keys are missing, mock the order.
    if (!razorpay) {
      const orderId = `MOCK_ORDER_${Date.now()}`;
      await updateAppointmentPaymentFields(appointmentId, {
        status: "payment_pending_verification",
        paymentStatus: "pending_payment",
        payment_status: "pending_payment",
        paymentMode: "RAZORPAY",
        orderId,
        order_id: orderId,
        amount: String(amount || ""),
      });

      return res.json({
        mock: true,
        orderId,
        keyId: "",
      });
    }

    const order = await razorpay.orders.create({
      amount: amountPaise,
      currency,
      receipt: appointmentId,
    });

    const orderId = order.id;

    await updateAppointmentPaymentFields(appointmentId, {
      status: "payment_pending_verification",
      paymentStatus: "pending_payment",
      payment_status: "pending_payment",
      paymentMode: "RAZORPAY",
      orderId,
      order_id: orderId,
      amount: String(amount || ""),
    });

    return res.json({
      mock: false,
      orderId,
      keyId: config.RAZORPAY_KEY_ID, // public key id only
    });
  } catch (e) {
    console.error("[create-order]", e);
    return res.status(500).json({ error: e?.message || "Failed to create order" });
  }
});

// 2) Verify payment signature + confirm appointment
app.post("/api/payment/verify-payment", async (req, res) => {
  try {
    const appointmentId = requireString(req.body?.appointmentId);
    const orderId = requireString(req.body?.orderId);
    const paymentId = requireString(req.body?.paymentId);
    const signature = requireString(req.body?.signature);

    if (!appointmentId || !orderId || !paymentId || !signature) {
      return res.status(400).json({ error: "appointmentId, orderId, paymentId, signature are required" });
    }

    let verified = false;

    // Mock mode (or missing server keys): accept the callback.
    if (!razorpay) {
      verified = true;
    } else {
      // Signature verification:
      // generated_signature = hmac_sha256(order_id + "|" + razorpay_payment_id, secret)
      const generated = crypto
        .createHmac("sha256", config.RAZORPAY_KEY_SECRET)
        .update(orderId + "|" + paymentId)
        .digest("hex");

      const a = Buffer.from(generated, "utf8");
      const b = Buffer.from(signature, "utf8");
      verified = a.length === b.length && crypto.timingSafeEqual(a, b);
    }

    if (!verified) {
      return res.json({ verified: false });
    }

    await updateAppointmentPaymentFields(appointmentId, {
      status: "payment_pending_verification",
      paymentStatus: "paid",
      payment_status: "paid",
      adminVerified: false,
      paymentMode: "RAZORPAY",
      transactionId: paymentId,
      orderId,
      order_id: orderId,
      paymentId,
      payment_id: paymentId,
      paymentSignature: signature,
      payment_signature: signature,
    });

    // Email + SMS notification on booking.
    await notifications.notifyBooked(appointmentId);

    return res.json({ verified: true, paymentId, signature });
  } catch (e) {
    console.error("[verify-payment]", e);
    return res.status(500).json({ error: e?.message || "Failed to verify payment" });
  }
});

// 3) Appointment cancelled notification
app.post("/api/notifications/appointment-cancelled", async (req, res) => {
  try {
    const appointmentId = requireString(req.body?.appointmentId);
    const reason = requireString(req.body?.reason);
    if (!appointmentId) return res.status(400).json({ error: "appointmentId is required" });

    await notifications.notifyCancelled(appointmentId, reason, req.body?.cancelledByRole);
    return res.json({ ok: true });
  } catch (e) {
    console.error("[appointment-cancelled]", e);
    // Don't block the client if notification fails.
    return res.json({ ok: false, error: e?.message || "Notification failed" });
  }
});

// 3.1) Appointment booked notification (for flows that bypass Razorpay verify)
app.post("/api/notifications/appointment-booked", async (req, res) => {
  try {
    const appointmentId = requireString(req.body?.appointmentId);
    if (!appointmentId) return res.status(400).json({ error: "appointmentId is required" });

    await notifications.notifyBooked(appointmentId);
    return res.json({ ok: true });
  } catch (e) {
    console.error("[appointment-booked]", e);
    return res.json({ ok: false, error: e?.message || "Notification failed" });
  }
});

// 4) AI Symptom Checker (rule-based)
app.post("/api/symptom-checker", async (req, res) => {
  try {
    const symptoms = (req.body?.symptoms || "").toString();
    const result = await recommendDoctors(symptoms);

    const note =
      "This suggestion is not a diagnosis. If symptoms are severe or worsening, please seek medical care immediately.";

    return res.json({ ...result, note });
  } catch (e) {
    return res.status(500).json({ error: e?.message || "Symptom check failed" });
  }
});

// Reminder scheduler (bonus)
notifications.reminderSchedulerStart();

app.listen(config.PORT, () => {
  console.log(`Appointiva backend running on port ${config.PORT} (mock mode: ${!razorpay})`);
});

