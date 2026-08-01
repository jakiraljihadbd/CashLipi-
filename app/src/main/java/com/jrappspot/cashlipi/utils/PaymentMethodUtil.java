package com.jrappspot.cashlipi.utils;

import android.content.Context;

import com.jrappspot.cashlipi.R;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * পেমেন্ট মাধ্যম (cash/bkash/nagad/rocket/bank/other) সংক্রান্ত আইকন ও লেবেল — একই জায়গা
 * থেকে ব্যবহার করা হয় যাতে AddTransactionActivity, আয়-ব্যয় লিস্ট (কার্ড আইটেম) এবং
 * ফিল্টার চিপ — সব জায়গায় একই আইকন ও নাম দেখা যায়।
 *
 * getLabel(Context, key) ব্যবহার করলে বর্তমানে সিলেক্ট করা ভাষা (bn/en/hi/ar) অনুযায়ী
 * নাম আসবে (strings.xml থেকে)। Context ছাড়া getLabel(key) শুধু পুরনো কোড ভাঙা এড়াতে
 * রাখা হয়েছে — নতুন কল-সাইটে সবসময় Context-ভিত্তিক ভার্সনটাই ব্যবহার করা উচিত।
 */
public final class PaymentMethodUtil {

    private PaymentMethodUtil() {}

    public static final Map<String, String> LABELS = new LinkedHashMap<>();
    static {
        LABELS.put("cash", "ক্যাশ");
        LABELS.put("bkash", "বিকাশ");
        LABELS.put("nagad", "নগদ");
        LABELS.put("rocket", "রকেট");
        LABELS.put("bank", "ব্যাংক");
        LABELS.put("other", "অন্যান্য");
    }

    public static int getIconRes(String key) {
        if (key == null) return R.drawable.ic_method_cash;
        switch (key) {
            case "bkash":  return R.drawable.ic_method_bkash;
            case "nagad":  return R.drawable.ic_method_nagad;
            case "rocket": return R.drawable.ic_method_rocket;
            case "bank":   return R.drawable.ic_method_bank;
            case "other":  return R.drawable.ic_method_other;
            default:       return R.drawable.ic_method_cash;
        }
    }

    /** @deprecated Context নিয়ে getLabel(Context, key) ব্যবহার করুন, যাতে সিলেক্ট করা ভাষায় নাম আসে। */
    @Deprecated
    public static String getLabel(String key) {
        String label = LABELS.get(key);
        return label != null ? label : LABELS.get("cash");
    }

    public static String getLabel(Context context, String key) {
        int res;
        switch (key == null ? "cash" : key) {
            case "bkash":  res = R.string.payment_method_bkash;  break;
            case "nagad":  res = R.string.payment_method_nagad;  break;
            case "rocket": res = R.string.payment_method_rocket; break;
            case "bank":   res = R.string.payment_method_bank;   break;
            case "other":  res = R.string.payment_method_other;  break;
            default:       res = R.string.payment_method_cash;   break;
        }
        return context.getString(res);
    }
}
