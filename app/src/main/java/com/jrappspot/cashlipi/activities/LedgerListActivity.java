package com.jrappspot.cashlipi.activities;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.*;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.utils.FontUtils;
import com.jrappspot.cashlipi.adapters.LedgerListAdapter;
import com.jrappspot.cashlipi.models.LedgerEntry;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import com.jrappspot.cashlipi.utils.DateFilterUtil;
import com.jrappspot.cashlipi.utils.TransactionSheetHelper;
import java.util.ArrayList;
import java.util.List;

public class LedgerListActivity extends BaseActivity {
    private DatabaseManager db;
    private RecyclerView rv;
    private LinearLayout emptyState;
    private EditText etSearch;
    private TextView tvTotal, tvCount, tvDenaTotal, tvPabonaTotal;
    private List<LedgerEntry> allList=new ArrayList<>(), filteredList=new ArrayList<>();
    private String typeFilter="all";   // all/dena/pabona
    private String paidFilter="all";   // all/paid/unpaid
    private String dateFilter="all";   // all/today/week/month/year

    @Override protected void onCreate(Bundle s){
        super.onCreate(s);setContentView(R.layout.activity_ledger_list);
        FontUtils.applyToView(this, findViewById(android.R.id.content));
        db=DatabaseManager.getInstance(this);
        rv=findViewById(R.id.rvList);emptyState=findViewById(R.id.emptyState);
        etSearch=findViewById(R.id.etSearch);tvTotal=findViewById(R.id.tvSummaryTotal);
        tvCount=findViewById(R.id.tvSummaryCount);
        tvDenaTotal=findViewById(R.id.tvDenaTotal);tvPabonaTotal=findViewById(R.id.tvPabonaTotal);
        rv.setLayoutManager(new LinearLayoutManager(this));
        setupDateChips();setupTypeChips();setupPaidChips();
        ImageView ivClearL = findViewById(R.id.ivClearSearch);
        etSearch.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}
            public void onTextChanged(CharSequence s,int a,int b,int c){if(ivClearL!=null)ivClearL.setVisibility(s.length()>0?View.VISIBLE:View.GONE);applyFilter();}public void afterTextChanged(Editable s){}});
        if(ivClearL!=null)ivClearL.setOnClickListener(v->{etSearch.setText("");etSearch.requestFocus();});
        findViewById(R.id.btnAddNew).setOnClickListener(v->
            startActivity(new android.content.Intent(this, AddLedgerActivity.class)));
        loadData();
    }
    @Override protected void onResume(){super.onResume();loadData();}
    private void loadData(){allList=new ArrayList<>(db.getLedgerList());
        if(tvDenaTotal!=null)tvDenaTotal.setText(DatabaseManager.formatAmount(db.getTotalDena()));
        if(tvPabonaTotal!=null)tvPabonaTotal.setText(DatabaseManager.formatAmount(db.getTotalPabona()));
        applyFilter();}

    private void setupDateChips(){
        String[] labels={"সব","আজ","সপ্তাহ","মাস","বছর"};
        String[] keys={"all","today","week","month","year"};
        LinearLayout chipRow=findViewById(R.id.chipRowDate);
        if(chipRow==null)return;chipRow.removeAllViews();
        for(int i=0;i<labels.length;i++){final String key=keys[i];TextView chip=new TextView(this);
            chip.setText(labels[i]);chip.setTextSize(12.5f);
            chip.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setPadding(36,18,36,18);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);lp.setMarginEnd(8);chip.setLayoutParams(lp);
            chip.setClickable(true);chip.setFocusable(true);
            boolean selected=key.equals(dateFilter);
            chip.setBackground(getResources().getDrawable(selected?R.drawable.bg_chip_selected:R.drawable.bg_chip_unselected));
            chip.setTextColor(selected ? androidx.core.content.ContextCompat.getColor(this, R.color.white) : androidx.core.content.ContextCompat.getColor(this, R.color.chipUnselectedText));
            if(selected) chip.startAnimation(AnimationUtils.loadAnimation(this, R.anim.chip_scale));
            chip.setOnClickListener(v->{dateFilter=key;setupDateChips();applyFilter();});chipRow.addView(chip);}
    }

    private void setupTypeChips(){
        int[] ids={R.id.chipAll,R.id.chipDena,R.id.chipPabona};
        String[] keys={"all","dena","pabona"};
        for(int i=0;i<ids.length;i++){
            final String k=keys[i];TextView chip=findViewById(ids[i]);
            if(chip==null)continue;
            boolean selected=k.equals(typeFilter);
            chip.setBackground(getResources().getDrawable(selected?R.drawable.bg_chip_selected:R.drawable.bg_chip_unselected));
            chip.setTextColor(selected ? androidx.core.content.ContextCompat.getColor(this, R.color.white) : androidx.core.content.ContextCompat.getColor(this, R.color.chipUnselectedText));
            if(selected) chip.startAnimation(AnimationUtils.loadAnimation(this, R.anim.chip_scale));
            chip.setOnClickListener(v->{typeFilter=k;setupTypeChips();applyFilter();});
        }
    }
    private void setupPaidChips(){
        int[] ids={R.id.chipUnpaid,R.id.chipPaid};
        String[] keys={"unpaid","paid"};
        for(int i=0;i<ids.length;i++){
            final String k=keys[i];TextView chip=findViewById(ids[i]);
            if(chip==null)continue;
            boolean selected=k.equals(paidFilter);
            chip.setBackground(getResources().getDrawable(selected?R.drawable.bg_chip_selected:R.drawable.bg_chip_unselected));
            chip.setTextColor(selected ? androidx.core.content.ContextCompat.getColor(this, R.color.white) : androidx.core.content.ContextCompat.getColor(this, R.color.chipUnselectedText));
            if(selected) chip.startAnimation(AnimationUtils.loadAnimation(this, R.anim.chip_scale));
            chip.setOnClickListener(v->{paidFilter=k;setupPaidChips();applyFilter();});
        }
    }

    private void applyFilter(){
        String q=etSearch.getText()!=null?etSearch.getText().toString().toLowerCase().trim():"";
        filteredList=new ArrayList<>();
        for(LedgerEntry e:allList){
            if(!q.isEmpty()&&!e.getPerson().toLowerCase().contains(q)&&!e.getNote().toLowerCase().contains(q))continue;
            if("dena".equals(typeFilter)&&!e.isDena())continue;
            if("pabona".equals(typeFilter)&&!e.isPabona())continue;
            if("paid".equals(paidFilter)&&!e.isPaid())continue;
            if("unpaid".equals(paidFilter)&&e.isPaid())continue;
            if(!DateFilterUtil.matches(e.getDate(), dateFilter))continue;
            filteredList.add(e);
        }
        double total=0;for(LedgerEntry e:filteredList)total+=e.getAmount();
        if(tvTotal!=null)tvTotal.setText(DatabaseManager.formatAmount(total));
        if(tvCount!=null)tvCount.setText(filteredList.size()+" টি এন্ট্রি");
        if(filteredList.isEmpty()){rv.setVisibility(View.GONE);emptyState.setVisibility(View.VISIBLE);}
        else{rv.setVisibility(View.VISIBLE);emptyState.setVisibility(View.GONE);
            rv.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_fall_down));
            rv.setAdapter(new LedgerListAdapter(this,filteredList,(item,pos)->{
                TransactionSheetHelper.showLedgerSheet(this, db, item, this::loadData);
            }));
            rv.scheduleLayoutAnimation();
        }
    }
}
