package com.jrappspot.cashlipi.utils;

import android.content.Context;
import android.view.inputmethod.InputMethodManager;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.textfield.TextInputEditText;

public class AmountInputHelper {

    private static final String TAG = "CalcKeyboard";

    /**
     * attach() — প্রতিটি amount field-এ custom keyboard লাগায়।
     * DatabaseManager-এ isCustomKeyboardEnabled() false হলে
     * system numeric keyboard ব্যবহার হবে।
     */
    public static void attach(FragmentActivity activity, TextInputEditText... fields) {
        DatabaseManager db = DatabaseManager.getInstance(activity);
        boolean useCustom = db.isCustomKeyboardEnabled();

        for (TextInputEditText field : fields) {
            if (field == null) continue;
            applyKeyboardMode(activity, field, useCustom);
        }
    }

    /**
     * re-attach() — Settings থেকে ফিরলে Activity.onResume()-এ call করতে পারো
     * যাতে preference change সাথে সাথে reflect করে।
     */
    public static void reAttach(FragmentActivity activity, TextInputEditText... fields) {
        attach(activity, fields);
    }

    // ─── Internal ────────────────────────────────────────────────────────

    private static void applyKeyboardMode(FragmentActivity activity,
                                          TextInputEditText field,
                                          boolean useCustom) {
        if (useCustom) {
            // Custom calculator keyboard
            field.setShowSoftInputOnFocus(false);
            field.setFocusable(true);
            field.setFocusableInTouchMode(true);
            field.setInputType(android.text.InputType.TYPE_NULL); // system keyboard block
            field.setOnClickListener(v -> openCustom(activity, field));
            field.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) { hideSoft(activity); openCustom(activity, field); }
            });
        } else {
            // System numeric keyboard
            field.setShowSoftInputOnFocus(true);
            field.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER |
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            );
            field.setOnClickListener(null);
            field.setOnFocusChangeListener(null);
        }
    }

    private static void openCustom(FragmentActivity activity, TextInputEditText field) {
        hideSoft(activity);
        FragmentManager fm = activity.getSupportFragmentManager();
        if (fm.findFragmentByTag(TAG) != null) return;

        String cur = field.getText() != null ? field.getText().toString().trim() : "";
        CalculatorKeyboardBottomSheet sheet = CalculatorKeyboardBottomSheet.newInstance(cur);

        sheet.setOnLiveResultListener((resultForField, expressionText) -> {
            String current = field.getText() != null ? field.getText().toString() : "";
            if (!current.equals(resultForField)) {
                field.setText(resultForField);
                if (field.getText() != null)
                    field.setSelection(field.getText().length());
            }
        });

        sheet.setOnAmountConfirmedListener(amount -> {
            field.setText(amount);
            if (field.getText() != null)
                field.setSelection(field.getText().length());
        });

        sheet.show(fm, TAG);
    }

    private static void hideSoft(FragmentActivity activity) {
        try {
            InputMethodManager imm = (InputMethodManager)
                    activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && activity.getCurrentFocus() != null)
                imm.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        } catch (Exception ignored) {}
    }
}
