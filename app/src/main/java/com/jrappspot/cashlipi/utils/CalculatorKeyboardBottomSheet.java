package com.jrappspot.cashlipi.utils;

import android.content.Context;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.jrappspot.cashlipi.R;

public class CalculatorKeyboardBottomSheet extends BottomSheetDialogFragment {

    public interface OnAmountConfirmedListener {
        void onAmountConfirmed(String amount);
    }

    /**
     * Live callback:
     *   resultForField  → শুধু number result (পরিমাণ field-এ যাবে)
     *   expressionText  → পুরো expression (calculator top bar-এ দেখাবে)
     */
    public interface OnLiveResultListener {
        void onLiveResult(String resultForField, String expressionText);
    }

    private OnAmountConfirmedListener confirmedListener;
    private OnLiveResultListener liveListener;
    private String initialValue = "";

    private TextView tvExpression;
    private StringBuilder expression = new StringBuilder();

    public static CalculatorKeyboardBottomSheet newInstance(String currentValue) {
        CalculatorKeyboardBottomSheet sheet = new CalculatorKeyboardBottomSheet();
        Bundle args = new Bundle();
        args.putString("currentValue", currentValue);
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnAmountConfirmedListener(OnAmountConfirmedListener l) { confirmedListener = l; }
    public void setOnLiveResultListener(OnLiveResultListener l) { liveListener = l; }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.CalculatorBottomSheetTheme);
        if (getArguments() != null)
            initialValue = getArguments().getString("currentValue", "");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_calculator, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if (dialog != null) {
            View bs = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bs != null) {
                BottomSheetBehavior<View> b = BottomSheetBehavior.from(bs);
                b.setState(BottomSheetBehavior.STATE_EXPANDED);
                b.setSkipCollapsed(true);
            }
        }

        tvExpression = view.findViewById(R.id.tvCalcExpression);

        if (!initialValue.isEmpty() && !initialValue.equals("0")) {
            expression.append(initialValue);
            tvExpression.setText(initialValue);
            push();
        }

        bindButtons(view);
    }

    // ── Button binding ─────────────────────────────────────────────────────

    private void bindButtons(View v) {
        int[]    ids = { R.id.btn0,R.id.btn1,R.id.btn2,R.id.btn3,
                         R.id.btn4,R.id.btn5,R.id.btn6,R.id.btn7,
                         R.id.btn8,R.id.btn9,R.id.btn00,R.id.btn000 };
        String[] vals= { "0","1","2","3","4","5","6","7","8","9","00","000" };

        for (int i = 0; i < ids.length; i++) {
            final String val = vals[i];
            MaterialButton btn = v.findViewById(ids[i]);
            if (btn != null) btn.setOnClickListener(x -> { haptic(); appendNum(val); });
        }
        v.findViewById(R.id.btnDot).setOnClickListener(x        -> { haptic(); appendDot(); });
        v.findViewById(R.id.btnPlus).setOnClickListener(x       -> { haptic(); appendOp("+"); });
        v.findViewById(R.id.btnMinus).setOnClickListener(x      -> { haptic(); appendOp("-"); });
        v.findViewById(R.id.btnMul).setOnClickListener(x        -> { haptic(); appendOp("×"); });
        v.findViewById(R.id.btnDiv).setOnClickListener(x        -> { haptic(); appendOp("÷"); });
        v.findViewById(R.id.btnPercent).setOnClickListener(x    -> { haptic(); appendOp("%"); });
        v.findViewById(R.id.btnParenOpen).setOnClickListener(x  -> { haptic(); appendRaw("("); });
        v.findViewById(R.id.btnParenClose).setOnClickListener(x -> { haptic(); appendRaw(")"); });
        v.findViewById(R.id.btnBackspace).setOnClickListener(x  -> { haptic(); backspace(); });
        v.findViewById(R.id.btnClear).setOnClickListener(x      -> { haptic(); clearAll(); });
        v.findViewById(R.id.btnEquals).setOnClickListener(x     -> { haptic(); evaluateInPlace(); });
        v.findViewById(R.id.btnDone).setOnClickListener(x       -> { haptic(); confirm(); });
    }

    // ── Input handlers ─────────────────────────────────────────────────────

    private void appendNum(String val) {
        if ((val.equals("0") || val.equals("00") || val.equals("000"))
                && lastNumber().equals("0")) return;
        expression.append(val);
        refreshExprBar();
        push();
    }

    private void appendDot() {
        if (lastNumber().contains(".")) return;
        if (lastNumber().isEmpty()) expression.append("0");
        expression.append(".");
        refreshExprBar();
        push();
    }

    private void appendOp(String op) {
        if (expression.length() == 0) {
            if (op.equals("-")) expression.append("-");
            return;
        }
        char last = expression.charAt(expression.length() - 1);
        if ("+-×÷%".indexOf(last) >= 0) expression.deleteCharAt(expression.length() - 1);
        expression.append(op);
        refreshExprBar();
        push();
    }

    private void appendRaw(String ch) {
        expression.append(ch);
        refreshExprBar();
        push();
    }

    private void backspace() {
        if (expression.length() > 0) {
            expression.deleteCharAt(expression.length() - 1);
            refreshExprBar();
            push();
        }
    }

    private void clearAll() {
        expression.setLength(0);
        tvExpression.setText("");
        if (liveListener != null) liveListener.onLiveResult("", "");
    }

    private void evaluateInPlace() {
        String res = eval(expression.toString());
        if (res != null) {
            expression.setLength(0);
            expression.append(res);
            tvExpression.setText(res);
            if (liveListener != null) liveListener.onLiveResult(cleanNum(res), res);
        }
    }

    private void confirm() {
        String expr = expression.toString().trim();
        if (expr.isEmpty()) { dismiss(); return; }
        String res = eval(expr);
        if (res == null) {
            try { Double.parseDouble(expr); res = expr; }
            catch (Exception e) { return; }
        }
        if (confirmedListener != null) confirmedListener.onAmountConfirmed(cleanNum(res));
        dismiss();
    }

    // ── Push live update ───────────────────────────────────────────────────

    /**
     * Rules:
     * • পরিমাণ field  → শুধু evaluated number result (e.g. "40")
     *                    expression incomplete হলে শেষ valid number বা empty
     * • Expression bar → পুরো typed expression (e.g. "10 + 20 + 30 - 5")
     */
    private void push() {
        if (liveListener == null) return;
        String expr = expression.toString();
        String exprDisplay = prettyExpr(expr);

        if (expr.isEmpty()) {
            liveListener.onLiveResult("", "");
            return;
        }

        String res = eval(expr);
        if (res != null) {
            // Complete expression → show result in field
            liveListener.onLiveResult(cleanNum(res), exprDisplay);
        } else {
            // Incomplete (operator at end, open paren, etc.)
            // Show last complete number in field, expression in bar
            String lastNum = lastCompletedNumber(expr);
            liveListener.onLiveResult(lastNum, exprDisplay);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void refreshExprBar() {
        tvExpression.setText(prettyExpr(expression.toString()));
    }

    private String prettyExpr(String e) {
        return e.replace("+", " + ").replace("-", " - ")
                .replace("×", " × ").replace("÷", " ÷ ")
                .replaceAll(" {2,}", " ").trim();
    }

    private String cleanNum(String s) {
        if (s == null) return "";
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }

    /** Get the last fully typed number before a trailing operator */
    private String lastCompletedNumber(String expr) {
        // Remove trailing operators to get last clean expression
        String trimmed = expr.replaceAll("[+\\-×÷%(]+$", "");
        if (trimmed.isEmpty()) return "";
        String res = eval(trimmed);
        return res != null ? cleanNum(res) : "";
    }

    private String lastNumber() {
        String e = expression.toString();
        int i = e.length() - 1;
        while (i >= 0 && (Character.isDigit(e.charAt(i)) || e.charAt(i) == '.')) i--;
        return e.substring(i + 1);
    }

    // ── Evaluator ──────────────────────────────────────────────────────────

    private int pos;
    private String cs;

    @Nullable
    private String eval(String expr) {
        try {
            String e = expr.replace("×", "*").replace("÷", "/");
            pos = 0; cs = e.trim();
            double r = pExpr();
            if (pos != cs.length()) return null;
            if (Double.isInfinite(r) || Double.isNaN(r)) return null;
            if (r == Math.floor(r)) return String.valueOf((long) r);
            return String.valueOf(Math.round(r * 1_000_000.0) / 1_000_000.0);
        } catch (Exception e) { return null; }
    }

    private double pExpr() {
        double r = pTerm();
        while (pos < cs.length()) {
            char op = cs.charAt(pos);
            if      (op=='+') { pos++; r += pTerm(); }
            else if (op=='-') { pos++; r -= pTerm(); }
            else break;
        }
        return r;
    }

    private double pTerm() {
        double r = pFactor();
        while (pos < cs.length()) {
            char op = cs.charAt(pos);
            if (op=='*') { pos++; r *= pFactor(); }
            else if (op=='/') { pos++; double d=pFactor(); if(d==0) throw new ArithmeticException(); r/=d; }
            else if (op=='%') { pos++; r = r * pFactor() / 100.0; }
            else break;
        }
        return r;
    }

    private double pFactor() {
        while (pos<cs.length() && cs.charAt(pos)==' ') pos++;
        if (pos>=cs.length()) throw new RuntimeException();
        char ch = cs.charAt(pos);
        if (ch=='(') { pos++; double r=pExpr(); while(pos<cs.length()&&cs.charAt(pos)==' ')pos++; if(pos<cs.length()&&cs.charAt(pos)==')')pos++; return r; }
        if (ch=='-') { pos++; return -pFactor(); }
        if (ch=='+') { pos++; return pFactor(); }
        int s=pos;
        while (pos<cs.length()&&(Character.isDigit(cs.charAt(pos))||cs.charAt(pos)=='.')) pos++;
        if (s==pos) throw new RuntimeException();
        return Double.parseDouble(cs.substring(s,pos));
    }

    // ── Haptic ─────────────────────────────────────────────────────────────

    private void haptic() {
        try {
            Vibrator v = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                    v.vibrate(VibrationEffect.createOneShot(22, VibrationEffect.DEFAULT_AMPLITUDE));
                else v.vibrate(22);
            }
        } catch (Exception ignored) {}
    }
}
