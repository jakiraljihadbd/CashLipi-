package com.jrappspot.cashlipi.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.activities.AddPersonActivity;
import com.jrappspot.cashlipi.activities.PersonDetailActivity;
import com.jrappspot.cashlipi.adapters.PersonAdapter;
import com.jrappspot.cashlipi.models.Person;
import com.jrappspot.cashlipi.utils.DatabaseManager;

import java.util.ArrayList;
import java.util.List;

/**
 * দেনা-পাওনা পেজ — এখানে ব্যবহারকারী তার সাথে হিসাব রাখা প্রতিটি ব্যক্তি/প্রতিষ্ঠান যোগ করে।
 * এই ধাপে শুধু পরিচিতি (নাম/ছবি/সম্পর্ক/ফোন/ঠিকানা) তৈরি হয় — লেনদেনের হিসাব পরের ধাপে
 * PersonDetailActivity-তে person.id ধরে যুক্ত হবে।
 */
public class DenaPawnaFragment extends Fragment {

    private DatabaseManager db;
    private RecyclerView rvList;
    private LinearLayout emptyState;
    private TextView tvPersonCount;
    private final List<Person> allPersons = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dena_pawna, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);
        db = DatabaseManager.getInstance(requireContext());

        rvList = root.findViewById(R.id.rvPersonList);
        emptyState = root.findViewById(R.id.emptyState);
        tvPersonCount = root.findViewById(R.id.tvPersonCount);
        rvList.setLayoutManager(new LinearLayoutManager(requireContext()));

        root.findViewById(R.id.btnAddPerson).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddPersonActivity.class)));

        loadData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData(); // নতুন ব্যক্তি যোগ/এডিট/মুছার পর তালিকা রিফ্রেশ
    }

    private void loadData() {
        allPersons.clear();
        allPersons.addAll(db.getPersonList());

        if (allPersons.isEmpty()) {
            rvList.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            tvPersonCount.setText("যাদের সাথে হিসাব রাখবেন, তাদের যোগ করুন");
        } else {
            emptyState.setVisibility(View.GONE);
            rvList.setVisibility(View.VISIBLE);
            tvPersonCount.setText(allPersons.size() + " জন যুক্ত আছে");

            PersonAdapter adapter = new PersonAdapter(requireContext(), allPersons, (person, position) -> {
                Intent i = new Intent(requireContext(), PersonDetailActivity.class);
                i.putExtra(PersonDetailActivity.EXTRA_PERSON_ID, person.getId());
                startActivity(i);
            });
            rvList.setAdapter(adapter);
            rvList.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_fall_down));
            rvList.scheduleLayoutAnimation();
        }
    }
}
