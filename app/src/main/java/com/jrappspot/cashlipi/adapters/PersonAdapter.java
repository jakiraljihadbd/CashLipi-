package com.jrappspot.cashlipi.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.Person;
import com.jrappspot.cashlipi.models.PersonStat;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * দেনা-পাওনা পেজের ব্যক্তি তালিকার অ্যাডাপ্টার — ছবি/নাম/সম্পর্ক/ফোন/ঠিকানার পাশাপাশি
 * প্রতিটা ব্যক্তির মোট লেনদেন ও অপরিশোধিত এন্ট্রির সংখ্যা ব্যাজ আকারে দেখায় (statsMap থেকে, ঐচ্ছিক)।
 */
public class PersonAdapter extends RecyclerView.Adapter<PersonAdapter.VH> {

    public interface OnPersonClick {
        void onClick(Person person, int position);
    }

    private static final int[] AVATAR_BGS = {
            R.drawable.bg_avatar_circle_1, R.drawable.bg_avatar_circle_2,
            R.drawable.bg_avatar_circle_3, R.drawable.bg_avatar_circle_4,
            R.drawable.bg_avatar_circle_5
    };

    private final Context ctx;
    private final List<Person> items;
    private final OnPersonClick listener;
    private final Map<String, PersonStat> statsMap; // key: person.getName().trim().toLowerCase() — nullable

    public PersonAdapter(Context ctx, List<Person> items, OnPersonClick listener) {
        this(ctx, items, listener, null);
    }

    public PersonAdapter(Context ctx, List<Person> items, OnPersonClick listener, Map<String, PersonStat> statsMap) {
        this.ctx = ctx;
        this.items = items;
        this.listener = listener;
        this.statsMap = statsMap;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_person, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Person p = items.get(position);

        h.tvName.setText(p.getName().isEmpty() ? "নাম নেই" : p.getName());

        if (p.hasRelation()) {
            h.tvDot.setVisibility(View.VISIBLE);
            h.tvRelation.setVisibility(View.VISIBLE);
            h.tvRelation.setText(p.getRelation());
        } else {
            h.tvDot.setVisibility(View.GONE);
            h.tvRelation.setVisibility(View.GONE);
        }

        if (p.hasPhone()) {
            h.rowPhone.setVisibility(View.VISIBLE);
            h.tvPhone.setText(p.getPhone());
        } else {
            h.rowPhone.setVisibility(View.GONE);
        }

        if (p.hasAddress()) {
            h.rowAddress.setVisibility(View.VISIBLE);
            h.tvAddress.setText(p.getAddress());
        } else {
            h.rowAddress.setVisibility(View.GONE);
        }

        if (p.hasPhoto() && new File(p.getPhotoPath()).exists()) {
            h.avatarInitial.setVisibility(View.GONE);
            h.ivPhoto.setVisibility(View.VISIBLE);
            Glide.with(ctx).load(new File(p.getPhotoPath())).transform(new CircleCrop()).into(h.ivPhoto);
        } else {
            h.ivPhoto.setVisibility(View.GONE);
            h.avatarInitial.setVisibility(View.VISIBLE);
            h.tvInitial.setText(p.getInitial());
            int colorIdx = Math.abs(p.getName().hashCode()) % AVATAR_BGS.length;
            h.avatarInitial.setBackgroundResource(AVATAR_BGS[colorIdx]);
        }

        PersonStat stat = statsMap != null ? statsMap.get(p.getName().trim().toLowerCase()) : null;
        if (stat != null && stat.hasAnyTxn()) {
            h.rowStats.setVisibility(View.VISIBLE);
            h.tvTxnCount.setText(stat.totalCount + " টি লেনদেন");
            if (stat.hasUnpaid()) {
                h.tvUnpaidBadge.setVisibility(View.VISIBLE);
                h.tvUnpaidBadge.setBackgroundResource(R.drawable.bg_badge_unpaid);
                h.tvUnpaidBadge.setTextColor(ctx.getResources().getColor(R.color.denaActiveGradEnd));
                h.tvUnpaidBadge.setText(stat.unpaidCount + " টি অপরিশোধিত");
            } else {
                h.tvUnpaidBadge.setVisibility(View.VISIBLE);
                h.tvUnpaidBadge.setBackgroundResource(R.drawable.bg_badge_paid);
                h.tvUnpaidBadge.setTextColor(ctx.getResources().getColor(R.color.successColor));
                h.tvUnpaidBadge.setText("সব পরিশোধিত");
            }
        } else {
            h.rowStats.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(p, h.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        LinearLayout avatarInitial, rowPhone, rowAddress, rowStats;
        TextView tvInitial, tvName, tvDot, tvRelation, tvPhone, tvAddress, tvTxnCount, tvUnpaidBadge;

        VH(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivPersonPhoto);
            avatarInitial = itemView.findViewById(R.id.avatarInitial);
            tvInitial = itemView.findViewById(R.id.tvInitial);
            tvName = itemView.findViewById(R.id.tvPersonName);
            tvDot = itemView.findViewById(R.id.tvRelationDot);
            tvRelation = itemView.findViewById(R.id.tvPersonRelation);
            rowPhone = itemView.findViewById(R.id.rowPhone);
            tvPhone = itemView.findViewById(R.id.tvPersonPhone);
            rowAddress = itemView.findViewById(R.id.rowAddress);
            tvAddress = itemView.findViewById(R.id.tvPersonAddress);
            rowStats = itemView.findViewById(R.id.rowStats);
            tvTxnCount = itemView.findViewById(R.id.tvTxnCount);
            tvUnpaidBadge = itemView.findViewById(R.id.tvUnpaidBadge);
        }
    }
}
