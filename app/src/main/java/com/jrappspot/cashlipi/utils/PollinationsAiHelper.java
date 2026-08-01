package com.jrappspot.cashlipi.utils;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Pollinations AI (https://pollinations.ai) — কোনো API key ছাড়াই ব্যবহারযোগ্য ফ্রি
 * openai-compatible টেক্সট এন্ডপয়েন্ট। CashLipi-এর AI ভয়েস-এন্ট্রি ও AI ক্যাটাগরি
 * জেনারেশন — দুটো ফিচারই এই একই হেল্পার ব্যবহার করে, যাতে নেটওয়ার্ক-কলিং লজিক একজায়গায়
 * কেন্দ্রীভূত থাকে (একবার ঠিক করলে সব জায়গায় কার্যকর হয়)।
 *
 * আগে GET রিকোয়েস্টে পুরো প্রম্পট URL-এ এনকোড করে পাঠানো হতো — সেই বাগ আগেই POST + JSON
 * body-তে সরিয়ে ঠিক করা হয়েছিল।
 *
 * এখন যে সমস্যাটা ঠিক করা হলো: Pollinations-এর অ্যানোনিমাস (কোনো referrer/token ছাড়া) টিয়ার
 * বর্তমানে খুবই কড়াকড়ি রেট-লিমিটেড (প্রতি ১৫ সেকেন্ডে ১টা রিকোয়েস্ট) এবং মাঝে মাঝে সাময়িক
 * সার্ভার এরর দেয় — আগের কোড এই পরিস্থিতিতে কোনো রিট্রাই ছাড়াই সাথে সাথে "AI বুঝতে পারেনি"
 * দেখিয়ে দিত, এমনকি একটা সামান্য সাময়িক গ্লিচ হলেও। এখন:
 *  ১) রিকোয়েস্টের সাথে একটা "referrer" পাঠানো হচ্ছে (Pollinations-এর অফিসিয়াল ডকুমেন্টেশন
 *     অনুযায়ী, এটা অ্যাপটাকে শনাক্ত করে এবং অ্যানোনিমাস রিকোয়েস্ট আরও নির্ভরযোগ্যভাবে
 *     প্রসেস হওয়ার সম্ভাবনা বাড়ায়)।
 *  ২) রেট-লিমিট (HTTP 429) বা সাময়িক সার্ভার এরর (5xx) হলে একবার শর্ট ব্যাকঅফের পর
 *     স্বয়ংক্রিয়ভাবে আবার চেষ্টা করা হয়, ব্যবহারকারীকে আবার বাটনে চাপতে হয় না।
 *  ৩) এরর মেসেজ এখন কারণ অনুযায়ী নির্দিষ্ট (রেট-লিমিট vs নেটওয়ার্ক vs পার্স-এরর), যাতে
 *     সমস্যাটা কী তা বোঝা সহজ হয় এবং ভবিষ্যতে ডিবাগ করা সহজ হয়।
 */
public final class PollinationsAiHelper {

    private static final String TAG = "CashLipiAI";
    private static final String ENDPOINT = "https://text.pollinations.ai/openai";
    // অ্যাপের নিজস্ব পরিচয় — anonymous রিকোয়েস্টকে চিহ্নিত করে, Pollinations-এর ডকুমেন্টেশনে
    // সুপারিশকৃত পদ্ধতি (কোনো secret token ক্লায়েন্ট কোডে না রেখেই)।
    private static final String REFERRER = "cashlipi.app";
    private static final int MAX_ATTEMPTS = 2;

    private PollinationsAiHelper() {}

    /**
     * একটি প্রম্পট পাঠিয়ে বিশুদ্ধ JSONObject উত্তর ফেরত দেয়। ব্যাকগ্রাউন্ড থ্রেড থেকে
     * কল করতে হবে (নেটওয়ার্ক কল, main thread-এ কল করা যাবে না)।
     */
    public static JSONObject callJson(String prompt) throws Exception {
        Exception lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return callJsonOnce(prompt);
            } catch (RateLimitedException e) {
                lastError = e;
                if (attempt < MAX_ATTEMPTS) {
                    Log.w(TAG, "রেট-লিমিট হিট হয়েছে, ৪ সেকেন্ড পর আবার চেষ্টা করা হচ্ছে (attempt " + attempt + ")");
                    Thread.sleep(4000);
                }
            } catch (RetryableServerException e) {
                lastError = e;
                if (attempt < MAX_ATTEMPTS) {
                    Log.w(TAG, "সাময়িক সার্ভার এরর, ২ সেকেন্ড পর আবার চেষ্টা করা হচ্ছে (attempt " + attempt + ")");
                    Thread.sleep(2000);
                }
            }
        }
        if (lastError instanceof RateLimitedException) {
            throw new IllegalStateException("AI সার্ভার এই মুহূর্তে ব্যস্ত (অনেক বেশি রিকোয়েস্ট), কিছুক্ষণ পর আবার চেষ্টা করুন", lastError);
        }
        throw new IllegalStateException("AI সার্ভারে সাময়িক সমস্যা হচ্ছে, একটু পর আবার চেষ্টা করুন", lastError);
    }

    private static JSONObject callJsonOnce(String prompt) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", "openai");
        // jsonMode এবং response_format যুক্ত করে JSON কঠোরভাবে বলবে
        body.put("jsonMode", true);
        JSONObject responseFormat = new JSONObject();
        responseFormat.put("type", "json_object");
        body.put("response_format", responseFormat);
        body.put("referrer", REFERRER);

        JSONArray messages = new JSONArray();
        
        JSONObject sysMsg = new JSONObject();
        sysMsg.put("role", "system");
        sysMsg.put("content", "আপনি একজন অভিজ্ঞ JSON জেনারেটর। শুধুমাত্র বিশুদ্ধ, সঠিক JSON অবজেক্ট প্রদান করুন। কোনো মার্কডাউন, ব্যাখ্যা বা অতিরিক্ত টেক্সট নয়। শুধু JSON।");
        
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        
        messages.put(sysMsg);
        messages.put(userMsg);
        body.put("messages", messages);
        body.put("max_tokens", 1024);

        URL url = new URL(ENDPOINT + "?referrer=" + REFERRER);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("User-Agent", "CashLipi-Android/1.0");
        conn.setRequestProperty("Referer", "https://" + REFERRER + "/");
        conn.setDoOutput(true);
        conn.setConnectTimeout(20000);  // বর্ধিত কানেক্ট টাইমআউট
        conn.setReadTimeout(40000);      // বর্ধিত রিড টাইমআউট

        byte[] bodyBytes = body.toString().getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(bodyBytes.length);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(bodyBytes);
            os.flush();
        }

        int status = conn.getResponseCode();
        Log.d(TAG, "Pollinations HTTP স্ট্যাটাস: " + status);
        
        InputStreamReader streamReader = new InputStreamReader(
                status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(streamReader);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        String raw = sb.toString().trim();

        Log.d(TAG, "Pollinations রেসপন্স (প্রথম ২০০ অক্ষর): " + raw.substring(0, Math.min(200, raw.length())));

        if (status == 429) {
            Log.e(TAG, "Pollinations রেট-লিমিট (HTTP 429): " + raw);
            throw new RateLimitedException("রেট-লিমিট হিট");
        }
        if (status >= 500 && status < 600) {
            Log.e(TAG, "Pollinations সাময়িক সার্ভার এরর (HTTP " + status + "): " + raw);
            throw new RetryableServerException("সার্ভার এরর " + status);
        }
        if (status < 200 || status >= 300) {
            Log.e(TAG, "Pollinations HTTP " + status + ": " + raw);
            throw new IllegalStateException("AI সার্ভার এরর (" + status + ")");
        }

        String content = extractJsonFromResponse(raw);
        if (content == null || content.isEmpty()) {
            Log.e(TAG, "JSON এক্সট্র্যাক্ট ব্যর্থ, raw: " + raw);
            throw new IllegalStateException("AI উত্তর পার্স করতে ব্যর্থ");
        }

        try {
            return new JSONObject(content);
        } catch (Exception e) {
            Log.e(TAG, "JSON পার্স ব্যর্থ: " + content, e);
            throw new IllegalStateException("AI উত্তর বিশুদ্ধ JSON নয়", e);
        }
    }

    /** OpenAI-compatible রেসপন্স থেকে JSON কন্টেন্ট এক্সট্র্যাক্ট করে */
    private static String extractJsonFromResponse(String raw) {
        String content;
        try {
            JSONObject wrapper = new JSONObject(raw);
            JSONArray choices = wrapper.optJSONArray("choices");
            if (choices != null && choices.length() > 0) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.optJSONObject("message");
                if (message != null) {
                    content = message.optString("content", "");
                } else {
                    content = "";
                }
            } else {
                // সরাসরি JSON হতে পারে
                content = raw;
            }
        } catch (Exception e) {
            // wrapper পার্স ব্যর্থ, সরাসরি raw ব্যবহার করুন
            content = raw;
        }

        if (content.isEmpty()) return null;

        // মার্কডাউন ফেন্স রিমুভ করুন
        content = content.replaceAll("(?s)```json\\s*|```\\s*", "").trim();
        
        // JSON অবজেক্ট এক্সট্র্যাক্ট করুন { থেকে }
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        
        return null;
    }

    /** HTTP 429 — অ্যানোনিমাস রেট-লিমিট হিট হয়েছে, শর্ট ব্যাকঅফের পর রিট্রাই করার যোগ্য। */
    private static class RateLimitedException extends Exception {
        RateLimitedException(String message) { super(message); }
    }

    /** HTTP 5xx — সাময়িক সার্ভার সমস্যা, রিট্রাই করার যোগ্য। */
    private static class RetryableServerException extends Exception {
        RetryableServerException(String message) { super(message); }
    }
}
