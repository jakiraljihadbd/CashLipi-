package com.jrappspot.cashlipi.utils;

import androidx.core.content.ContextCompat;

import android.app.Activity;
import androidx.fragment.app.FragmentActivity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.LedgerEntry;
import com.jrappspot.cashlipi.models.Transaction;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Shared helper that builds the premium action bottom sheet
 * (Edit / Details / Share / Delete / Toggle-paid) and the
 * premium edit dialogs for Transactions and LedgerEntries.
 */
public class TransactionSheetHelper {

    public interface Refresh { void run(); }

    // ═══════════════════════════════════════════
    //  TRANSACTION (income / expense / savings)
    // ═══════════════════════════════════════════

    public static void showTransactionSheet(Activity act, DatabaseManager db, String type,
                                              Transaction item, Refresh onChange) {
        BottomSheetDialog dialog = new BottomSheetDialog(act, R.style.PremiumBottomSheetDialog);
        View v = LayoutInflater.from(act).inflate(R.layout.bottom_sheet_transaction_actions, null);
        dialog.setContentView(v);

        TextView sheetIcon = v.findViewById(R.id.sheetIcon);
        TextView sheetTitle = v.findViewById(R.id.sheetTitle);
        TextView sheetSubtitle = v.findViewById(R.id.sheetSubtitle);
        TextView sheetAmount = v.findViewById(R.id.sheetAmount);

        int iconBg, amountColor;
        String icon;
        switch (type) {
            case "expense": icon = ""; iconBg = R.drawable.bg_icon_circle_expense; amountColor = R.color.amountExpense; break;
            case "savings": icon = ""; iconBg = R.drawable.bg_icon_circle_savings; amountColor = R.color.savingsColor; break;
            default: icon = ""; iconBg = R.drawable.bg_icon_circle_income; amountColor = R.color.amountIncome; break;
        }
        sheetIcon.setText(icon);
        sheetIcon.setBackground(act.getResources().getDrawable(iconBg));
        sheetTitle.setText(item.getDisplayTitle());
        sheetSubtitle.setText(DatabaseManager.formatDateDisplay(item.getDate())
                + "  •  " + DatabaseManager.formatTimeDisplay(item.getTime()));
        sheetAmount.setText(DatabaseManager.formatAmount(item.getAmount()));
        sheetAmount.setTextColor(androidx.core.content.ContextCompat.getColor(act, amountColor));

        // hide paid-toggle row (not applicable for transactions)
        View togglePaid = v.findViewById(R.id.actionTogglePaid);
        togglePaid.setVisibility(View.GONE);

        v.findViewById(R.id.actionEdit).setOnClickListener(x -> {
            dialog.dismiss();
            showEditTransactionDialog(act, db, type, item, onChange);
        });

        v.findViewById(R.id.actionDetails).setOnClickListener(x -> {
            dialog.dismiss();
            showTransactionDetails(act, type, item);
        });

        v.findViewById(R.id.actionShare).setOnClickListener(x -> {
            dialog.dismiss();
            shareTransaction(act, type, item);
        });

        v.findViewById(R.id.actionDelete).setOnClickListener(x -> {
            dialog.dismiss();
            confirmDeleteTransaction(act, db, type, item, onChange);
        });

        dialog.show();
    }

    private static int findIndex(List<Transaction> list, Transaction item) {
        String targetId = item.getId();
        for (int i = 0; i < list.size(); i++) {
            Transaction t = list.get(i);
            if (t == item) return i;
            if (targetId != null && !targetId.isEmpty() && targetId.equals(t.getId())) return i;
        }
        return -1;
    }

    private static List<Transaction> listFor(DatabaseManager db, String type) {
        switch (type) {
            case "expense": return db.getExpenseList();
            case "savings": return db.getSavingsList();
            default: return db.getIncomeList();
        }
    }

    public static void confirmDeleteTransaction(Activity act, DatabaseManager db, String type,
                                                   Transaction item, Refresh onChange) {
        new AlertDialog.Builder(act, R.style.PremiumDialog)
                .setTitle("মুছে ফেলবেন?")
                .setMessage("\"" + item.getDisplayTitle() + "\" এন্ট্রিটি ট্র্যাশে যাবে।")
                .setPositiveButton("হ্যাঁ, মুছুন", (d, w) -> {
                    List<Transaction> list = listFor(db, type);
                    int idx = findIndex(list, item);
                    if (idx >= 0) {
                        switch (type) {
                            case "expense": db.deleteExpense(idx); break;
                            case "savings": db.deleteSavings(idx); break;
                            default: db.deleteIncome(idx); break;
                        }
                    }
                    Toast.makeText(act, " মুছে গেছে", Toast.LENGTH_SHORT).show();
                    if (onChange != null) onChange.run();
                })
                .setNegativeButton("না, রাখুন", null)
                .show();
    }

    private static void shareTransaction(Activity act, String type, Transaction item) {
        String typeLabel = "income".equals(type) ? "আয়" : "expense".equals(type) ? "ব্যয়" : "সঞ্চয়";
        String text = " " + typeLabel + " বিবরণ\n"
                + "─────────────\n"
                + "শিরোনাম: " + item.getDisplayTitle() + "\n"
                + "পরিমাণ: " + DatabaseManager.formatAmount(item.getAmount()) + "\n"
                + "তারিখ: " + DatabaseManager.formatDateDisplay(item.getDate()) + "\n"
                + "সময়: " + DatabaseManager.formatTimeDisplay(item.getTime())
                + (item.getNote() != null && !item.getNote().isEmpty() ? "\nনোট: " + item.getNote() : "");
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        act.startActivity(Intent.createChooser(intent, "শেয়ার করুন"));
    }

    private static void showTransactionDetails(Activity act, String type, Transaction item) {
        View v = LayoutInflater.from(act).inflate(R.layout.dialog_transaction_details, null);
        ((TextView) v.findViewById(R.id.tvDetailTitle)).setText(item.getDisplayTitle());
        ((TextView) v.findViewById(R.id.tvDetailAmount)).setText(DatabaseManager.formatAmount(item.getAmount()));
        v.findViewById(R.id.rowDetailCategory).setVisibility(View.GONE);
        v.findViewById(R.id.rowDetailPerson).setVisibility(View.GONE);
        v.findViewById(R.id.rowDetailStatus).setVisibility(View.GONE);
        ((TextView) v.findViewById(R.id.tvDetailDate)).setText(DatabaseManager.formatDateDisplay(item.getDate()));
        ((TextView) v.findViewById(R.id.tvDetailTime)).setText(DatabaseManager.formatTimeDisplay(item.getTime()));
        if (item.getNote() != null && !item.getNote().isEmpty()) {
            ((TextView) v.findViewById(R.id.tvDetailNote)).setText(item.getNote());
        } else {
            v.findViewById(R.id.rowDetailNote).setVisibility(View.GONE);
        }
        new AlertDialog.Builder(act, R.style.PremiumDialog)
                .setView(v)
                .setPositiveButton("বন্ধ করুন", null)
                .show();
    }

    public static void showEditTransactionDialog(Activity act, DatabaseManager db, String type,
                                                   Transaction item, Refresh onChange) {
        View v = LayoutInflater.from(act).inflate(R.layout.dialog_edit_transaction, null);
        TextInputLayout tilTitle = v.findViewById(R.id.tilEditTitle);
        TextInputEditText etTitle = v.findViewById(R.id.etEditTitle);
        TextInputLayout tilAmount = v.findViewById(R.id.tilEditAmount);
        TextInputEditText etAmount = v.findViewById(R.id.etEditAmount);
        TextInputEditText etNote = v.findViewById(R.id.etEditNote);
        TextView tvDate = v.findViewById(R.id.tvEditDate);
        TextView tvTime = v.findViewById(R.id.tvEditTime);
        View btnDate = v.findViewById(R.id.btnEditDate);
        View btnTime = v.findViewById(R.id.btnEditTime);

        switch (type) {
            case "expense": tilTitle.setHint("ব্যয়ের ক্যাটাগরি"); break;
            case "savings": tilTitle.setHint("ব্যাংক/মাধ্যমের নাম (ঐচ্ছিক)"); break;
            default: tilTitle.setHint("আয়ের উৎস"); break;
        }

        if ("savings".equals(type)) {
            etTitle.setText(item.getBankName());
        } else {
            etTitle.setText(item.getDisplayTitle());
        }
        etAmount.setText(String.valueOf((long) item.getAmount()));
        // Attach custom calculator keyboard to edit amount field
        if (act instanceof FragmentActivity) {
            AmountInputHelper.attach((FragmentActivity) act, etAmount);
        }
        etNote.setText(item.getNote());

        final String[] selDate = {item.getDate().isEmpty() ? DatabaseManager.nowDate() : item.getDate()};
        final String[] selTime = {item.getTime().isEmpty() ? DatabaseManager.nowTime() : item.getTime()};
        tvDate.setText(DatabaseManager.formatDateDisplay(selDate[0]));
        tvTime.setText(DatabaseManager.formatTimeDisplay(selTime[0]));

        btnDate.setOnClickListener(x -> {
            Calendar c = Calendar.getInstance();
            try {
                String[] p = selDate[0].split("-");
                c.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]));
            } catch (Exception ignored) {}
            new DatePickerDialog(act, (view, y, m, d) -> {
                selDate[0] = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d);
                tvDate.setText(DatabaseManager.formatDateDisplay(selDate[0]));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnTime.setOnClickListener(x -> {
            Calendar c = Calendar.getInstance();
            try {
                String[] p = selTime[0].split(":");
                c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(p[0]));
                c.set(Calendar.MINUTE, Integer.parseInt(p[1]));
            } catch (Exception ignored) {}
            new TimePickerDialog(act, (view, h, min) -> {
                selTime[0] = String.format(Locale.US, "%02d:%02d", h, min);
                tvTime.setText(DatabaseManager.formatTimeDisplay(selTime[0]));
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
        });

        AlertDialog dialog = new AlertDialog.Builder(act, R.style.PremiumDialog)
                .setTitle(" সম্পাদনা করুন")
                .setView(v)
                .setPositiveButton("সংরক্ষণ করুন", null)
                .setNegativeButton("বাতিল", null)
                .create();

        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
            String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
            String amtStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
            String note = etNote.getText() != null ? etNote.getText().toString().trim() : "";

            if (!"savings".equals(type) && title.isEmpty()) {
                tilTitle.setError("এই ঘরটি খালি রাখা যাবে না");
                return;
            }
            tilTitle.setError(null);

            double amount;
            try {
                amount = Double.parseDouble(amtStr);
                if (amount <= 0) throw new NumberFormatException();
            } catch (Exception e) {
                tilAmount.setError("সঠিক পরিমাণ লিখুন");
                return;
            }
            tilAmount.setError(null);

            switch (type) {
                case "expense":
                    item.setCategory(title);
                    break;
                case "savings":
                    item.setBankName(title);
                    break;
                default:
                    item.setSource(title);
                    break;
            }
            item.setAmount(amount);
            item.setDate(selDate[0]);
            item.setTime(selTime[0]);
            item.setNote(note);

            List<Transaction> list = listFor(db, type);
            int idx = findIndex(list, item);
            if (idx >= 0) {
                switch (type) {
                    case "expense": db.updateExpense(idx, item); break;
                    case "savings": db.updateSavings(idx, item); break;
                    default: db.updateIncome(idx, item); break;
                }
            }
            Toast.makeText(act, " আপডেট হয়েছে", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            if (onChange != null) onChange.run();
        });
    }

    // ═══════════════════════════════════════════
    //  LEDGER (debt / receivable)
    // ═══════════════════════════════════════════

    public static void showLedgerSheet(Activity act, DatabaseManager db,
                                         LedgerEntry item, Refresh onChange) {
        BottomSheetDialog dialog = new BottomSheetDialog(act, R.style.PremiumBottomSheetDialog);
        View v = LayoutInflater.from(act).inflate(R.layout.bottom_sheet_transaction_actions, null);
        dialog.setContentView(v);

        boolean isDena = item.isDena();
        TextView sheetIcon = v.findViewById(R.id.sheetIcon);
        TextView sheetTitle = v.findViewById(R.id.sheetTitle);
        TextView sheetSubtitle = v.findViewById(R.id.sheetSubtitle);
        TextView sheetAmount = v.findViewById(R.id.sheetAmount);

        sheetIcon.setText(isDena ? "" : "");
        sheetIcon.setBackground(act.getResources().getDrawable(
                isDena ? R.drawable.bg_icon_circle_ledger : R.drawable.bg_icon_circle_receivable));
        sheetTitle.setText(item.getPerson());
        sheetSubtitle.setText(DatabaseManager.formatDateDisplay(item.getDate())
                + "  •  " + DatabaseManager.formatTimeDisplay(item.getTime())
                + (item.isPaid() ? ("  •   " + (isDena ? "দিলাম" : "পেলাম")) : "  •   বাকি"));
        sheetAmount.setText(DatabaseManager.formatAmount(item.getAmount()));
        sheetAmount.setTextColor(androidx.core.content.ContextCompat.getColor(act, isDena ? R.color.amountDebt : R.color.amountReceivable));

        // Toggle paid row
        View togglePaid = v.findViewById(R.id.actionTogglePaid);
        TextView togglePaidLabel = v.findViewById(R.id.togglePaidLabel);
        togglePaid.setVisibility(View.VISIBLE);
        if (item.isPaid()) {
            togglePaidLabel.setText("↩️ অপরিশোধিত করুন");
        } else {
            // পাওনার পরিশোধে "পেলাম" (টাকা পেয়েছি), দেনার পরিশোধে "দিলাম" (টাকা দিয়েছি)
            togglePaidLabel.setText(isDena ? "✅ দিলাম" : "✅ পেলাম");
        }
        togglePaid.setOnClickListener(x -> {
            dialog.dismiss();
            if (item.isPaid()) {
                // ইতিমধ্যে পরিশোধিত → অপরিশোধিত করতে সরাসরি টগল, কোনো প্রশ্ন করার দরকার নেই।
                // তবে "আয়/ব্যয় হিসেবে" পরিশোধ করা থাকলে, সাথে অটো-তৈরি হওয়া আয়/ব্যয় এন্ট্রিটাও
                // মুছে দেয় — নাহলে পরে আবার পরিশোধ করলে ডুপ্লিকেট লেনদেন তৈরি হয়ে যাবে।
                List<LedgerEntry> list = db.getLedgerList();
                int idx = findLedgerIndex(list, item);
                if (idx >= 0) {
                    LedgerEntry entry = list.get(idx);
                    if ("incomeExpense".equals(entry.getSettleTo()) && !entry.getSettleTxnId().isEmpty()) {
                        if (entry.isPabona()) db.deleteIncomeById(entry.getSettleTxnId());
                        else db.deleteExpenseById(entry.getSettleTxnId());
                        entry.setSettleTxnId("");
                        db.updateLedger(idx, entry);
                    }
                    db.toggleLedgerPaid(idx);
                }
                if (onChange != null) onChange.run();
            } else {
                showSettleSourceDialog(act, db, item, onChange);
            }
        });

        v.findViewById(R.id.actionEdit).setOnClickListener(x -> {
            dialog.dismiss();
            showEditLedgerDialog(act, db, item, onChange);
        });

        v.findViewById(R.id.actionDetails).setOnClickListener(x -> {
            dialog.dismiss();
            showLedgerDetails(act, item);
        });

        v.findViewById(R.id.actionShare).setOnClickListener(x -> {
            dialog.dismiss();
            shareLedger(act, item);
        });

        v.findViewById(R.id.actionDelete).setOnClickListener(x -> {
            dialog.dismiss();
            confirmDeleteLedger(act, db, item, onChange);
        });

        dialog.show();
    }

    private static int findLedgerIndex(List<LedgerEntry> list, LedgerEntry item) {
        String targetId = item.getId();
        for (int i = 0; i < list.size(); i++) {
            LedgerEntry e = list.get(i);
            if (e == item) return i;
            if (targetId != null && !targetId.isEmpty() && targetId.equals(e.getId())) return i;
        }
        return -1;
    }

    /**
     * কোনো দেনা-পাওনা এন্ট্রি "পরিশোধিত" করার আগে জিজ্ঞাসা করে টাকাটা কোথা থেকে এলো/গেল —
     * ব্যালেন্স, সঞ্চয়, আয়/ব্যয় হিসেবে, নাকি কোথাও প্রভাব ছাড়াই শুধু বুককিপিং। প্রতিটা অপশনে
     * "বর্তমান → এই লেনদেনের পর" লাইভ প্রিভিউ দেখায়, যাতে নিশ্চিত হয়ে বেছে নেওয়া যায়। এই
     * পছন্দ অনুযায়ী DatabaseManager.getBalance()/getTotalSavings()/getTotalIncome()/
     * getTotalExpense() সঠিকভাবে হিসাব করে।
     */
    private static void showSettleSourceDialog(Activity act, DatabaseManager db,
                                                 LedgerEntry item, Refresh onChange) {
        boolean isDena = item.isDena();
        double amount = item.getAmount();

        View v = LayoutInflater.from(act).inflate(R.layout.dialog_settle_source, null);
        TextView tvQuestion = v.findViewById(R.id.tvSettleQuestion);
        android.widget.ImageView ivIcon = v.findViewById(R.id.ivSettleIcon);
        ivIcon.setBackground(act.getResources().getDrawable(
                isDena ? R.drawable.bg_icon_circle_ledger : R.drawable.bg_icon_circle_receivable));
        RadioGroup rg = v.findViewById(R.id.rgSettleOptions);
        RadioButton rbBalance = v.findViewById(R.id.rbSettleBalance);
        RadioButton rbSavings = v.findViewById(R.id.rbSettleSavings);
        RadioButton rbIncomeExpense = v.findViewById(R.id.rbSettleIncomeExpense);
        RadioButton rbNone = v.findViewById(R.id.rbSettleNone);
        TextView tvBefore = v.findViewById(R.id.tvSettleBefore);
        TextView tvAfter = v.findViewById(R.id.tvSettleAfter);

        tvQuestion.setText(isDena
                ? "এই " + DatabaseManager.formatAmount(amount) + " টাকা কোথা থেকে দিলেন?"
                : "এই " + DatabaseManager.formatAmount(amount) + " টাকা কোথায় রাখলেন?");

        rbBalance.setText(isDena ? "ব্যালেন্স থেকে (মূল হিসাব থেকে কমবে)" : "ব্যালেন্সে (মূল হিসাবে যোগ হবে)");
        rbSavings.setText(isDena ? "সঞ্চয় থেকে (সঞ্চয়ের হিসাব থেকে কমবে)" : "সঞ্চয়ে (সঞ্চয়ের হিসাবে যোগ হবে)");
        rbIncomeExpense.setText(isDena ? "ব্যয় হিসেবে যোগ হবে" : "আয় হিসেবে যোগ হবে");
        rbNone.setText(isDena ? "কোথাও থেকে না (শুধু হিসাব থেকে বাদ)" : "কোথাও না (শুধু হিসাব থেকে বাদ)");

        double curBalance = db.getBalance();
        double curSavings = db.getTotalSavings();
        double curIncome = db.getTotalIncome();
        double curExpense = db.getTotalExpense();

        Runnable updatePreview = () -> {
            int checked = rg.getCheckedRadioButtonId();
            if (checked == R.id.rbSettleBalance) {
                double after = isDena ? curBalance - amount : curBalance + amount;
                tvBefore.setText("ব্যালেন্স ৳" + DatabaseManager.formatAmount(curBalance));
                tvAfter.setText("৳" + DatabaseManager.formatAmount(after));
            } else if (checked == R.id.rbSettleSavings) {
                double after = isDena ? curSavings - amount : curSavings + amount;
                tvBefore.setText("সঞ্চয় ৳" + DatabaseManager.formatAmount(curSavings));
                tvAfter.setText("৳" + DatabaseManager.formatAmount(after));
            } else if (checked == R.id.rbSettleIncomeExpense) {
                if (isDena) {
                    double after = curExpense + amount;
                    tvBefore.setText("ব্যয় ৳" + DatabaseManager.formatAmount(curExpense));
                    tvAfter.setText("৳" + DatabaseManager.formatAmount(after));
                } else {
                    double after = curIncome + amount;
                    tvBefore.setText("আয় ৳" + DatabaseManager.formatAmount(curIncome));
                    tvAfter.setText("৳" + DatabaseManager.formatAmount(after));
                }
            } else {
                tvBefore.setText("ব্যালেন্স ৳" + DatabaseManager.formatAmount(curBalance));
                tvAfter.setText("৳" + DatabaseManager.formatAmount(curBalance) + " (অপরিবর্তিত)");
            }
        };
        rg.setOnCheckedChangeListener((group, checkedId) -> updatePreview.run());
        updatePreview.run();

        new AlertDialog.Builder(act, R.style.PremiumDialog)
                .setView(v)
                .setPositiveButton("পরিশোধ নিশ্চিত করুন", (d, w) -> {
                    String settleTo;
                    int checked = rg.getCheckedRadioButtonId();
                    if (checked == R.id.rbSettleBalance) settleTo = "balance";
                    else if (checked == R.id.rbSettleSavings) settleTo = "savings";
                    else if (checked == R.id.rbSettleIncomeExpense) settleTo = "incomeExpense";
                    else settleTo = "none";

                    List<LedgerEntry> list = db.getLedgerList();
                    int idx = findLedgerIndex(list, item);
                    if (idx >= 0) {
                        LedgerEntry updated = list.get(idx);
                        updated.setSettleTo(settleTo);

                        // "আয়/ব্যয় হিসেবে" বেছে নিলে শুধু ব্যালেন্সে যোগ-বিয়োগ না করে, আয়/ব্যয়
                        // তালিকায় একটা আসল, দেখা যাওয়ার মতো লেনদেনও তৈরি করে — নাম, তারিখ ইত্যাদি
                        // বিবরণ নোটে অটো বসিয়ে দেয়, যাতে পরে কোন পাওনা/দেনার বিপরীতে এই আয়/ব্যয়
                        // যোগ হয়েছে তা স্পষ্ট বোঝা যায়।
                        if ("incomeExpense".equals(settleTo)) {
                            String personName = updated.getPerson();
                            String entryDate = DatabaseManager.formatDateDisplay(updated.getDate());
                            Transaction txn = new Transaction();
                            txn.setDate(DatabaseManager.nowDate());
                            txn.setTime(DatabaseManager.nowTime());
                            if (updated.isPabona()) {
                                txn.setType("income");
                                txn.setSource("পাওনা প্রাপ্তি");
                                txn.setNote(personName + " এর কাছ থেকে " + entryDate + " তারিখের পাওনা আদায় করা হয়েছে");
                            } else {
                                txn.setType("expense");
                                txn.setCategory("দেনা পরিশোধ");
                                txn.setNote(personName + " কে " + entryDate + " তারিখের দেনা পরিশোধ করা হয়েছে");
                            }
                            txn.setAmount(updated.getAmount());
                            Transaction saved = updated.isPabona() ? db.addIncome(txn) : db.addExpense(txn);
                            updated.setSettleTxnId(saved.getId());
                        }

                        db.updateLedger(idx, updated);
                        db.toggleLedgerPaid(idx);
                    }
                    if (onChange != null) onChange.run();
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private static void confirmDeleteLedger(Activity act, DatabaseManager db,
                                              LedgerEntry item, Refresh onChange) {
        new AlertDialog.Builder(act, R.style.PremiumDialog)
                .setTitle("মুছে ফেলবেন?")
                .setMessage("\"" + item.getPerson() + "\" এন্ট্রিটি ট্র্যাশে যাবে।")
                .setPositiveButton("হ্যাঁ, মুছুন", (d, w) -> {
                    List<LedgerEntry> list = db.getLedgerList();
                    int idx = findLedgerIndex(list, item);
                    if (idx >= 0) db.deleteLedger(idx);
                    Toast.makeText(act, " মুছে গেছে", Toast.LENGTH_SHORT).show();
                    if (onChange != null) onChange.run();
                })
                .setNegativeButton("না, রাখুন", null)
                .show();
    }

    private static void shareLedger(Activity act, LedgerEntry item) {
        String typeLabel = item.isDena() ? " দেনা" : " পাওনা";
        String text = " " + typeLabel + " বিবরণ\n"
                + "─────────────\n"
                + "ব্যক্তি: " + item.getPerson() + "\n"
                + "পরিমাণ: " + DatabaseManager.formatAmount(item.getAmount()) + "\n"
                + "তারিখ: " + DatabaseManager.formatDateDisplay(item.getDate()) + "\n"
                + "সময়: " + DatabaseManager.formatTimeDisplay(item.getTime()) + "\n"
                + "স্ট্যাটাস: " + (item.isPaid() ? (" " + (item.isDena() ? "দিলাম" : "পেলাম")) : " বাকি")
                + (item.getCategory() != null && !item.getCategory().isEmpty() ? "\nক্যাটাগরি: " + item.getCategory() : "")
                + (item.getNote() != null && !item.getNote().isEmpty() ? "\nনোট: " + item.getNote() : "");
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        act.startActivity(Intent.createChooser(intent, "শেয়ার করুন"));
    }

    private static void showLedgerDetails(Activity act, LedgerEntry item) {
        View v = LayoutInflater.from(act).inflate(R.layout.dialog_transaction_details, null);
        ((TextView) v.findViewById(R.id.tvDetailTitle)).setText(item.getPerson());
        ((TextView) v.findViewById(R.id.tvDetailAmount)).setText(DatabaseManager.formatAmount(item.getAmount()));

        if (item.getCategory() != null && !item.getCategory().isEmpty()) {
            ((TextView) v.findViewById(R.id.tvDetailCategory)).setText(item.getCategory());
        } else {
            v.findViewById(R.id.rowDetailCategory).setVisibility(View.GONE);
        }

        ((TextView) v.findViewById(R.id.tvDetailPerson)).setText(item.getTypeDisplay());
        ((TextView) v.findViewById(R.id.tvDetailDate)).setText(DatabaseManager.formatDateDisplay(item.getDate()));
        ((TextView) v.findViewById(R.id.tvDetailTime)).setText(DatabaseManager.formatTimeDisplay(item.getTime()));
        ((TextView) v.findViewById(R.id.tvDetailStatus)).setText(
                item.isPaid() ? (" " + (item.isDena() ? "দিলাম" : "পেলাম")) : " বাকি");

        if (item.getNote() != null && !item.getNote().isEmpty()) {
            ((TextView) v.findViewById(R.id.tvDetailNote)).setText(item.getNote());
        } else {
            v.findViewById(R.id.rowDetailNote).setVisibility(View.GONE);
        }

        new AlertDialog.Builder(act, R.style.PremiumDialog)
                .setView(v)
                .setPositiveButton("বন্ধ করুন", null)
                .show();
    }

    public static void showEditLedgerDialog(Activity act, DatabaseManager db,
                                             LedgerEntry item, Refresh onChange) {
        View v = LayoutInflater.from(act).inflate(R.layout.dialog_edit_ledger, null);
        TextInputEditText etPerson = v.findViewById(R.id.etEditPerson);
        TextInputEditText etCategory = v.findViewById(R.id.etEditCategory);
        TextInputLayout tilAmount = v.findViewById(R.id.tilEditAmount);
        TextInputEditText etAmount = v.findViewById(R.id.etEditAmount);
        TextInputEditText etNote = v.findViewById(R.id.etEditNote);
        TextView tvDate = v.findViewById(R.id.tvEditDate);
        TextView tvTime = v.findViewById(R.id.tvEditTime);
        View btnDate = v.findViewById(R.id.btnEditDate);
        View btnTime = v.findViewById(R.id.btnEditTime);
        TextView btnTypeDena = v.findViewById(R.id.btnEditTypeDena);
        TextView btnTypePabona = v.findViewById(R.id.btnEditTypePabona);

        etPerson.setText(item.getPerson());
        etCategory.setText(item.getCategory());
        etAmount.setText(String.valueOf((long) item.getAmount()));
        // Attach custom calculator keyboard to ledger edit amount field
        if (act instanceof FragmentActivity) {
            AmountInputHelper.attach((FragmentActivity) act, etAmount);
        }
        etNote.setText(item.getNote());

        final String[] selDate = {item.getDate().isEmpty() ? DatabaseManager.nowDate() : item.getDate()};
        final String[] selTime = {item.getTime().isEmpty() ? DatabaseManager.nowTime() : item.getTime()};
        final String[] selType = {item.getType()};
        tvDate.setText(DatabaseManager.formatDateDisplay(selDate[0]));
        tvTime.setText(DatabaseManager.formatTimeDisplay(selTime[0]));

        Runnable updateTypeUI = () -> {
            if ("dena".equals(selType[0])) {
                btnTypeDena.setBackground(act.getResources().getDrawable(R.drawable.bg_type_active_dena));
                btnTypeDena.setTextColor(ContextCompat.getColor(act, R.color.white));
                btnTypePabona.setBackground(act.getResources().getDrawable(R.drawable.bg_dialog_field));
                btnTypePabona.setTextColor(ContextCompat.getColor(act, R.color.secondaryTextDark));
            } else {
                btnTypePabona.setBackground(act.getResources().getDrawable(R.drawable.bg_type_active_pabona));
                btnTypePabona.setTextColor(ContextCompat.getColor(act, R.color.white));
                btnTypeDena.setBackground(act.getResources().getDrawable(R.drawable.bg_dialog_field));
                btnTypeDena.setTextColor(ContextCompat.getColor(act, R.color.secondaryTextDark));
            }
        };
        updateTypeUI.run();
        btnTypeDena.setOnClickListener(x -> { selType[0] = "dena"; updateTypeUI.run(); });
        btnTypePabona.setOnClickListener(x -> { selType[0] = "pabona"; updateTypeUI.run(); });

        btnDate.setOnClickListener(x -> {
            Calendar c = Calendar.getInstance();
            try {
                String[] p = selDate[0].split("-");
                c.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]));
            } catch (Exception ignored) {}
            new DatePickerDialog(act, (view, y, m, d) -> {
                selDate[0] = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d);
                tvDate.setText(DatabaseManager.formatDateDisplay(selDate[0]));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnTime.setOnClickListener(x -> {
            Calendar c = Calendar.getInstance();
            try {
                String[] p = selTime[0].split(":");
                c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(p[0]));
                c.set(Calendar.MINUTE, Integer.parseInt(p[1]));
            } catch (Exception ignored) {}
            new TimePickerDialog(act, (view, h, min) -> {
                selTime[0] = String.format(Locale.US, "%02d:%02d", h, min);
                tvTime.setText(DatabaseManager.formatTimeDisplay(selTime[0]));
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
        });

        AlertDialog dialog = new AlertDialog.Builder(act, R.style.PremiumDialog)
                .setTitle(" সম্পাদনা করুন")
                .setView(v)
                .setPositiveButton("সংরক্ষণ করুন", null)
                .setNegativeButton("বাতিল", null)
                .create();

        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
            String person = etPerson.getText() != null ? etPerson.getText().toString().trim() : "";
            String category = etCategory.getText() != null ? etCategory.getText().toString().trim() : "";
            String amtStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
            String note = etNote.getText() != null ? etNote.getText().toString().trim() : "";

            if (person.isEmpty()) { Toast.makeText(act, "নাম লিখুন", Toast.LENGTH_SHORT).show(); return; }

            double amount;
            try {
                amount = Double.parseDouble(amtStr);
                if (amount <= 0) throw new NumberFormatException();
            } catch (Exception e) {
                tilAmount.setError("সঠিক পরিমাণ লিখুন");
                return;
            }
            tilAmount.setError(null);

            item.setPerson(person);
            item.setCategory(category);
            item.setAmount(amount);
            item.setType(selType[0]);
            item.setDate(selDate[0]);
            item.setTime(selTime[0]);
            item.setNote(note);

            List<LedgerEntry> list = db.getLedgerList();
            int idx = findLedgerIndex(list, item);
            if (idx >= 0) db.updateLedger(idx, item);

            Toast.makeText(act, " আপডেট হয়েছে", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            if (onChange != null) onChange.run();
        });
    }
}
