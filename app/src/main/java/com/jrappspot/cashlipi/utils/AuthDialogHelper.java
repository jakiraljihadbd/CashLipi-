package com.jrappspot.cashlipi.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.jrappspot.cashlipi.R;

/**
 * 🎨 AuthDialogHelper
 *
 * লগইন/সাইনআপ স্ক্রিনের জন্য একটি সুন্দর, থিম-মিলে যাওয়া কাস্টম পপআপ।
 * সাধারণ AlertDialog এর বদলে এটা ব্যবহার করলে অ্যাপের গ্রেডিয়েন্ট
 * ডিজাইনের সাথে মানানসই একটা popup দেখাবে।
 */
public class AuthDialogHelper {

    public enum Type {
        WRONG_PASSWORD,   // পাসওয়ার্ড ভুল
        NOT_REGISTERED,   // অ্যাকাউন্ট খুঁজে পাওয়া যায়নি
        NETWORK_ERROR,    // ইন্টারনেট সমস্যা
        GENERIC           // অন্য যেকোনো সমস্যা
    }

    public interface OnActionListener {
        void onClick();
    }

    public static Dialog show(Context context, Type type, String title, String message,
                               String positiveText, OnActionListener onPositive,
                               String secondaryText, OnActionListener onSecondary) {

        Dialog dialog = new Dialog(context);
        Window window = dialog.getWindow();
        if (window != null) {
            window.requestFeature(Window.FEATURE_NO_TITLE);
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setCancelable(true);

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_auth_message, null);

        View iconCircleBg = view.findViewById(R.id.iconCircleBg);
        ImageView ivIcon = view.findViewById(R.id.ivDialogIcon);
        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = view.findViewById(R.id.tvDialogMessage);
        Button btnPositive = view.findViewById(R.id.btnDialogPositive);
        TextView btnSecondary = view.findViewById(R.id.btnDialogSecondary);

        int accentColor;
        int iconRes;
        switch (type) {
            case WRONG_PASSWORD:
                accentColor = Color.parseColor("#F59E0B"); // amber
                iconRes = R.drawable.ic_dialog_lock;
                break;
            case NOT_REGISTERED:
                accentColor = Color.parseColor("#EF4444"); // red
                iconRes = R.drawable.ic_dialog_person_off;
                break;
            case NETWORK_ERROR:
                accentColor = Color.parseColor("#6366F1"); // indigo
                iconRes = R.drawable.ic_dialog_wifi_off;
                break;
            default:
                accentColor = Color.parseColor("#F59E0B"); // amber
                iconRes = R.drawable.ic_dialog_warning;
                break;
        }

        Drawable circleBg = iconCircleBg.getBackground().mutate();
        circleBg.setColorFilter(accentColor, android.graphics.PorterDuff.Mode.SRC_IN);
        iconCircleBg.setBackground(circleBg);
        ivIcon.setImageResource(iconRes);

        tvTitle.setText(title);
        tvMessage.setText(message);

        btnPositive.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accentColor));
        btnPositive.setText(positiveText != null ? positiveText : "ঠিক আছে");
        btnPositive.setOnClickListener(v -> {
            dialog.dismiss();
            if (onPositive != null) onPositive.onClick();
        });

        if (secondaryText != null) {
            btnSecondary.setVisibility(View.VISIBLE);
            btnSecondary.setText(secondaryText);
            btnSecondary.setOnClickListener(v -> {
                dialog.dismiss();
                if (onSecondary != null) onSecondary.onClick();
            });
        } else {
            btnSecondary.setVisibility(View.GONE);
        }

        dialog.setContentView(view);
        if (window != null) {
            window.setLayout(
                (int) (context.getResources().getDisplayMetrics().widthPixels * 0.86),
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        dialog.show();
        return dialog;
    }
}
