package com.jrappspot.cashlipi.utils;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * GoogleDriveSyncManager — Drive API v3 দিয়ে real backup upload/sync।
 *
 * কাজ:
 *  ১. App-এর data (DatabaseManager এর JSON) Drive-এ "CashLipi_backup.json" নামে আপলোড করে
 *  ২. ফাইল আগে থাকলে আপডেট করে (overwrite), না থাকলে নতুন তৈরি করে
 *  ৩. drive.file scope ব্যবহার করে — শুধু এই app যা বানায় তাতেই access থাকে
 */
public class GoogleDriveSyncManager {

    private static final String TAG = "GoogleDriveSync";
    private static final String DRIVE_FILES_URL  = "https://www.googleapis.com/drive/v3/files";
    private static final String DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files";
    private static final String BACKUP_FILE_NAME = "CashLipi_backup.json";

    public interface SyncCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    private final Context context;
    private final OkHttpClient http;
    private final ExecutorService executor;

    public GoogleDriveSyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.http = new OkHttpClient();
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     *  মূল sync method — ব্যাকগ্রাউন্ড থ্রেডে চালায়, callback মেইন থ্রেডে রিটার্ন করে না
     *    (UI কোডেই handler/runOnUiThread দিয়ে দেখাতে হবে)
     */
    public void syncNow(String jsonContent, SyncCallback callback) {
        executor.execute(() -> {
            try {
                DatabaseManager db = DatabaseManager.getInstance(context);
                String email = db.getGoogleEmail();
                if (email == null || email.isEmpty()) {
                    callback.onFailure("Google অ্যাকাউন্টে সাইন ইন করুন");
                    return;
                }

                String token = GoogleSignInHelper.getAccessToken(context, email);
                if (token == null) {
                    callback.onFailure("Access token পাওয়া যায়নি। Google পুনরায় সাইন ইন করুন।");
                    return;
                }

                String existingFileId = findExistingBackupFileId(token);

                if (existingFileId != null) {
                    updateFile(token, existingFileId, jsonContent);
                    Log.d(TAG, " Drive ফাইল আপডেট হয়েছে: " + existingFileId);
                } else {
                    String newId = createFile(token, jsonContent);
                    Log.d(TAG, " নতুন Drive ফাইল তৈরি হয়েছে: " + newId);
                }

                db.saveLastDriveSyncTime(System.currentTimeMillis());
                callback.onSuccess(" Google Drive-এ সিঙ্ক সম্পন্ন হয়েছে");

            } catch (Exception e) {
                Log.e(TAG, " Sync ব্যর্থ", e);
                callback.onFailure("সিঙ্ক ব্যর্থ: " + e.getMessage());
            }
        });
    }

    /** আগের backup ফাইল আছে কিনা চেক করে (drive.file scope-এ শুধু app-created ফাইল দেখা যায়) */
    private String findExistingBackupFileId(String token) throws IOException {
        String url = DRIVE_FILES_URL
            + "?q=" + java.net.URLEncoder.encode("name='" + BACKUP_FILE_NAME + "' and trashed=false", "UTF-8")
            + "&spaces=drive&fields=files(id,name)";

        Request req = new Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer " + token)
            .get()
            .build();

        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                Log.w(TAG, "ফাইল খোঁজা ব্যর্থ: " + resp.code());
                return null;
            }
            String body = resp.body() != null ? resp.body().string() : "";
            com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
            com.google.gson.JsonArray files = obj.getAsJsonArray("files");
            if (files != null && files.size() > 0) {
                return files.get(0).getAsJsonObject().get("id").getAsString();
            }
        }
        return null;
    }

    /** নতুন backup ফাইল তৈরি করে (multipart upload — metadata + content) */
    private String createFile(String token, String jsonContent) throws IOException {
        String metadata = "{\"name\":\"" + BACKUP_FILE_NAME + "\",\"mimeType\":\"application/json\"}";

        RequestBody requestBody = new MultipartBody.Builder()
            .setType(MultipartBody.ALTERNATIVE)
            .addPart(MultipartBody.Part.create(
                okhttp3.RequestBody.create(metadata, MediaType.parse("application/json"))))
            .addPart(MultipartBody.Part.create(
                okhttp3.RequestBody.create(jsonContent, MediaType.parse("application/json"))))
            .build();

        Request req = new Request.Builder()
            .url(DRIVE_UPLOAD_URL + "?uploadType=multipart")
            .addHeader("Authorization", "Bearer " + token)
            .post(requestBody)
            .build();

        try (Response resp = http.newCall(req).execute()) {
            String body = resp.body() != null ? resp.body().string() : "";
            if (!resp.isSuccessful()) {
                throw new IOException("Drive আপলোড ব্যর্থ (" + resp.code() + "): " + body);
            }
            com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
            return obj.get("id").getAsString();
        }
    }

    /** আগের backup ফাইল আপডেট করে (PATCH দিয়ে content overwrite) */
    private void updateFile(String token, String fileId, String jsonContent) throws IOException {
        RequestBody body = RequestBody.create(jsonContent, MediaType.parse("application/json"));

        Request req = new Request.Builder()
            .url(DRIVE_UPLOAD_URL + "/" + fileId + "?uploadType=media")
            .addHeader("Authorization", "Bearer " + token)
            .patch(body)
            .build();

        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                String err = resp.body() != null ? resp.body().string() : "";
                throw new IOException("Drive আপডেট ব্যর্থ (" + resp.code() + "): " + err);
            }
        }
    }

    /** Drive থেকে backup ফাইল ডাউনলোড করে JSON স্ট্রিং রিটার্ন করে (restore-এর জন্য) */
    public void downloadBackup(SyncCallback callback) {
        executor.execute(() -> {
            try {
                DatabaseManager db = DatabaseManager.getInstance(context);
                String email = db.getGoogleEmail();
                if (email == null || email.isEmpty()) {
                    callback.onFailure("Google অ্যাকাউন্টে সাইন ইন করুন");
                    return;
                }
                String token = GoogleSignInHelper.getAccessToken(context, email);
                if (token == null) {
                    callback.onFailure("Access token পাওয়া যায়নি");
                    return;
                }
                String fileId = findExistingBackupFileId(token);
                if (fileId == null) {
                    callback.onFailure("Drive-এ কোনো ব্যাকআপ পাওয়া যায়নি");
                    return;
                }

                Request req = new Request.Builder()
                    .url(DRIVE_FILES_URL + "/" + fileId + "?alt=media")
                    .addHeader("Authorization", "Bearer " + token)
                    .get().build();

                try (Response resp = http.newCall(req).execute()) {
                    if (!resp.isSuccessful()) {
                        callback.onFailure("ডাউনলোড ব্যর্থ (" + resp.code() + ")");
                        return;
                    }
                    String json = resp.body() != null ? resp.body().string() : "";
                    callback.onSuccess(json);
                }
            } catch (Exception e) {
                Log.e(TAG, " Download ব্যর্থ", e);
                callback.onFailure("ডাউনলোড ব্যর্থ: " + e.getMessage());
            }
        });
    }
}
