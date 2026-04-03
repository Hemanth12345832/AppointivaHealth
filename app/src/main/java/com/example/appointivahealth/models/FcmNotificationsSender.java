package com.example.appointivahealth.models;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class FcmNotificationsSender {

    private String userFcmToken;
    private String title;
    private String body;
    private Context context;

    private final String postUrl = "https://fcm.googleapis.com/fcm/send";
    // Using Legacy FCM API Key for simplicity, usually stored securely or backend
    private final String fcmServerKey = "YOUR_FCM_SERVER_KEY_HERE"; 

    public FcmNotificationsSender(String userFcmToken, String title, String body, Context context) {
        this.userFcmToken = userFcmToken;
        this.title = title;
        this.body = body;
        this.context = context;
    }

    public void SendNotifications() {
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        JSONObject mainObj = new JSONObject();
        try {
            mainObj.put("to", userFcmToken);
            JSONObject notiObject = new JSONObject();
            notiObject.put("title", title);
            notiObject.put("body", body);
            notiObject.put("icon", "icon"); // generic icon

            mainObj.put("notification", notiObject);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, postUrl, mainObj, response -> {
                Log.d("FCM_SENDER", "FCM Sent Successfully: " + response.toString());
            }, error -> {
                Log.d("FCM_SENDER", "FCM Failed: " + error.toString());
            }) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> header = new HashMap<>();
                    header.put("content-type", "application/json");
                    header.put("authorization", "key=" + fcmServerKey);
                    return header;
                }
            };
            requestQueue.add(request);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
