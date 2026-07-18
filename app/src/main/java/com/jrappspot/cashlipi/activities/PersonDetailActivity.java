package com.jrappspot.cashlipi.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.adapters.PersonLedgerAdapter;
import com.jrappspot.cashlipi.models.LedgerEntry;
import com.jrappspot.cashlipi.models.Person;
import com.jrappspot.cashlipi.utils.AmountInputHelper;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import com.jrappspot.cashlipi.utils.FirestoreSyncManager;
import com.jrappspot.cashlipi.utils.SuccessPopup;
import com.jrappspot.cashlipi.utils.TransactionSheetHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * একজন ব্যক্তির বিস্তারিত পেজ — কমপ্যাক্ট হেডারে পরিচিতি (ছবি/নাম/সম্পর্ক, হোয়াটসঅ্যাপ/কল/মেইল
 * বাটন, ⋮ মেনুতে এডিট/মুছুন) এবং নিচে এই ব্যক্তির সাথে থাকা সব দিলাম/পেলাম (LedgerEntry, নাম
 * মিলিয়ে) লেনদেন — নিট বকেয়ার সামারি বার, প্রতিটা এন্ট্রির পাশে রানিং ব্যালেন্স, এবং নিচে
 * পেলাম/দিলাম বাটনে চেপে দ্রুত নতুন এন্ট্রি যোগ করার বটম শিট। আইটেমে ট্যাপ করলে বিদ্যমান
 * TransactionSheetHelper.showLedgerSheet() (সম্পাদনা/বিস্তারিত/শেয়ার/পরিশোধ/মুছুন) খোলে।
 */
public class PersonDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PERSON_ID = "extra_person_id";

    private static final int[] AVATAR_BGS = {
            R.drawable.bg_avatar_circle_1, R.drawable.bg_avatar_circle_2,
            R.drawable.bg_avatar_circle_3, R.drawable.bg_avatar_circle_4,
            R.drawable.bg_avatar_circle_5
    };

    private DatabaseManager db;
    private String personId;
    private Person person;

    private ImageView ivPhoto;
    private LinearLayout avatarInitial;
    private TextView tvInitial, tvName, tvRelationBadge, tvPhone;
    private ImageView btnWhatsapp, btnCall, btnMail;

    // দেনা-পাওনা লেনদেন অংশ
    private LinearLayout summaryBar, emptyLedgerState;
    private TextView tvSummaryLabel, tvSummaryAmount, tvSummarySub;
    private RecyclerView rvPersonLedger;
    private Button btnAddPelam, btnAddDilam;
    private PersonLedgerAdapter ledgerAdapter;
    private final List<PersonLedgerAdapter.Row> ledgerRows = new ArrayList<>();

    // Quick-add bottom sheet-এর জন্য অস্থায়ী state
    private String sheetDate = "", sheetTime = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_detail);
        db = DatabaseManager.getInstance(this);

        personId = getIntent().getStringExtra(EXTRA_PERSON_ID);

        ivPhoto = findViewById(R.id.ivDetailPhoto);
        avatarInitial = findViewById(R.id.avatarInitialDetail);
        tvInitial = findViewById(R.id.tvDetailInitial);
        tvName = findViewById(R.id.tvDetailName);
        tvRelationBadge = findViewById(R.id.tvDetailRelationBadge);
        tvPhone = findViewById(R.id.tvDetailPhone);
        btnWhatsapp = findViewById(R.id.btnWhatsapp);
        btnCall = findViewById(R.id.btnCall);
        btnMail = findViewById(R.id.btnMail);

        summaryBar       = findViewById(R.id.summaryBar);
        tvSummaryLabel   = findViewById(R.id.tvSummaryLabel);
        tvSummaryAmount  = findViewById(R.id.tvSummaryAmount);
        tvSummarySub     = findViewById(R.id.tvSummarySub);
        rvPersonLedger   = findViewById(R.id.rvPersonLedger);
        emptyLedgerState = findViewById(R.id.emptyLedgerState);
        btnAddPelam      = findViewById(R.id.btnAddPelam);
        btnAddDilam      = findViewById(R.id.btnAddDilam);
        rvPersonLedger.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnCall.setOnClickListener(v -> callPerson());
        btnWhatsapp.setOnClickListener(v -> openWhatsapp());
        btnMail.setOnClickListener(v -> sendMail());
        findViewById(R.id.btnMorePerson).setOnClickListener(this::showMoreMenu);

        // পেলাম → পাওনা (আমি পাব) এন্ট্রি, দিলাম → দেনা (আমি দেব) এন্ট্রি — এই পেজের জন্য শুধু
        // colloquial লেবেল, বাকি অ্যাপের দেনা/পাওনা মডেলই অক্ষত থাকে
        btnAddPelam.setOnClickListener(v -> showAddLedgerSheet("pabona"));
        btnAddDilam.setOnClickListener(v -> showAddLedgerSheet("dena"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPerson();
        loadLedger();
    }

    private void loadPerson() {
        person = db.getPersonById(personId);
        if (person == null) { finish(); return; }

        tvName.setText(person.getName().isEmpty() ? "নাম নেই" : person.getName());

        if (person.hasRelation()) {
            tvRelationBadge.setVisibility(View.VISIBLE);
            tvRelationBadge.setText(person.getRelation());
        } else {
            tvRelationBadge.setVisibility(View.GONE);
        }

        if (person.hasPhone()) {
            tvPhone.setVisibility(View.VISIBLE);
            tvPhone.setText(person.getPhone());
        } else {
            tvPhone.setVisibility(View.GONE);
        }

        btnCall.setVisibility(person.hasPhone() ? View.VISIBLE : View.GONE);
        btnWhatsapp.setVisibility(person.hasPhone() ? View.VISIBLE : View.GONE);
        btnMail.setVisibility(person.hasEmail() ? View.VISIBLE : View.GONE);

        if (person.hasPhoto() && new File(person.getPhotoPath()).exists()) {
            avatarInitial.setVisibility(View.GONE);
            ivPhoto.setVisibility(View.VISIBLE);
            Glide.with(this).load(new File(person.getPhotoPath())).transform(new CircleCrop()).into(ivPhoto);
        } else {
            ivPhoto.setVisibility(View.GONE);
            avatarInitial.setVisibility(View.VISIBLE);
            tvInitial.setText(person.getInitial());
            int colorIdx = Math.abs(person.getName().hashCode()) % AVATAR_BGS.length;
            avatarInitial.setBackgroundResource(AVATAR_BGS[colorIdx]);
        }
    }

    /**
     * এই ব্যক্তির নামে থাকা সব LedgerEntry (person নাম মিলিয়ে) লোড করে, পুরনো থেকে নতুন সাজিয়ে
     * প্রতিটার পর রানিং ব্যালেন্স হিসাব করে, তারপর নতুন থেকে পুরনো ক্রমে (উপরে সর্বশেষ) দেখায়।
     */
    private void loadLedger() {
        if (person == null) return;
        List<LedgerEntry> raw = db.getLedgerForPersonName(person.getName());

        List<LedgerEntry> chrono = new ArrayList<>(raw);
        Collections.sort(chrono, (a, b) -> {
            String ka = a.getDate() + " " + a.getTime();
            String kb = b.getDate() + " " + b.getTime();
            int c = ka.compareTo(kb);
            if (c != 0) return c;
            return a.getCreatedAt().compareTo(b.getCreatedAt());
        });

        ledgerRows.clear();
        double running = 0;
        for (LedgerEntry e : chrono) {
            if (!e.isPaid()) {
                running += e.isPabona() ? e.getAmount() : -e.getAmount();
            }
            ledgerRows.add(new PersonLedgerAdapter.Row(e, running));
        }
        Collections.reverse(ledgerRows); // সর্বশেষ লেনদেন উপরে

        int unpaidCount = 0;
        for (LedgerEntry e : raw) if (!e.isPaid()) unpaidCount++;

        // সামারি বার
        if (raw.isEmpty()) {
            summaryBar.setVisibility(View.GONE);
        } else {
            summaryBar.setVisibility(View.VISIBLE);
            double net = ledgerRows.isEmpty() ? 0 : ledgerRows.get(0).balanceAfter;
            if (net > 0.5) {
                summaryBar.setBackgroundResource(R.drawable.bg_type_active_pabona);
                tvSummaryLabel.setText("আপনি পাবেন");
                tvSummaryAmount.setText(DatabaseManager.formatAmount(net));
            } else if (net < -0.5) {
                summaryBar.setBackgroundResource(R.drawable.bg_type_active_dena);
                tvSummaryLabel.setText("আপনি দেবেন");
                tvSummaryAmount.setText(DatabaseManager.formatAmount(Math.abs(net)));
            } else {
                summaryBar.setBackgroundResource(R.drawable.bg_type_settled);
                tvSummaryLabel.setText("হিসাব");
                tvSummaryAmount.setText("সব পরিশোধিত ✓");
            }
            tvSummarySub.setText("মোট " + raw.size() + " টি লেনদেন\n" + unpaidCount + " টি বাকি");
        }

        // তালিকা বনাম খালি অবস্থা
        if (ledgerRows.isEmpty()) {
            rvPersonLedger.setVisibility(View.GONE);
            emptyLedgerState.setVisibility(View.VISIBLE);
        } else {
            rvPersonLedger.setVisibility(View.VISIBLE);
            emptyLedgerState.setVisibility(View.GONE);
            ledgerAdapter = new PersonLedgerAdapter(this, ledgerRows,
                    entry -> TransactionSheetHelper.showLedgerSheet(this, db, entry, () -> {
                        loadLedger();
                        com.jrappspot.cashlipi.utils.BackupManager.getInstance(this).triggerAutoGoogleDriveSync();
                        FirestoreSyncManager.getInstance(this).uploadAllData(null);
                    }));
            rvPersonLedger.setAdapter(ledgerAdapter);
        }
    }

    /** নিচের পেলাম/দিলাম বাটনে চাপলে এই ব্যক্তির নাম প্রি-ফিল করে দ্রুত এন্ট্রি যোগ করার শিট। */
    private void showAddLedgerSheet(String type) {
        if (person == null) return;
        boolean isDena = "dena".equals(type);

        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.PremiumBottomSheetDialog);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_add_person_ledger, null);
        dialog.setContentView(v);

        TextView tvTitle = v.findViewById(R.id.tvSheetTitle);
        TextView tvPersonChip = v.findViewById(R.id.tvSheetPersonName);
        TextInputLayout tilAmount = v.findViewById(R.id.tilSheetAmount);
        TextInputEditText etAmount = v.findViewById(R.id.etSheetAmount);
        TextInputEditText etNote = v.findViewById(R.id.etSheetNote);
        TextView tvDate = v.findViewById(R.id.tvSheetDate);
        TextView tvTime = v.findViewById(R.id.tvSheetTime);
        Button btnSave = v.findViewById(R.id.btnSheetSave);

        tvTitle.setText(isDena ? " দিলাম যোগ করুন" : " পেলাম যোগ করুন");
        tvPersonChip.setText(person.getName());
        btnSave.setBackgroundResource(isDena ? R.drawable.bg_type_active_dena : R.drawable.bg_type_active_pabona);
        btnSave.setText(isDena ? " দিলাম সংরক্ষণ করুন" : " পেলাম সংরক্ষণ করুন");

        AmountInputHelper.attach(this, etAmount);

        sheetDate = DatabaseManager.nowDate();
        sheetTime = DatabaseManager.nowTime();
        tvDate.setText(DatabaseManager.formatDateDisplay(sheetDate));
        tvTime.setText(DatabaseManager.formatTimeDisplay(sheetTime));

        tvDate.setOnClickListener(x -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, y, m, d) -> {
                sheetDate = String.format("%04d-%02d-%02d", y, m + 1, d);
                tvDate.setText(DatabaseManager.formatDateDisplay(sheetDate));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
        tvTime.setOnClickListener(x -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(this, (view, h, min) -> {
                sheetTime = String.format("%02d:%02d", h, min);
                tvTime.setText(DatabaseManager.formatTimeDisplay(sheetTime));
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
        });

        int[] qIds = {R.id.btnSheetQ100, R.id.btnSheetQ500, R.id.btnSheetQ1000, R.id.btnSheetQ5000};
        int[] qVals = {100, 500, 1000, 5000};
        for (int i = 0; i < qIds.length; i++) {
            final int val = qVals[i];
            Button qb = v.findViewById(qIds[i]);
            qb.setOnClickListener(x -> {
                String cur = etAmount.getText() != null ? etAmount.getText().toString() : "";
                try {
                    double base = cur.isEmpty() ? 0 : Double.parseDouble(cur);
                    etAmount.setText(String.valueOf((long) (base + val)));
                } catch (Exception e) {
                    etAmount.setText(String.valueOf(val));
                }
                etAmount.setSelection(etAmount.getText().length());
            });
        }

        btnSave.setOnClickListener(x -> {
            String amtStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
            if (amtStr.isEmpty()) { tilAmount.setError("পরিমাণ লিখুন"); return; }
            double amount;
            try {
                amount = Double.parseDouble(amtStr);
                if (amount <= 0) throw new NumberFormatException();
            } catch (Exception e) {
                tilAmount.setError("সঠিক পরিমাণ লিখুন");
                return;
            }
            tilAmount.setError(null);

            LedgerEntry entry = new LedgerEntry();
            entry.setPerson(person.getName());
            entry.setType(type);
            entry.setAmount(amount);
            entry.setDate(sheetDate.isEmpty() ? DatabaseManager.nowDate() : sheetDate);
            entry.setTime(sheetTime.isEmpty() ? DatabaseManager.nowTime() : sheetTime);
            entry.setNote(etNote.getText() != null ? etNote.getText().toString().trim() : "");
            entry.setPaid(false);
            db.addLedger(entry);

            com.jrappspot.cashlipi.widgets.FinanceWidgetProvider.updateAll(this);
            com.jrappspot.cashlipi.utils.BackupManager.getInstance(this).triggerAutoGoogleDriveSync();
            FirestoreSyncManager.getInstance(this).uploadAllData(null);

            dialog.dismiss();
            loadLedger();

            SuccessPopup.Category cat = isDena ? SuccessPopup.Category.DENA : SuccessPopup.Category.PABONA;
            SuccessPopup.show(this, cat,
                    (isDena ? "দিলাম" : "পেলাম") + " যোগ সফল হয়েছে!",
                    person.getName() + "-এর হিসাব আপডেট হয়েছে।",
                    null, null);
        });

        dialog.show();
    }

    private void callPerson() {
        if (person == null || !person.hasPhone()) return;
        try {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + person.getPhone())));
        } catch (Exception e) {
            Toast.makeText(this, "কল করা যায়নি", Toast.LENGTH_SHORT).show();
        }
    }

    private void openWhatsapp() {
        if (person == null || !person.hasPhone()) return;
        String phone = person.getPhone().replaceAll("[^0-9+]", "");
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + phone));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "হোয়াটসঅ্যাপ খোলা যায়নি", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMail() {
        if (person == null || !person.hasEmail()) return;
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + person.getEmail()));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "মেইল অ্যাপ পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
        }
    }

    private void showMoreMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, 1, 0, "সম্পাদনা করুন");
        menu.getMenu().add(0, 2, 1, "মুছে ফেলুন");
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                Intent i = new Intent(this, AddPersonActivity.class);
                i.putExtra(AddPersonActivity.EXTRA_EDIT_PERSON_ID, personId);
                startActivity(i);
                return true;
            } else if (item.getItemId() == 2) {
                confirmDelete();
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("মুছে ফেলবেন?")
                .setMessage(person.getName() + " কে দেনা-পাওনা তালিকা থেকে মুছে ফেলা হবে।")
                .setPositiveButton("মুছুন", (dialog, which) -> {
                    int index = db.getPersonIndexById(personId);
                    db.deletePerson(index);
                    Toast.makeText(this, "মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }
}
