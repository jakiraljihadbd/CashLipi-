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
import com.jrappspot.cashlipi.adapters.TransactionListAdapter;
import com.jrappspot.cashlipi.models.Transaction;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import com.jrappspot.cashlipi.utils.DateFilterUtil;
import com.jrappspot.cashlipi.utils.TransactionSheetHelper;
import java.util.ArrayList;
import java.util.List;

public class SavingsListActivity extends BaseActivity {
    private DatabaseManager db;
    private RecyclerView rv;
    private LinearLayout emptyState;
    private EditText etSearch;
    private TextView tvTotal,tvCount;
    private List<Transaction> allList=new ArrayList<>(),filteredList=new ArrayList<>();
    private String currentFilter="all";

    @Override protected void onCreate(Bundle s){
        super.onCreate(s);setContentView(R.layout.activity_list_common);
        FontUtils.applyToView(this, findViewById(android.R.id.content));
        db=DatabaseManager.getInstance(this);
        rv=findViewById(R.id.rvList);emptyState=findViewById(R.id.emptyState);
        etSearch=findViewById(R.id.etSearch);tvTotal=findViewById(R.id.tvSummaryTotal);tvCount=findViewById(R.id.tvSummaryCount);
        rv.setLayoutManager(new LinearLayoutManager(this));
        TextView tvHeader=findViewById(R.id.tvListHeader);if(tvHeader!=null)tvHeader.setText(" সঞ্চয় তালিকা");
        View header=findViewById(R.id.listHeader);if(header!=null)header.setBackground(getResources().getDrawable(R.drawable.bg_header_savings));
        setupFilterChips();
        ImageView ivClearS = findViewById(R.id.ivClearSearch);
        etSearch.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){if(ivClearS!=null)ivClearS.setVisibility(s.length()>0?View.VISIBLE:View.GONE);applyFilter();}public void afterTextChanged(Editable s){}});
        if(ivClearS!=null)ivClearS.setOnClickListener(v->{etSearch.setText("");etSearch.requestFocus();});
        findViewById(R.id.btnAddNew).setOnClickListener(v->
            startActivity(new android.content.Intent(this, AddSavingsActivity.class)));
        loadData();
    }
    @Override protected void onResume(){super.onResume();loadData();}
    private void loadData(){allList=new ArrayList<>(db.getSavingsList());applyFilter();}
    private void setupFilterChips(){
        String[] labels={"সব","আজ","সপ্তাহ","মাস","বছর"};String[] keys={"all","today","week","month","year"};
        LinearLayout chipRow=findViewById(R.id.chipRow);if(chipRow==null)return;chipRow.removeAllViews();
        for(int i=0;i<labels.length;i++){final String key=keys[i];TextView chip=new TextView(this);
            chip.setText(labels[i]);chip.setTextSize(12.5f);
            chip.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setPadding(36,18,36,18);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);lp.setMarginEnd(8);chip.setLayoutParams(lp);
            chip.setClickable(true);chip.setFocusable(true);
            boolean selected=key.equals(currentFilter);
            chip.setBackground(getResources().getDrawable(selected?R.drawable.bg_chip_selected:R.drawable.bg_chip_unselected));
            chip.setTextColor(selected ? androidx.core.content.ContextCompat.getColor(this, R.color.white) : androidx.core.content.ContextCompat.getColor(this, R.color.chipUnselectedText));
            if(selected) chip.startAnimation(AnimationUtils.loadAnimation(this, R.anim.chip_scale));
            chip.setOnClickListener(v->{currentFilter=key;setupFilterChips();applyFilter();});chipRow.addView(chip);}
    }
    private void applyFilter(){
        String q=etSearch.getText()!=null?etSearch.getText().toString().toLowerCase().trim():"";
        filteredList=new ArrayList<>();
        for(Transaction t:allList){
            if(!q.isEmpty()&&!t.getDisplayTitle().toLowerCase().contains(q)&&!t.getNote().toLowerCase().contains(q))continue;
            if(!DateFilterUtil.matches(t.getDate(), currentFilter))continue;
            filteredList.add(t);
        }
        double total=0;for(Transaction t:filteredList)total+=t.getAmount();
        tvTotal.setText(DatabaseManager.formatAmount(total));tvCount.setText(filteredList.size()+" টি এন্ট্রি");
        if(filteredList.isEmpty()){rv.setVisibility(View.GONE);emptyState.setVisibility(View.VISIBLE);}
        else{rv.setVisibility(View.VISIBLE);emptyState.setVisibility(View.GONE);
            rv.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_fall_down));
            rv.setAdapter(new TransactionListAdapter(this,filteredList,"savings",(item,pos)->{
                TransactionSheetHelper.showTransactionSheet(this, db, "savings", item, this::loadData);
            },null));
            rv.scheduleLayoutAnimation();
        }
    }
}
