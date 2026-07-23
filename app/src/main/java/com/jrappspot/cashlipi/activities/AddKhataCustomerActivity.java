package com.jrappspot.cashlipi.activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.KhataCustomer;
import com.jrappspot.cashlipi.utils.DatabaseManager;

import java.io.File;
import java.util.Calendar;

/**
 * "নতুন ব্যক্তি" ফর্ম — দেনা-পাওনা পেজে নতুন ব্যক্তি/প্রতিষ্ঠান যোগ করার স্ক্রিন।
 * ছবি (ক্রপসহ), নাম + সম্পর্ক, ফোন নম্বর (ফোনবুক থেকেও নেওয়া যায়), ঠিকানা ও তারিখ-সময় নিয়ে সেইভ করে।
 * নাম বা মোবাইল নম্বরের যেকোনো একটি লিখলেই সেইভ বাটন সক্রিয় হয়।
 */
public class AddKhataCustomerActivity extends AppCompatActivity {

    public static final String EXTRA_EDIT_PERSON_ID = "extra_edit_person_id";

    private static final int REQ_CONTACT_PERMISSION = 501;

    private DatabaseManager db;
    private FrameLayout photoFrame;
    private ImageView ivPhoto;
    private View photoRing, photoPlaceholder;
    private EditText etName, etRelation, etPhone, etAddress, etEmail, etOpeningBalance;
    private LinearLayout btnSaveKhataCustomer;

    private String pendingPhotoPath = "";
    private String selectedDate;
    private String selectedTime;
    private KhataCustomer editingKhataCustomer = null; // এডিট মোডে non-null

    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Intent> cropLauncher;
    private ActivityResultLauncher<Intent> contactPickLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_khata_customer);
        db = DatabaseManager.getInstance(this);

        registerLaunchers();
        bindViews();
        setupDefaults();
        setupListeners();

        String editId = getIntent().getStringExtra(EXTRA_EDIT_PERSON_ID);
        if (editId != null) loadForEdit(editId);
    }

    private void bindViews() {
        photoFrame = findViewById(R.id.photoFrame);
        ivPhoto = findViewById(R.id.ivKhataCustomerPhoto);
        photoRing = findViewById(R.id.photoRing);
        photoPlaceholder = findViewById(R.id.photoPlaceholder);
        etName = findViewById(R.id.etName);
        etRelation = findViewById(R.id.etRelation);
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);
        etEmail = findViewById(R.id.etEmail);
        etOpeningBalance = findViewById(R.id.etOpeningBalance);
        btnSaveKhataCustomer = findViewById(R.id.btnSaveKhataCustomer);
    }

    private void registerLaunchers() {
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) launchCrop(uri);
        });

        cropLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                String path = result.getData().getStringExtra(PhotoCropActivity.EXTRA_RESULT_PATH);
                if (path != null) applyPhoto(path);
            }
        });

        contactPickLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                handlePickedContact(result.getData().getData());
            }
        });
    }

    private void setupDefaults() {
        // তারিখ ও সময় পেজে দেখানো হয় না (ইউজারের অনুরোধে সরানো হয়েছে) — তবে সংরক্ষণের
        // সময় KhataCustomer রেকর্ডে টাইমস্ট্যাম্প থাকা দরকার, তাই ব্যাকগ্রাউন্ডে বর্তমান
        // তারিখ-সময় ধরে রাখা হচ্ছে।
        Calendar c = Calendar.getInstance();
        selectedDate = String.format("%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
        selectedTime = String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
        refreshSaveEnabled();
    }

    private void setupListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        photoFrame.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        findViewById(R.id.btnPickContact).setOnClickListener(v -> pickFromContacts());

        TextWatcher watcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { refreshSaveEnabled(); }
            public void afterTextChanged(Editable s) {}
        };
        etName.addTextChangedListener(watcher);
        etPhone.addTextChangedListener(watcher);

        btnSaveKhataCustomer.setOnClickListener(v -> saveKhataCustomer());
    }

    // ═══════════════════════════════════════════
    //  ছবি: গ্যালারি → ক্রপ → প্রিভিউ
    // ═══════════════════════════════════════════
    private void launchCrop(Uri uri) {
        Intent i = new Intent(this, PhotoCropActivity.class);
        i.putExtra(PhotoCropActivity.EXTRA_IMAGE_URI, uri.toString());
        cropLauncher.launch(i);
    }

    private void applyPhoto(String path) {
        pendingPhotoPath = path;
        photoPlaceholder.setVisibility(View.GONE);
        photoRing.setVisibility(View.VISIBLE);
        ivPhoto.setVisibility(View.VISIBLE);
        Glide.with(this).load(new File(path)).transform(new CircleCrop()).into(ivPhoto);
    }

    // ═══════════════════════════════════════════
    //  ফোনবুক থেকে নাম্বার বাছাই
    // ═══════════════════════════════════════════
    private void pickFromContacts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS}, REQ_CONTACT_PERMISSION);
            return;
        }
        openContactPicker();
    }

    private void openContactPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        contactPickLauncher.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions, @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CONTACT_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openContactPicker();
            } else {
                Toast.makeText(this, "ফোনবুক থেকে নম্বর নিতে অনুমতি প্রয়োজন", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handlePickedContact(@Nullable Uri contactUri) {
        if (contactUri == null) return;
        try (Cursor cursor = getContentResolver().query(contactUri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                int nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                if (numberIdx >= 0) {
                    String number = cursor.getString(numberIdx);
                    if (number != null) etPhone.setText(number.replaceAll("\\s+", ""));
                }
                if (nameIdx >= 0 && (etName.getText() == null || etName.getText().toString().trim().isEmpty())) {
                    String name = cursor.getString(nameIdx);
                    if (name != null) etName.setText(name);
                }
            }
        } catch (Exception ignored) {
        }
    }

    // ═══════════════════════════════════════════
    //  সেইভ বাটন — নাম বা মোবাইল যেকোনো একটি থাকলেই সক্রিয়
    // ═══════════════════════════════════════════
    private void refreshSaveEnabled() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        boolean enabled = !name.isEmpty() || !phone.isEmpty();
        btnSaveKhataCustomer.setEnabled(enabled);
        btnSaveKhataCustomer.setAlpha(enabled ? 1f : 0.45f);
    }

    private void loadForEdit(String personId) {
        KhataCustomer p = db.getKhataCustomerById(personId);
        if (p == null) return;
        editingKhataCustomer = p;

        etName.setText(p.getName());
        etRelation.setText(p.getBusinessTag());
        etPhone.setText(p.getPhone());
        etAddress.setText(p.getAddress());
        etEmail.setText(p.getEmail());
        if (etOpeningBalance != null) {
            if (Math.abs(p.getOpeningBalance()) > 0.004) {
                etOpeningBalance.setText(String.valueOf(p.getOpeningBalance()));
            }
            // এডিট মোডে পূর্বের জের বদলানো যায় না — সেটা ইতিমধ্যে একটা এন্ট্রি হিসেবে সেভ হয়ে গেছে,
            // পরিবর্তন করতে হলে গ্রাহকের এন্ট্রি লিস্ট থেকে সরাসরি এডিট করতে হবে।
            etOpeningBalance.setEnabled(false);
            etOpeningBalance.setAlpha(0.6f);
        }
        if (!p.getDate().isEmpty()) { selectedDate = p.getDate(); }
        if (!p.getTime().isEmpty()) { selectedTime = p.getTime(); }
        if (p.hasPhoto() && new File(p.getPhotoPath()).exists()) applyPhoto(p.getPhotoPath());
        refreshSaveEnabled();
    }

    private void saveKhataCustomer() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        if (name.isEmpty() && phone.isEmpty()) {
            Toast.makeText(this, "নাম অথবা মোবাইল নম্বর দিন", Toast.LENGTH_SHORT).show();
            return;
        }

        KhataCustomer p = editingKhataCustomer != null ? editingKhataCustomer : new KhataCustomer();
        p.setName(name.isEmpty() ? phone : name);
        p.setBusinessTag(etRelation.getText() != null ? etRelation.getText().toString().trim() : "");
        p.setPhone(phone);
        p.setAddress(etAddress.getText() != null ? etAddress.getText().toString().trim() : "");
        p.setEmail(etEmail.getText() != null ? etEmail.getText().toString().trim() : "");
        if (etOpeningBalance != null) {
            String obStr = etOpeningBalance.getText() != null ? etOpeningBalance.getText().toString().trim() : "";
            double ob = 0;
            if (!obStr.isEmpty()) {
                try { ob = Double.parseDouble(obStr); } catch (Exception ignored) {}
            }
            // এডিট মোডে পূর্বের জের বদলানো যায় না (সেটা প্রথম দিনই একটা এন্ট্রি হিসেবে তৈরি
            // হয়ে গেছে) — শুধু নতুন গ্রাহক তৈরির সময়ই এই মান ব্যবহার হয়।
            if (editingKhataCustomer == null) p.setOpeningBalance(ob);
        }
        p.setDate(selectedDate);
        p.setTime(selectedTime);
        if (!pendingPhotoPath.isEmpty()) p.setPhotoPath(pendingPhotoPath);

        if (editingKhataCustomer != null) {
            int index = db.getKhataCustomerIndexById(editingKhataCustomer.getId());
            db.updateKhataCustomer(index, p);
        } else {
            db.addKhataCustomer(p);
        }

        Toast.makeText(this, "সেইভ হয়েছে", Toast.LENGTH_SHORT).show();
        finish();
    }
}
