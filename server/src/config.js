const dotenv = require("dotenv");

dotenv.config();

const env = process.env;

function requireOrEmpty(name) {
  const v = env[name];
  return typeof v === "string" ? v : "";
}

const config = {
  PORT: Number(env.PORT || 4000),

  RAZORPAY_KEY_ID: requireOrEmpty("RAZORPAY_KEY_ID"),
  RAZORPAY_KEY_SECRET: requireOrEmpty("RAZORPAY_KEY_SECRET"),

  // Firebase
  FIREBASE_DATABASE_URL: requireOrEmpty("FIREBASE_DATABASE_URL"),
  // Prefer service account JSON string for local/dev.
  FIREBASE_SERVICE_ACCOUNT_JSON: requireOrEmpty("FIREBASE_SERVICE_ACCOUNT_JSON"),

  // Email (nodemailer via SMTP)
  SMTP_HOST: requireOrEmpty("SMTP_HOST"),
  SMTP_PORT: Number(env.SMTP_PORT || 587),
  SMTP_USER: requireOrEmpty("SMTP_USER"),
  SMTP_PASS: requireOrEmpty("SMTP_PASS"),
  EMAIL_FROM: requireOrEmpty("EMAIL_FROM"),

  // SMS (optional)
  TWILIO_ACCOUNT_SID: requireOrEmpty("TWILIO_ACCOUNT_SID"),
  TWILIO_AUTH_TOKEN: requireOrEmpty("TWILIO_AUTH_TOKEN"),
  TWILIO_FROM: requireOrEmpty("TWILIO_FROM"),

  // Scheduler reminder
  REMINDER_LOOKAHEAD_MINUTES: Number(env.REMINDER_LOOKAHEAD_MINUTES || 5),
};

config.isRazorpayConfigured = !!(config.RAZORPAY_KEY_ID && config.RAZORPAY_KEY_SECRET);
config.isFirebaseConfigured = !!(config.FIREBASE_DATABASE_URL && config.FIREBASE_SERVICE_ACCOUNT_JSON);

module.exports = config;

