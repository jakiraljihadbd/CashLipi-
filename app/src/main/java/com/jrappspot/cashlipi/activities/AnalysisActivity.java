package com.jrappspot.cashlipi.activities;

import android.os.Bundle;
import androidx.core.content.ContextCompat;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;



import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.utils.FontUtils;
import com.jrappspot.cashlipi.models.Transaction;
import com.jrappspot.cashlipi.utils.DatabaseManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnalysisActivity extends BaseActivity {

    private DatabaseManager db;

    // Proportional color palettes — values from colors.xml
    private int[] PIE_COLORS_EXPENSE;
    private int[] PIE_COLORS_INCOME;

    private void initPieColors() {
        PIE_COLORS_EXPENSE = new int[]{
            androidx.core.content.ContextCompat.getColor(this, R.color.analysisPie1),
            androidx.core.content.ContextCompat.getColor(this, R.color.analysisPie2),
            androidx.core.content.ContextCompat.getColor(this, R.color.analysisPie3),
            androidx.core.content.ContextCompat.getColor(this, R.color.analysisPie4),
            androidx.core.content.ContextCompat.getColor(this, R.color.analysisPie5),
            androidx.core.content.ContextCompat.getColor(this, R.color.analysisPie6),
            androidx.core.content.ContextCompat.getColor(this, R.color.analysisPie7),
            androidx.core.content.ContextCompat.getColor(this, R.color.analysisPie8),
            androidx.core.content.ContextCompat.getColor(this, R.color.analysisPie9),
            androidx.core.content.ContextCompat.getColor(this, R.color.analysisPie10),
            androidx.core.content.ContextCompat.getColor(this, R.color.analysisPie11),
            androidx.core.content.ContextCompat.getColor(this, R.color.analysisPie12)
        };
        PIE_COLORS_INCOME = new int[]{
            androidx.core.content.ContextCompat.getColor(this, R.color.analysisChartIncome),
            androidx.core.content.ContextCompat.getColor(this, R.color.analysisPie2),
            androidx.core.content.ContextCompat.getColor(this, R.color.analysisPie3),
            androidx.core.content.ContextCompat.getColor(this, R.color.analysisPie4),
            androidx.core.content.ContextCompat.getColor(this, R.color.analysisPie5),
            androidx.core.content.ContextCompat.getColor(this, R.color.analysisPie6),
            androidx.core.content.ContextCompat.getColor(this, R.color.analysisPie7),
            androidx.core.content.ContextCompat.getColor(this, R.color.analysisPie8),
            androidx.core.content.ContextCompat.getColor(this, R.color.analysisPie9),
            androidx.core.content.ContextCompat.getColor(this, R.color.analysisPie10),
            androidx.core.content.ContextCompat.getColor(this, R.color.analysisPie11),
            androidx.core.content.ContextCompat.getColor(this, R.color.analysisPie12)
        };
    }

    @Override
    protected void onCreate(Bundle s) {
        initPieColors();
        super.onCreate(s);
        setContentView(R.layout.activity_analysis);
        FontUtils.applyToView(this, findViewById(android.R.id.content));
        db = DatabaseManager.getInstance(this);
        loadAnalysis();
    }

    @Override
    protected void onResume() { super.onResume(); loadAnalysis(); }

    private void loadAnalysis() {
        double income  = db.getTotalIncome();
        double expense = db.getTotalExpense();
        double savings = db.getTotalSavings();
        double balance = db.getBalance();

        setText(R.id.tvAnalysisIncome,  DatabaseManager.formatAmount(income));
        setText(R.id.tvAnalysisExpense, DatabaseManager.formatAmount(expense));
        setText(R.id.tvAnalysisSavings, DatabaseManager.formatAmount(savings));
        setText(R.id.tvAnalysisBalance, DatabaseManager.formatAmount(balance));

        // Health score
        int score = db.calcHealthScore();
        setText(R.id.tvHealthScore, score + "/100");
        String label;
        int scoreColor;
        if (score >= 80)      { label = "চমৎকার আর্থিক স্বাস্থ্য! "; scoreColor = androidx.core.content.ContextCompat.getColor(this, R.color.analysisScoreExcellent); }
        else if (score >= 60) { label = "ভালো পরিস্থিতি ";            scoreColor = androidx.core.content.ContextCompat.getColor(this, R.color.analysisScoreGood); }
        else if (score >= 40) { label = "উন্নতির সুযোগ আছে ";         scoreColor = androidx.core.content.ContextCompat.getColor(this, R.color.analysisScoreAverage); }
        else                  { label = "জরুরি পদক্ষেপ দরকার ";      scoreColor = androidx.core.content.ContextCompat.getColor(this, R.color.analysisScorePoor); }
        setText(R.id.tvHealthLabel, label);
        TextView tvScore = findViewById(R.id.tvHealthScore);
        if (tvScore != null) tvScore.setTextColor(scoreColor);

        // Animate health bar width proportionally
        View bar = findViewById(R.id.healthProgressBar);
        if (bar != null) {
            bar.post(() -> {
                int parentW = ((View) bar.getParent()).getWidth();
                bar.getLayoutParams().width = (int)(parentW * score / 100f);
                bar.setBackgroundColor(scoreColor);
                bar.requestLayout();
            });
        }

        buildExpensePieChart(expense);
        buildIncomePieChart();
        buildMonthlyBarChart();
        buildSavingsLineChart();
    }

    // ── EXPENSE PIE — proportional colors ────────────────────────────
    private void buildExpensePieChart(double totalExpense) {
        PieChart chart = findViewById(R.id.pieChartExpense);
        LinearLayout legend = findViewById(R.id.pieLegendContainer);
        if (chart == null) return;

        Map<String, Float> catMap = new LinkedHashMap<>();
        for (Transaction t : db.getExpenseList()) {
            String cat = t.getDisplayTitle();
            catMap.put(cat, catMap.getOrDefault(cat, 0f) + (float) t.getAmount());
        }

        if (catMap.isEmpty()) {
            chart.setVisibility(View.GONE);
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors  = new ArrayList<>();
        int ci = 0;
        for (Map.Entry<String, Float> e : catMap.entrySet()) {
            entries.add(new PieEntry(e.getValue(), ""));
            colors.add(PIE_COLORS_EXPENSE[ci % PIE_COLORS_EXPENSE.length]);
            ci++;
        }

        PieDataSet ds = new PieDataSet(entries, "");
        ds.setColors(colors);
        ds.setValueTextSize(0f); // hide values on slice
        ds.setSliceSpace(2f);

        stylePieChart(chart);
        chart.setData(new PieData(ds));
        chart.animateY(900);
        chart.invalidate();

        // Custom legend rows
        if (legend != null) {
            legend.removeAllViews();
            ci = 0;
            for (Map.Entry<String, Float> e : catMap.entrySet()) {
                float pct = totalExpense > 0 ? (e.getValue() / (float) totalExpense * 100f) : 0f;
                addLegendRow(legend, PIE_COLORS_EXPENSE[ci % PIE_COLORS_EXPENSE.length],
                        e.getKey(), DatabaseManager.formatAmount(e.getValue()),
                        String.format(Locale.getDefault(), "%.1f%%", pct));
                ci++;
            }
        }
    }

    // ── INCOME PIE ────────────────────────────────────────────────────
    private void buildIncomePieChart() {
        PieChart chart = findViewById(R.id.pieChartIncome);
        LinearLayout legend = findViewById(R.id.incomeLegendContainer);
        if (chart == null) return;

        Map<String, Float> catMap = new LinkedHashMap<>();
        for (Transaction t : db.getIncomeList()) {
            String cat = t.getDisplayTitle();
            catMap.put(cat, catMap.getOrDefault(cat, 0f) + (float) t.getAmount());
        }

        if (catMap.isEmpty()) {
            chart.setVisibility(View.GONE);
            return;
        }

        float total = 0;
        for (float v : catMap.values()) total += v;

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors  = new ArrayList<>();
        int ci = 0;
        for (Map.Entry<String, Float> e : catMap.entrySet()) {
            entries.add(new PieEntry(e.getValue(), ""));
            colors.add(PIE_COLORS_INCOME[ci % PIE_COLORS_INCOME.length]);
            ci++;
        }

        PieDataSet ds = new PieDataSet(entries, "");
        ds.setColors(colors);
        ds.setValueTextSize(0f);
        ds.setSliceSpace(2f);

        stylePieChart(chart);
        chart.setData(new PieData(ds));
        chart.animateY(900);
        chart.invalidate();

        if (legend != null) {
            legend.removeAllViews();
            ci = 0;
            for (Map.Entry<String, Float> e : catMap.entrySet()) {
                float pct = total > 0 ? (e.getValue() / total * 100f) : 0f;
                addLegendRow(legend, PIE_COLORS_INCOME[ci % PIE_COLORS_INCOME.length],
                        e.getKey(), DatabaseManager.formatAmount(e.getValue()),
                        String.format(Locale.getDefault(), "%.1f%%", pct));
                ci++;
            }
        }
    }

    private void stylePieChart(PieChart chart) {
        chart.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent));
        chart.setHoleColor(0x00000000);
        chart.setHoleRadius(42f);
        chart.setTransparentCircleRadius(48f);
        chart.setTransparentCircleColor(0x221A1A2E);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setEntryLabelColor(ContextCompat.getColor(this, R.color.white));
        chart.setEntryLabelTextSize(10f);
        chart.setDrawEntryLabels(false);
    }

    private void addLegendRow(LinearLayout parent, int color,
                               String label, String amount, String pct) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.setMargins(0, 3, 0, 3);
        row.setLayoutParams(rp);

        // Color dot
        View dot = new View(this);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(12, 12);
        dp.setMargins(0, 0, 8, 0);
        dot.setLayoutParams(dp);
        dot.setBackgroundColor(color);
        row.addView(dot);

        // Label
        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.analysisAxisLabel));
        tvLabel.setTextSize(12f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvLabel.setLayoutParams(lp);
        row.addView(tvLabel);

        // Amount
        TextView tvAmt = new TextView(this);
        tvAmt.setText(amount);
        tvAmt.setTextColor(0xFFFFFFFF);
        tvAmt.setTextSize(12f);
        tvAmt.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        ap.setMargins(6, 0, 10, 0);
        tvAmt.setLayoutParams(ap);
        row.addView(tvAmt);

        // Percent
        TextView tvPct = new TextView(this);
        tvPct.setText(pct);
        tvPct.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.analysisPctLabel));
        tvPct.setTextSize(11f);
        row.addView(tvPct);

        parent.addView(row);
    }

    // ── MONTHLY BAR CHART ─────────────────────────────────────────────
    private void buildMonthlyBarChart() {
        BarChart chart = findViewById(R.id.barChartMonthly);
        if (chart == null) return;

        List<BarEntry> incEntries = new ArrayList<>();
        List<BarEntry> expEntries = new ArrayList<>();
        String[] monthLabels = new String[6];
        SimpleDateFormat sdf = new SimpleDateFormat("MMM", new Locale("bn"));
        Calendar cal = Calendar.getInstance();

        for (int i = 5; i >= 0; i--) {
            Calendar c = (Calendar) cal.clone();
            c.add(Calendar.MONTH, -i);
            int yr = c.get(Calendar.YEAR), mo = c.get(Calendar.MONTH) + 1;
            double inc = 0, exp = 0;
            for (Transaction t : db.getIncomeByMonth(yr, mo))  inc += t.getAmount();
            for (Transaction t : db.getExpenseByMonth(yr, mo)) exp += t.getAmount();
            int idx = 5 - i;
            incEntries.add(new BarEntry(idx, (float) inc));
            expEntries.add(new BarEntry(idx, (float) exp));
            monthLabels[idx] = (mo) + "/" + (yr % 100);
        }

        BarDataSet dsInc = new BarDataSet(incEntries, "আয়");
        dsInc.setColor(androidx.core.content.ContextCompat.getColor(this, R.color.analysisChartIncome));
        dsInc.setValueTextColor(ContextCompat.getColor(this, R.color.white));
        dsInc.setValueTextSize(9f);

        BarDataSet dsExp = new BarDataSet(expEntries, "ব্যয়");
        dsExp.setColor(androidx.core.content.ContextCompat.getColor(this, R.color.analysisChartExpense));
        dsExp.setValueTextColor(ContextCompat.getColor(this, R.color.white));
        dsExp.setValueTextSize(9f);

        float bw = 0.3f;
        BarData bd = new BarData(dsInc, dsExp);
        bd.setBarWidth(bw);

        chart.setData(bd);
        chart.groupBars(0f, 0.3f, 0.05f);
        chart.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent));
        chart.getDescription().setEnabled(false);
        chart.setGridBackgroundColor(ContextCompat.getColor(this, R.color.transparent));
        chart.setDrawGridBackground(false);
        chart.setDrawBorders(false);

        XAxis xa = chart.getXAxis();
        xa.setValueFormatter(new IndexAxisValueFormatter(monthLabels));
        xa.setPosition(XAxis.XAxisPosition.BOTTOM);
        xa.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.analysisAxisLabel));
        xa.setGridColor(0x22FFFFFF);
        xa.setAxisLineColor(0x44FFFFFF);
        xa.setGranularity(1f);
        xa.setAxisMinimum(0f);
        xa.setAxisMaximum(6f);

        YAxis yl = chart.getAxisLeft();
        yl.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.analysisAxisLabel));
        yl.setGridColor(0x22FFFFFF);
        yl.setAxisLineColor(0x44FFFFFF);
        yl.setAxisMinimum(0f);
        chart.getAxisRight().setEnabled(false);

        Legend lg = chart.getLegend();
        lg.setTextColor(ContextCompat.getColor(this, R.color.white));
        lg.setTextSize(12f);

        chart.animateY(900);
        chart.invalidate();
    }

    // ── MONTHLY SAVINGS LINE CHART ────────────────────────────────────
    private void buildSavingsLineChart() {
        LineChart chart = findViewById(R.id.lineChartSavings);
        if (chart == null) return;

        List<Entry> savEntries = new ArrayList<>();
        String[] monthLabels = new String[6];
        Calendar cal = Calendar.getInstance();

        for (int i = 5; i >= 0; i--) {
            Calendar c = (Calendar) cal.clone();
            c.add(Calendar.MONTH, -i);
            int yr = c.get(Calendar.YEAR), mo = c.get(Calendar.MONTH) + 1;
            // Savings = cumulative up to this month (simplified: use total for now)
            double sav = 0;
            // Calculate net (income - expense) for this month as monthly savings
            double inc = 0, exp = 0;
            for (Transaction t : db.getIncomeByMonth(yr, mo))  inc += t.getAmount();
            for (Transaction t : db.getExpenseByMonth(yr, mo)) exp += t.getAmount();
            sav = Math.max(0, inc - exp);
            int idx = 5 - i;
            savEntries.add(new Entry(idx, (float) sav));
            monthLabels[idx] = mo + "/" + (yr % 100);
        }

        LineDataSet ds = new LineDataSet(savEntries, "মাসিক নেট সঞ্চয়");
        ds.setColor(androidx.core.content.ContextCompat.getColor(this, R.color.analysisChartIncome));
        ds.setCircleColor(androidx.core.content.ContextCompat.getColor(this, R.color.analysisChartIncome));
        ds.setLineWidth(2.5f);
        ds.setCircleRadius(4f);
        ds.setDrawCircleHole(true);
        ds.setCircleHoleColor(androidx.core.content.ContextCompat.getColor(this, R.color.analysisChartBg));
        ds.setValueTextColor(ContextCompat.getColor(this, R.color.white));
        ds.setValueTextSize(9f);
        ds.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        ds.setDrawFilled(true);
        ds.setFillColor(0x4410B981);
        ds.setFillAlpha(80);

        chart.setData(new LineData(ds));
        chart.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent));
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setDrawBorders(false);

        XAxis xa = chart.getXAxis();
        xa.setValueFormatter(new IndexAxisValueFormatter(monthLabels));
        xa.setPosition(XAxis.XAxisPosition.BOTTOM);
        xa.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.analysisAxisLabel));
        xa.setGridColor(0x22FFFFFF);
        xa.setGranularity(1f);

        YAxis yl = chart.getAxisLeft();
        yl.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.analysisAxisLabel));
        yl.setGridColor(0x22FFFFFF);
        yl.setAxisMinimum(0f);
        chart.getAxisRight().setEnabled(false);

        Legend lg = chart.getLegend();
        lg.setTextColor(ContextCompat.getColor(this, R.color.white));

        chart.animateXY(800, 800);
        chart.invalidate();
    }

    private void setText(int id, String text) {
        TextView tv = findViewById(id);
        if (tv != null) tv.setText(text);
    }
}
