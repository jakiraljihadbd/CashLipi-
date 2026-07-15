package com.jrappspot.cashlipi.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.jrappspot.cashlipi.R;

/**
 * আয়, ব্যয়, দেনা, পাওনা ও সঞ্চয় — এই পাঁচটি ক্যাটাগরির যেকোনো একটিতে নতুন এন্ট্রি
 * সংরক্ষণ করার পর এই সাকসেস পপআপ দেখানো হয়। প্রতিটি ক্যাটাগরির নিজস্ব থিম কালার
 * অনুযায়ী আইকন, টাইটেল ও প্রাইমারি বাটনের রঙ বদলে যায়।
 */
public class SuccessPopup {

    public enum Category { INCOME, EXPENSE, DENA, PABONA, SAVINGS }

    public interface ActionListener {
        void onAction();
    }

    public static void show(Context context,
                             Category category,
                             String title,
                             String message,
                             ActionListener onAddAgain,
                             ActionListener onViewList) {

        Dialog dialog = new Dialog(context, R.style.AppDialog);
        Window window = dialog.getWindow();
        if (window != null) {
            window.requestFeature(Window.FEATURE_NO_TITLE);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            // পুরো স্ক্রিন-প্রস্থ জুড়ে উইন্ডো নিয়ে ভিতরের কার্ডটাকে (FrameLayout gravity=center দিয়ে)
            // ঠিক মাঝখানে বসানো হচ্ছে — এতে কোনো ডিভাইসেই বাম দিকে সরে যাওয়ার সমস্যা হবে না
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
        }
        dialog.setContentView(R.layout.dialog_success_popup);
        dialog.setCancelable(true);

        int accentColor = ContextCompat.getColor(context, getAccentColorRes(category));

        // আইকন সার্কেল রঙ
        FrameLayout iconCircle = dialog.findViewById(R.id.successIconCircle);
        if (iconCircle != null) {
            Drawable bg = iconCircle.getBackground().mutate();
            DrawableCompat.setTint(bg, accentColor);
            iconCircle.setBackground(bg);
        }

        // টাইটেল রঙ
        TextView tvTitle = dialog.findViewById(R.id.tvSuccessTitle);
        if (tvTitle != null) {
            tvTitle.setText(title);
            tvTitle.setTextColor(accentColor);
        }

        TextView tvMessage = dialog.findViewById(R.id.tvSuccessMessage);
        if (tvMessage != null) tvMessage.setText(message);

        TextView tvSubtitle = dialog.findViewById(R.id.tvSuccessSubtitle);
        if (tvSubtitle != null) tvSubtitle.setText(context.getString(R.string.app_name));

        // প্রাইমারি বাটন রঙ ("আবার যোগ করুন")
        TextView btnAddAgain = dialog.findViewById(R.id.btnAddAgain);
        if (btnAddAgain != null) {
            Drawable btnBg = btnAddAgain.getBackground().mutate();
            DrawableCompat.setTint(btnBg, accentColor);
            btnAddAgain.setBackground(btnBg);
            btnAddAgain.setOnClickListener(v -> {
                dialog.dismiss();
                if (onAddAgain != null) onAddAgain.onAction();
            });
        }

        TextView btnViewList = dialog.findViewById(R.id.btnViewList);
        if (btnViewList != null) {
            btnViewList.setOnClickListener(v -> {
                dialog.dismiss();
                if (onViewList != null) onViewList.onAction();
            });
        }

        ImageView btnClose = dialog.findViewById(R.id.btnCloseSuccessPopup);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private static int getAccentColorRes(Category category) {
        switch (category) {
            case INCOME: return R.color.incomeColor;
            case EXPENSE: return R.color.expenseColor;
            case DENA: return R.color.denaColor;
            case PABONA: return R.color.pabonaColor;
            case SAVINGS: return R.color.savingsColor;
            default: return R.color.incomeColor;
        }
    }

    private static int getLightColorRes(Category category) {
        switch (category) {
            case INCOME: return R.color.incomeLight;
            case EXPENSE: return R.color.expenseLight;
            case DENA: return R.color.denaLight;
            case PABONA: return R.color.pabonaLight;
            case SAVINGS: return R.color.savingsBg;
            default: return R.color.incomeLight;
        }
    }
}
