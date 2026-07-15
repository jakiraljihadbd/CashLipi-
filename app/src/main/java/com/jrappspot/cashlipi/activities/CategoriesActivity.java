package com.jrappspot.cashlipi.activities;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import android.view.*;
import android.widget.*;

import com.google.android.material.textfield.TextInputEditText;
import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import java.util.List;

public class CategoriesActivity extends BaseActivity {
    private DatabaseManager db;
    private String currentType="income";

    @Override protected void onCreate(Bundle s){
        super.onCreate(s);setContentView(R.layout.activity_categories);
        db=DatabaseManager.getInstance(this);
        setupTypeButtons();
        loadCategories();
    }

    private void setupTypeButtons(){
        Button btnIncome = findViewById(R.id.btnTypeIncome);
        Button btnExpense = findViewById(R.id.btnTypeExpense);

        btnIncome.setOnClickListener(v -> {
            currentType = "income";
            btnIncome.setBackgroundResource(R.drawable.bg_btn_income);
            btnIncome.setTextColor(0xFFFFFFFF);
            btnExpense.setBackgroundResource(R.drawable.bg_type_inactive);
            btnExpense.setTextColor(0xFF64748B);
            loadCategories();
        });

        btnExpense.setOnClickListener(v -> {
            currentType = "expense";
            btnExpense.setBackgroundResource(R.drawable.bg_expense_active);
            btnExpense.setTextColor(0xFFFFFFFF);
            btnIncome.setBackgroundResource(R.drawable.bg_type_inactive);
            btnIncome.setTextColor(0xFF64748B);
            loadCategories();
        });
    }

    private void loadCategories(){
        LinearLayout container=findViewById(R.id.categoryContainer);
        if(container==null)return;container.removeAllViews();
        List<String> cats=db.getCategories(currentType);
        for(String cat:cats){
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setBackground(getResources().getDrawable(R.drawable.bg_card_white));
            row.setPadding(16,12,16,12);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0,0,0,8);row.setLayoutParams(lp);
            TextView tv=new TextView(this);tv.setText(cat);tv.setTextSize(14f);
            tv.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.textPrimary));
            tv.setLayoutParams(new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));
            Button btnDel = new Button(this);
            btnDel.setText("🗑");
            btnDel.setTextSize(16f);
            btnDel.setMinWidth(0);
            btnDel.setMinimumWidth(0);
            btnDel.setPadding(16, 8, 16, 8);
            btnDel.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            btnDel.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.errorColor));
            LinearLayout.LayoutParams delLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
            btnDel.setLayoutParams(delLp);
            btnDel.setOnClickListener(v -> {
                db.removeCategory(currentType, cat);
                loadCategories();
                Toast.makeText(this, "🗑 মুছে গেছে", Toast.LENGTH_SHORT).show();
            });
            row.addView(tv);
            row.addView(btnDel);
            container.addView(row);
        }
        // Add new
        EditText et=findViewById(R.id.etNewCategory);
        if(et!=null)et.setHint("income".equals(currentType)?"নতুন আয়ের ক্যাটাগরি":"নতুন ব্যয়ের ক্যাটাগরি");
        Button btnAdd=findViewById(R.id.btnAddCategory);
        if(btnAdd!=null)btnAdd.setOnClickListener(v->{
            String nc=et!=null&&et.getText()!=null?et.getText().toString().trim():"";
            if(nc.isEmpty()){Toast.makeText(this,"ক্যাটাগরি লিখুন",Toast.LENGTH_SHORT).show();return;}
            db.addCategory(currentType,nc);if(et!=null)et.setText("");loadCategories();
            Toast.makeText(this," যোগ হয়েছে",Toast.LENGTH_SHORT).show();
        });
    }
}
