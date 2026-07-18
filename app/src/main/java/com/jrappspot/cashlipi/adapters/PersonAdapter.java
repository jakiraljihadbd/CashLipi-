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

import java.io.File;
import java.util.List;

/**
 * দেনা-পাওনা পেজের ব্যক্তি তালিকার অ্যাডাপ্টার — বড়, বোল্ড UI।
 * ছবি থাকলে গোলাকার ছবি, না থাকলে নামের প্রথম অক্ষরসহ রঙিন অ্যাভাটার।
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

    public PersonAdapter(Context ctx, List<Person> items, OnPersonClick listener) {
        this.ctx = ctx;
        this.items = items;
        this.listener = listener;
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
            h.tvPhone.setVisibility(View.VISIBLE);
            h.tvPhone.setText(p.getPhone());
        } else if (p.hasAddress()) {
            h.tvPhone.setVisibility(View.VISIBLE);
            h.tvPhone.setText(p.getAddress());
        } else {
            h.tvPhone.setVisibility(View.GONE);
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

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(p, h.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        LinearLayout avatarInitial;
        TextView tvInitial, tvName, tvDot, tvRelation, tvPhone;

        VH(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivPersonPhoto);
            avatarInitial = itemView.findViewById(R.id.avatarInitial);
            tvInitial = itemView.findViewById(R.id.tvInitial);
            tvName = itemView.findViewById(R.id.tvPersonName);
            tvDot = itemView.findViewById(R.id.tvRelationDot);
            tvRelation = itemView.findViewById(R.id.tvPersonRelation);
            tvPhone = itemView.findViewById(R.id.tvPersonPhone);
        }
    }
}
