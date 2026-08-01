package com.jrappspot.cashlipi.utils;

import androidx.core.content.ContextCompat;

import android.app.Activity;
import androidx.fragment.app.FragmentActivity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.LedgerEntry;
import com.jrappspot.cashlipi.models.Person;
import com.jrappspot.cashlipi.models.Transaction;

import java.io.File;
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
                .setTitle(act.getString(R.string.ts_delete_confirm_title))
                .setMessage(act.getString(R.string.ts_delete_confirm_msg, item.getDisplayTitle()))
                .setPositiveButton(act.getString(R.string.ts_yes_delete), (d, w) -> {
                    List<Transaction> list = listFor(db, type);
                    int idx = findIndex(list, item);
                    if (idx >= 0) {
                        switch (type) {
                            case "expense": db.deleteExpense(idx); break;
                            case "savings": db.deleteSavings(idx); break;
                            default: db.deleteIncome(idx); break;
                        }
                    }
                    Toast.makeText(act, act.getString(R.string.ts_deleted_toast), Toast.LENGTH_SHORT).show();
                    if (onChange != null) onChange.run();
                })
                .setNegativeButton(act.getString(R.string.ts_no_keep), null)
                .show();
    }

    private static void shareTransaction(Activity act, String type, Transaction item) {
        String typeLabel = "income".equals(type) ? act.getString(R.string.type_label_income) : "expense".equals(type) ? act.getString(R.string.type_label_expense) : act.getString(R.string.type_label_savings);
        String text = act.getString(R.string.ts_share_transaction_template, typeLabel, item.getDisplayTitle(),
                DatabaseManager.formatAmount(item.getAmount()), DatabaseManager.formatDateDisplay(item.getDate()),
                DatabaseManager.formatTimeDisplay(item.getTime()))
                + (item.getNote() != null && !item.getNote().isEmpty() ? act.getString(R.string.ts_share_note_line, item.getNote()) : "");
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        act.startActivity(Intent.createChooser(intent, act.getString(R.string.ts_share_action)));
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
                .setPositiveButton(act.getString(R.string.ts_close), null)
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
            case "expense": tilTitle.setHint(act.getString(R.string.hint_expense_category)); break;
            case "savings": tilTitle.setHint(act.getString(R.string.hint_savings_bank_name)); break;
            default: tilTitle.setHint(act.getString(R.string.hint_income_source)); break;
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
                .setTitle(act.getString(R.string.ts_edit_dialog_title))
                .setView(v)
                .setPositiveButton(act.getString(R.string.ts_save_btn), null)
                .setNegativeButton(act.getString(R.string.cancel), null)
                .create();

        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
            String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
            String amtStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
            String note = etNote.getText() != null ? etNote.getText().toString().trim() : "";

            if (!"savings".equals(type) && title.isEmpty()) {
                tilTitle.setError(act.getString(R.string.ts_field_required_error));
                return;
            }
            tilTitle.setError(null);

            double amount;
            try {
                amount = Double.parseDouble(amtStr);
                if (amount <= 0) throw new NumberFormatException();
            } catch (Exception e) {
                tilAmount.setError(act.getString(R.string.ts_invalid_amount_error));
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
            Toast.makeText(act, act.getString(R.string.ts_updated_toast), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            if (onChange != null) onChange.run();
        });
    }

    // ═══════════════════════════════════════════
    //  LEDGER (debt / receivable)
    // ═══════════════════════════════════════════

    private static final int[] SHEET_AVATAR_BGS = {
            R.drawable.bg_avatar_circle_1, R.drawable.bg_avatar_circle_2,
            R.drawable.bg_avatar_circle_3, R.drawable.bg_avatar_circle_4,
            R.drawable.bg_avatar_circle_5
    };

    public static void showLedgerSheet(Activity act, DatabaseManager db,
                                         LedgerEntry item, Refresh onChange) {
        showLedgerSheet(act, db, item, null, onChange);
    }

    /**
     * @param person যদি দেনা-পাওনা ব্যক্তির নিজস্ব পেজ (PersonDetailActivity) থেকে খোলা হয়, তার Person
     *               অবজেক্ট দিলে হেডারে ব্যক্তির আসল ছবি (বা ছবি না থাকলে নামের প্রথম অক্ষরসহ রঙিন
     *               বৃত্ত) দেখানো হয়। সাধারণ দেনা-পাওনা লিস্ট (LedgerListActivity) থেকে খুললে null
     *               দিলে আগের মতোই দেনা/পাওনা আইকন দেখাবে — বাকির খাতার সাথে এর কোনো সম্পর্ক নেই।
     */
    public static void showLedgerSheet(Activity act, DatabaseManager db,
                                         LedgerEntry item, Person person, Refresh onChange) {
        BottomSheetDialog dialog = new BottomSheetDialog(act, R.style.PremiumBottomSheetDialog);
        View v = LayoutInflater.from(act).inflate(R.layout.bottom_sheet_transaction_actions, null);
        dialog.setContentView(v);

        boolean isDena = item.isDena();
        TextView sheetIcon = v.findViewById(R.id.sheetIcon);
        ImageView sheetPersonPhoto = v.findViewById(R.id.sheetPersonPhoto);
        TextView sheetTitle = v.findViewById(R.id.sheetTitle);
        TextView sheetSubtitle = v.findViewById(R.id.sheetSubtitle);
        TextView sheetAmount = v.findViewById(R.id.sheetAmount);

        if (person != null) {
            // দেনা-পাওনা ব্যক্তির পেজ থেকে খোলা হলে: ছবি থাকলে ছবি, নাহলে নামের অক্ষরসহ রঙিন বৃত্ত
            if (person.hasPhoto() && new File(person.getPhotoPath()).exists()) {
                sheetIcon.setVisibility(View.GONE);
                sheetPersonPhoto.setVisibility(View.VISIBLE);
                Glide.with(act).load(new File(person.getPhotoPath())).transform(new CircleCrop()).into(sheetPersonPhoto);
            } else {
                sheetPersonPhoto.setVisibility(View.GONE);
                sheetIcon.setVisibility(View.VISIBLE);
                String name = person.getName();
                sheetIcon.setText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.getDefault()));
                int colorIdx = Math.abs(name.hashCode()) % SHEET_AVATAR_BGS.length;
                sheetIcon.setBackgroundResource(SHEET_AVATAR_BGS[colorIdx]);
            }
        } else {
            sheetPersonPhoto.setVisibility(View.GONE);
            sheetIcon.setVisibility(View.VISIBLE);
            sheetIcon.setText(isDena ? "" : "");
            sheetIcon.setBackground(act.getResources().getDrawable(
                    isDena ? R.drawable.bg_icon_circle_ledger : R.drawable.bg_icon_circle_receivable));
        }

        sheetTitle.setText(item.getPerson());
        String typeLabel = isDena ? act.getString(R.string.ledger_type_dena) : act.getString(R.string.ledger_type_pabona);
        String statusLabel = item.isPaid() ? ("  •   " + (isDena ? act.getString(R.string.status_paid_dena) : act.getString(R.string.status_paid_pabona)))
                : item.isPartiallyPaid() ? ("  •   " + act.getString(R.string.status_partial_paid, DatabaseManager.formatAmount(item.getRemainingAmount())))
                : "  •   " + act.getString(R.string.status_due);
        String noteSuffix = item.getNote().isEmpty() ? "" : ("  •   " + item.getNote());
        sheetSubtitle.setText(typeLabel
                + "  •  " + DatabaseManager.formatDateDisplay(item.getDate())
                + "  •  " + DatabaseManager.formatTimeDisplay(item.getTime())
                + statusLabel + noteSuffix);
        sheetAmount.setText(DatabaseManager.formatAmount(item.getAmount()));
        sheetAmount.setTextColor(androidx.core.content.ContextCompat.getColor(act, isDena ? R.color.amountDebt : R.color.amountReceivable));

        // Toggle paid row + আংশিক পরিশোধ বাতিল রো
        View togglePaid = v.findViewById(R.id.actionTogglePaid);
        TextView togglePaidLabel = v.findViewById(R.id.togglePaidLabel);
        View actionResetPartial = v.findViewById(R.id.actionResetPartial);
        togglePaid.setVisibility(View.VISIBLE);
        if (item.isPaid()) {
            togglePaidLabel.setText(act.getString(R.string.ts_mark_unpaid));
            actionResetPartial.setVisibility(View.GONE);
        } else if (item.isPartiallyPaid()) {
            togglePaidLabel.setText(act.getString(R.string.ts_pay_more_prefix, DatabaseManager.formatAmount(item.getRemainingAmount())));
            actionResetPartial.setVisibility(View.VISIBLE);
        } else {
            // পাওনার পরিশোধে "পেলাম" (টাকা পেয়েছি), দেনার পরিশোধে "দিলাম" (টাকা দিয়েছি)
            togglePaidLabel.setText(isDena ? act.getString(R.string.ts_mark_paid_dena) : act.getString(R.string.ts_mark_paid_pabona));
            actionResetPartial.setVisibility(View.GONE);
        }
        togglePaid.setOnClickListener(x -> {
            dialog.dismiss();
            if (item.isPaid()) {
                // ইতিমধ্যে সম্পূর্ণ পরিশোধিত → পুরো পরিশোধ (আংশিক/সম্পূর্ণ যাই হোক) বাতিল করে
                // একদম বাকি অবস্থায় ফিরিয়ে দেয়। "আয়/ব্যয় হিসেবে" পরিশোধ করা থাকলে সাথে অটো-তৈরি
                // হওয়া আয়/ব্যয় এন্ট্রিও মুছে যায় — নাহলে পরে আবার পরিশোধ করলে ডুপ্লিকেট হয়ে যেত।
                List<LedgerEntry> list = db.getLedgerList();
                int idx = findLedgerIndex(list, item);
                if (idx >= 0) db.resetLedgerPayment(idx);
                if (onChange != null) onChange.run();
            } else {
                showSettleSourceDialog(act, db, item, onChange);
            }
        });
        actionResetPartial.setOnClickListener(x -> {
            dialog.dismiss();
            new AlertDialog.Builder(act, R.style.PremiumDialog)
                    .setTitle(act.getString(R.string.ts_cancel_payment_title))
                    .setMessage(act.getString(R.string.ts_cancel_payment_msg,
                            DatabaseManager.formatAmount(item.getPaidAmount()), DatabaseManager.formatAmount(item.getAmount())))
                    .setPositiveButton(act.getString(R.string.ts_yes_cancel), (d, w) -> {
                        List<LedgerEntry> list = db.getLedgerList();
                        int idx = findLedgerIndex(list, item);
                        if (idx >= 0) db.resetLedgerPayment(idx);
                        if (onChange != null) onChange.run();
                    })
                    .setNegativeButton(act.getString(R.string.ts_no), null)
                    .show();
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
        double remaining = item.getRemainingAmount();

        View v = LayoutInflater.from(act).inflate(R.layout.dialog_settle_source, null);
        TextView tvQuestion = v.findViewById(R.id.tvSettleQuestion);
        TextView tvRemaining = v.findViewById(R.id.tvSettleRemaining);
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

        TextView btnPayFull = v.findViewById(R.id.btnPayFull);
        TextView btnPayPartial = v.findViewById(R.id.btnPayPartial);
        TextInputLayout tilPartialAmount = v.findViewById(R.id.tilPartialAmount);
        TextInputEditText etPartialAmount = v.findViewById(R.id.etPartialAmount);

        tvQuestion.setText(isDena
                ? act.getString(R.string.ts_pay_money_question)
                : act.getString(R.string.ts_receive_money_question));
        tvRemaining.setText(act.getString(R.string.ts_total_due_prefix, DatabaseManager.formatAmount(remaining))
                + (item.isPartiallyPaid() ? act.getString(R.string.ts_already_paid_note, DatabaseManager.formatAmount(item.getPaidAmount())) : ""));

        rbBalance.setText(isDena ? act.getString(R.string.rb_balance_dena) : act.getString(R.string.rb_balance_pabona));
        rbSavings.setText(isDena ? act.getString(R.string.rb_savings_dena) : act.getString(R.string.rb_savings_pabona));
        rbIncomeExpense.setText(isDena ? act.getString(R.string.rb_incomeexpense_dena) : act.getString(R.string.rb_incomeexpense_pabona));
        rbNone.setText(isDena ? act.getString(R.string.rb_none_dena) : act.getString(R.string.rb_none_pabona));

        // ইতিমধ্যে একবার আংশিক পরিশোধ হয়ে থাকলে, ব্যালেন্স/সঞ্চয়ের হিসাব ঠিক রাখতে বাকি পরিশোধও
        // একই মাধ্যমে (settleTo) করতে হবে — তাই অন্য অপশনগুলো বন্ধ করে দেওয়া হয়
        if (item.isPartiallyPaid()) {
            String lockedTo = item.getSettleTo();
            if ("savings".equals(lockedTo)) rbSavings.setChecked(true);
            else if ("incomeExpense".equals(lockedTo)) rbIncomeExpense.setChecked(true);
            else if ("none".equals(lockedTo)) rbNone.setChecked(true);
            else rbBalance.setChecked(true);
            rbBalance.setEnabled(false);
            rbSavings.setEnabled(false);
            rbIncomeExpense.setEnabled(false);
            rbNone.setEnabled(false);
        }

        // পুরো পরিশোধ / আংশিক পরিশোধ টগল
        final boolean[] isPartialMode = {false};
        etPartialAmount.setText(String.valueOf((long) remaining));
        if (act instanceof FragmentActivity) {
            AmountInputHelper.attach((FragmentActivity) act, etPartialAmount);
        }

        Runnable[] updatePreviewHolder = new Runnable[1];
        Runnable updateToggleUI = () -> {
            if (isPartialMode[0]) {
                btnPayPartial.setBackgroundResource(R.drawable.bg_type_active_pabona);
                btnPayPartial.setTextColor(ContextCompat.getColor(act, R.color.white));
                btnPayFull.setBackgroundResource(R.drawable.bg_dialog_field);
                btnPayFull.setTextColor(ContextCompat.getColor(act, R.color.textSecondary));
                tilPartialAmount.setVisibility(View.VISIBLE);
            } else {
                btnPayFull.setBackgroundResource(R.drawable.bg_type_active_pabona);
                btnPayFull.setTextColor(ContextCompat.getColor(act, R.color.white));
                btnPayPartial.setBackgroundResource(R.drawable.bg_dialog_field);
                btnPayPartial.setTextColor(ContextCompat.getColor(act, R.color.textSecondary));
                tilPartialAmount.setVisibility(View.GONE);
            }
        };
        updateToggleUI.run();

        btnPayFull.setOnClickListener(x -> {
            isPartialMode[0] = false;
            updateToggleUI.run();
            if (updatePreviewHolder[0] != null) updatePreviewHolder[0].run();
        });
        btnPayPartial.setOnClickListener(x -> {
            isPartialMode[0] = true;
            updateToggleUI.run();
            if (updatePreviewHolder[0] != null) updatePreviewHolder[0].run();
        });

        double curBalance = db.getBalance();
        double curSavings = db.getTotalSavings();
        double curIncome = db.getTotalIncome();
        double curExpense = db.getTotalExpense();

        Runnable updatePreview = () -> {
            double payAmount = currentPayAmount(isPartialMode[0], remaining, etPartialAmount);
            int checked = rg.getCheckedRadioButtonId();
            if (checked == R.id.rbSettleBalance) {
                double after = isDena ? curBalance - payAmount : curBalance + payAmount;
                tvBefore.setText(act.getString(R.string.ts_balance_prefix, DatabaseManager.formatAmount(curBalance)));
                tvAfter.setText("৳" + DatabaseManager.formatAmount(after));
            } else if (checked == R.id.rbSettleSavings) {
                double after = isDena ? curSavings - payAmount : curSavings + payAmount;
                tvBefore.setText(act.getString(R.string.ts_savings_prefix, DatabaseManager.formatAmount(curSavings)));
                tvAfter.setText("৳" + DatabaseManager.formatAmount(after));
            } else if (checked == R.id.rbSettleIncomeExpense) {
                if (isDena) {
                    double after = curExpense + payAmount;
                    tvBefore.setText(act.getString(R.string.ts_expense_prefix, DatabaseManager.formatAmount(curExpense)));
                    tvAfter.setText("৳" + DatabaseManager.formatAmount(after));
                } else {
                    double after = curIncome + payAmount;
                    tvBefore.setText(act.getString(R.string.ts_income_prefix, DatabaseManager.formatAmount(curIncome)));
                    tvAfter.setText("৳" + DatabaseManager.formatAmount(after));
                }
            } else {
                tvBefore.setText(act.getString(R.string.ts_balance_prefix, DatabaseManager.formatAmount(curBalance)));
                tvAfter.setText("৳" + DatabaseManager.formatAmount(curBalance) + act.getString(R.string.ts_unchanged_suffix));
            }
        };
        updatePreviewHolder[0] = updatePreview;
        rg.setOnCheckedChangeListener((group, checkedId) -> updatePreview.run());
        etPartialAmount.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { updatePreview.run(); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
        updatePreview.run();

        AlertDialog settleDialog = new AlertDialog.Builder(act, R.style.PremiumDialog)
                .setView(v)
                .setPositiveButton(act.getString(R.string.ts_confirm_payment), null)
                .setNegativeButton(act.getString(R.string.cancel), null)
                .create();
        settleDialog.show();
        settleDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
            double payAmount = currentPayAmount(isPartialMode[0], remaining, etPartialAmount);
            if (payAmount <= 0) {
                Toast.makeText(act, act.getString(R.string.ts_invalid_amount_error), Toast.LENGTH_SHORT).show();
                return;
            }
            if (payAmount > remaining + 0.01) {
                Toast.makeText(act, act.getString(R.string.ts_exceeds_due, DatabaseManager.formatAmount(remaining)), Toast.LENGTH_SHORT).show();
                return;
            }

            String settleTo;
            int checked = rg.getCheckedRadioButtonId();
            if (checked == R.id.rbSettleBalance) settleTo = "balance";
            else if (checked == R.id.rbSettleSavings) settleTo = "savings";
            else if (checked == R.id.rbSettleIncomeExpense) settleTo = "incomeExpense";
            else settleTo = "none";

            List<LedgerEntry> list = db.getLedgerList();
            int idx = findLedgerIndex(list, item);
            if (idx >= 0) {
                String txnId = "";
                // "আয়/ব্যয় হিসেবে" বেছে নিলে শুধু ব্যালেন্সে যোগ-বিয়োগ না করে, আয়/ব্যয়
                // তালিকায় একটা আসল, দেখা যাওয়ার মতো লেনদেনও তৈরি করে — যতটুকু এই ধাপে
                // পরিশোধ হচ্ছে (আংশিক হলে শুধু ততটুকুই) তার সমান পরিমাণে।
                if ("incomeExpense".equals(settleTo)) {
                    LedgerEntry updated = list.get(idx);
                    String personName = updated.getPerson();
                    String entryDate = DatabaseManager.formatDateDisplay(updated.getDate());
                    Transaction txn = new Transaction();
                    txn.setDate(DatabaseManager.nowDate());
                    txn.setTime(DatabaseManager.nowTime());
                    if (updated.isPabona()) {
                        txn.setType("income");
                        txn.setSource(act.getString(R.string.ts_receivable_collection_source));
                        txn.setNote(act.getString(R.string.ts_receivable_note, personName, entryDate));
                    } else {
                        txn.setType("expense");
                        txn.setCategory(act.getString(R.string.ts_debt_payment_category));
                        txn.setNote(act.getString(R.string.ts_debt_payment_note, personName, entryDate));
                    }
                    txn.setAmount(payAmount);
                    Transaction saved = updated.isPabona() ? db.addIncome(txn) : db.addExpense(txn);
                    txnId = saved.getId();
                }
                db.addPartialPayment(idx, payAmount, settleTo, txnId);
            }
            settleDialog.dismiss();
            if (onChange != null) onChange.run();
        });
    }

    private static double currentPayAmount(boolean isPartialMode, double remaining, TextInputEditText etPartialAmount) {
        if (!isPartialMode) return remaining;
        String s = etPartialAmount.getText() != null ? etPartialAmount.getText().toString().trim() : "";
        try {
            double v = Double.parseDouble(s);
            return Math.max(0, v);
        } catch (Exception e) {
            return 0;
        }
    }

    private static void confirmDeleteLedger(Activity act, DatabaseManager db,
                                              LedgerEntry item, Refresh onChange) {
        new AlertDialog.Builder(act, R.style.PremiumDialog)
                .setTitle(act.getString(R.string.ts_delete_confirm_title))
                .setMessage(act.getString(R.string.ts_delete_confirm_msg, item.getPerson()))
                .setPositiveButton(act.getString(R.string.ts_yes_delete), (d, w) -> {
                    List<LedgerEntry> list = db.getLedgerList();
                    int idx = findLedgerIndex(list, item);
                    if (idx >= 0) db.deleteLedger(idx);
                    Toast.makeText(act, act.getString(R.string.ts_deleted_toast), Toast.LENGTH_SHORT).show();
                    if (onChange != null) onChange.run();
                })
                .setNegativeButton(act.getString(R.string.ts_no_keep), null)
                .show();
    }

    private static void shareLedger(Activity act, LedgerEntry item) {
        String typeLabel = item.isDena() ? act.getString(R.string.ts_type_dena) : act.getString(R.string.ts_type_pabona);
        String status = item.isPaid() ? (" " + (item.isDena() ? act.getString(R.string.status_paid_dena) : act.getString(R.string.status_paid_pabona))) : " " + act.getString(R.string.status_due);
        String text = act.getString(R.string.ts_share_ledger_template, typeLabel, item.getPerson(),
                DatabaseManager.formatAmount(item.getAmount()), DatabaseManager.formatDateDisplay(item.getDate()),
                DatabaseManager.formatTimeDisplay(item.getTime()), status)
                + (item.getCategory() != null && !item.getCategory().isEmpty() ? act.getString(R.string.ts_share_category_line, item.getCategory()) : "")
                + (item.getNote() != null && !item.getNote().isEmpty() ? act.getString(R.string.ts_share_note_line, item.getNote()) : "");
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        act.startActivity(Intent.createChooser(intent, act.getString(R.string.ts_share_action)));
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
                item.isPaid() ? (" " + (item.isDena() ? act.getString(R.string.status_paid_dena) : act.getString(R.string.status_paid_pabona))) : " " + act.getString(R.string.status_due));

        if (item.getNote() != null && !item.getNote().isEmpty()) {
            ((TextView) v.findViewById(R.id.tvDetailNote)).setText(item.getNote());
        } else {
            v.findViewById(R.id.rowDetailNote).setVisibility(View.GONE);
        }

        new AlertDialog.Builder(act, R.style.PremiumDialog)
                .setView(v)
                .setPositiveButton(act.getString(R.string.ts_close), null)
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
                .setTitle(act.getString(R.string.ts_edit_dialog_title))
                .setView(v)
                .setPositiveButton(act.getString(R.string.ts_save_btn), null)
                .setNegativeButton(act.getString(R.string.cancel), null)
                .create();

        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
            String person = etPerson.getText() != null ? etPerson.getText().toString().trim() : "";
            String category = etCategory.getText() != null ? etCategory.getText().toString().trim() : "";
            String amtStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
            String note = etNote.getText() != null ? etNote.getText().toString().trim() : "";

            if (person.isEmpty()) { Toast.makeText(act, act.getString(R.string.ts_enter_name_error), Toast.LENGTH_SHORT).show(); return; }

            double amount;
            try {
                amount = Double.parseDouble(amtStr);
                if (amount <= 0) throw new NumberFormatException();
            } catch (Exception e) {
                tilAmount.setError(act.getString(R.string.ts_invalid_amount_error));
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

            Toast.makeText(act, act.getString(R.string.ts_updated_toast), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            if (onChange != null) onChange.run();
        });
    }
}
