package com.jrappspot.cashlipi.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.jrappspot.cashlipi.R;

/**
 * সম্পূর্ণ খালি Fragment — দেনা-পাওনা, বাকির খাতা ও বাজেট পেজের জন্য পুনর্ব্যবহারযোগ্য।
 * স্পেসিফিকেশন অনুযায়ী এই ধাপে এসব পেজে কোনো UI এলিমেন্ট, ডেটা, বা লজিক প্রয়োজন নেই —
 * শুধু নেভিগেশন সিস্টেমে সঠিকভাবে যুক্ত একটা ফাঁকা স্ক্রিন হলেই যথেষ্ট।
 */
public class EmptyFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_empty, container, false);
    }
}
