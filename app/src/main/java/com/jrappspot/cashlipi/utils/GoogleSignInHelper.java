package com.jrappspot.cashlipi.utils;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class GoogleSignInHelper {

    private static final String TAG = "GoogleSignInHelper";
    //  FIX: Web Client ID (OAuth 2.0 → Web application type থেকে নিন)
    private static final String WEB_CLIENT_ID =
        "65351462715-dhm0a8k60fd356ifn6sq40m8fa00u99c.apps.googleusercontent.com";

    public interface SignInCallback {
        void onSuccess(String name, String email, String photoUrl);
        void onFailure(String error);
    }

    private final GoogleSignInClient client;

    public GoogleSignInHelper(Activity activity, String unused) {
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .requestProfile()
            //  Drive sync: appdata + file scope (শুধু এই app যেসব ফাইল বানায় সেগুলোতে access)
            .requestScopes(new Scope("https://www.googleapis.com/auth/drive.file"))
            .build();
        client = GoogleSignIn.getClient(activity, options);
    }

    /**
     *  Drive sync-এর জন্য OAuth access token নিয়ে আসে (background thread-এ কল করতে হবে)।
     * GoogleAuthUtil ব্যবহার করে scoped access token fetch করে।
     */
    public static String getAccessToken(android.content.Context context, String accountEmail) {
        try {
            String scope = "oauth2:https://www.googleapis.com/auth/drive.file";
            android.accounts.Account account = new android.accounts.Account(accountEmail, "com.google");
            return com.google.android.gms.auth.GoogleAuthUtil.getToken(context, account, scope);
        } catch (Exception e) {
            Log.e(TAG, " Access token ব্যর্থ: " + e.getMessage());
            return null;
        }
    }

    /**
     *  FIX: প্রতিবার sign-in এর আগে sign-out করা হচ্ছে।
     * এটা না করলে account chooser dialog কাজ না করতে পারে।
     */
    public void getSignInIntentAfterSignOut(Activity activity, SignInReadyCallback cb) {
        // FirebaseAuth session-ও সাথে সাথে সাফ করা হচ্ছে, নাহলে account
        // switch করলে পুরনো uid-এর session লেগে থেকে নতুন account-এর ডাটা
        // ভুল জায়গায় sync হতে পারে।
        FirebaseAuth.getInstance().signOut();
        client.signOut().addOnCompleteListener(activity, task -> {
            cb.onReady(client.getSignInIntent());
        });
    }

    public interface SignInReadyCallback {
        void onReady(Intent intent);
    }

    // পুরানো method — backward compat
    public Intent getSignInIntent() {
        return client.getSignInIntent();
    }

    public void handleResult(Intent data, SignInCallback cb) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            authenticateWithFirebase(account, cb);
        } catch (ApiException e) {
            //  FIX: status code লগ করুন — error এর কারণ বুঝতে সাহায্য করবে
            Log.e(TAG, " Sign-in failed. StatusCode: " + e.getStatusCode()
                + " | Message: " + e.getMessage());
            String reason = getReadableError(e.getStatusCode());
            cb.onFailure("Google Sign-In ব্যর্থ (" + e.getStatusCode() + "): " + reason);
        }
    }

    /**
     * 🔧 SESSION REPAIR: যেসব user এই fix আসার আগে থেকেই Google দিয়ে login
     * করা ছিল, তাদের FirebaseAuth session কখনোই তৈরি হয়নি — ফলে তারা
     * logout/login না করা পর্যন্ত "PERMISSION_DENIED" পেতেই থাকবে।
     *
     * এই method background-এ silently (কোনো UI/popup ছাড়াই) Google account
     * চেক করে idToken রিফ্রেশ করে এবং FirebaseAuth session তৈরি করে দেয়।
     * FirestoreSyncManager প্রতিটা sync-এর আগে এটা call করে, তাই user
     * কিছু না করলেও পরের sync attempt-এই session নিজে নিজে ঠিক হয়ে যায়।
     */
    public static void silentReauth(android.content.Context context, SignInCallback cb) {
        GoogleSignInAccount cached = GoogleSignIn.getLastSignedInAccount(context);
        if (cached == null) {
            cb.onFailure("কোনো Google account cache করা নেই");
            return;
        }
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .requestProfile()
            .build();
        GoogleSignInClient silentClient = GoogleSignIn.getClient(context, options);
        silentClient.silentSignIn()
            .addOnSuccessListener(account -> {
                Log.d(TAG, "🔧 Silent Google re-auth সফল: " + account.getEmail());
                authenticateWithFirebase(account, cb);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "🔧 Silent Google re-auth ব্যর্থ: " + e.getMessage());
                cb.onFailure("Silent re-auth ব্যর্থ: " +
                    (e.getMessage() != null ? e.getMessage() : "অজানা সমস্যা"));
            });
    }

    /**
     * Google idToken কে FirebaseAuth credential-এ exchange করে প্রকৃত
     * FirebaseAuth session তৈরি করে — এটাই মূল fix, handleResult() এবং
     * silentReauth() দুই জায়গা থেকেই ব্যবহৃত হয়।
     */
    private static void authenticateWithFirebase(GoogleSignInAccount account, SignInCallback cb) {
        String name  = account.getDisplayName() != null ? account.getDisplayName() : "";
        String email = account.getEmail() != null ? account.getEmail() : "";
        String photo = account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : "";
        String idToken = account.getIdToken();

        if (idToken == null || idToken.isEmpty()) {
            Log.e(TAG, "ID token পাওয়া যায়নি — WEB_CLIENT_ID চেক করুন");
            cb.onFailure("Google Sign-In ব্যর্থ: ID token পাওয়া যায়নি");
            return;
        }

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnSuccessListener(authResult -> {
                Log.d(TAG, " Firebase Auth session ready. uid=" +
                    (authResult.getUser() != null ? authResult.getUser().getUid() : "null"));
                cb.onSuccess(name, email, photo);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, " Firebase Auth sign-in ব্যর্থ: " + e.getMessage());
                cb.onFailure("Firebase Auth ব্যর্থ: " +
                    (e.getMessage() != null ? e.getMessage() : "অজানা সমস্যা"));
            });
    }

    private String getReadableError(int code) {
        switch (code) {
            case 10:  return "Developer Error — SHA-1 fingerprint বা OAuth Client ID ভুল। Google Console চেক করুন।";
            case 12500: return "Sign-in cancelled";
            case 12501: return "Sign-in cancelled by user";
            case 7:   return "Network error — ইন্টারনেট সংযোগ চেক করুন";
            default:  return "Unknown error";
        }
    }

    public void signOut(Activity activity, Runnable onDone) {
        // FirebaseAuth session-ও সাইন আউট করা হচ্ছে (আগে শুধু Google client
        // সাইন আউট হতো, FirebaseAuth uid সেশন থেকেই যেত)।
        FirebaseAuth.getInstance().signOut();
        client.signOut().addOnCompleteListener(activity, t -> {
            if (onDone != null) onDone.run();
        });
    }

    public static GoogleSignInAccount getLastSignedIn(Activity activity) {
        return GoogleSignIn.getLastSignedInAccount(activity);
    }
}
