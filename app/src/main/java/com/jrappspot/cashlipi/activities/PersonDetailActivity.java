package com.jrappspot.cashlipi.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
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
 * বাটন, ⋮ মেনুতে এডিট/মুছুন) এবং নিচে এই ব্যক্তির সাথে থাকা সব দেনা/পাওনা (LedgerEntry, নাম
 * মিলিয়ে) লেনদেন — নিট বকেয়ার সামারি বার, প্রতিটা এন্ট্রির পাশে রানিং ব্যালেন্স, এবং নিচে
 * পাওনা/দেনা বাটনে চেপে দ্রুত নতুন এন্ট্রি যোগ করার বটম শিট। আইটেমে ট্যাপ করলে বিদ্যমান
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
    private androidx.cardview.widget.CardView heroCard;
    private LinearLayout heroCardInner, emptyLedgerState, personSearchBox, personToolsRow;
    private TextView tvHeroLabel, tvHeroAmount, tvHeroSub;
    private ImageView btnShareStatement, ivClearPersonSearch, btnToggleView, btnExportPdf;
    private EditText etPersonLedgerSearch;
    private ImageView btnPersonFilter;
    private View personFilterActiveDot;
    private TextView tvEmptyLedgerTitle, tvEmptyLedgerSub;
    private RecyclerView rvPersonLedger;
    private LinearLayout tableViewContainer, tableRowsContainer;
    private TextView tvTableTotalDena, tvTableTotalPabona;
    private Button btnAddPelam, btnAddDilam;
    private PersonLedgerAdapter ledgerAdapter;
    private String viewMode = "card"; // card | table

    /** পুরো ব্যক্তির ledger (রানিং ব্যালেন্স সহ, নতুন-থেকে-পুরনো) — সার্চ/ফিল্টার এখান থেকে চালানো হয়। */
    private final List<PersonLedgerAdapter.Row> allRows = new ArrayList<>();
    private List<LedgerEntry> allRawEntries = new ArrayList<>();
    private String currentFilter = "all"; // all | unpaid | paid
    private String currentQuery = "";

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

        heroCard          = findViewById(R.id.heroCard);
        heroCardInner     = findViewById(R.id.heroCardInner);
        tvHeroLabel       = findViewById(R.id.tvHeroLabel);
        tvHeroAmount      = findViewById(R.id.tvHeroAmount);
        tvHeroSub         = findViewById(R.id.tvHeroSub);
        btnShareStatement = findViewById(R.id.btnShareStatement);

        personToolsRow        = findViewById(R.id.personToolsRow);
        personSearchBox       = findViewById(R.id.personSearchBox);
        etPersonLedgerSearch  = findViewById(R.id.etPersonLedgerSearch);
        ivClearPersonSearch   = findViewById(R.id.ivClearPersonSearch);
        btnPersonFilter       = findViewById(R.id.btnPersonFilter);
        personFilterActiveDot = findViewById(R.id.personFilterActiveDot);
        btnToggleView         = findViewById(R.id.btnToggleView);
        btnExportPdf          = findViewById(R.id.btnExportPdf);

        rvPersonLedger      = findViewById(R.id.rvPersonLedger);
        tableViewContainer  = findViewById(R.id.tableViewContainer);
        tableRowsContainer  = findViewById(R.id.tableRowsContainer);
        tvTableTotalDena    = findViewById(R.id.tvTableTotalDena);
        tvTableTotalPabona  = findViewById(R.id.tvTableTotalPabona);
        emptyLedgerState    = findViewById(R.id.emptyLedgerState);
        tvEmptyLedgerTitle  = findViewById(R.id.tvEmptyLedgerTitle);
        tvEmptyLedgerSub    = findViewById(R.id.tvEmptyLedgerSub);
        btnAddPelam         = findViewById(R.id.btnAddPelam);
        btnAddDilam         = findViewById(R.id.btnAddDilam);
        rvPersonLedger.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnCall.setOnClickListener(v -> callPerson());
        btnWhatsapp.setOnClickListener(v -> openWhatsapp());
        btnMail.setOnClickListener(v -> sendMail());
        findViewById(R.id.btnMorePerson).setOnClickListener(this::showMoreMenu);

        // পাওনা (+) → পাওনা এন্ট্রি (আমি পাব), দেনা (−) → দেনা এন্ট্রি (আমি দেব)
        btnAddPelam.setOnClickListener(v -> showAddLedgerSheet("pabona"));
        btnAddDilam.setOnClickListener(v -> showAddLedgerSheet("dena"));
        btnShareStatement.setOnClickListener(v -> shareStatement());
        btnToggleView.setOnClickListener(v -> {
            viewMode = "card".equals(viewMode) ? "table" : "card";
            btnToggleView.setImageResource("card".equals(viewMode) ? R.drawable.ic_view_grid : R.drawable.ic_nav_dena_pawna);
            applyFiltersAndRender();
        });
        // PDF এক্সপোর্ট বাটন — আপাতত শুধু UI, ফাংশন পরে যোগ হবে
        btnExportPdf.setOnClickListener(v -> Toast.makeText(this, "PDF এক্সপোর্ট শীঘ্রই আসছে", Toast.LENGTH_SHORT).show());

        etPersonLedgerSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                currentQuery = s.toString().trim();
                ivClearPersonSearch.setVisibility(currentQuery.isEmpty() ? View.GONE : View.VISIBLE);
                applyFiltersAndRender();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        ivClearPersonSearch.setOnClickListener(v -> etPersonLedgerSearch.setText(""));

        btnPersonFilter.setOnClickListener(this::showPersonFilterMenu);
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
     * প্রতিটার পর রানিং ব্যালেন্স হিসাব করে allRows-এ (নতুন-থেকে-পুরনো) রাখে, হিরো কার্ড আপডেট
     * করে, তারপর সার্চ/ফিল্টার প্রয়োগ করে তালিকা রেন্ডার করে।
     */
    private void loadLedger() {
        if (person == null) return;
        allRawEntries = db.getLedgerForPersonName(person.getName());

        List<LedgerEntry> chrono = new ArrayList<>(allRawEntries);
        Collections.sort(chrono, (a, b) -> {
            String ka = a.getDate() + " " + a.getTime();
            String kb = b.getDate() + " " + b.getTime();
            int c = ka.compareTo(kb);
            if (c != 0) return c;
            return a.getCreatedAt().compareTo(b.getCreatedAt());
        });

        allRows.clear();
        double running = 0, totalGot = 0, totalGave = 0;
        int unpaidCount = 0;
        for (LedgerEntry e : chrono) {
            if (!e.isPaid()) {
                running += e.isPabona() ? e.getAmount() : -e.getAmount();
                unpaidCount++;
            }
            if (e.isPabona()) totalGot += e.getAmount(); else totalGave += e.getAmount();
            allRows.add(new PersonLedgerAdapter.Row(e, running));
        }
        Collections.reverse(allRows); // সর্বশেষ লেনদেন উপরে

        // হিরো কার্ড — কম্প্যাক্ট: নিট হিসাব + মোট লেনদেন/বাকি/পাওনা/দেনা এক জায়গায়
        if (allRawEntries.isEmpty()) {
            heroCard.setVisibility(View.GONE);
            personToolsRow.setVisibility(View.GONE);
        } else {
            heroCard.setVisibility(View.VISIBLE);
            personToolsRow.setVisibility(View.VISIBLE);

            double net = allRows.isEmpty() ? 0 : allRows.get(0).balanceAfter;
            if (net > 0.5) {
                heroCardInner.setBackgroundResource(R.drawable.bg_hero_flat_pabona);
                tvHeroLabel.setText("আপনি পাবেন");
                tvHeroAmount.setText(DatabaseManager.formatAmount(net));
            } else if (net < -0.5) {
                heroCardInner.setBackgroundResource(R.drawable.bg_hero_flat_dena);
                tvHeroLabel.setText("আপনি দেবেন");
                tvHeroAmount.setText(DatabaseManager.formatAmount(Math.abs(net)));
            } else {
                heroCardInner.setBackgroundResource(R.drawable.bg_hero_flat_settled);
                tvHeroLabel.setText("নিট হিসাব");
                tvHeroAmount.setText("সব পরিশোধিত ✓");
            }
            tvHeroSub.setText("মোট " + allRawEntries.size() + " টি লেনদেন  •  " + unpaidCount + " টি বাকি\n"
                    + "মোট পাওনা ৳" + DatabaseManager.formatAmount(totalGot)
                    + "  •  মোট দেনা ৳" + DatabaseManager.formatAmount(totalGave));
        }

        applyFiltersAndRender();
    }

    /** ফিল্টার আইকনে ট্যাপ করলে সব/অপরিশোধিত/পরিশোধিত অপশনসহ পপ-আপ মেনু দেখায়। */
    private void showPersonFilterMenu(View anchor) {
        android.widget.PopupMenu menu = new android.widget.PopupMenu(this, anchor);
        menu.getMenu().add(0, 0, 0, "সব");
        menu.getMenu().add(0, 1, 1, "অপরিশোধিত");
        menu.getMenu().add(0, 2, 2, "পরিশোধিত");
        menu.setOnMenuItemClickListener(item -> {
            String[] keys = {"all", "unpaid", "paid"};
            setPersonFilter(keys[item.getItemId()]);
            return true;
        });
        menu.show();
    }

    /** ফিল্টার নির্বাচন করলে সেটা মনে রাখে ও তালিকা রিফ্রেশ করে; ডিফল্ট ("সব") ছাড়া অন্য কিছু
     *  বাছাই করা থাকলে ফিল্টার আইকনের কোণায় একটা ছোট ডট দেখায়, যাতে বোঝা যায় ফিল্টার সক্রিয়। */
    private void setPersonFilter(String filter) {
        currentFilter = filter;
        personFilterActiveDot.setVisibility("all".equals(filter) ? View.GONE : View.VISIBLE);
        applyFiltersAndRender();
    }

    /**
     * currentFilter ও currentQuery অনুযায়ী allRows থেকে ফিল্টার করে viewMode অনুযায়ী
     * কার্ড-লিস্ট (RecyclerView) অথবা ছক (টেবিল) — যেটা সক্রিয় সেটাতে দেখায়।
     */
    private void applyFiltersAndRender() {
        if (person == null) return;

        List<PersonLedgerAdapter.Row> filtered = new ArrayList<>();
        String q = currentQuery.toLowerCase(java.util.Locale.US);
        for (PersonLedgerAdapter.Row row : allRows) {
            LedgerEntry e = row.entry;
            if ("unpaid".equals(currentFilter) && e.isPaid()) continue;
            if ("paid".equals(currentFilter) && !e.isPaid()) continue;
            if (!q.isEmpty()) {
                String hay = ((e.getNote() == null ? "" : e.getNote()) + " "
                        + (e.getCategory() == null ? "" : e.getCategory()) + " "
                        + DatabaseManager.formatAmount(e.getAmount())).toLowerCase(java.util.Locale.US);
                if (!hay.contains(q)) continue;
            }
            filtered.add(row);
        }

        if (allRawEntries.isEmpty()) {
            rvPersonLedger.setVisibility(View.GONE);
            tableViewContainer.setVisibility(View.GONE);
            emptyLedgerState.setVisibility(View.VISIBLE);
            tvEmptyLedgerTitle.setText("এই ব্যক্তির সাথে এখনও কোনো লেনদেন যোগ করা হয়নি");
            tvEmptyLedgerSub.setText("নিচের বাটনে চেপে প্রথম দেনা বা পাওনা যোগ করুন");
        } else if (filtered.isEmpty()) {
            rvPersonLedger.setVisibility(View.GONE);
            tableViewContainer.setVisibility(View.GONE);
            emptyLedgerState.setVisibility(View.VISIBLE);
            tvEmptyLedgerTitle.setText("কোনো লেনদেন খুঁজে পাওয়া যায়নি");
            tvEmptyLedgerSub.setText("সার্চ বা ফিল্টার পাল্টে আবার চেষ্টা করুন");
        } else {
            emptyLedgerState.setVisibility(View.GONE);
            if ("table".equals(viewMode)) {
                rvPersonLedger.setVisibility(View.GONE);
                tableViewContainer.setVisibility(View.VISIBLE);
                renderTableView(filtered);
            } else {
                tableViewContainer.setVisibility(View.GONE);
                rvPersonLedger.setVisibility(View.VISIBLE);
                ledgerAdapter = new PersonLedgerAdapter(this, filtered,
                        entry -> TransactionSheetHelper.showLedgerSheet(this, db, entry, () -> {
                            loadLedger();
                            com.jrappspot.cashlipi.utils.BackupManager.getInstance(this).triggerAutoGoogleDriveSync();
                            FirestoreSyncManager.getInstance(this).uploadAllData(null);
                        }));
                rvPersonLedger.setAdapter(ledgerAdapter);
            }
        }
    }

    /**
     * ছক (টেবিল) ভিউ — বিবরণ/দেনা/পাওনা কলামে সাজিয়ে দেখায়, প্রতিটা সারিতে ট্যাপ করলে একই
     * TransactionSheetHelper.showLedgerSheet() খোলে, নিচে মোট দেনা/পাওনার যোগফল দেখায়।
     */
    private void renderTableView(List<PersonLedgerAdapter.Row> filtered) {
        tableRowsContainer.removeAllViews();
        double sumDena = 0, sumPabona = 0;

        for (PersonLedgerAdapter.Row row : filtered) {
            LedgerEntry e = row.entry;
            boolean isDena = e.isDena();
            if (isDena) sumDena += e.getAmount(); else sumPabona += e.getAmount();

            View rowView = LayoutInflater.from(this).inflate(R.layout.item_person_ledger_table_row, tableRowsContainer, false);
            TextView tvTitle = rowView.findViewById(R.id.tvTRowTitle);
            TextView tvDateTime = rowView.findViewById(R.id.tvTRowDateTime);
            TextView tvChip = rowView.findViewById(R.id.tvTRowChip);
            TextView tvDena = rowView.findViewById(R.id.tvTRowDena);
            TextView tvPabona = rowView.findViewById(R.id.tvTRowPabona);
            View colorBar = rowView.findViewById(R.id.tvTRowColorBar);
            colorBar.setBackgroundColor(ContextCompat.getColor(this, isDena ? R.color.denaColor : R.color.pabonaColor));

            String note = e.getNote() != null && !e.getNote().isEmpty() ? e.getNote()
                    : (e.getCategory() != null ? e.getCategory() : "");
            tvTitle.setText(note.isEmpty() ? (isDena ? "দেনা" : "পাওনা") : note);
            tvDateTime.setText(DatabaseManager.formatDateDisplay(e.getDate()) + ", " + DatabaseManager.formatTimeDisplay(e.getTime()));

            tvDena.setText(isDena ? DatabaseManager.formatAmount(e.getAmount()) : "");
            tvPabona.setText(isDena ? "" : DatabaseManager.formatAmount(e.getAmount()));

            if (e.isPaid()) {
                tvChip.setVisibility(View.VISIBLE);
                // পাওনা পরিশোধ হলে "পেলাম", দেনা পরিশোধ হলে "দিলাম" — বাম পাশের দেনা/পাওনা রঙিন
                // বার/কলাম অপরিবর্তিত থাকে, শুধু এই ডান পাশের স্ট্যাটাস চিপ পাল্টায়
                tvChip.setText(isDena ? "দিলাম" : "পেলাম");
                tvChip.setTextColor(ContextCompat.getColor(this, R.color.amountIncome));
                DrawableCompat.setTint(DrawableCompat.wrap(tvChip.getBackground().mutate()),
                        ContextCompat.getColor(this, R.color.dividerColor));
            } else {
                double bal = row.balanceAfter;
                tvChip.setVisibility(View.VISIBLE);
                if (bal > 0.5) {
                    tvChip.setText("পাবেন " + DatabaseManager.formatAmount(bal));
                    tvChip.setTextColor(ContextCompat.getColor(this, R.color.pabonaColor));
                    DrawableCompat.setTint(DrawableCompat.wrap(tvChip.getBackground().mutate()), ContextCompat.getColor(this, R.color.pabonaLight));
                } else if (bal < -0.5) {
                    tvChip.setText("দেবেন " + DatabaseManager.formatAmount(Math.abs(bal)));
                    tvChip.setTextColor(ContextCompat.getColor(this, R.color.denaColor));
                    DrawableCompat.setTint(DrawableCompat.wrap(tvChip.getBackground().mutate()), ContextCompat.getColor(this, R.color.denaLight));
                } else {
                    tvChip.setText("সমান");
                    tvChip.setTextColor(ContextCompat.getColor(this, R.color.textSecondary));
                    DrawableCompat.setTint(DrawableCompat.wrap(tvChip.getBackground().mutate()), ContextCompat.getColor(this, R.color.dividerColor));
                }
            }

            // ছক ভিউতে এন্ট্রি ট্যাপ করে এডিট করা যাবে না — এটা শুধু দেখার জন্য (রিড-অনলি)।
            // এডিট করতে হলে কার্ড ভিউতে গিয়ে করতে হবে।

            tableRowsContainer.addView(rowView);
        }

        tvTableTotalDena.setText(DatabaseManager.formatAmount(sumDena));
        tvTableTotalPabona.setText(DatabaseManager.formatAmount(sumPabona));
    }

    /** পুরো লেনদেন বিবরণী সহজ টেক্সট আকারে শেয়ার করে (WhatsApp/মেসেজ/ইমেইল ইত্যাদিতে)। */
    private void shareStatement() {
        if (person == null || allRawEntries.isEmpty()) {
            Toast.makeText(this, "শেয়ার করার মতো কোনো লেনদেন নেই", Toast.LENGTH_SHORT).show();
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(person.getName()).append(" — দেনা-পাওনা বিবরণী\n");
        sb.append("তৈরি: CashLipi অ্যাপ দিয়ে\n\n");

        double net = allRows.isEmpty() ? 0 : allRows.get(0).balanceAfter;
        if (net > 0.5) sb.append("নিট হিসাব: আপনি পাবেন ৳").append(DatabaseManager.formatAmount(net)).append("\n\n");
        else if (net < -0.5) sb.append("নিট হিসাব: আপনি দেবেন ৳").append(DatabaseManager.formatAmount(Math.abs(net))).append("\n\n");
        else sb.append("নিট হিসাব: সব পরিশোধিত\n\n");

        for (PersonLedgerAdapter.Row row : allRows) {
            LedgerEntry e = row.entry;
            sb.append(DatabaseManager.formatDateDisplay(e.getDate())).append(", ").append(DatabaseManager.formatTimeDisplay(e.getTime()))
              .append(" — ").append(e.isDena() ? "দেনা" : "পাওনা")
              .append(" ৳").append(DatabaseManager.formatAmount(e.getAmount()));
            if (e.isPaid()) sb.append(" (").append(e.isDena() ? "দিলাম" : "পেলাম").append(")");
            if (e.getNote() != null && !e.getNote().isEmpty()) sb.append(" — ").append(e.getNote());
            sb.append("\n");
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, person.getName() + " — দেনা-পাওনা বিবরণী");
        intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        try {
            startActivity(Intent.createChooser(intent, "বিবরণী শেয়ার করুন"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "শেয়ার করার কোনো অ্যাপ পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
        }
    }

    /** নিচের পাওনা/দেনা বাটনে চাপলে এই ব্যক্তির নাম প্রি-ফিল করে দ্রুত এন্ট্রি যোগ করার শিট। */
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

        tvTitle.setText(isDena ? " দেনা যোগ করুন" : " পাওনা যোগ করুন");
        tvPersonChip.setText(person.getName());
        btnSave.setBackgroundResource(isDena ? R.drawable.bg_type_active_dena : R.drawable.bg_type_active_pabona);
        btnSave.setText(isDena ? " দেনা সংরক্ষণ করুন" : " পাওনা সংরক্ষণ করুন");

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
                    (isDena ? "দেনা" : "পাওনা") + " যোগ সফল হয়েছে!",
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
