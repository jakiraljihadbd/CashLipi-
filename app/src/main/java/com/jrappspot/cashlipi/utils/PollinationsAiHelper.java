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
 * Pollinations AI (https://pollinations.ai) — CashLipi-এর সব AI ফিচার (ভয়েস-এন্ট্রি,
 * AI ক্যাটাগরি জেনারেশন, AI চ্যাট) এই একই হেল্পার ব্যবহার করে, যাতে নেটওয়ার্ক-কলিং লজিক
 * একজায়গায় কেন্দ্রীভূত থাকে (একবার ঠিক করলে সব জায়গায় কার্যকর হয়)।
 *
 * ২০২৬ সাল থেকে Pollinations-এর পুরনো anonymous/no-key এন্ডপয়েন্ট (text.pollinations.ai)
 * বন্ধ/অস্থিতিশীল হয়ে গেছে — এখন নতুন unified এন্ডপয়েন্ট (gen.pollinations.ai) ব্যবহার করতে হয়,
 * এবং সব জেনারেশন রিকোয়েস্টে বাধ্যতামূলক Authorization: Bearer API key লাগে।
 *
 * মাল্টি-কি ফলব্যাক: নিচে API_KEYS-এ একাধিক key রাখা আছে। একটা key ব্যর্থ হলে
 * (401/402/403/429/5xx — অর্থাৎ ভুল key, ব্যালেন্স শেষ, পারমিশন নেই, রেট-লিমিট, বা সাময়িক
 * সার্ভার সমস্যা) স্বয়ংক্রিয়ভাবে পরের key দিয়ে আবার চেষ্টা করা হয়। সব key ব্যর্থ হলে তবেই
 * ব্যবহারকারীকে এরর দেখানো হয়।
 *
 * TODO: প্রোডাকশনে যাওয়ার আগে এই key গুলো BuildConfig/local.properties-এ সরিয়ে নেওয়া ভালো,
 * যাতে সোর্স কোডে/APK-তে সরাসরি না থাকে। এখন টেস্টের জন্য সরাসরি বসানো হলো।
 */
public final class PollinationsAiHelper {

    private static final String TAG = "CashLipiAI";
    private static final String ENDPOINT = "https://gen.pollinations.ai/v1/chat/completions";
    private static final String MODEL = "openai";  // gen.pollinations.ai-এর বর্তমান মডেল লিস্টে available

    // FIX: Pollinations-এ অনেক মডেল (এই "openai" সহ) সম্পূর্ণ ফ্রি/anonymous — কোনো
    // Authorization key বা ব্যালেন্স ছাড়াই কাজ করে (রেট-লিমিট সাপেক্ষে)। আগে কোড সবসময়
    // sk_ paid key পাঠাত, আর ওই key-গুলোর ব্যালেন্স শেষ হয়ে যাওয়ায় 402 পাচ্ছিল।
    // এখন প্রথমে key ছাড়াই (anonymous, ফ্রি) চেষ্টা করা হয়; sk_ key-গুলো শুধু ব্যাকআপ —
    // anonymous মোড রেট-লিমিটেড/ব্যর্থ হলে তখনই ব্যবহার হয়।
    private static final String[] API_KEYS = {
            null,  // ফ্রি/anonymous — কোনো Authorization header ছাড়াই
            "sk_atzUprMYwV0kVXsTd4fw4wxUT1Arx2AH",
            "sk_jo4MV2D0IBP75Nv7ojW8Zx0PDXCfsBu9",
            "sk_z47kElJ8NwmRl3woCl1SSKJAl3AbrUi5",
            "sk_iLGfassqbw1aNbYF4iBTmgrEVlOD0S6K",
            "sk_wQJO5CBaCVAXmLymI86LhNfHqZN29W9K",
    };

    private PollinationsAiHelper() {}

    /**
     * একটি প্রম্পট পাঠিয়ে বিশুদ্ধ JSONObject উত্তর ফেরত দেয় (AI ভয়েস-এন্ট্রি, ক্যাটাগরি
     * জেনারেশন ইত্যাদির জন্য — কড়াভাবে JSON আউটপুট বাধ্য করা হয়)। ব্যাকগ্রাউন্ড থ্রেড থেকে
     * কল করতে হবে (নেটওয়ার্ক কল, main thread-এ কল করা যাবে না)।
     */
    public static JSONObject callJson(String prompt) throws Exception {
        String jsonSystemPrompt = "You are a JSON generator. You MUST respond with ONLY valid JSON object, nothing else. "
                + "No markdown, no explanations, no extra text. Just pure JSON. "
                + "If the user's instructions ask for certain field VALUES to be written in Bengali (বাংলা), "
                + "you must write those field values in Bengali script, not English, even though your instructions are in English.";

        String raw = callWithKeyFallback(jsonSystemPrompt, prompt, 512, 0.3);
        String content = extractContentFromResponse(raw);
        if (content == null || content.isEmpty()) {
            Log.e(TAG, "Failed to extract JSON from response: " + raw);
            throw new IllegalStateException("AI উত্তর পার্স করতে ব্যর্থ");
        }

        content = content.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) {
            Log.e(TAG, "Could not find JSON object in: " + content);
            throw new IllegalStateException("AI উত্তর বিশুদ্ধ JSON নয়");
        }
        String jsonStr = content.substring(start, end + 1);

        try {
            JSONObject result = new JSONObject(jsonStr);
            Log.d(TAG, "✓ Successfully parsed JSON");
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse JSON: " + jsonStr, e);
            throw new IllegalStateException("AI উত্তর বিশুদ্ধ JSON নয়", e);
        }
    }

    /**
     * সাধারণ চ্যাট/টেক্সট রিপ্লাই ফেরত দেয় (AI চ্যাট ফিচারের জন্য — মুক্ত ফরম্যাটে বাংলা
     * উত্তর, JSON বাধ্য করা হয় না)। ব্যাকগ্রাউন্ড থ্রেড থেকে কল করতে হবে।
     */
    public static String callText(String systemPrompt, String userMsg) throws Exception {
        String raw = callWithKeyFallback(systemPrompt, userMsg, 1024, 0.6);
        String content = extractContentFromResponse(raw);
        if (content == null || content.isEmpty()) {
            throw new IllegalStateException("AI উত্তর পার্স করতে ব্যর্থ");
        }
        return content.trim();
    }

    /** প্রতিটা key ধরে ধরে চেষ্টা করে (প্রথমে null = anonymous/ফ্রি) — একটাতে ব্যর্থ হলে পরেরটায় যায়। */
    private static String callWithKeyFallback(String systemPrompt, String userMsg, int maxTokens, double temperature) throws Exception {
        Exception lastError = null;
        for (int i = 0; i < API_KEYS.length; i++) {
            String key = API_KEYS[i];
            String label = (key == null) ? "Anonymous (ফ্রি)" : ("Key #" + i);
            for (int attempt = 1; attempt <= 2; attempt++) {
                try {
                    return callOnce(key, systemPrompt, userMsg, maxTokens, temperature);
                } catch (RateLimitedException e) {
                    lastError = e;
                    if (attempt < 2) {
                        Log.w(TAG, label + " রেট-লিমিট হিট, ৪ সেকেন্ড পর আবার চেষ্টা");
                        Thread.sleep(4000);
                    } else {
                        Log.w(TAG, label + " রেট-লিমিটেড, পরের key-তে যাওয়া হচ্ছে");
                    }
                } catch (RetryableServerException e) {
                    lastError = e;
                    if (attempt < 2) {
                        Log.w(TAG, label + " সাময়িক সার্ভার এরর, ২ সেকেন্ড পর আবার চেষ্টা");
                        Thread.sleep(2000);
                    } else {
                        Log.w(TAG, label + " সার্ভার এরর অব্যাহত, পরের key-তে যাওয়া হচ্ছে");
                    }
                } catch (KeyRejectedException e) {
                    lastError = e;
                    Log.w(TAG, label + " রিজেক্টেড (" + e.getMessage() + "), পরের key-তে যাওয়া হচ্ছে");
                    break;
                }
            }
        }
        if (lastError instanceof RateLimitedException) {
            throw new IllegalStateException("AI সার্ভার এই মুহূর্তে ব্যস্ত (সব key-তে রেট-লিমিট), কিছুক্ষণ পর আবার চেষ্টা করুন", lastError);
        }
        if (lastError instanceof KeyRejectedException) {
            throw new IllegalStateException("সব AI key-তে সমস্যা (মেয়াদোত্তীর্ণ/ব্যালেন্স শেষ): " + lastError.getMessage(), lastError);
        }
        throw new IllegalStateException("AI সার্ভারে সাময়িক সমস্যা হচ্ছে, একটু পর আবার চেষ্টা করুন", lastError);
    }

    private static String callOnce(String apiKey, String systemPrompt, String userMsg, int maxTokens, double temperature) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", MODEL);

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
        messages.put(new JSONObject().put("role", "user").put("content", userMsg));
        body.put("messages", messages);
        body.put("max_tokens", maxTokens);
        body.put("temperature", temperature);

        Log.d(TAG, "=== Sending Request to Pollinations (" + ENDPOINT + ") ===");

        URL url = new URL(ENDPOINT);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("User-Agent", "CashLipi-Android/1.0");
        // FIX: key null হলে (anonymous/ফ্রি চেষ্টা) Authorization header পাঠানো হয় না
        if (apiKey != null) {
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        }
        conn.setDoOutput(true);
        conn.setConnectTimeout(25000);
        conn.setReadTimeout(45000);

        byte[] bodyBytes = body.toString().getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(bodyBytes.length);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(bodyBytes);
            os.flush();
        }

        int status = conn.getResponseCode();
        Log.d(TAG, "HTTP Status: " + status);

        InputStreamReader streamReader = new InputStreamReader(
                status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(streamReader);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        String raw = sb.toString().trim();

        if (status == 429) {
            Log.e(TAG, "Rate Limited (HTTP 429)");
            throw new RateLimitedException("রেট-লিমিট হিট");
        }
        if (status == 401) {
            Log.e(TAG, "Unauthorized (HTTP 401): " + raw);
            throw new KeyRejectedException("key ভুল/মেয়াদোত্তীর্ণ (401)");
        }
        if (status == 402) {
            Log.e(TAG, "Insufficient balance (HTTP 402): " + raw);
            throw new KeyRejectedException("ব্যালেন্স শেষ (402)");
        }
        if (status == 403) {
            Log.e(TAG, "Forbidden (HTTP 403): " + raw);
            throw new KeyRejectedException("পারমিশন নেই (403)");
        }
        if (status >= 500 && status < 600) {
            Log.e(TAG, "Server Error (HTTP " + status + ")");
            throw new RetryableServerException("সার্ভার এরর " + status);
        }
        if (status < 200 || status >= 300) {
            Log.e(TAG, "HTTP Error " + status + ": " + raw);
            throw new IllegalStateException("AI সার্ভার এরর (" + status + ")");
        }

        return raw;
    }

    /** OpenAI-compatible রেসপন্স থেকে content স্ট্রিং এক্সট্র্যাক্ট করে */
    private static String extractContentFromResponse(String raw) {
        try {
            JSONObject wrapper = new JSONObject(raw);
            JSONArray choices = wrapper.optJSONArray("choices");
            if (choices != null && choices.length() > 0) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.optJSONObject("message");
                if (message != null) {
                    String extractedContent = message.optString("content", "");
                    if (!extractedContent.isEmpty()) return extractedContent;
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Not OpenAI wrapper format, treating raw response as content");
        }
        return raw;
    }

    /** HTTP 429 — রেট-লিমিট হিট হয়েছে, শর্ট ব্যাকঅফের পর রিট্রাই করার যোগ্য। */
    private static class RateLimitedException extends Exception {
        RateLimitedException(String message) { super(message); }
    }

    /** HTTP 5xx — সাময়িক সার্ভার সমস্যা, রিট্রাই করার যোগ্য। */
    private static class RetryableServerException extends Exception {
        RetryableServerException(String message) { super(message); }
    }

    /** HTTP 401/402/403 — এই key দিয়ে কাজ হবে না, রিট্রাই না করে পরের key-তে যাওয়া উচিত। */
    private static class KeyRejectedException extends Exception {
        KeyRejectedException(String message) { super(message); }
    }
}
