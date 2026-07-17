package com.jrappspot.cashlipi.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.utils.FontUtils;
import com.jrappspot.cashlipi.adapters.MethodSpinnerAdapter;
import com.jrappspot.cashlipi.adapters.TransactionListAdapter;
import com.jrappspot.cashlipi.models.Transaction;
import com.jrappspot.cashlipi.utils.AmountInputHelper;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import com.jrappspot.cashlipi.utils.FirestoreSyncManager;
import com.jrappspot.cashlipi.utils.SuccessPopup;
import java.util.Calendar;
import java.util.List;

public class AddSavingsActivity extends BaseActivity {
    private DatabaseManager db;
    private TextInputEditText etBankName, etAmount, etNote;
    private TextInputLayout tilAmount;
    private TextView tvDate, tvTime, tvCurrentBalance;
    private Spinner spinnerMethod;
    private RecyclerView rvRecentSavings;
    private LinearLayout emptyState;
    private String selectedDate="", selectedTime="";

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_add_savings);
        FontUtils.applyToView(this, findViewById(android.R.id.content));
        db = DatabaseManager.getInstance(this);
        etBankName = findViewById(R.id.etBankName);
        etAmount   = findViewById(R.id.etAmount);
        etNote     = findViewById(R.id.etNote);
        tilAmount  = findViewById(R.id.tilAmount);
        tvDate     = findViewById(R.id.tvDate);
        tvTime     = findViewById(R.id.tvTime);
        tvCurrentBalance = findViewById(R.id.tvCurrentBalance);
        spinnerMethod = findViewById(R.id.spinnerMethod);
        rvRecentSavings = findViewById(R.id.rvRecentSavings);
        emptyState = findViewById(R.id.emptyState);
        rvRecentSavings.setLayoutManager(new LinearLayoutManager(this));
        rvRecentSavings.setNestedScrollingEnabled(false);

        String[] methods = {"নগদ টাকা", "ব্যাংক", "বিকাশ", "নগদ", "রকেট", "অন্যান্য"};
        int[] methodIcons = {
            R.drawable.ic_method_cash,
            R.drawable.ic_method_bank,
            R.drawable.ic_method_bkash,
            R.drawable.ic_method_nagad,
            R.drawable.ic_method_rocket,
            R.drawable.ic_method_other
        };
        MethodSpinnerAdapter methodAdapter = new MethodSpinnerAdapter(this, methods, methodIcons);
        spinnerMethod.setAdapter(methodAdapter);

        selectedDate = DatabaseManager.nowDate();
        selectedTime = DatabaseManager.nowTime();
        tvDate.setText(DatabaseManager.formatDateDisplay(selectedDate));
        tvTime.setText(DatabaseManager.formatTimeDisplay(selectedTime));
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

        spinnerMethod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> p,View v,int pos,long id){
                boolean isBank=pos==1;
                findViewById(R.id.bankNameRow).setVisibility(isBank?View.VISIBLE:View.GONE);
            }
            public void onNothingSelected(AdapterView<?> p){}
        });
        tvDate.setOnClickListener(v->{Calendar c=Calendar.getInstance();new DatePickerDialog(this,(view,y,m,d)->{selectedDate=String.format("%04d-%02d-%02d",y,m+1,d);tvDate.setText(DatabaseManager.formatDateDisplay(selectedDate));},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();});
        tvTime.setOnClickListener(v->{Calendar c=Calendar.getInstance();new TimePickerDialog(this,(view,h,min)->{selectedTime=String.format("%02d:%02d",h,min);tvTime.setText(DatabaseManager.formatTimeDisplay(selectedTime));},c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),false).show();});
        findViewById(R.id.btnSaveSavings).setOnClickListener(v->saveSavings());
        findViewById(R.id.tvViewAllSavings).setOnClickListener(v->startActivity(new Intent(this,SavingsListActivity.class)));
        loadRecentSavings();
    }

    @Override protected void onResume(){super.onResume();loadRecentSavings();updateBalance();}

    private void updateBalance(){
        tvCurrentBalance.setText("বর্তমান ব্যালেন্স: "+DatabaseManager.formatAmount(db.getBalance()));
    }

    private void saveSavings(){
        String amtStr=etAmount.getText()!=null?etAmount.getText().toString().trim():"";
        String bank=etBankName.getText()!=null?etBankName.getText().toString().trim():"";
        String note=etNote.getText()!=null?etNote.getText().toString().trim():"";
        String[] methodKeys={"cash","bank","bkash","nagad","rocket","other"};
        int pos=spinnerMethod.getSelectedItemPosition();
        String method=methodKeys[pos<methodKeys.length?pos:0];

        if(amtStr.isEmpty()){tilAmount.setError("পরিমাণ লিখুন");etAmount.requestFocus();return;}
        double amount;
        try{amount=Double.parseDouble(amtStr);if(amount<=0)throw new NumberFormatException();}
        catch(Exception e){tilAmount.setError("সঠিক পরিমাণ লিখুন");etAmount.requestFocus();return;}
        tilAmount.setError(null);

        Transaction t=new Transaction();
        t.setMethod(method);t.setBankName(bank);t.setAmount(amount);
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
        selectedDate=DatabaseManager.nowDate();selectedTime=DatabaseManager.nowTime();
        tvDate.setText(DatabaseManager.formatDateDisplay(selectedDate));
        tvTime.setText(DatabaseManager.formatTimeDisplay(selectedTime));
        updateBalance();loadRecentSavings();
    }

    private void loadRecentSavings(){
        List<Transaction> list=db.getRecentIncome(5);
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
