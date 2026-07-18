package com.jrappspot.cashlipi.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.Person;
import com.jrappspot.cashlipi.utils.DatabaseManager;

import java.io.File;

/**
 * একজন ব্যক্তির বিস্তারিত পেজ — কমপ্যাক্ট হেডারে পরিচিতি দেখায়, ডান পাশে হোয়াটসঅ্যাপ/কল/মেইল
 * বাটন এবং ⋮ মেনুতে এডিট/মুছার অপশন থাকে। এই ধাপে শুধু পরিচিতি অংশ প্রস্তুত — এই ব্যক্তির
 * person.id ধরে দিলাম/পেলাম লেনদেনের তালিকা ও যোগ-বাটন ভবিষ্যতে নিচের placeholder জায়গায় যুক্ত হবে।
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

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnCall.setOnClickListener(v -> callPerson());
        btnWhatsapp.setOnClickListener(v -> openWhatsapp());
        btnMail.setOnClickListener(v -> sendMail());
        findViewById(R.id.btnMorePerson).setOnClickListener(this::showMoreMenu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPerson();
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
