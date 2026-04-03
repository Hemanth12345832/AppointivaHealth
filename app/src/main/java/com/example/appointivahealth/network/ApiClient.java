package com.example.appointivahealth.network;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.appointivahealth.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ApiClient {

    public interface JsonCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }

    private static String getBaseUrl(Context context) {
        return context.getString(R.string.api_base_url).trim();
    }

    public static void notifyAppointmentCancelled(Context context, JSONObject payload, JsonCallback callback) {
        post(context, "/api/notifications/appointment-cancelled", payload, callback);
    }

    public static void notifyAppointmentBooked(Context context, JSONObject payload, JsonCallback callback) {
        post(context, "/api/notifications/appointment-booked", payload, callback);
    }

    public static void symptomCheck(Context context, JSONObject payload, JsonCallback callback) {
        post(context, "/api/symptom-checker", payload, callback);
    }

    private static void post(Context context, String path, JSONObject payload, JsonCallback callback) {
        String baseUrl = getBaseUrl(context);
        String url = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        url += path;

        RequestQueue requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                payload,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        callback.onSuccess(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String msg = "Network error";
                        if (error != null && error.getMessage() != null) msg = error.getMessage();
                        callback.onError(msg);
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        // Timeout helps avoid hanging UI.
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(15000, 1, 1));
        requestQueue.add(request);
    }
}

