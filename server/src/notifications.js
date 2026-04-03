const nodemailer = require("nodemailer");
const twilio = require("twilio");
const cron = require("node-cron");
const config = require("./config");
const { getDb } = require("./firebaseAdmin");

function getTransport() {
  if (!config.SMTP_HOST || !config.SMTP_USER || !config.SMTP_PASS || !config.EMAIL_FROM) return null;
  return nodemailer.createTransport({
    host: config.SMTP_HOST,
    port: config.SMTP_PORT,
    secure: config.SMTP_PORT === 465,
    auth: {
      user: config.SMTP_USER,
      pass: config.SMTP_PASS,
    },
  });
}

function getTwilioClient() {
  if (!config.TWILIO_ACCOUNT_SID || !config.TWILIO_AUTH_TOKEN) return null;
  return twilio(config.TWILIO_ACCOUNT_SID, config.TWILIO_AUTH_TOKEN);
}

async function readOnce(ref) {
  const snap = await ref.once("value");
  return snap.val();
}

function buildAppointmentDetails(appointment, patient, doctor) {
  const date = appointment?.date || "";
  const time = appointment?.time || "";
  return {
    appointmentId: appointment?.id || "",
    patientName: patient?.name || appointment?.patientName || "Patient",
    patientEmail: patient?.email || "",
    patientPhone: patient?.phone || "",
    doctorName: doctor?.name || appointment?.doctorName || "Doctor",
    doctorSpecialization: doctor?.specialization || "",
    date,
    time,
  };
}

function renderBookedEmail(details) {
  const spec = details.doctorSpecialization ? ` (${details.doctorSpecialization})` : "";
  return {
    subject: `Appointment booked with ${details.doctorName}${spec}`,
    text: `Hi ${details.patientName},\n\nYour appointment is booked.\n\nDoctor: ${details.doctorName}${spec}\nDate: ${details.date}\nTime: ${details.time}\n\nThanks,\nAppointiva Health`,
  };
}

function renderCancelledEmail(details, reason) {
  return {
    subject: `Appointment cancelled (${details.doctorName})`,
    text: `Hi ${details.patientName},\n\nYour appointment has been cancelled.\n\nDoctor: ${details.doctorName}\nDate: ${details.date}\nTime: ${details.time}\nReason: ${reason || "N/A"}\n\nThanks,\nAppointiva Health`,
  };
}

function renderReminderEmail(details) {
  const spec = details.doctorSpecialization ? ` (${details.doctorSpecialization})` : "";
  return {
    subject: `Reminder: appointment with ${details.doctorName}${spec}`,
    text: `Hi ${details.patientName},\n\nThis is a reminder that your appointment is scheduled in about 1 hour.\n\nDoctor: ${details.doctorName}${spec}\nDate: ${details.date}\nTime: ${details.time}\n\nSee you soon.\nAppointiva Health`,
  };
}

async function sendEmail(to, subject, text) {
  const transport = getTransport();
  if (!transport) {
    console.log("[email-mock] to=", to, "subject=", subject, "\n", text);
    return { mock: true };
  }
  await transport.sendMail({
    from: config.EMAIL_FROM,
    to,
    subject,
    text,
  });
  return { mock: false };
}

async function sendSms(toPhone, body) {
  const client = getTwilioClient();
  if (!client || !config.TWILIO_FROM) {
    console.log("[sms-mock] to=", toPhone, "\n", body);
    return { mock: true };
  }
  await client.messages.create({
    from: config.TWILIO_FROM,
    to: toPhone,
    body,
  });
  return { mock: false };
}

async function loadAppointmentWithUsers(appointmentId) {
  const db = getDb();
  if (!db) return { appointment: null };

  const appointment = await readOnce(db.ref(`Appointments/${appointmentId}`));
  if (!appointment) return { appointment: null };

  const patientId = appointment.patientId;
  const doctorId = appointment.doctorId;

  const [patient, doctor] = await Promise.all([
    patientId ? readOnce(db.ref(`Users/${patientId}`)) : Promise.resolve(null),
    doctorId ? readOnce(db.ref(`Users/${doctorId}`)) : Promise.resolve(null),
  ]);

  return { appointment, patient, doctor };
}

async function notifyBooked(appointmentId) {
  const { appointment, patient, doctor } = await loadAppointmentWithUsers(appointmentId);
  if (!appointment) return { ok: false, error: "Appointment not found" };

  const details = buildAppointmentDetails(appointment, patient, doctor);
  const email = renderBookedEmail(details);

  // Send email to patient (and doctor if available).
  const emailResults = [];
  if (details.patientEmail) {
    emailResults.push(await sendEmail(details.patientEmail, email.subject, email.text));
  }

  // Optionally notify doctor too (if they have email).
  if (doctor?.email) {
    emailResults.push(await sendEmail(doctor.email, email.subject, email.text));
  }

  // SMS to patient (optional).
  if (details.patientPhone) {
    await sendSms(details.patientPhone, `Appointment confirmed. ${details.date} at ${details.time} with ${details.doctorName}.`);
  }

  return { ok: true, emailResults };
}

async function notifyCancelled(appointmentId, reason, cancelledByRole) {
  const { appointment, patient, doctor } = await loadAppointmentWithUsers(appointmentId);
  if (!appointment) return { ok: false, error: "Appointment not found" };

  const details = buildAppointmentDetails(appointment, patient, doctor);
  const email = renderCancelledEmail(details, reason);

  const emailResults = [];
  if (details.patientEmail) {
    emailResults.push(await sendEmail(details.patientEmail, email.subject, email.text));
  }

  if (doctor?.email) {
    emailResults.push(await sendEmail(doctor.email, email.subject, email.text));
  }

  if (details.patientPhone) {
    await sendSms(details.patientPhone, `Appointment cancelled. ${details.date} at ${details.time}. Reason: ${reason || "N/A"}`);
  }

  return { ok: true, emailResults, cancelledByRole: cancelledByRole || "" };
}

async function notifyReminder(appointmentId) {
  const { appointment, patient, doctor } = await loadAppointmentWithUsers(appointmentId);
  if (!appointment) return { ok: false, error: "Appointment not found" };

  const details = buildAppointmentDetails(appointment, patient, doctor);
  const email = renderReminderEmail(details);

  if (details.patientEmail) {
    await sendEmail(details.patientEmail, email.subject, email.text);
  }

  if (doctor?.email) {
    await sendEmail(doctor.email, email.subject, email.text);
  }

  if (details.patientPhone) {
    await sendSms(details.patientPhone, `Reminder: your appointment is in about 1 hour with ${details.doctorName} (${details.date} ${details.time}).`);
  }

  return { ok: true };
}

function parseDateTimeLocal(dateStr, timeStr) {
  // dateStr expected: yyyy-MM-dd, timeStr expected: HH:mm
  try {
    const [y, m, d] = (dateStr || "").split("-").map((x) => parseInt(x, 10));
    const [hh, mm] = (timeStr || "").split(":").map((x) => parseInt(x, 10));
    if (![y, m, d, hh, mm].every((v) => Number.isFinite(v))) return null;
    return new Date(y, m - 1, d, hh, mm, 0, 0);
  } catch (e) {
    return null;
  }
}

async function reminderSchedulerStart() {
  const db = getDb();
  if (!db) {
    console.log("[reminder] Firebase not configured; reminder scheduler disabled.");
    return;
  }

  // Run frequently; only send when the appointment is ~1 hour away.
  cron.schedule("* * * * *", async () => {
    try {
      const now = new Date();
      const targetMs = now.getTime() + 60 * 60 * 1000; // 1 hour from now
      const toleranceMin = Number(config.REMINDER_LOOKAHEAD_MINUTES || 5);
      const lower = targetMs - toleranceMin * 60 * 1000;
      const upper = targetMs + toleranceMin * 60 * 1000;

      const snapshot = await db
        .ref("Appointments")
        .orderByChild("status")
        .equalTo("confirmed")
        .once("value");

      const values = snapshot.val() || {};
      const appointmentIds = Object.keys(values);

      for (const appointmentId of appointmentIds) {
        const appt = values[appointmentId];
        const reminderSentAt = appt?.reminderSentAt || "";
        if (reminderSentAt) continue;

        const apptTime = parseDateTimeLocal(appt?.date, appt?.time);
        if (!apptTime) continue;

        const t = apptTime.getTime();
        if (t >= lower && t <= upper) {
          await notifyReminder(appointmentId);
          await db.ref(`Appointments/${appointmentId}/reminderSentAt`).set(new Date().toISOString());
        }
      }
    } catch (e) {
      console.warn("[reminder] scheduler error:", e?.message || e);
    }
  });
}

module.exports = {
  notifyBooked,
  notifyCancelled,
  notifyReminder,
  reminderSchedulerStart,
};

