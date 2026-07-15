package com.jrappspot.cashlipi.activities;
import android.os.Bundle;
import android.widget.*;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.models.LedgerEntry;
import com.jrappspot.cashlipi.models.Transaction;
import com.jrappspot.cashlipi.utils.DatabaseManager;
import java.util.List;

public class AccountingActivity extends BaseActivity {
    private DatabaseManager db;

    @Override protected void onCreate(Bundle s){
        super.onCreate(s);setContentView(R.layout.activity_accounting);
        db=DatabaseManager.getInstance(this);
        loadAccountingData();
    }
    @Override protected void onResume(){super.onResume();loadAccountingData();}

    private void loadAccountingData(){
        double totalIncome=db.getTotalIncome();
        double totalExpense=db.getTotalExpense();
        double totalSavings=db.getTotalSavings();
        double totalDena=db.getTotalDena();
        double totalPabona=db.getTotalPabona();
        double balance=db.getBalance();
        double net=balance+totalPabona-totalDena;

        setText(R.id.tvAccIncome,DatabaseManager.formatAmount(totalIncome));
        setText(R.id.tvAccExpense,DatabaseManager.formatAmount(totalExpense));
        setText(R.id.tvAccSavings,DatabaseManager.formatAmount(totalSavings));
        setText(R.id.tvAccBalance,DatabaseManager.formatAmount(balance));
        setText(R.id.tvAccDena,DatabaseManager.formatAmount(totalDena));
        setText(R.id.tvAccPabona,DatabaseManager.formatAmount(totalPabona));
        setText(R.id.tvNetPosition,DatabaseManager.formatAmount(net));
        setText(R.id.tvHealthScore,""+db.calcHealthScore()+"/100");

        // Income count
        setText(R.id.tvIncomeCount,db.getIncomeList().size()+" টি এন্ট্রি");
        setText(R.id.tvExpenseCount,db.getExpenseList().size()+" টি এন্ট্রি");
        setText(R.id.tvSavingsCount,db.getSavingsList().size()+" টি এন্ট্রি");
        setText(R.id.tvLedgerCount,db.getLedgerList().size()+" টি এন্ট্রি");
    }

    private void setText(int id,String text){
        TextView tv=findViewById(id);if(tv!=null)tv.setText(text);
    }
}
