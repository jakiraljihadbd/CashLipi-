package com.jrappspot.cashlipi.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.KhataCustomer;
import com.jrappspot.cashlipi.models.KhataCustomerStat;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * দেনা-পাওনা পেজের ব্যক্তি তালিকার অ্যাডাপ্টার — ছবি/নাম/সম্পর্ক/ফোন/ঠিকানার পাশাপাশি
 * প্রতিটা ব্যক্তির মোট লেনদেন ও অপরিশোধিত এন্ট্রির সংখ্যা ব্যাজ আকারে দেখায় (statsMap থেকে, ঐচ্ছিক)।
 *
 * cardStyle (0-4) কার্ডের ভিজুয়াল থিম বদলায় — "থিম চেঞ্জ" আইকন থেকে বাছাই করা হয়।
 * অ্যাভাটার সবসময় গোল না হয়ে বড়, রাউন্ডেড-স্কয়ার আকারে দেখানো হয়।
 */
public class KhataCustomerAdapter extends RecyclerView.Adapter<KhataCustomerAdapter.VH> {

    public interface OnKhataCustomerClick {
        void onClick(KhataCustomer customerName, int position);
    }

    // কার্ড স্টাইল আইডি — থিম-চেঞ্জ ডায়ালগের ইনডেক্সের সাথে মিলে
    public static final int STYLE_CLASSIC = 0;
    public static final int STYLE_MINIMAL = 1;
    public static final int STYLE_GRADIENT = 2;
    public static final int STYLE_BOLD = 3;
    public static final int STYLE_COMPACT = 4;

    private static final int[] AVATAR_BGS = {
            R.drawable.bg_avatar_square_1, R.drawable.bg_avatar_square_2,
            R.drawable.bg_avatar_square_3, R.drawable.bg_avatar_square_4,
            R.drawable.bg_avatar_square_5
    };

    private static final int[] CARD_BGS = {
            R.drawable.bg_card_style_classic, R.drawable.bg_card_style_minimal,
            R.drawable.bg_card_style_gradient, R.drawable.bg_card_style_bold,
            R.drawable.bg_card_style_compact
    };

    private final Context ctx;
    private final List<KhataCustomer> items;
    private final OnKhataCustomerClick listener;
    private final Map<String, KhataCustomerStat> statsMap; // key: customerName.getName().trim().toLowerCase() — nullable
    private final int cardStyle;

    public KhataCustomerAdapter(Context ctx, List<KhataCustomer> items, OnKhataCustomerClick listener) {
        this(ctx, items, listener, null, STYLE_CLASSIC);
    }

    public KhataCustomerAdapter(Context ctx, List<KhataCustomer> items, OnKhataCustomerClick listener, Map<String, KhataCustomerStat> statsMap) {
        this(ctx, items, listener, statsMap, STYLE_CLASSIC);
    }

    public KhataCustomerAdapter(Context ctx, List<KhataCustomer> items, OnKhataCustomerClick listener, Map<String, KhataCustomerStat> statsMap, int cardStyle) {
        this.ctx = ctx;
        this.items = items;
        this.listener = listener;
        this.statsMap = statsMap;
        this.cardStyle = (cardStyle >= 0 && cardStyle < CARD_BGS.length) ? cardStyle : STYLE_CLASSIC;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_khata_customer, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        KhataCustomer p = items.get(position);

        applyCardStyle(h);

        h.tvName.setText(p.getName().isEmpty() ? "নাম নেই" : p.getName());

        if (p.hasBusinessTag()) {
            h.tvDot.setVisibility(View.VISIBLE);
            h.tvRelation.setVisibility(View.VISIBLE);
            h.tvRelation.setText(p.getBusinessTag());
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
            int radiusPx = (int) (18 * ctx.getResources().getDisplayMetrics().density);
            Glide.with(ctx).load(new File(p.getPhotoPath())).transform(new RoundedCorners(radiusPx)).into(h.ivPhoto);
        } else {
            h.ivPhoto.setVisibility(View.GONE);
            h.avatarInitial.setVisibility(View.VISIBLE);
            h.tvInitial.setText(p.getInitial());
            int colorIdx = Math.abs(p.getName().hashCode()) % AVATAR_BGS.length;
            h.avatarInitial.setBackgroundResource(AVATAR_BGS[colorIdx]);
        }

        KhataCustomerStat stat = statsMap != null ? statsMap.get(p.getId()) : null;
        if (stat != null && stat.hasAnyTxn()) {
            h.rowStats.setVisibility(View.VISIBLE);
            h.tvTxnCount.setText(stat.totalCount + " টি লেনদেন");
            h.tvUnpaidBadge.setVisibility(View.VISIBLE);
            if (stat.isDue()) {
                // গ্রাহক বাকি আছেন — লাল ব্যাজ, দ্রুত চোখে পড়ে
                h.tvUnpaidBadge.setBackgroundResource(R.drawable.bg_badge_unpaid);
                h.tvUnpaidBadge.setTextColor(ctx.getResources().getColor(R.color.bakiColor));
                h.tvUnpaidBadge.setText("বাকি ৳" + formatAmount(stat.getNetAmount()));
                h.statusDot.setVisibility(View.VISIBLE);
                h.statusDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        ctx.getResources().getColor(R.color.bakiColor)));
            } else if (stat.isAdvance()) {
                h.tvUnpaidBadge.setBackgroundResource(R.drawable.bg_badge_paid);
                h.tvUnpaidBadge.setTextColor(ctx.getResources().getColor(R.color.jomaColor));
                h.tvUnpaidBadge.setText("অগ্রিম ৳" + formatAmount(stat.getNetAmount()));
                h.statusDot.setVisibility(View.VISIBLE);
                h.statusDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        ctx.getResources().getColor(R.color.jomaColor)));
            } else {
                h.tvUnpaidBadge.setBackgroundResource(R.drawable.bg_badge_paid);
                h.tvUnpaidBadge.setTextColor(ctx.getResources().getColor(R.color.successColor));
                h.tvUnpaidBadge.setText("হিসাব ক্লিয়ার");
                h.statusDot.setVisibility(View.GONE);
            }
        } else {
            h.rowStats.setVisibility(View.GONE);
            h.statusDot.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(p, h.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    private String formatAmount(double v) {
        if (v == Math.floor(v)) return String.format(java.util.Locale.US, "%,.0f", v);
        return String.format(java.util.Locale.US, "%,.2f", v);
    }

    // ── থিম-চেঞ্জ: কার্ডের ব্যাকগ্রাউন্ড/এলিভেশন/প্যাডিং/অ্যাভাটার সাইজ বাছাই করা স্টাইল অনুযায়ী বসায় ──
    private void applyCardStyle(VH h) {
        h.itemView.setBackgroundResource(CARD_BGS[cardStyle]);

        float density = ctx.getResources().getDisplayMetrics().density;
        int padDefault = (int) (16 * density);
        int padCompact = (int) (12 * density);
        int avatarDefault = (int) (66 * density);
        int avatarCompact = (int) (52 * density);

        switch (cardStyle) {
            case STYLE_MINIMAL:
                h.itemView.setElevation(0f);
                h.itemView.setPadding(padDefault, padDefault, padDefault, padDefault);
                setAvatarSize(h, avatarDefault);
                break;
            case STYLE_COMPACT:
                h.itemView.setElevation((int) (3 * density));
                h.itemView.setPadding(padCompact, padCompact, padCompact, padCompact);
                setAvatarSize(h, avatarCompact);
                break;
            case STYLE_GRADIENT:
            case STYLE_BOLD:
            case STYLE_CLASSIC:
            default:
                h.itemView.setElevation((int) (6 * density));
                h.itemView.setPadding(padDefault, padDefault, padDefault, padDefault);
                setAvatarSize(h, avatarDefault);
                break;
        }
    }

    private void setAvatarSize(VH h, int sizePx) {
        ViewGroup.LayoutParams frameLp = h.avatarFrame.getLayoutParams();
        frameLp.width = sizePx;
        frameLp.height = sizePx;
        h.avatarFrame.setLayoutParams(frameLp);

        ViewGroup.LayoutParams photoLp = h.ivPhoto.getLayoutParams();
        photoLp.width = sizePx;
        photoLp.height = sizePx;
        h.ivPhoto.setLayoutParams(photoLp);

        ViewGroup.LayoutParams initialLp = h.avatarInitial.getLayoutParams();
        initialLp.width = sizePx;
        initialLp.height = sizePx;
        h.avatarInitial.setLayoutParams(initialLp);
    }

    static class VH extends RecyclerView.ViewHolder {
        FrameLayout avatarFrame;
        ImageView ivPhoto;
        LinearLayout avatarInitial, rowPhone, rowAddress, rowStats;
        TextView tvInitial, tvName, tvDot, tvRelation, tvPhone, tvAddress, tvTxnCount, tvUnpaidBadge;
        View statusDot;

        VH(@NonNull View itemView) {
            super(itemView);
            avatarFrame = itemView.findViewById(R.id.avatarFrame);
            ivPhoto = itemView.findViewById(R.id.ivKhataCustomerPhoto);
            avatarInitial = itemView.findViewById(R.id.avatarInitial);
            tvInitial = itemView.findViewById(R.id.tvInitial);
            tvName = itemView.findViewById(R.id.tvKhataCustomerName);
            tvDot = itemView.findViewById(R.id.tvRelationDot);
            tvRelation = itemView.findViewById(R.id.tvKhataCustomerRelation);
            rowPhone = itemView.findViewById(R.id.rowPhone);
            tvPhone = itemView.findViewById(R.id.tvKhataCustomerPhone);
            rowAddress = itemView.findViewById(R.id.rowAddress);
            tvAddress = itemView.findViewById(R.id.tvKhataCustomerAddress);
            rowStats = itemView.findViewById(R.id.rowStats);
            tvTxnCount = itemView.findViewById(R.id.tvTxnCount);
            tvUnpaidBadge = itemView.findViewById(R.id.tvUnpaidBadge);
            statusDot = itemView.findViewById(R.id.ivKhataCustomerStatusDot);
        }
    }
}
