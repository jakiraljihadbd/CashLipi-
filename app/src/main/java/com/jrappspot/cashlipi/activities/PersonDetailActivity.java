package com.jrappspot.cashlipi.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
 * একজন ব্যক্তির বিস্তারিত পেজ — পরিচিতি দেখায় ও এডিট/ডিলিট/কল করার সুযোগ দেয়।
 * এই ধাপে শুধু পরিচিতি অংশ প্রস্তুত — এই ব্যক্তির person.id ধরে দিলাম/পেলাম লেনদেনের
 * তালিকা ও যোগ-বাটন এখানেই ভবিষ্যতে যুক্ত হবে (নিচের placeholder জায়গায়)।
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
    private TextView tvInitial, tvName, tvRelationBadge, tvPhone, tvAddress;

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
        tvAddress = findViewById(R.id.tvDetailAddress);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCall).setOnClickListener(v -> callPerson());
        findViewById(R.id.btnEditPerson).setOnClickListener(v -> {
            Intent i = new Intent(this, AddPersonActivity.class);
            i.putExtra(AddPersonActivity.EXTRA_EDIT_PERSON_ID, personId);
            startActivity(i);
        });
        findViewById(R.id.btnDeletePerson).setOnClickListener(v -> confirmDelete());
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

        if (person.hasAddress()) {
            tvAddress.setVisibility(View.VISIBLE);
            tvAddress.setText(person.getAddress());
        } else {
            tvAddress.setVisibility(View.GONE);
        }

        findViewById(R.id.btnCall).setVisibility(person.hasPhone() ? View.VISIBLE : View.GONE);

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
