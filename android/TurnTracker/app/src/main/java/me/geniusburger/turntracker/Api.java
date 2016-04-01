package me.geniusburger.turntracker;

import android.content.Context;
import android.system.ErrnoException;
import android.system.OsConstants;
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
import java.text.ParseException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.geniusburger.turntracker.model.Task;
import me.geniusburger.turntracker.model.Turn;
import me.geniusburger.turntracker.model.User;

public class Api {

    public static final int RESULT_NETWORK = -1;
    public static final int RESULT_TIMEOUT = -2;
    public static final int RESULT_JSON = -3;
    public static final int RESULT_SERVER = -4;
    public static final int RESULT_NOT_FOUND = -5;
    public static final int RESULT_UNREACHABLE = -6;

    private static final String TAG = Api.class.getSimpleName();
    private static final int TIMEOUT_MS = 5000;

    private Preferences prefs;

    public Api(Context context) {
        prefs = new Preferences(context);
    }

    private JsonResponse jsonHttp(String method, String path, JSONObject body, Map<String, String> queryParams) {
        URL url = null;
        HttpURLConnection conn = null;
        try {
            url = new URL(getUrl(path, queryParams));
            conn = (HttpURLConnection) url.openConnection();
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
            int code = RESULT_NETWORK;
            try {
                code = conn.getResponseCode();
            } catch (IOException noCode) {
                // try to get an errno exception
                Throwable cause = e.getCause();
                if(cause instanceof ErrnoException) {
                    ErrnoException ee = (ErrnoException)cause;
                    if(OsConstants.EHOSTUNREACH == ee.errno) {
                        return new JsonResponse(RESULT_UNREACHABLE);
                    }
                    // let it show the network error
                }
            }
            Log.e(TAG, String.format("jsonHttp failed for URL '%s', code %s", url, code), e);
            return new JsonResponse(code);
        }
    }

    private JsonResponse httpPost(String path, JSONObject body) {
        return httpPost(path, body, null);
    }

    private JsonResponse httpPost(String path, JSONObject body, Map<String, String> queryParams) {
        return jsonHttp("POST", path, body, queryParams);
    }

    private JsonResponse httpPut(String path, JSONObject body) {
        return httpPut(path, body, null);
    }

    private JsonResponse httpPut(String path, JSONObject body, Map<String, String> queryParams) {
        return jsonHttp("PUT", path, body, queryParams);
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
                return new User(user.getLong("id"), user.getString("username"), user.getString("displayname"), user.getString("androidtoken"));
            } catch (JSONException e) {
                Log.w(TAG, "user not found in JSON", e);
                return new User(RESULT_NOT_FOUND);
            }
        }

        if(res.e != null) {
            Log.e(TAG, "failed to get user, HTTP res " + res.code, res.e);
        } else {
            Log.e(TAG, "failed to get user, HTTP res " + res.code);
        }

        if(500 == res.code) {
            return new User(RESULT_SERVER);
        }

        if(res.code < 0) {
            return new User(res.code);
        }

        return null;
    }

    private void processJsonTurns(JSONArray jsonTurns, List<Turn> turns) throws JSONException, ParseException {
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

    public boolean updateToken(String token) {
        try {
            JSONObject body = new JSONObject();
            body.put("user_id", String.valueOf(prefs.getUserId()));
            body.put("token", token);
            JsonResponse res = httpPut("android", body);

            if(200 == res.code) {
                return true;
            } else {
                if(res.e != null) {
                    Log.e(TAG, "failed to update android token, HTTP res " + res.code, res.e);
                } else {
                    Log.e(TAG, "failed to update android token, HTTP res " + res.code);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build json", e);
        }
        return false;
    }

    public boolean deleteToken() {
        Map<String, String> params = new HashMap<>(1);
        params.put("user_id", String.valueOf(prefs.getUserId()));
        JsonResponse res = httpDelete("android", params);
        if(200 == res.code) {
            return true;
        } else {
            if(res.e != null) {
                Log.e(TAG, "failed to delete android token, HTTP res " + res.code, res.e);
            } else {
                Log.e(TAG, "failed to delete android token, HTTP res " + res.code);
            }
        }
        return false;
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
        } catch (JSONException | ParseException e) {
            Log.e(TAG, "Failed to build json",e );
        }
        return false;
    }

    /**
     * Save a new or updated task
     * @param task The task the save or update.
     * @return The task ID on success, 0 on failure.
     */
    public long saveTask(Task task, List<Long> users) {
        try {
            JSONObject body = new JSONObject();
            body.put("id", task.id);
            body.put("name", task.name);
            body.put("hours", task.periodicHours);
            body.put("creator", task.creatorUserID);
            JSONArray userArray = new JSONArray();
            for(Long userId : users) {
                userArray.put(userId);
            }
            body.put("users", userArray);

            JsonResponse res = httpPut("task", body);

            if(200 == res.code) {
                return res.json.getLong("task_id");
            } else {
                if(res.e != null) {
                    Log.e(TAG, "failed to save turn, HTTP res " + res.code, res.e);
                } else {
                    Log.e(TAG, "failed to save turn, HTTP res " + res.code);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build json", e);
        }
        return 0;
    }

    public boolean setSubscription(Task task) {
        if(!task.notification) {
            return deleteSubscription(task);
        }
        try {
            JSONObject body = new JSONObject();
            body.put("userId", prefs.getUserId());
            body.put("taskId", task.id);
            JSONObject note = new JSONObject();
            note.put("reason_id", task.reasonID);
            note.put("method_id", task.methodID);
            note.put("reminder", task.reminder ? 1 : 0);
            body.put("note", note);
            JsonResponse res = httpPut("subscription", body);

            if(200 == res.code) {
                return true;
            } else {
                if(res.e != null) {
                    Log.e(TAG, "failed to set subscription, HTTP res " + res.code, res.e);
                } else {
                    Log.e(TAG, "failed to set subscription, HTTP res " + res.code);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build setSubscription json", e);
        }
        return false;
    }

    private boolean deleteSubscription(Task task) {
        Map<String, String> params = new HashMap<>(2);
        params.put("userId", String.valueOf(prefs.getUserId()));
        params.put("taskId", String.valueOf(task.id));
        JsonResponse res = httpDelete("subscription", params);

        if(200 == res.code) {
            return true;
        } else {
            if(res.e != null) {
                Log.e(TAG, "failed to delete subscription, HTTP res " + res.code, res.e);
            } else {
                Log.e(TAG, "failed to delete subscription, HTTP res " + res.code);
            }
        }
        return false;
    }

    // return 0 on error
    public long takeTurn(long taskId, Calendar date, List<User> users, List<Turn> turns) {
        try {
            JSONObject body = new JSONObject();
            body.put("user_id", prefs.getUserId());
            body.put("task_id", taskId);
            if(date != null) {
                body.put("date", date.getTimeInMillis());
            }
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
        } catch (JSONException | ParseException e) {
            Log.e(TAG, "Failed to build json", e);
        }
        return 0;
    }

    public boolean getTaskUsers(long taskId, List<User> users) {
        Map<String, String> params = new HashMap<>(1);
        params.put("id", String.valueOf(taskId));
        JsonResponse res = httpGet("taskusers", params);

        if(200 == res.code) {
            try {
                JSONArray jsonUsers = res.json.getJSONArray("users");
                int len = jsonUsers.length();
                users.clear();
                if(len > 0) {
                    for(int i = 0; i < len; i++) {
                        JSONObject user = jsonUsers.getJSONObject(i);
                        users.add(new User(user.getLong("id"), user.getString("name"), user.getInt("selected") == 1));
                    }
                }
                return true;
            } catch (JSONException e) {
                Log.e(TAG, "failed to extract task users from JSON", e);
            }
        } else {
            if(res.e != null) {
                Log.e(TAG, "failed to get task users, HTTP res " + res.code, res.e);
            } else {
                Log.e(TAG, "failed to get task users, HTTP res " + res.code);
            }
        }
        return false;
    }

    public boolean getStatus(Task task, List<User> users, List<Turn> turns, Map<Integer, String> reasons, Map<Integer, String> methods) {
        Map<String, String> params = new HashMap<>(1);
        params.put("task_id", String.valueOf(task.id));
        params.put("user_id", String.valueOf(prefs.getUserId()));
        JsonResponse res = httpGet("turns-status", params);

        if(200 == res.code) {
            try {
                getEnumValues(res.json.getJSONArray("reasons"), reasons);
                getEnumValues(res.json.getJSONArray("methods"), methods);
                processJsonUsers(res.json.getJSONArray("users"), users);
                processJsonTurns(res.json.getJSONArray("turns"), turns);
                task.update(res.json.getJSONObject("task"));
                return true;
            } catch (JSONException | ParseException e) {
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

    private void getEnumValues(JSONArray json, Map<Integer, String> values) throws JSONException {
        int len = json.length();
        for(int i = 0; i < len; i++) {
            JSONObject e = json.getJSONObject(i);
            values.put(e.getInt("id"), e.getString("description"));
        }
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
            } catch (JSONException | ParseException e) {
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
