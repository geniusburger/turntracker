package me.geniusburger.turntracker;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.geniusburger.turntracker.model.Task;
import me.geniusburger.turntracker.model.Turn;
import me.geniusburger.turntracker.model.User;

public class Api {

    public static final int RESULT_UNKNOWN = -1;
    public static final int RESULT_TIMEOUT = -2;
    public static final int RESULT_JSON = -3;

    private static final String TAG = Api.class.getSimpleName();
    private static final int TIMEOUT_MS = 5000;

    private Preferences prefs;

    public Api(Context context) {
        prefs = new Preferences(context);
    }

    private JsonResponse jsonHttp(String method, String path, JSONObject body, Map<String, String> queryParams) {
        URL url = null;
        try {
            url = new URL(getUrl(path, queryParams));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestMethod(method);

            if(body != null) {
                conn.setDoOutput(true);
                DataOutputStream writer = new DataOutputStream(conn.getOutputStream());
                writer.writeBytes(body.toString());
                writer.flush();
                writer.close();
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = reader.readLine()) != null) {
                response.append(inputLine);
            }
            reader.close();
            return new JsonResponse(conn.getResponseCode(), response.toString());
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "jsonHttp failed to encode", e);
            return new JsonResponse(RESULT_JSON);
        } catch (SocketTimeoutException e) {
            Log.e(TAG, String.format("jsonHttp timeout for URL '%s'", url.toString()), e);
            return new JsonResponse(RESULT_TIMEOUT);
        } catch (IOException e) {
            Log.e(TAG, String.format("jsonHttp failed for URL '%s'", null == url ? "null" : url.toString()), e);
            return new JsonResponse(RESULT_UNKNOWN);
        }
    }

    private JsonResponse httpPost(String path, JSONObject body) {
        return httpPost(path, body, null);
    }

    private JsonResponse httpPost(String path, JSONObject body, Map<String, String> queryParams) {
        return jsonHttp("POST", path, body, queryParams);
    }

    private JsonResponse httpGet(String path) {
        return httpGet(path, null);
    }

    private JsonResponse httpGet(String path, Map<String, String> queryParams) {
        return jsonHttp("GET", path, null, queryParams);
    }

    private JsonResponse httpDelete(String path) {
        return httpDelete(path, null);
    }

    private JsonResponse httpDelete(String path, Map<String, String> queryParams) {
        return jsonHttp("DELETE", path, null, queryParams);
    }

    public User getUser(String username) {
        Map<String, String> params = new HashMap<>(1);
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

    private void processJsonTurns(JSONArray jsonTurns, List<Turn> turns) throws JSONException {
        int len = jsonTurns.length();
        turns.clear();
        for(int i = 0; i < len; i++) {
            turns.add(new Turn(jsonTurns.getJSONObject(i)));
        }
    }

    private void processJsonUsers(JSONArray jsonUsers, List<User> users) throws JSONException {
        int len = jsonUsers.length();
        users.clear();
        if(len > 0) {
            for(int i = 0; i < len; i++) {
                users.add(new User(jsonUsers.getJSONObject(i)));
            }
            int maxTurns = users.get(users.size() - 1).turns;
            for(User user : users) {
                user.consecutiveTurns = maxTurns - user.turns;
            }
        }
    }

    public boolean deleteTurn(long turnId, long taskId, List<User> users, List<Turn> turns) {
        try {
            Map<String, String> params = new HashMap<>(3);
            params.put("turn_id", String.valueOf(turnId));
            params.put("user_id", String.valueOf(prefs.getUserId()));
            params.put("task_id", String.valueOf(taskId));
            JsonResponse res = httpDelete("turn", params);

            if(200 == res.code) {
                processJsonUsers(res.json.getJSONArray("users"), users);
                processJsonTurns(res.json.getJSONArray("turns"), turns);
                return true;
            } else {
                if(res.e != null) {
                    Log.e(TAG, "failed to undo turn, HTTP res " + res.code, res.e);
                } else {
                    Log.e(TAG, "failed to undo turn, HTTP res " + res.code);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build json",e );
        }
        return false;
    }

    // return 0 on error
    public long takeTurn(long taskId, List<User> users, List<Turn> turns) {
        try {
            JSONObject body = new JSONObject();
            body.put("user_id", prefs.getUserId());
            body.put("task_id", taskId);
            JsonResponse res = httpPost("turn", body);

            if(200 == res.code) {
                processJsonUsers(res.json.getJSONArray("users"), users);
                processJsonTurns(res.json.getJSONArray("turns"), turns);
                return res.json.getLong("turnId");
            } else {
                if(res.e != null) {
                    Log.e(TAG, "failed to take turn, HTTP res " + res.code, res.e);
                } else {
                    Log.e(TAG, "failed to take turn, HTTP res " + res.code);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build json",e );
        }
        return 0;
    }

    public boolean getStatus(long taskId, List<User> users, List<Turn> turns) {
        Map<String, String> params = new HashMap<>(1);
        params.put("task_id", String.valueOf(taskId));
        JsonResponse res = httpGet("turns-status", params);

        if(200 == res.code) {
            try {
                processJsonUsers(res.json.getJSONArray("users"), users);
                processJsonTurns(res.json.getJSONArray("turns"), turns);
                return true;
            } catch (JSONException e) {
                Log.e(TAG, "failed to extract status from JSON", e);
            }
        } else {
            if(res.e != null) {
                Log.e(TAG, "failed to get status, HTTP res " + res.code, res.e);
            } else {
                Log.e(TAG, "failed to get status, HTTP res " + res.code);
            }
        }
        return false;
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
