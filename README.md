# Appointiva Health 🏥

Appointiva Health is a comprehensive medical appointment and clinic management application. Built natively for Android, it features a robust role-based system for Patients, Doctors, and Administrators, tightly integrated with a Node.js backend. 

The application streamlines the entire clinical pipeline from AI-driven symptom preliminary checks to secure payment processing and verified appointment scheduling.

## 🚀 Key Features

*   **Role-Based Dashboards**: Dedicated portal interfaces for Patients (booking/records), Doctors (managing appointments/patients), and Admins (system verification and reporting).
*   **Gemini AI Symptom Checker**: A conversational medical assistant built on **Google Gemini 2.5 Flash**. It analyzes spoken or typed symptoms, recommends the exact specialist to visit, persists chat histories across sessions, and supports native Android Voice-to-Text input.
*   **Secure Payment Verification Workflow**: Integrates **Razorpay Checkout SDK**. Patient payments are routed into a "Pending Verification" queue, securely halting the Doctor from finalizing the appointment until an Administrator verifies the transaction and amount manually.
*   **Medical Record Cloud Storage**: Enables doctors to seamlessly upload patient prescriptions or medical documents directly to **Firebase Cloud Storage**, replacing local data limits with reliable cloud URLs.
*   **Feedback & Reporting Systems**: Full administrative data analytics tracking cancelled or completed appointments and comprehensive patient feedback systems.

## 🛠️ Architecture & Tech Stack

The architecture is divided into two decoupled monolithic servers: a thick Android client and a supportive Express.js server for API integrations.

### **Frontend (Android)**
*   **Language**: Java, XML
*   **Design**: Material Components, dynamic Android UI bindings, Glide for image loading.
*   **Authentication**: Firebase Authentication.
*   **Data Lake**: Firebase Realtime Database (using optimized `child()` query rules for robust data synchronization).
*   **File Storage**: Firebase Storage.

### **Backend (Node.js)**
*   **Framework**: Express.js
*   **AI Engine**: `@google/genai` (Google Gemini AI).
*   **Payments**: Razorpay Node SDK (Order creation and webhook validation).
*   **Notifications**: Nodemailer / Twilio architecture (Automated appointment scheduling/rejection emails).
*   **Environment**: Dotenv for managing system secrets.

## ⚙️ Installation & Setup

### Requirements
*   Android Studio
*   Node.js (v18+)
*   Firebase Project (Auth, Realtime DB, Storage enabled)
*   Razorpay API Keys
*   Google Gemini API Key

### Backend Setup
1. Navigate to the server folder: `cd server`
2. Install dependencies: `npm install`
3. Configure the environment variables by creating a `.env` file from the example, and insert your keys:
   ```env
   PORT=4000
   RAZORPAY_KEY_ID=your_key
   RAZORPAY_KEY_SECRET=your_secret
   GEMINI_API_KEY=your_gemini_key
   ```
4. Start the server: `npm run start`

### Android Setup
1. Open the project in **Android Studio**.
2. Connect your **Firebase** instance by placing your `google-services.json` inside the `app/` directory.
3. Update `strings.xml` to point the `api_base_url` to your backend's local network IP.
4. Sync Gradle and run the application on an emulator or physical device.

## 🤝 Contributing
Contributions, issues, and feature requests are welcome!

### Author
[Hemanth12345832](https://github.com/Hemanth12345832)

---
*Built with ❤️ for better healthcare management.*
