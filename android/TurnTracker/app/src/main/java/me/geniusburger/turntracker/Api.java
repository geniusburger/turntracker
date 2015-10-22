package me.geniusburger.turntracker;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import me.geniusburger.turntracker.model.Task;
import me.geniusburger.turntracker.model.User;

public class Api {

    public static final int RESULT_UNKNOWN = -1;
    public static final int RESULT_TIMEOUT = -2;
    public static final int RESULT_JSON = -3;

    private static final String TAG = "Api";
    private static final int TIMEOUT_MS = 5000;

    private Preferences prefs;

    public Api(Context context) {
        prefs = new Preferences(context);
    }

    private JsonResponse JsonResponse(String path) {
        return httpGet(path, null);
    }

    private JsonResponse httpGet(String path, Map<String, String> queryParams) {
        URL url = null;
        try {
            url = new URL(getUrl(path, queryParams));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = reader.readLine()) != null) {
                response.append(inputLine);
            }
            reader.close();
            return new JsonResponse(conn.getResponseCode(), response.toString());
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "httpget failed to encode", e);
            return new JsonResponse(RESULT_JSON);
        } catch (SocketTimeoutException e) {
            Log.e(TAG, String.format("httpget timeout for URL '%s'", url.toString()), e);
            return new JsonResponse(RESULT_TIMEOUT);
        } catch (IOException e) {
            Log.e(TAG, String.format("httpGet failed for URL '%s'", null == url ? "null" : url.toString()), e);
            return new JsonResponse(RESULT_UNKNOWN);
        }
    }

    public User getUser(String username) {
        Map<String, String> params = new HashMap<>();
        params.put("username", username);
        JsonResponse res = httpGet("user", params);
        if(200 == res.code) {
            try {
                JSONObject user = res.json.getJSONObject("user");
                return new User(user.getLong("id"), user.getString("username"), user.getString("displayname"));
            } catch (JSONException e) {
                Log.e(TAG, "failed to extract user from JSON", e);
            }
        } else {
            if(res.e != null) {
                Log.e(TAG, "failed to get user, HTTP res " + res.code, res.e);
            } else {
                Log.e(TAG, "failed to get user, HTTP res " + res.code);
            }
        }
        return null;
    }

    public Task[] getTasks() {
        Map<String, String> params = new HashMap<>();
        params.put("userid", String.valueOf(prefs.getUserId()));
        JsonResponse res = httpGet("tasks", params);

        if(200 == res.code) {
            try {
                JSONArray jsonTasks = res.json.getJSONArray("tasks");
                int len = jsonTasks.length();
                Task[] tasks = new Task[len];
                for (int i = 0; i < len; i++){
                    tasks[i] = new Task(jsonTasks.getJSONObject(i));
                }
                return tasks;
            } catch (JSONException e) {
                Log.e(TAG, "failed to extract tasks from JSON", e);
            }
        } else {
            if(res.e != null) {
                Log.e(TAG, "failed to get tasks, HTTP res " + res.code, res.e);
            } else {
                Log.e(TAG, "failed to get tasks, HTTP res " + res.code);
            }
        }
        return null;
    }

    private String getUrl(String path, Map<String, String> queryParams) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        sb.append("http://");
        sb.append(prefs.getServerIP());
        sb.append(":");
        sb.append(prefs.getServerPort());
        sb.append("/api/");
        sb.append(path);

        if(queryParams != null && !queryParams.isEmpty()) {
            sb.append("?");
            boolean first = true;
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                if(first) {
                    first = false;
                } else {
                    sb.append("&");
                }
                sb.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                sb.append("=");
                sb.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }
        }

        return sb.toString();
    }

    private class JsonResponse {

        public int code = 0;
        public JSONObject json;
        public JSONException e;

        public JsonResponse(int code) {
            this.code = code;
            json = new JSONObject();
        }

        public JsonResponse(int code, String response) {
            this.code = code;
            try {
                json = new JSONObject(response);
            } catch (JSONException e) {
                Log.e(TAG, "failed to parse JSON res", e);
                this.e = e;
                this.code = -1;
            }
        }
    }
}
