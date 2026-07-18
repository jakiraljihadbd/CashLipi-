package com.jrappspot.cashlipi.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.activities.DashboardActivity;
import com.jrappspot.cashlipi.utils.DatabaseManager;

import java.text.DecimalFormat;

/**
 * হোম-স্ক্রিন উইজেট — গ্লাস-স্টাইল কার্ডে আয়, ব্যয়, দেনা-পাওনা, সঞ্চয় ও মোট ব্যালেন্স দেখায়।
 * ডেটা DatabaseManager (SharedPreferences ভিত্তিক) থেকে সরাসরি রিড করা হয়, তাই আলাদা কোনো
 * সার্ভিস বা ContentProvider দরকার হয় না — উইজেট একই অ্যাপ প্রসেসের মধ্যেই আপডেট হয়।
 */
public class FinanceWidgetProvider extends AppWidgetProvider {

    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#,##0");

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId);
        }
    }

    private static void updateWidget(Context context, AppWidgetManager appWidgetManager, int widgetId) {
        try {
        DatabaseManager db = DatabaseManager.getInstance(context);

        double income = db.getTotalIncome();
        double expense = db.getTotalExpense();
        double savings = db.getTotalSavings();
        double dena = db.getTotalDena();
        double pabona = db.getTotalPabona();
        double ledgerNet = pabona - dena;
        double balance = db.getBalance();

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_finance_glass);

        views.setTextViewText(R.id.widgetBalanceAmount, formatTaka(balance));
        views.setTextViewText(R.id.widgetIncomeAmount, formatTaka(income));
        views.setTextViewText(R.id.widgetExpenseAmount, formatTaka(expense));
        views.setTextViewText(R.id.widgetLedgerAmount, formatTaka(ledgerNet));
        views.setTextViewText(R.id.widgetSavingsAmount, formatTaka(savings));

        // পুরো উইজেটে ট্যাপ করলে অ্যাপ খুলবে
        Intent openIntent = new Intent(context, DashboardActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                context, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetRoot, openPendingIntent);

        // রিফ্রেশ বাটনে ট্যাপ করলে শুধু এই উইজেটটাই আবার আপডেট হবে
        Intent refreshIntent = new Intent(context, FinanceWidgetProvider.class);
        refreshIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{widgetId});
        PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(
                context, widgetId, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetRefreshBtn, refreshPendingIntent);

        appWidgetManager.updateAppWidget(widgetId, views);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String formatTaka(double amount) {
        boolean negative = amount < 0;
        String formatted = AMOUNT_FORMAT.format(Math.abs(amount));
        return (negative ? "-৳" : "৳") + formatted;
    }

    /**
     * অ্যাপের ভেতরে যেকোনো ট্রানজেকশন/লেজার/সেভিংস সেভ হওয়ার পর এটা কল করলে
     * হোম-স্ক্রিনে থাকা সব উইজেট সাথে সাথে রিফ্রেশ হয়ে যাবে।
     */
    public static void updateAll(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, FinanceWidgetProvider.class);
        int[] ids = appWidgetManager.getAppWidgetIds(componentName);
        for (int id : ids) {
            updateWidget(context, appWidgetManager, id);
        }
    }
}
