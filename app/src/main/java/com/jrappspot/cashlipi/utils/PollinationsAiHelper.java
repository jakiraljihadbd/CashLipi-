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
 * গুরুত্বপূর্ণ: আগে GET রিকোয়েস্টে পুরো প্রম্পট (ক্যাটাগরি তালিকাসহ) URL-এ এনকোড করে
 * পাঠানো হতো — ক্যাটাগরি তালিকা বড় হলে URL অনেক লম্বা হয়ে যেত, যার ফলে মাঝেমধ্যে
 * "AI বুঝতে পারেনি" এরর দেখাতো। এখন POST + JSON body ব্যবহার করা হচ্ছে বলে প্রম্পট যত
 * বড়ই হোক (ক্যাটাগরি তালিকা যতই বাড়ানো হোক), URL length limit-এর কোনো সমস্যা হয় না।
 */
public final class PollinationsAiHelper {

    private static final String TAG = "CashLipiAI";
    private static final String ENDPOINT = "https://text.pollinations.ai/openai";

    private PollinationsAiHelper() {}

    /**
     * একটি প্রম্পট পাঠিয়ে বিশুদ্ধ JSONObject উত্তর ফেরত দেয়। ব্যাকগ্রাউন্ড থ্রেড থেকে
     * কল করতে হবে (নেটওয়ার্ক কল, main thread-এ কল করা যাবে না)।
     */
    public static JSONObject callJson(String prompt) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", "openai");
        body.put("jsonMode", true);

        JSONArray messages = new JSONArray();
        JSONObject sysMsg = new JSONObject();
        sysMsg.put("role", "system");
        sysMsg.put("content", "তুমি শুধুমাত্র বিশুদ্ধ JSON আউটপুট দাও, অন্য কোনো লেখা, ব্যাখ্যা বা মার্কডাউন দাও না।");
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.put(sysMsg);
        messages.put(userMsg);
        body.put("messages", messages);

        URL url = new URL(ENDPOINT);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        InputStreamReader streamReader = new InputStreamReader(
                status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(streamReader);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        String raw = sb.toString().trim();

        if (status < 200 || status >= 300) {
            Log.e(TAG, "Pollinations HTTP " + status + ": " + raw);
            throw new IllegalStateException("AI সার্ভার এরর: " + status);
        }

        // openai-compatible রেসপন্স ফরম্যাট: {"choices":[{"message":{"content":"...json string..."}}]}
        String content;
        try {
            JSONObject wrapper = new JSONObject(raw);
            JSONArray choices = wrapper.optJSONArray("choices");
            if (choices != null && choices.length() > 0) {
                content = choices.getJSONObject(0).getJSONObject("message").getString("content");
            } else {
                content = raw; // কিছু ক্ষেত্রে সরাসরি বডিই বিশুদ্ধ JSON হতে পারে
            }
        } catch (Exception ignoredWrapper) {
            content = raw;
        }

        content = content.replaceAll("(?s)```json|```", "").trim();
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) {
            Log.e(TAG, "JSON পার্স ব্যর্থ, raw content: " + content);
            throw new IllegalStateException("AI থেকে সঠিক উত্তর আসেনি");
        }
        return new JSONObject(content.substring(start, end + 1));
    }
}
