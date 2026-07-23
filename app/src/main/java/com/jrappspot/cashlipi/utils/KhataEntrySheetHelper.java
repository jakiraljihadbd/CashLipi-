package com.jrappspot.cashlipi.utils;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.KhataEntry;

import java.util.Calendar;
import java.util.List;

/**
 * বাকির খাতার একটা এন্ট্রির (বাকি/জমা) জন্য অ্যাকশন বটম-শিট — সম্পাদনা, বিস্তারিত, শেয়ার,
 * আদায়-চিহ্নিত করা এবং মুছে ফেলা। TransactionSheetHelper-এর সাথে হুবহু একই লুক (একই
 * bottom_sheet_transaction_actions.xml লেআউট ব্যবহার করে) কিন্তু KhataEntry-এর জন্য
 * স্বতন্ত্রভাবে লেখা, যাতে দেনা-পাওনা মডিউলের কোড স্পর্শ করতে না হয়।
 */
public class KhataEntrySheetHelper {

    public interface Refresh { void run(); }

    public static void showKhataEntrySheet(Activity act, DatabaseManager db,
                                            KhataEntry item, Refresh onChange) {
        BottomSheetDialog dialog = new BottomSheetDialog(act, R.style.PremiumBottomSheetDialog);
        View v = LayoutInflater.from(act).inflate(R.layout.bottom_sheet_transaction_actions, null);
        dialog.setContentView(v);

        boolean isBaki = item.isBaki();
        TextView sheetIcon = v.findViewById(R.id.sheetIcon);
        TextView sheetTitle = v.findViewById(R.id.sheetTitle);
        TextView sheetSubtitle = v.findViewById(R.id.sheetSubtitle);
        TextView sheetAmount = v.findViewById(R.id.sheetAmount);

        sheetIcon.setText(isBaki ? "৳" : "✓");
        sheetIcon.setBackground(act.getResources().getDrawable(
                isBaki ? R.drawable.bg_icon_circle_ledger : R.drawable.bg_icon_circle_receivable));
        sheetTitle.setText(item.getCustomerName());
        sheetSubtitle.setText(DatabaseManager.formatDateDisplay(item.getDate())
                + "  •  " + DatabaseManager.formatTimeDisplay(item.getTime())
                + "  •  " + item.getTypeDisplay()
                + (item.isPaid() ? " (আদায় হয়েছে)" : ""));
        sheetAmount.setText(DatabaseManager.formatAmount(item.getAmount()));
        sheetAmount.setTextColor(ContextCompat.getColor(act, isBaki ? R.color.bakiColor : R.color.jomaColor));

        // "আদায় করা হয়েছে" টগল — শুধু বাকি এন্ট্রির জন্য প্রাসঙ্গিক (জমা এন্ট্রি নিজেই একটা
        // সম্পন্ন পরিশোধ, তাই তার জন্য টগল দেখানোর দরকার নেই)
        View togglePaid = v.findViewById(R.id.actionTogglePaid);
        TextView togglePaidLabel = v.findViewById(R.id.togglePaidLabel);
        if (isBaki) {
            togglePaid.setVisibility(View.VISIBLE);
            togglePaidLabel.setText(item.isPaid() ? "↩️ পুনরায় বাকি করুন" : "✅ আদায় হয়েছে চিহ্নিত করুন");
            togglePaid.setOnClickListener(x -> {
                dialog.dismiss();
                List<KhataEntry> list = db.getKhataEntryList();
                int idx = findIndex(list, item);
                if (idx >= 0) {
                    KhataEntry entry = list.get(idx);
                    entry.setPaid(!entry.isPaid());
                    entry.setPaidDate(entry.isPaid() ? DatabaseManager.nowDate() : "");
                    db.updateKhataEntry(idx, entry);
                }
                if (onChange != null) onChange.run();
            });
        } else {
            togglePaid.setVisibility(View.GONE);
        }

        v.findViewById(R.id.actionEdit).setOnClickListener(x -> {
            dialog.dismiss();
            showEditKhataEntryDialog(act, db, item, onChange);
        });

        v.findViewById(R.id.actionDetails).setOnClickListener(x -> {
            dialog.dismiss();
            showKhataEntryDetails(act, item);
        });

        v.findViewById(R.id.actionShare).setOnClickListener(x -> {
            dialog.dismiss();
            shareKhataEntry(act, item);
        });

        v.findViewById(R.id.actionDelete).setOnClickListener(x -> {
            dialog.dismiss();
            confirmDeleteKhataEntry(act, db, item, onChange);
        });

        dialog.show();
    }

    private static int findIndex(List<KhataEntry> list, KhataEntry item) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(item.getId())) return i;
        }
        return -1;
    }

    private static void confirmDeleteKhataEntry(Activity act, DatabaseManager db,
                                                 KhataEntry item, Refresh onChange) {
        new AlertDialog.Builder(act, R.style.PremiumDialog)
                .setTitle("মুছে ফেলবেন?")
                .setMessage("\"" + item.getCustomerName() + "\" এর এই এন্ট্রিটি স্থায়ীভাবে মুছে যাবে।")
                .setPositiveButton("হ্যাঁ, মুছুন", (d, w) -> {
                    List<KhataEntry> list = db.getKhataEntryList();
                    int idx = findIndex(list, item);
                    if (idx >= 0) db.deleteKhataEntry(idx);
                    Toast.makeText(act, "মুছে গেছে", Toast.LENGTH_SHORT).show();
                    if (onChange != null) onChange.run();
                })
                .setNegativeButton("না, রাখুন", null)
                .show();
    }

    private static void shareKhataEntry(Activity act, KhataEntry item) {
        String text = " বাকির খাতা এন্ট্রি\n"
                + "─────────────\n"
                + "গ্রাহক: " + item.getCustomerName() + "\n"
                + "ধরন: " + item.getTypeDisplay() + "\n"
                + "পরিমাণ: ৳" + DatabaseManager.formatAmount(item.getAmount()) + "\n"
                + "তারিখ: " + DatabaseManager.formatDateDisplay(item.getDate()) + "\n"
                + "সময়: " + DatabaseManager.formatTimeDisplay(item.getTime())
                + (item.getCategory() != null && !item.getCategory().isEmpty() ? "\nবিবরণ: " + item.getCategory() : "")
                + (item.getNote() != null && !item.getNote().isEmpty() ? "\nনোট: " + item.getNote() : "");
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        act.startActivity(Intent.createChooser(intent, "শেয়ার করুন"));
    }

    private static void showKhataEntryDetails(Activity act, KhataEntry item) {
        View v = LayoutInflater.from(act).inflate(R.layout.dialog_transaction_details, null);
        ((TextView) v.findViewById(R.id.tvDetailTitle)).setText(item.getCustomerName());
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
                item.isBaki() ? (item.isPaid() ? "আদায় হয়েছে" : "বাকি আছে") : "জমা সম্পন্ন");

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

    public static void showEditKhataEntryDialog(Activity act, DatabaseManager db,
                                                  KhataEntry item, Refresh onChange) {
        View v = LayoutInflater.from(act).inflate(R.layout.dialog_edit_khata_entry, null);

        TextInputEditText etCustomer = v.findViewById(R.id.etEditKhataCustomer);
        TextInputEditText etCategory = v.findViewById(R.id.etEditCategory);
        TextInputLayout tilAmount = v.findViewById(R.id.tilEditAmount);
        TextInputEditText etAmount = v.findViewById(R.id.etEditAmount);
        View btnTypeBaki = v.findViewById(R.id.btnEditTypeDena);
        View btnTypeJoma = v.findViewById(R.id.btnEditTypePabona);
        View btnDate = v.findViewById(R.id.btnEditDate);
        TextView tvDate = v.findViewById(R.id.tvEditDate);
        View btnTime = v.findViewById(R.id.btnEditTime);
        TextView tvTime = v.findViewById(R.id.tvEditTime);
        TextInputEditText etNote = v.findViewById(R.id.etEditNote);

        etCustomer.setText(item.getCustomerName());
        etCustomer.setEnabled(false); // গ্রাহক বদলানো যাবে না — নতুন এন্ট্রি বানাতে হবে
        etCategory.setText(item.getCategory());
        etAmount.setText(item.getAmount() == Math.floor(item.getAmount())
                ? String.valueOf((long) item.getAmount()) : String.valueOf(item.getAmount()));
        etNote.setText(item.getNote());

        final String[] selType = {item.getType()};
        final String[] selDate = {item.getDate().isEmpty() ? DatabaseManager.nowDate() : item.getDate()};
        final String[] selTime = {item.getTime().isEmpty() ? DatabaseManager.nowTime() : item.getTime()};
        tvDate.setText(DatabaseManager.formatDateDisplay(selDate[0]));
        tvTime.setText(DatabaseManager.formatTimeDisplay(selTime[0]));

        Runnable refreshTypeUi = () -> {
            boolean baki = "baki".equals(selType[0]);
            if (btnTypeBaki instanceof TextView) {
                ((TextView) btnTypeBaki).setBackground(act.getResources().getDrawable(
                        baki ? R.drawable.bg_type_active_dena : R.drawable.bg_dialog_field));
                ((TextView) btnTypeBaki).setTextColor(ContextCompat.getColor(act,
                        baki ? R.color.white : R.color.secondaryTextDark));
            }
            if (btnTypeJoma instanceof TextView) {
                ((TextView) btnTypeJoma).setBackground(act.getResources().getDrawable(
                        !baki ? R.drawable.bg_type_active_pabona : R.drawable.bg_dialog_field));
                ((TextView) btnTypeJoma).setTextColor(ContextCompat.getColor(act,
                        !baki ? R.color.white : R.color.secondaryTextDark));
            }
        };
        refreshTypeUi.run();
        btnTypeBaki.setOnClickListener(x -> { selType[0] = "baki"; refreshTypeUi.run(); });
        btnTypeJoma.setOnClickListener(x -> { selType[0] = "joma"; refreshTypeUi.run(); });

        btnDate.setOnClickListener(x -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(act, (dp, y, m, d) -> {
                selDate[0] = String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m + 1, d);
                tvDate.setText(DatabaseManager.formatDateDisplay(selDate[0]));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnTime.setOnClickListener(x -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(act, (tp, h, min) -> {
                selTime[0] = String.format(java.util.Locale.US, "%02d:%02d", h, min);
                tvTime.setText(DatabaseManager.formatTimeDisplay(selTime[0]));
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
        });

        AlertDialog dialog = new AlertDialog.Builder(act, R.style.PremiumDialog)
                .setTitle("এন্ট্রি সম্পাদনা করুন")
                .setView(v)
                .setPositiveButton("সংরক্ষণ করুন", null)
                .setNegativeButton("বাতিল", null)
                .create();

        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
            double amount;
            try {
                amount = Double.parseDouble(etAmount.getText() != null ? etAmount.getText().toString().trim() : "");
                if (amount <= 0) throw new NumberFormatException();
            } catch (Exception e) {
                tilAmount.setError("সঠিক পরিমাণ লিখুন");
                return;
            }
            tilAmount.setError(null);

            List<KhataEntry> list = db.getKhataEntryList();
            int idx = findIndex(list, item);
            if (idx >= 0) {
                KhataEntry updated = list.get(idx);
                updated.setType(selType[0]);
                updated.setCategory(etCategory.getText() != null ? etCategory.getText().toString().trim() : "");
                updated.setAmount(amount);
                updated.setDate(selDate[0]);
                updated.setTime(selTime[0]);
                updated.setNote(etNote.getText() != null ? etNote.getText().toString().trim() : "");
                db.updateKhataEntry(idx, updated);
            }
            Toast.makeText(act, "আপডেট হয়েছে", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            if (onChange != null) onChange.run();
        });
    }
}
