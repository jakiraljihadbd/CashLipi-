package com.jrappspot.cashlipi;

import android.app.Application;
import android.os.Build;

import com.jrappspot.cashlipi.utils.DatabaseManager;
import com.jrappspot.cashlipi.utils.LocaleHelper;
import com.jrappspot.cashlipi.utils.ThemeUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AccountApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // 🐞 CRASH LOGGER — যেকোনো crash হলে stack trace একটা টেক্সট ফাইলে
        // সেভ হবে: /Android/data/com.jrappspot.cashlipi/files/crash_log.txt
        // ফাইল ম্যানেজার দিয়ে ওই ফাইল খুলে content কপি করে পাঠালেই
        // exact crash reason বের করা যাবে (PC/adb ছাড়াই)।
        final Thread.UncaughtExceptionHandler defaultHandler =
                Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                File dir = getExternalFilesDir(null);
                if (dir == null) dir = getFilesDir();
                File logFile = new File(dir, "crash_log.txt");

                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                throwable.printStackTrace(pw);

                String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
                String header = "\n===== CRASH @ " + time
                        + " | Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")"
                        + " | " + Build.MANUFACTURER + " " + Build.MODEL + " =====\n";

                try (FileWriter fw = new FileWriter(logFile, true)) {
                    fw.write(header);
                    fw.write(sw.toString());
                    fw.write("\n");
                }
            } catch (Exception ignored) {
                // log লেখা ব্যর্থ হলেও অন্তত app স্বাভাবিকভাবে crash হোক
            }

            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            } else {
                System.exit(2);
            }
        });

        DatabaseManager db = DatabaseManager.getInstance(this);
        ThemeUtils.applySavedTheme(db);
        LocaleHelper.applyLocale(this); // saved language restore
    }
}
