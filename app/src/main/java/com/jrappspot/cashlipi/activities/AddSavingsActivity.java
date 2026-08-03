package com.jrappspot.cashlipi.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.utils.FontUtils;
import com.jrappspot.cashlipi.adapters.TransactionListAdapter;
import com.jrappspot.cashlipi.models.Transaction;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import com.jrappspot.cashlipi.utils.FirestoreSyncManager;
import com.jrappspot.cashlipi.utils.PaymentMethodUtil;
import com.jrappspot.cashlipi.utils.SuccessPopup;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * সঞ্চয় যোগ করার পেজ — বিকাশ/নগদ/রকেট ইত্যাদির ব্র্যান্ড-কালার আইকনসহ বড়, টাচ-ফ্রেন্ডলি
 * পেমেন্ট মাধ্যম চিপ গ্রিড (AddTransactionActivity-এর মতো), বড় ইনপুট ফিল্ড ও বাটন।
 */
public class AddSavingsActivity extends BaseActivity {
    private DatabaseManager db;
    private TextInputEditText etBankName, etAmount, etNote;
    private TextInputLayout tilAmount;
    private TextView tvDateText, tvTimeText, tvCurrentBalance;
    private LinearLayout rowPaymentMethods;
    private RecyclerView rvRecentSavings;
    private LinearLayout emptyState;
    private String selectedDate="", selectedTime="";
    private String selectedMethod = "cash";

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_add_savings);
        FontUtils.applyToView(this, findViewById(android.R.id.content));
        db = DatabaseManager.getInstance(this);
        etBankName = findViewById(R.id.etBankName);
        etAmount   = findViewById(R.id.etAmount);
        etNote     = findViewById(R.id.etNote);
        tilAmount  = findViewById(R.id.tilAmount);
        tvDateText = findViewById(R.id.tvDateText);
        tvTimeText = findViewById(R.id.tvTimeText);
        tvCurrentBalance = findViewById(R.id.tvCurrentBalance);
        rowPaymentMethods = findViewById(R.id.rowPaymentMethods);
        rvRecentSavings = findViewById(R.id.rvRecentSavings);
        emptyState = findViewById(R.id.emptyState);
        rvRecentSavings.setLayoutManager(new LinearLayoutManager(this));
        rvRecentSavings.setNestedScrollingEnabled(false);

        loadPaymentMethods();

        selectedDate = DatabaseManager.nowDate();
        selectedTime = DatabaseManager.nowTime();
        tvDateText.setText(DatabaseManager.formatDateDisplay(selectedDate));
        tvTimeText.setText(DatabaseManager.formatTimeDisplay(selectedTime));
        updateBalance();

        int[] qIds = {R.id.btn100,R.id.btn500,R.id.btn1000,R.id.btn2000,R.id.btn5000,R.id.btn10000,R.id.btn20000,R.id.btn50000};
        int[] qVals= {5,10,20,50,100,200,500,1000};
        for(int i=0;i<qIds.length;i++){
            final int val=qVals[i];
            Button b=findViewById(qIds[i]);
            if(b!=null)b.setOnClickListener(v->{
                String cur=etAmount.getText()!=null?etAmount.getText().toString():"";
                if(cur.isEmpty())etAmount.setText(String.valueOf(val));
                else{try{etAmount.setText(String.valueOf((int)(Double.parseDouble(cur)+val)));}catch(Exception e2){etAmount.setText(String.valueOf(val));}}
                etAmount.setSelection(etAmount.getText().length());
            });
        }

        View tvDate = findViewById(R.id.tvDate);
        View tvTime = findViewById(R.id.tvTime);
        tvDate.setOnClickListener(v->{Calendar c=Calendar.getInstance();new DatePickerDialog(this,(view,y,m,d)->{selectedDate=String.format("%04d-%02d-%02d",y,m+1,d);tvDateText.setText(DatabaseManager.formatDateDisplay(selectedDate));},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();});
        tvTime.setOnClickListener(v->{Calendar c=Calendar.getInstance();new TimePickerDialog(this,(view,h,min)->{selectedTime=String.format("%02d:%02d",h,min);tvTimeText.setText(DatabaseManager.formatTimeDisplay(selectedTime));},c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),false).show();});
        findViewById(R.id.btnSaveSavings).setOnClickListener(v->saveSavings());
        findViewById(R.id.tvViewAllSavings).setOnClickListener(v->startActivity(new Intent(this,SavingsListActivity.class)));
        loadRecentSavings();
    }

    @Override protected void onResume(){super.onResume();loadRecentSavings();updateBalance();}

    private void updateBalance(){
        tvCurrentBalance.setText("বর্তমান ব্যালেন্স: "+DatabaseManager.formatAmount(db.getBalance()));
    }

    /** পেমেন্ট মাধ্যম গ্রিড — বিকাশ/নগদ/রকেট/ব্যাংক ইত্যাদির নিজস্ব ব্র্যান্ড-কালার আইকনসহ,
     *  সিলেক্ট করা আইটেমের ওপর টিক-মার্ক ব্যাজ ও পিংক বর্ডার হাইলাইট। */
    private void loadPaymentMethods(){
        if (rowPaymentMethods == null) return;
        rowPaymentMethods.removeAllViews();

        int total = PaymentMethodUtil.LABELS.size();
        int i = 0;
        for (Map.Entry<String, String> entry : PaymentMethodUtil.LABELS.entrySet()) {
            String key = entry.getKey();
            String label = entry.getValue();

            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(20, 16, 20, 16);
            item.setClickable(true);
            item.setFocusable(true);

            boolean selected = key.equals(selectedMethod);
            item.setBackground(getResources().getDrawable(selected
                    ? R.drawable.bg_txn_payment_item_selected_savings
                    : R.drawable.bg_txn_payment_item));

            View iconWrap = wrapMethodIconWithCheck(PaymentMethodUtil.getIconRes(key), selected, 56);

            TextView labelView = new TextView(this);
            labelView.setText(label);
            labelView.setTextSize(11.5f);
            labelView.setMaxLines(1);
            labelView.setGravity(Gravity.CENTER);
            labelView.setTypeface(labelView.getTypeface(), android.graphics.Typeface.BOLD);
            labelView.setTextColor(selected
                    ? androidx.core.content.ContextCompat.getColor(this, R.color.iePinkText)
                    : androidx.core.content.ContextCompat.getColor(this, R.color.textPrimary));
            LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            labelLp.topMargin = 8;
            labelView.setLayoutParams(labelLp);

            item.addView(iconWrap);
            item.addView(labelView);

            LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            itemLp.setMarginEnd((i != total - 1) ? 10 : 0);
            item.setLayoutParams(itemLp);

            item.setOnClickListener(v -> {
                selectedMethod = key;
                findViewById(R.id.bankNameRow).setVisibility("bank".equals(selectedMethod) ? View.VISIBLE : View.GONE);
                loadPaymentMethods();
            });

            rowPaymentMethods.addView(item);
            i++;
        }
    }

    private View wrapMethodIconWithCheck(int iconRes, boolean selected, int sizePx) {
        FrameLayout frame = new FrameLayout(this);
        frame.setLayoutParams(new LinearLayout.LayoutParams(sizePx, sizePx));

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setLayoutParams(new FrameLayout.LayoutParams(sizePx, sizePx));
        frame.addView(icon);

        if (selected) {
            ImageView badge = new ImageView(this);
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setShape(GradientDrawable.OVAL);
            badgeBg.setColor(0xB3000000);
            badge.setBackground(badgeBg);
            badge.setImageResource(R.drawable.ic_checkmark_plain);
            badge.setColorFilter(0xFFFFFFFF);
            int badgeSize = Math.round(sizePx * 0.62f);
            int pad = Math.round(badgeSize * 0.2f);
            badge.setPadding(pad, pad, pad, pad);
            FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(badgeSize, badgeSize);
            badgeLp.gravity = Gravity.CENTER;
            badge.setLayoutParams(badgeLp);
            frame.addView(badge);
        }
        return frame;
    }

    private void saveSavings(){
        String amtStr=etAmount.getText()!=null?etAmount.getText().toString().trim():"";
        String bank=etBankName.getText()!=null?etBankName.getText().toString().trim():"";
        String note=etNote.getText()!=null?etNote.getText().toString().trim():"";

        if(amtStr.isEmpty()){tilAmount.setError("পরিমাণ লিখুন");etAmount.requestFocus();return;}
        double amount;
        try{amount=Double.parseDouble(amtStr);if(amount<=0)throw new NumberFormatException();}
        catch(Exception e){tilAmount.setError("সঠিক পরিমাণ লিখুন");etAmount.requestFocus();return;}
        tilAmount.setError(null);

        Transaction t=new Transaction();
        t.setMethod(selectedMethod);t.setBankName(bank);t.setAmount(amount);
        t.setDate(selectedDate.isEmpty()?DatabaseManager.nowDate():selectedDate);
        t.setTime(selectedTime.isEmpty()?DatabaseManager.nowTime():selectedTime);
        t.setNote(note);t.setType("savings");t.setSourceType("direct");
        db.addSavings(t);
        com.jrappspot.cashlipi.widgets.FinanceWidgetProvider.updateAll(this);
        com.jrappspot.cashlipi.utils.BackupManager.getInstance(this).triggerAutoGoogleDriveSync();
        // 🔥 Firebase auto-sync
        FirestoreSyncManager.getInstance(this).uploadAllData(null);
        SuccessPopup.show(this, SuccessPopup.Category.SAVINGS,
                "সঞ্চয় যোগ সফল হয়েছে!",
                "আপনার সঞ্চয় তালিকা সফলভাবে আপডেট হয়েছে।",
                () -> etAmount.requestFocus(),
                () -> startActivity(new Intent(this, SavingsListActivity.class)));
        etAmount.setText("");etNote.setText("");etBankName.setText("");
        selectedMethod = "cash";
        findViewById(R.id.bankNameRow).setVisibility(View.GONE);
        loadPaymentMethods();
        selectedDate=DatabaseManager.nowDate();selectedTime=DatabaseManager.nowTime();
        tvDateText.setText(DatabaseManager.formatDateDisplay(selectedDate));
        tvTimeText.setText(DatabaseManager.formatTimeDisplay(selectedTime));
        updateBalance();loadRecentSavings();
    }

    private void loadRecentSavings(){
        List<Transaction> sl=db.getSavingsList().subList(0,Math.min(5,db.getSavingsList().size()));
        if(sl.isEmpty()){rvRecentSavings.setVisibility(View.GONE);emptyState.setVisibility(View.VISIBLE);}
        else{rvRecentSavings.setVisibility(View.VISIBLE);emptyState.setVisibility(View.GONE);
            rvRecentSavings.setAdapter(new TransactionListAdapter(this,sl,"savings",(item,pos)->{
                new AlertDialog.Builder(this,R.style.AppDialog).setTitle(item.getMethodDisplay())
                .setItems(new String[]{" মুছুন"},(d,w)->{db.deleteSavings(pos);com.jrappspot.cashlipi.utils.BackupManager.getInstance(this).triggerAutoGoogleDriveSync();
        // 🔥 Firebase auto-sync
        FirestoreSyncManager.getInstance(this).uploadAllData(null);loadRecentSavings();updateBalance();Toast.makeText(this," মুছে গেছে",Toast.LENGTH_SHORT).show();}).show();
            },null));
        }
    }
}
