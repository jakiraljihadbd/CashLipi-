package com.jrappspot.cashlipi.adapters;

import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.ChatMessage;

import java.util.List;

/**
 * ক্যাশলিপি AI চ্যাট অ্যাডাপ্টার — ইউজার বাবল, বট মেসেজ (কপি/রিজেনারেট
 * অ্যাকশনসহ), আর "টাইপিং..." ইন্ডিকেটর — তিনটা ভিউ টাইপ হ্যান্ডেল করে।
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 0;
    private static final int TYPE_BOT = 1;
    private static final int TYPE_TYPING = 2;

    public interface ActionListener {
        void onCopy(String text);
        void onRegenerate();
    }

    private final List<ChatMessage> messages;
    private final ActionListener listener;

    public ChatAdapter(List<ChatMessage> messages, ActionListener listener) {
        this.messages = messages;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage m = messages.get(position);
        if (m.getRole() == ChatMessage.Role.USER) return TYPE_USER;
        if (m.getRole() == ChatMessage.Role.TYPING) return TYPE_TYPING;
        return TYPE_BOT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_USER) {
            return new UserVH(inflater.inflate(R.layout.item_chat_user, parent, false));
        } else if (viewType == TYPE_TYPING) {
            return new TypingVH(inflater.inflate(R.layout.item_chat_typing, parent, false));
        } else {
            return new BotVH(inflater.inflate(R.layout.item_chat_bot, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage m = messages.get(position);
        if (holder instanceof UserVH) {
            ((UserVH) holder).tvMessage.setText(m.getText());
        } else if (holder instanceof BotVH) {
            BotVH h = (BotVH) holder;
            h.tvMessage.setText(m.getText());
            boolean isLast = position == messages.size() - 1;
            h.btnRegenerate.setVisibility(isLast ? View.VISIBLE : View.GONE);
            h.btnCopy.setOnClickListener(v -> {
                if (listener != null) listener.onCopy(m.getText());
            });
            h.btnRegenerate.setOnClickListener(v -> {
                if (listener != null) listener.onRegenerate();
            });
        } else if (holder instanceof TypingVH) {
            ((TypingVH) holder).startAnim();
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof TypingVH) {
            ((TypingVH) holder).stopAnim();
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class UserVH extends RecyclerView.ViewHolder {
        TextView tvMessage;
        UserVH(View v) { super(v); tvMessage = v.findViewById(R.id.tvMessage); }
    }

    static class BotVH extends RecyclerView.ViewHolder {
        TextView tvMessage;
        View btnCopy, btnRegenerate;
        BotVH(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.tvMessage);
            btnCopy = v.findViewById(R.id.btnCopy);
            btnRegenerate = v.findViewById(R.id.btnRegenerate);
        }
    }

    static class TypingVH extends RecyclerView.ViewHolder {
        View dot1, dot2, dot3;
        AnimatorSet animSet;
        TypingVH(View v) {
            super(v);
            dot1 = v.findViewById(R.id.dot1);
            dot2 = v.findViewById(R.id.dot2);
            dot3 = v.findViewById(R.id.dot3);
        }
        void startAnim() {
            stopAnim();
            ObjectAnimator a1 = bounce(dot1, 0);
            ObjectAnimator a2 = bounce(dot2, 150);
            ObjectAnimator a3 = bounce(dot3, 300);
            animSet = new AnimatorSet();
            animSet.playTogether(a1, a2, a3);
            animSet.start();
        }
        private ObjectAnimator bounce(View dot, long delay) {
            ObjectAnimator anim = ObjectAnimator.ofFloat(dot, "alpha", 0.3f, 1f, 0.3f);
            anim.setDuration(900);
            anim.setStartDelay(delay);
            anim.setRepeatCount(ObjectAnimator.INFINITE);
            return anim;
        }
        void stopAnim() {
            if (animSet != null) { animSet.cancel(); animSet = null; }
        }
    }
}
