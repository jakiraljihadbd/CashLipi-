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
import com.jrappspot.cashlipi.models.Person;
import com.jrappspot.cashlipi.models.PersonStat;

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
public class PersonAdapter extends RecyclerView.Adapter<PersonAdapter.VH> {

    public interface OnPersonClick {
        void onClick(Person person, int position);
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
    private final List<Person> items;
    private final OnPersonClick listener;
    private final Map<String, PersonStat> statsMap; // key: person.getName().trim().toLowerCase() — nullable
    private final int cardStyle;

    public PersonAdapter(Context ctx, List<Person> items, OnPersonClick listener) {
        this(ctx, items, listener, null, STYLE_CLASSIC);
    }

    public PersonAdapter(Context ctx, List<Person> items, OnPersonClick listener, Map<String, PersonStat> statsMap) {
        this(ctx, items, listener, statsMap, STYLE_CLASSIC);
    }

    public PersonAdapter(Context ctx, List<Person> items, OnPersonClick listener, Map<String, PersonStat> statsMap, int cardStyle) {
        this.ctx = ctx;
        this.items = items;
        this.listener = listener;
        this.statsMap = statsMap;
        this.cardStyle = (cardStyle >= 0 && cardStyle < CARD_BGS.length) ? cardStyle : STYLE_CLASSIC;
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

        applyCardStyle(h);

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
            int radiusPx = (int) (18 * ctx.getResources().getDisplayMetrics().density);
            Glide.with(ctx).load(new File(p.getPhotoPath())).transform(new RoundedCorners(radiusPx)).into(h.ivPhoto);
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
                // নেট বকেয়া দেনা না পাওনা তার উপর ভিত্তি করে ব্যাজের রং বদলায় — আগে সবসময়
                // দেনার (অ্যাম্বার) রং দেখাতো, এখন পাওনা বেশি হলে নীল রং দেখাবে।
                boolean netDena = stat.isNetDena();
                h.tvUnpaidBadge.setBackgroundResource(netDena ? R.drawable.bg_badge_unpaid : R.drawable.bg_badge_unpaid_pabona);
                h.tvUnpaidBadge.setTextColor(ctx.getResources().getColor(
                        netDena ? R.color.denaActiveGradEnd : R.color.pabonaActiveGradEnd));
                h.tvUnpaidBadge.setText(stat.unpaidCount + " টি অপরিশোধিত");

                // অ্যাভাটারের কোণায় ছোট রঙিন ডট — এক নজরে বোঝা যায় দেনা না পাওনা, লিস্ট
                // স্ক্যান করা সহজ হয়।
                h.statusDot.setVisibility(View.VISIBLE);
                h.statusDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        ctx.getResources().getColor(netDena ? R.color.denaColor : R.color.pabonaColor)));
            } else {
                h.tvUnpaidBadge.setVisibility(View.VISIBLE);
                h.tvUnpaidBadge.setBackgroundResource(R.drawable.bg_badge_paid);
                h.tvUnpaidBadge.setTextColor(ctx.getResources().getColor(R.color.successColor));
                h.tvUnpaidBadge.setText("সব পরিশোধিত");
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
            statusDot = itemView.findViewById(R.id.ivPersonStatusDot);
        }
    }
}
