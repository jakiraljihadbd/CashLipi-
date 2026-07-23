package com.jrappspot.cashlipi.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.jrappspot.cashlipi.fragments.DenaPawnaFragment;
import com.jrappspot.cashlipi.fragments.BakirKhataFragment;
import com.jrappspot.cashlipi.fragments.EmptyFragment;
import com.jrappspot.cashlipi.fragments.HomeFragment;
import com.jrappspot.cashlipi.fragments.IncomeExpenseFragment;
import com.jrappspot.cashlipi.fragments.SavingsFragment;
import com.jrappspot.cashlipi.fragments.SettingsFragment;

/**
 * ড্যাশবোর্ডের ৭টি পেজের জন্য FragmentStateAdapter।
 * প্রতিটা পেজ একটা স্বাধীন Fragment — একটার ক্র্যাশ বাকিগুলোকে প্রভাবিত করবে না।
 */
public class MainPagerAdapter extends FragmentStateAdapter {

    public static final int POSITION_HOME = 0;
    public static final int POSITION_INCOME_EXPENSE = 1;
    public static final int POSITION_DENA_PAWNA = 2;
    public static final int POSITION_SAVINGS = 3;
    public static final int POSITION_BAKIR_KHATA = 4;
    public static final int POSITION_BUDGET = 5;
    public static final int POSITION_SETTINGS = 6;
    public static final int PAGE_COUNT = 7;

    public MainPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case POSITION_INCOME_EXPENSE:
                return new IncomeExpenseFragment();
            case POSITION_DENA_PAWNA:
                return new DenaPawnaFragment();
            case POSITION_SAVINGS:
                return new SavingsFragment();
            case POSITION_BAKIR_KHATA:
                return new BakirKhataFragment();
            case POSITION_BUDGET:
                return new EmptyFragment();
            case POSITION_SETTINGS:
                return new SettingsFragment();
            case POSITION_HOME:
            default:
                return new HomeFragment();
        }
    }

    @Override
    public int getItemCount() {
        return PAGE_COUNT;
    }
}
