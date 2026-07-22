package com.jrappspot.cashlipi.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.jrappspot.cashlipi.models.LedgerEntry;
import com.jrappspot.cashlipi.models.Note;
import com.jrappspot.cashlipi.models.Person;
import com.jrappspot.cashlipi.models.TrashItem;
import com.jrappspot.cashlipi.models.Transaction;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * DatabaseManager — SharedPreferences based local database.
 * Converts JS db.js to Java.
 * All data stored as JSON strings in SharedPreferences.
 */
public class DatabaseManager {

    private static final String PREF_NAME = "cashlipi_account_db";
    private static final String KEY_INCOME = "income";
    private static final String KEY_EXPENSE = "expense";
    private static final String KEY_LEDGER = "ledger";
    private static final String KEY_PERSON = "dena_pawna_persons";
    private static final String KEY_SAVINGS = "savings";
    private static final String KEY_NOTES = "notes";
    private static final String KEY_TRASH = "trash";
    private static final String KEY_SETTINGS = "settings";
    private static final String KEY_CATEGORIES = "permanentCategories";
    private static final String KEY_USER_NAME = "_da_user_name";

    private static DatabaseManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;
    private final Context appContext;

    private DatabaseManager(Context context) {
        appContext = context.getApplicationContext();
        prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new GsonBuilder().create();
        initDefaults();
    }

    public static synchronized DatabaseManager getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseManager(context);
        }
        return instance;
    }

    // ═══════════════════════════════════════════
    //  INIT DEFAULTS
    // ═══════════════════════════════════════════
    private void initDefaults() {
        if (getIncomeList().isEmpty() && !prefs.contains(KEY_INCOME)) {
            saveList(KEY_INCOME, new ArrayList<>());
        }
        if (getExpenseList().isEmpty() && !prefs.contains(KEY_EXPENSE)) {
            saveList(KEY_EXPENSE, new ArrayList<>());
        }
        if (getLedgerList().isEmpty() && !prefs.contains(KEY_LEDGER)) {
            saveList(KEY_LEDGER, new ArrayList<>());
        }
        if (getPersonList().isEmpty() && !prefs.contains(KEY_PERSON)) {
            saveList(KEY_PERSON, new ArrayList<>());
        }
        if (getSavingsList().isEmpty() && !prefs.contains(KEY_SAVINGS)) {
            saveList(KEY_SAVINGS, new ArrayList<>());
        }
        if (getNotesList().isEmpty() && !prefs.contains(KEY_NOTES)) {
            saveList(KEY_NOTES, new ArrayList<>());
        }
        if (getTrashList().isEmpty() && !prefs.contains(KEY_TRASH)) {
            saveList(KEY_TRASH, new ArrayList<>());
        }
    }

    // ═══════════════════════════════════════════
    //  GENERIC JSON SAVE/LOAD
    // ═══════════════════════════════════════════
    private void saveList(String key, List<?> list) {
        prefs.edit().putString(key, gson.toJson(list)).apply();
    }

    private <T> List<T> loadList(String key, Type type) {
        String json = prefs.getString(key, null);
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            List<T> list = gson.fromJson(json, type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // ═══════════════════════════════════════════
    //  INCOME CRUD
    // ═══════════════════════════════════════════
    public List<Transaction> getIncomeList() {
        Type t = new TypeToken<List<Transaction>>() {}.getType();
        return loadList(KEY_INCOME, t);
    }

    public Transaction addIncome(Transaction t) {
        List<Transaction> list = getIncomeList();
        t.setId(generateId());
        t.setCreatedAt(nowIso());
        t.setType("income");
        list.add(0, t);
        saveList(KEY_INCOME, list);
        return t;
    }

    public boolean updateIncome(int index, Transaction updated) {
        List<Transaction> list = getIncomeList();
        if (index < 0 || index >= list.size()) return false;
        updated.setUpdatedAt(nowIso());
        list.set(index, updated);
        saveList(KEY_INCOME, list);
        return true;
    }

    public boolean deleteIncome(int index) {
        List<Transaction> list = getIncomeList();
        if (index < 0 || index >= list.size()) return false;
        Transaction removed = list.remove(index);
        saveList(KEY_INCOME, list);
        addToTrashFromTransaction(removed, KEY_INCOME);
        return true;
    }

    /** id দিয়ে সরাসরি একটা আয় এন্ট্রি মুছে ফেলে (ট্র্যাশে পাঠায় না) — দেনা-পাওনা "অপরিশোধিত"-এ
     *  ফিরিয়ে নিলে তার সাথে লিংক করা অটো-তৈরি আয়ের এন্ট্রি পরিষ্কার করতে ব্যবহৃত। */
    public boolean deleteIncomeById(String id) {
        if (id == null || id.isEmpty()) return false;
        List<Transaction> list = getIncomeList();
        for (int i = 0; i < list.size(); i++) {
            if (id.equals(list.get(i).getId())) {
                list.remove(i);
                saveList(KEY_INCOME, list);
                return true;
            }
        }
        return false;
    }

    public double getTotalIncome() {
        double total = 0;
        for (Transaction t : getIncomeList()) total += t.getAmount();
        // পাওনা পরিশোধের সময় "আয় হিসেবে" বেছে নেওয়া এন্ট্রিগুলোও আয়ের মোটে যোগ হয় — তবে শুধু
        // পুরনো (আপডেটের আগের) এন্ট্রি, যাদের জন্য এখনও কোনো প্রকৃত Transaction তৈরি হয়নি।
        // নতুন এন্ট্রির জন্য showSettleSourceDialog() ইতিমধ্যেই আয় তালিকায় একটা আসল লেনদেন যোগ
        // করে (settleTxnId দিয়ে লিংক করা), তাই সেটা getIncomeList() থেকেই গোনা হয়ে যায় — এখানে
        // আবার যোগ করলে দ্বিগুণ হয়ে যাবে।
        for (LedgerEntry e : getLedgerList()) {
            if (e.isPaid() && e.isPabona() && "incomeExpense".equals(e.getSettleTo())
                    && (e.getSettleTxnId() == null || e.getSettleTxnId().isEmpty())) {
                total += e.getAmount();
            }
        }
        return total;
    }

    // ═══════════════════════════════════════════
    //  EXPENSE CRUD
    // ═══════════════════════════════════════════
    public List<Transaction> getExpenseList() {
        Type t = new TypeToken<List<Transaction>>() {}.getType();
        return loadList(KEY_EXPENSE, t);
    }

    public Transaction addExpense(Transaction t) {
        List<Transaction> list = getExpenseList();
        t.setId(generateId());
        t.setCreatedAt(nowIso());
        t.setType("expense");
        list.add(0, t);
        saveList(KEY_EXPENSE, list);
        return t;
    }

    public boolean updateExpense(int index, Transaction updated) {
        List<Transaction> list = getExpenseList();
        if (index < 0 || index >= list.size()) return false;
        updated.setUpdatedAt(nowIso());
        list.set(index, updated);
        saveList(KEY_EXPENSE, list);
        return true;
    }

    public boolean deleteExpense(int index) {
        List<Transaction> list = getExpenseList();
        if (index < 0 || index >= list.size()) return false;
        Transaction removed = list.remove(index);
        saveList(KEY_EXPENSE, list);
        addToTrashFromTransaction(removed, KEY_EXPENSE);
        return true;
    }

    /** id দিয়ে সরাসরি একটা ব্যয় এন্ট্রি মুছে ফেলে (ট্র্যাশে পাঠায় না) — দেনা-পাওনা "অপরিশোধিত"-এ
     *  ফিরিয়ে নিলে তার সাথে লিংক করা অটো-তৈরি ব্যয়ের এন্ট্রি পরিষ্কার করতে ব্যবহৃত। */
    public boolean deleteExpenseById(String id) {
        if (id == null || id.isEmpty()) return false;
        List<Transaction> list = getExpenseList();
        for (int i = 0; i < list.size(); i++) {
            if (id.equals(list.get(i).getId())) {
                list.remove(i);
                saveList(KEY_EXPENSE, list);
                return true;
            }
        }
        return false;
    }

    public double getTotalExpense() {
        double total = 0;
        for (Transaction t : getExpenseList()) total += t.getAmount();
        // দেনা পরিশোধের সময় "ব্যয় হিসেবে" বেছে নেওয়া এন্ট্রিগুলোও ব্যয়ের মোটে যোগ হয় — তবে শুধু
        // পুরনো এন্ট্রি, যাদের জন্য এখনও কোনো প্রকৃত Transaction তৈরি হয়নি (দেখুন getTotalIncome())।
        for (LedgerEntry e : getLedgerList()) {
            if (e.isPaid() && e.isDena() && "incomeExpense".equals(e.getSettleTo())
                    && (e.getSettleTxnId() == null || e.getSettleTxnId().isEmpty())) {
                total += e.getAmount();
            }
        }
        return total;
    }

    // ═══════════════════════════════════════════
    //  SAVINGS CRUD
    // ═══════════════════════════════════════════
    public List<Transaction> getSavingsList() {
        Type t = new TypeToken<List<Transaction>>() {}.getType();
        return loadList(KEY_SAVINGS, t);
    }

    public Transaction addSavings(Transaction t) {
        List<Transaction> list = getSavingsList();
        t.setId(generateId());
        t.setCreatedAt(nowIso());
        t.setType("savings");
        list.add(0, t);
        saveList(KEY_SAVINGS, list);
        return t;
    }

    public boolean updateSavings(int index, Transaction updated) {
        List<Transaction> list = getSavingsList();
        if (index < 0 || index >= list.size()) return false;
        updated.setUpdatedAt(nowIso());
        list.set(index, updated);
        saveList(KEY_SAVINGS, list);
        return true;
    }

    public boolean deleteSavings(int index) {
        List<Transaction> list = getSavingsList();
        if (index < 0 || index >= list.size()) return false;
        Transaction removed = list.remove(index);
        saveList(KEY_SAVINGS, list);
        addToTrashFromTransaction(removed, KEY_SAVINGS);
        return true;
    }

    public double getTotalSavings() {
        double total = 0;
        for (Transaction t : getSavingsList()) total += t.getAmount();
        // দেনা-পাওনা পরিশোধের সময় "সঞ্চয়ে/থেকে" বেছে নেওয়া এন্ট্রিগুলোও সঞ্চয়ের হিসাবে যোগ হয়:
        // পাওনা পরিশোধ (টাকা পেয়ে সঞ্চয়ে রাখা হয়েছে) → বাড়ে, দেনা পরিশোধ (সঞ্চয় থেকে দেওয়া হয়েছে) → কমে
        for (LedgerEntry e : getLedgerList()) {
            if (e.isPaid() && "savings".equals(e.getSettleTo())) {
                total += e.isPabona() ? e.getAmount() : -e.getAmount();
            }
        }
        return total;
    }

    // ═══════════════════════════════════════════
    //  LEDGER CRUD
    // ═══════════════════════════════════════════
    public List<LedgerEntry> getLedgerList() {
        Type t = new TypeToken<List<LedgerEntry>>() {}.getType();
        return loadList(KEY_LEDGER, t);
    }

    /** নাম মিলিয়ে (case/space insensitive) কোনো একজন ব্যক্তির সব দেনা-পাওনা এন্ট্রি — দেনা-পাওনা পেজে ব্যবহারের জন্য। */
    public List<LedgerEntry> getLedgerForPersonName(String personName) {
        List<LedgerEntry> result = new java.util.ArrayList<>();
        if (personName == null || personName.trim().isEmpty()) return result;
        String target = personName.trim().toLowerCase();
        for (LedgerEntry e : getLedgerList()) {
            if (e.getPerson().trim().toLowerCase().equals(target)) result.add(e);
        }
        return result;
    }

    public LedgerEntry addLedger(LedgerEntry entry) {
        List<LedgerEntry> list = getLedgerList();
        entry.setId(generateId());
        entry.setCreatedAt(nowIso());
        list.add(0, entry);
        saveList(KEY_LEDGER, list);
        return entry;
    }

    public boolean updateLedger(int index, LedgerEntry updated) {
        List<LedgerEntry> list = getLedgerList();
        if (index < 0 || index >= list.size()) return false;
        updated.setUpdatedAt(nowIso());
        list.set(index, updated);
        saveList(KEY_LEDGER, list);
        return true;
    }

    public boolean deleteLedger(int index) {
        List<LedgerEntry> list = getLedgerList();
        if (index < 0 || index >= list.size()) return false;
        LedgerEntry removed = list.remove(index);
        saveList(KEY_LEDGER, list);
        addToTrashFromLedger(removed);
        return true;
    }

    public boolean toggleLedgerPaid(int index) {
        List<LedgerEntry> list = getLedgerList();
        if (index < 0 || index >= list.size()) return false;
        LedgerEntry entry = list.get(index);
        entry.setPaid(!entry.isPaid());
        if (entry.isPaid()) {
            entry.setPaidDate(nowDate());
        } else {
            entry.setPaidDate("");
        }
        entry.setUpdatedAt(nowIso());
        list.set(index, entry);
        saveList(KEY_LEDGER, list);
        return entry.isPaid();
    }

    public double getTotalDena() {
        double total = 0;
        for (LedgerEntry e : getLedgerList()) {
            if ("dena".equals(e.getType()) && !e.isPaid()) total += e.getAmount();
        }
        return total;
    }

    public double getTotalPabona() {
        double total = 0;
        for (LedgerEntry e : getLedgerList()) {
            if ("pabona".equals(e.getType()) && !e.isPaid()) total += e.getAmount();
        }
        return total;
    }

    // ═══════════════════════════════════════════
    //  দেনা-পাওনা — PERSON CRUD
    //  (শুধু পরিচিতি — লেনদেন পরে person.id ধরে যুক্ত হবে)
    // ═══════════════════════════════════════════
    public List<Person> getPersonList() {
        Type t = new TypeToken<List<Person>>() {}.getType();
        return loadList(KEY_PERSON, t);
    }

    public Person addPerson(Person person) {
        List<Person> list = getPersonList();
        person.setId(generateId());
        person.setCreatedAt(nowIso());
        if (person.getDate().isEmpty()) person.setDate(nowDate());
        if (person.getTime().isEmpty()) person.setTime(nowTime());
        list.add(0, person);
        saveList(KEY_PERSON, list);
        return person;
    }

    public boolean updatePerson(int index, Person updated) {
        List<Person> list = getPersonList();
        if (index < 0 || index >= list.size()) return false;
        updated.setUpdatedAt(nowIso());
        list.set(index, updated);
        saveList(KEY_PERSON, list);
        return true;
    }

    /**
     * ব্যক্তি মুছে ফেলার সাথে সাথে তার নামে থাকা সব দেনা-পাওনা এন্ট্রিও (LedgerEntry) মুছে দেয়।
     * আগে শুধু Person মুছে যেত কিন্তু তার লেজার এন্ট্রি থেকে যেত — ফলে হোম পেজের মোট দেনা/পাওনা/
     * ব্যালেন্স হিসাবে (যা লাইভ getLedgerList() থেকে হিসাব হয়) সেই ব্যক্তির টাকাটা রয়েই যেত।
     */
    public boolean deletePerson(int index) {
        List<Person> list = getPersonList();
        if (index < 0 || index >= list.size()) return false;
        Person removed = list.remove(index);
        saveList(KEY_PERSON, list);
        deleteLedgerEntriesForPersonName(removed.getName());
        return true;
    }

    /** কোনো ব্যক্তির নামে থাকা সব লেজার এন্ট্রি মুছে ট্র্যাশে পাঠায়; কতগুলো মুছল তা রিটার্ন করে। */
    public int deleteLedgerEntriesForPersonName(String personName) {
        if (personName == null || personName.trim().isEmpty()) return 0;
        String target = personName.trim().toLowerCase(Locale.ROOT);
        List<LedgerEntry> list = getLedgerList();
        List<LedgerEntry> kept = new ArrayList<>();
        int removedCount = 0;
        for (LedgerEntry e : list) {
            if (e.getPerson().trim().toLowerCase(Locale.ROOT).equals(target)) {
                addToTrashFromLedger(e);
                removedCount++;
            } else {
                kept.add(e);
            }
        }
        if (removedCount > 0) saveList(KEY_LEDGER, kept);
        return removedCount;
    }

    public Person getPersonById(String id) {
        if (id == null) return null;
        for (Person p : getPersonList()) {
            if (id.equals(p.getId())) return p;
        }
        return null;
    }

    public int getPersonIndexById(String id) {
        if (id == null) return -1;
        List<Person> list = getPersonList();
        for (int i = 0; i < list.size(); i++) {
            if (id.equals(list.get(i).getId())) return i;
        }
        return -1;
    }

    /** নাম মিলিয়ে (case/space insensitive) ব্যক্তি খোঁজে — হোম পেজ থেকে সরাসরি সর্বশেষ ব্যক্তির পেজে নেওয়ার জন্য। */
    public Person getPersonByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        String target = name.trim().toLowerCase(Locale.ROOT);
        for (Person p : getPersonList()) {
            if (p.getName().trim().toLowerCase(Locale.ROOT).equals(target)) return p;
        }
        return null;
    }

    /**
     * "dena" বা "pabona" টাইপের সর্বশেষ (সবচেয়ে নতুন) লেজার এন্ট্রিটা কার নামে — সেই Person
     * রিটার্ন করে। হোম পেজের দেনা/পাওনা কার্ডে চাপলে সরাসরি সেই ব্যক্তির পেজে নিয়ে যেতে ব্যবহৃত।
     */
    public Person getMostRecentLedgerPerson(String type) {
        List<LedgerEntry> all = getLedgerList();
        LedgerEntry latest = null;
        for (LedgerEntry e : all) {
            if (!type.equals(e.getType())) continue;
            if (latest == null || recencyKey(e).compareTo(recencyKey(latest)) > 0) latest = e;
        }
        if (latest == null) return null;
        return getPersonByName(latest.getPerson());
    }

    private static String recencyKey(LedgerEntry e) {
        String createdAt = e.getCreatedAt();
        if (createdAt != null && !createdAt.isEmpty()) return createdAt;
        return e.getDate() + " " + e.getTime();
    }

    // ═══════════════════════════════════════════
    //  NOTES CRUD
    // ═══════════════════════════════════════════
    public List<Note> getNotesList() {
        Type t = new TypeToken<List<Note>>() {}.getType();
        return loadList(KEY_NOTES, t);
    }

    public Note addNote(Note note) {
        List<Note> list = getNotesList();
        note.setId(generateId());
        note.setCreatedAt(nowIso());
        note.setDate(nowDate());
        note.setTime(nowTime());
        list.add(0, note);
        saveList(KEY_NOTES, list);
        return note;
    }

    public boolean updateNote(int index, Note updated) {
        List<Note> list = getNotesList();
        if (index < 0 || index >= list.size()) return false;
        updated.setUpdatedAt(nowIso());
        list.set(index, updated);
        saveList(KEY_NOTES, list);
        return true;
    }

    public boolean deleteNote(int index) {
        List<Note> list = getNotesList();
        if (index < 0 || index >= list.size()) return false;
        Note removed = list.remove(index);
        saveList(KEY_NOTES, list);
        addToTrashFromNote(removed);
        return true;
    }

    // ═══════════════════════════════════════════
    //  TRASH SYSTEM
    // ═══════════════════════════════════════════
    public List<TrashItem> getTrashList() {
        Type t = new TypeToken<List<TrashItem>>() {}.getType();
        List<TrashItem> all = loadList(KEY_TRASH, t);
        // Auto-clean items older than 30 days
        long cutoff = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        List<TrashItem> filtered = new ArrayList<>();
        for (TrashItem item : all) {
            try {
                long time = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                        .parse(item.getTrashedAt()).getTime();
                if (time > cutoff) filtered.add(item);
            } catch (Exception e) {
                filtered.add(item); // keep if can't parse
            }
        }
        return filtered;
    }

    private void addToTrashFromTransaction(Transaction t, String key) {
        List<TrashItem> trash = getTrashList();
        TrashItem item = new TrashItem();
        item.setTrashId(generateId());
        item.setTrashKey(key);
        item.setTrashedAt(nowIso());
        item.setId(t.getId());
        item.setAmount(t.getAmount());
        item.setSource(t.getSource());
        item.setCategory(t.getCategory());
        item.setMethod(t.getMethod());
        item.setType(t.getType());
        item.setDate(t.getDate());
        item.setTime(t.getTime());
        item.setNote(t.getNote());
        trash.add(0, item);
        saveList(KEY_TRASH, trash);
    }

    private void addToTrashFromLedger(LedgerEntry e) {
        List<TrashItem> trash = getTrashList();
        TrashItem item = new TrashItem();
        item.setTrashId(generateId());
        item.setTrashKey(KEY_LEDGER);
        item.setTrashedAt(nowIso());
        item.setId(e.getId());
        item.setAmount(e.getAmount());
        item.setPerson(e.getPerson());
        item.setCategory(e.getCategory());
        item.setType(e.getType());
        item.setDate(e.getDate());
        item.setTime(e.getTime());
        item.setNote(e.getNote());
        item.setPaid(e.isPaid());
        trash.add(0, item);
        saveList(KEY_TRASH, trash);
    }

    private void addToTrashFromNote(Note n) {
        List<TrashItem> trash = getTrashList();
        TrashItem item = new TrashItem();
        item.setTrashId(generateId());
        item.setTrashKey(KEY_NOTES);
        item.setTrashedAt(nowIso());
        item.setId(n.getId());
        item.setTitle(n.getTitle());
        item.setContent(n.getContent());
        item.setDate(n.getDate());
        item.setTime(n.getTime());
        trash.add(0, item);
        saveList(KEY_TRASH, trash);
    }

    public boolean permanentDelete(String trashId) {
        List<TrashItem> trash = getTrashList();
        for (int i = 0; i < trash.size(); i++) {
            if (trashId.equals(trash.get(i).getTrashId())) {
                trash.remove(i);
                saveList(KEY_TRASH, trash);
                return true;
            }
        }
        return false;
    }

    public boolean restoreFromTrash(String trashId) {
        List<TrashItem> trash = getTrashList();
        TrashItem found = null;
        int idx = -1;
        for (int i = 0; i < trash.size(); i++) {
            if (trashId.equals(trash.get(i).getTrashId())) {
                found = trash.get(i);
                idx = i;
                break;
            }
        }
        if (found == null) return false;

        String key = found.getTrashKey();
        if (KEY_INCOME.equals(key) || KEY_EXPENSE.equals(key) || KEY_SAVINGS.equals(key)) {
            Transaction t = new Transaction();
            t.setId(found.getId());
            t.setAmount(found.getAmount());
            t.setSource(found.getSource());
            t.setCategory(found.getCategory());
            t.setMethod(found.getMethod());
            t.setType(found.getType());
            t.setDate(found.getDate());
            t.setTime(found.getTime());
            t.setNote(found.getNote());
            t.setCreatedAt(nowIso());
            List<Transaction> list;
            if (KEY_INCOME.equals(key)) list = getIncomeList();
            else if (KEY_EXPENSE.equals(key)) list = getExpenseList();
            else list = getSavingsList();
            list.add(0, t);
            saveList(key, list);
        } else if (KEY_LEDGER.equals(key)) {
            LedgerEntry e = new LedgerEntry();
            e.setId(found.getId());
            e.setAmount(found.getAmount());
            e.setPerson(found.getPerson());
            e.setCategory(found.getCategory());
            e.setType(found.getType());
            e.setDate(found.getDate());
            e.setTime(found.getTime());
            e.setNote(found.getNote());
            e.setPaid(found.isPaid());
            e.setCreatedAt(nowIso());
            List<LedgerEntry> list = getLedgerList();
            list.add(0, e);
            saveList(KEY_LEDGER, list);
        } else if (KEY_NOTES.equals(key)) {
            Note n = new Note();
            n.setId(found.getId());
            n.setTitle(found.getTitle());
            n.setContent(found.getContent());
            n.setDate(found.getDate());
            n.setTime(found.getTime());
            n.setCreatedAt(nowIso());
            List<Note> list = getNotesList();
            list.add(0, n);
            saveList(KEY_NOTES, list);
        }

        trash.remove(idx);
        saveList(KEY_TRASH, trash);
        return true;
    }

    public void emptyTrash() {
        saveList(KEY_TRASH, new ArrayList<>());
    }

    // ═══════════════════════════════════════════
    //  SETTINGS
    // ═══════════════════════════════════════════
    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "");
    }

    public void saveUserName(String name) {
        prefs.edit().putString(KEY_USER_NAME, name).apply();
    }

    public boolean isDarkMode() {
        return prefs.getBoolean("darkMode", false);
    }

    public void setDarkMode(boolean enabled) {
        prefs.edit().putBoolean("darkMode", enabled).apply();
    }

    // ─── নেভিগেশন মেন্যু কাস্টমাইজেশন প্রেফারেন্স ────────────────────────
    /** "top" অথবা "bottom" — ডিফল্ট এখন "bottom" (কার্ভড U-নচ + FAB স্টাইল নিচেই থাকবে) */
    public String getNavPosition() {
        return prefs.getString("navPosition", "bottom");
    }

    public void setNavPosition(String position) {
        prefs.edit().putString("navPosition", position).apply();
    }

    /** true = বড় আইকন (ডিফল্ট), false = ছোট/কমপ্যাক্ট আইকন */
    public boolean isNavIconLarge() {
        return prefs.getBoolean("navIconLarge", true);
    }

    public void setNavIconLarge(boolean large) {
        prefs.edit().putBoolean("navIconLarge", large).apply();
    }

    /** ডান-বাম সোয়াইপ করে পেজ বদলানো — ডিফল্ট চালু */
    public boolean isNavSwipeEnabled() {
        return prefs.getBoolean("navSwipeEnabled", true);
    }

    public void setNavSwipeEnabled(boolean enabled) {
        prefs.edit().putBoolean("navSwipeEnabled", enabled).apply();
    }

    /** নেভিগেশন বারের ব্যাকগ্রাউন্ড রং — hex স্ট্রিং (যেমন "#0F1B2E"), ডিফল্ট ব্র্যান্ড নেভি */
    public String getNavBgColor() {
        return prefs.getString("navBgColor", "#0F1B2E");
    }

    public void setNavBgColor(String hexColor) {
        prefs.edit().putString("navBgColor", hexColor).apply();
    }

    /** নেভ বার প্রিসেট স্টাইল — "classic"|"floating"|"glass"|"gradient"|"minimal"|"neon"|"card", ডিফল্ট "classic" */
    public String getNavStyle() {
        return prefs.getString("navStyle", "classic");
    }

    public void setNavStyle(String styleKey) {
        prefs.edit().putString("navStyle", styleKey).apply();
    }

    // ─── Custom Calculator Keyboard preference ───────────────────────────
    public boolean isCustomKeyboardEnabled() {
        return prefs.getBoolean("customKeyboard", false); // default: বন্ধ
    }

    public void setCustomKeyboardEnabled(boolean enabled) {
        prefs.edit().putBoolean("customKeyboard", enabled).apply();
    }

    // ─── Language preference ─────────────────────────────────────────────
    public String getAppLanguage() {
        return prefs.getString("appLanguage", "bn"); // default: বাংলা
    }

    public void setAppLanguage(String langCode) {
        prefs.edit().putString("appLanguage", langCode).apply();
    }

    // ─── Custom Font (user-imported TTF) ─────────────────────────────────────
    // "default" = system font, otherwise absolute path to TTF in internal storage
    public String getCustomFontPath() {
        return prefs.getString("customFontPath", "default");
    }

    public void setCustomFontPath(String absolutePath) {
        prefs.edit().putString("customFontPath", absolutePath).apply();
    }

    public String getCustomFontName() {
        return prefs.getString("customFontName", "");
    }

    public void setCustomFontName(String name) {
        prefs.edit().putString("customFontName", name).apply();
    }

    public String getCurrency() {
        return prefs.getString("currency", "৳");
    }

    public void setCurrency(String currency) {
        prefs.edit().putString("currency", currency).apply();
    }

    // ═══════════════════════════════════════════
    //  PERMANENT CATEGORIES
    // ═══════════════════════════════════════════
    public List<String> getCategories(String type) {
        // type: "income" | "expense" | "dena" | "pabona" | "savings"
        String key = "cat_" + type;
        String json = prefs.getString(key, null);
        if (json == null) return getDefaultCategories(type);
        try {
            Type t = new TypeToken<List<String>>() {}.getType();
            List<String> list = gson.fromJson(json, t);
            return list != null ? list : getDefaultCategories(type);
        } catch (Exception e) {
            return getDefaultCategories(type);
        }
    }

    public void saveCategories(String type, List<String> list) {
        prefs.edit().putString("cat_" + type, gson.toJson(list)).apply();
    }

    public void addCategory(String type, String category) {
        List<String> list = getCategories(type);
        if (!list.contains(category)) {
            list.add(category);
            saveCategories(type, list);
        }
    }

    public void removeCategory(String type, String category) {
        List<String> list = getCategories(type);
        list.remove(category);
        saveCategories(type, list);
    }

    private List<String> getDefaultCategories(String type) {
        List<String> list = new ArrayList<>();
        if ("income".equals(type)) {
            String[] items = {"সার্ভিসিং", "ইলেকট্রনিক কাজ", "অটো চালানো",
                    "ব্যবসা", "বাবা", "মা", "ভাই", "অন্যান্য"};
            for (String s : items) list.add(s);
        } else if ("expense".equals(type)) {
            String[] items = {"খাবার", "যাতায়াত", "পোশাক",
                    "শিক্ষা", "চিকিৎসা", "দান-ছাদাকা", "অন্যান্য"};
            for (String s : items) list.add(s);
        }
        return list;
    }

    // ═══════════════════════════════════════════
    //  DASHBOARD SUMMARY
    // ═══════════════════════════════════════════
    public double getBalance() {
        double income = getTotalIncome();
        double expense = getTotalExpense();
        double savings = getTotalSavings();
        double paidPabona = 0, paidDena = 0;
        for (LedgerEntry e : getLedgerList()) {
            // শুধু "ব্যালেন্স" ধরে পরিশোধিত এন্ট্রিই মূল ব্যালেন্সে যোগ/বিয়োগ হয়; "সঞ্চয়"-এ পরিশোধিত
            // এন্ট্রি getTotalSavings()-এ আলাদাভাবে হিসাব হয়, আর "কোথাও না" শুধু বুককিপিং, কোনো
            // হিসাবে প্রভাব ফেলে না
            if (e.isPaid() && "balance".equals(e.getSettleTo())) {
                if ("pabona".equals(e.getType())) paidPabona += e.getAmount();
                else paidDena += e.getAmount();
            }
        }
        return income - expense + paidPabona - paidDena - savings;
    }

    // ═══════════════════════════════════════════
    //  CLEAR ALL DATA
    // ═══════════════════════════════════════════
    public void clearAllData() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_INCOME);
        editor.remove(KEY_EXPENSE);
        editor.remove(KEY_LEDGER);
        editor.remove(KEY_SAVINGS);
        editor.remove(KEY_NOTES);
        editor.remove(KEY_TRASH);
        editor.apply();
        initDefaults();
    }

    // ═══════════════════════════════════════════
    //  BACKUP / RESTORE (JSON)
    // ═══════════════════════════════════════════
    public String exportToJson() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"income\":").append(prefs.getString(KEY_INCOME, "[]")).append(",");
            sb.append("\"expense\":").append(prefs.getString(KEY_EXPENSE, "[]")).append(",");
            sb.append("\"ledger\":").append(prefs.getString(KEY_LEDGER, "[]")).append(",");
            sb.append("\"savings\":").append(prefs.getString(KEY_SAVINGS, "[]")).append(",");
            sb.append("\"notes\":").append(prefs.getString(KEY_NOTES, "[]")).append(",");
            sb.append("\"exportedAt\":\"").append(nowIso()).append("\"");
            sb.append("}");
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean importFromJson(String json) {
        try {
            com.google.gson.JsonObject obj = gson.fromJson(json, com.google.gson.JsonObject.class);
            SharedPreferences.Editor editor = prefs.edit();
            if (obj.has(KEY_INCOME)) editor.putString(KEY_INCOME, obj.get(KEY_INCOME).toString());
            if (obj.has(KEY_EXPENSE)) editor.putString(KEY_EXPENSE, obj.get(KEY_EXPENSE).toString());
            if (obj.has(KEY_LEDGER)) editor.putString(KEY_LEDGER, obj.get(KEY_LEDGER).toString());
            if (obj.has(KEY_SAVINGS)) editor.putString(KEY_SAVINGS, obj.get(KEY_SAVINGS).toString());
            if (obj.has(KEY_NOTES)) editor.putString(KEY_NOTES, obj.get(KEY_NOTES).toString());
            editor.apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ═══════════════════════════════════════════
    //  HELPER METHODS
    // ═══════════════════════════════════════════
    public static String generateId() {
        return Long.toString(System.currentTimeMillis(), 36)
                + Integer.toString(new Random().nextInt(Integer.MAX_VALUE), 36);
    }

    public static String nowIso() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                .format(new Date());
    }

    public static String nowDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public static String nowTime() {
        return new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
    }

    public static String formatDateDisplay(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "--";
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr);
            if (d == null) return dateStr;
            return new SimpleDateFormat("dd-MM-yy", Locale.US).format(d);
        } catch (Exception e) {
            return dateStr;
        }
    }

    public static String formatTimeDisplay(String time24) {
        if (time24 == null || time24.isEmpty()) return "--";
        try {
            String[] parts = time24.split(":");
            int h = Integer.parseInt(parts[0]);
            String m = parts.length > 1 ? parts[1] : "00";
            String ap = h >= 12 ? "PM" : "AM";
            h = h % 12;
            if (h == 0) h = 12;
            return h + ":" + m + " " + ap;
        } catch (Exception e) {
            return time24;
        }
    }

    public static String formatAmount(double amount) {
        long rounded = Math.round(amount);
        if (rounded == 0) return "৳ 0";
        return "৳ " + String.format(Locale.US, "%,d", rounded);
    }

    public static String getGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 12) return "সুপ্রভাত ";
        if (hour >= 12 && hour < 17) return "শুভ দুপুর ";
        if (hour >= 17 && hour < 20) return "শুভ বিকেল ";
        return "শুভ রাত্রি ";
    }

    // Recent transactions (last 5)
    public List<Transaction> getRecentIncome(int limit) {
        List<Transaction> list = getIncomeList();
        return list.subList(0, Math.min(limit, list.size()));
    }

    public List<Transaction> getRecentExpense(int limit) {
        List<Transaction> list = getExpenseList();
        return list.subList(0, Math.min(limit, list.size()));
    }

    public List<LedgerEntry> getRecentLedger(int limit) {
        List<LedgerEntry> list = getLedgerList();
        return list.subList(0, Math.min(limit, list.size()));
    }

    // Filter by month
    public List<Transaction> getIncomeByMonth(int year, int month) {
        return filterByMonth(getIncomeList(), year, month);
    }

    public List<Transaction> getExpenseByMonth(int year, int month) {
        return filterByMonth(getExpenseList(), year, month);
    }

    public List<Transaction> getSavingsByMonth(int year, int month) {
        return filterByMonth(getSavingsList(), year, month);
    }

    private List<Transaction> filterByMonth(List<Transaction> list, int year, int month) {
        String prefix = year + "-" + String.format(Locale.US, "%02d", month);
        List<Transaction> result = new ArrayList<>();
        for (Transaction t : list) {
            if (t.getDate().startsWith(prefix)) result.add(t);
        }
        return result;
    }

    // Calculate financial health score (0-100)
    public int calcHealthScore() {
        double income = getTotalIncome();
        double expense = getTotalExpense();
        double savings = getTotalSavings();
        if (income == 0) return 0;
        double savingsRate = (savings / income) * 100;
        double expenseRate = (expense / income) * 100;
        int score = 50;
        if (savingsRate >= 20) score += 30;
        else if (savingsRate >= 10) score += 15;
        else if (savingsRate > 0) score += 5;
        if (expenseRate <= 50) score += 20;
        else if (expenseRate <= 70) score += 10;
        else if (expenseRate <= 90) score += 5;
        else score -= 10;
        return Math.max(0, Math.min(100, score));
    }

    // ═══════════════════════════════════════════
    //  APP LOCK — encrypted storage (see SecureLockStore)
    // ═══════════════════════════════════════════
    // lock types
    public static final String LOCK_PIN     = SecureLockStore.LOCK_PIN;
    public static final String LOCK_PATTERN = SecureLockStore.LOCK_PATTERN;
    // ৫টা ফিক্সড সিকিউরিটি প্রশ্ন — index 0..4 (PIN/প্যাটার্ন রিসেটের জন্য ব্যবহৃত হয়)
    public static final String[] SECURITY_QUESTIONS = SecureLockStore.SECURITY_QUESTIONS;

    /**
     * App Lock ও সিকিউরিটি প্রশ্নের সব ডাটা EncryptedSharedPreferences-এ (Android
     * Keystore-backed AES-256) আলাদাভাবে রাখা হয় — বাকি অ্যাপ ডাটার (income/expense/...)
     * সাধারণ SharedPreferences থেকে সম্পূর্ণ আলাদা ফাইলে, যাতে sign-in/sign-out বা
     * restore করলেও এটা কখনো মুছে না যায়। শুধু uninstall বা "Clear app data"-তেই মুছবে।
     */
    private SecureLockStore lockStore(Context context) { return SecureLockStore.getInstance(context); }

    public boolean isLockEnabled()  { return lockStore(appContext).isLockEnabled(); }
    public String  getLockType()    { return lockStore(appContext).getLockType(); }
    public String  getLockSecret()  { return lockStore(appContext).getLockSecret(); }
    public boolean isFingerprintEnabled() { return lockStore(appContext).isFingerprintEnabled(); }

    public void setLockEnabled(boolean v)  { lockStore(appContext).setLockEnabled(v); }
    public void setLockType(String t)      { lockStore(appContext).setLockType(t); }
    public void setLockSecret(String h)    { lockStore(appContext).setLockSecret(h); }
    public void setFingerprintEnabled(boolean v) { lockStore(appContext).setFingerprintEnabled(v); }

    /** PIN/প্যাটার্নের হ্যাশ (কখনো plain-text না) + ফিঙ্গারপ্রিন্ট সেটিং এনক্রিপ্টেড স্টোরে সেভ করে। */
    public void saveLock(String type, String hashedSecret, boolean fingerprint) {
        lockStore(appContext).saveLock(type, hashedSecret, fingerprint);
    }

    public void disableLock() {
        lockStore(appContext).disableLock();
    }

    /** ৫টা প্রশ্নের হ্যাশ করা উত্তর এনক্রিপ্টেড স্টোরে সেভ করে (এখানেই normalize/hash হয়)। */
    public void saveSecurityAnswers(String[] answers) {
        lockStore(appContext).saveSecurityAnswers(answers);
    }

    public boolean hasSecurityQuestions() {
        return lockStore(appContext).hasSecurityQuestions();
    }

    /** input: প্রশ্নের index -> ইউজারের দেওয়া উত্তর। true হবে শুধু যদি সবগুলো মিলে যায়। */
    public boolean verifySecurityAnswers(java.util.Map<Integer, String> givenAnswers) {
        return lockStore(appContext).verifySecurityAnswers(givenAnswers);
    }

    // ── নতুন App Lock wizard: শুধু ONE সিকিউরিটি প্রশ্ন (Never ask 3 questions) ──
    public void saveSecurityQuestion(int questionIndex, String answer) {
        lockStore(appContext).saveSecurityQuestion(questionIndex, answer);
    }

    public boolean hasSecurityQuestion() {
        return lockStore(appContext).hasSecurityQuestion();
    }

    public int getSecurityQuestionIndex() {
        return lockStore(appContext).getSecurityQuestionIndex();
    }

    public boolean verifySecurityAnswer(String givenAnswer) {
        return lockStore(appContext).verifySecurityAnswer(givenAnswer);
    }

    public void clearSecurityQuestion() {
        lockStore(appContext).clearSecurityQuestion();
    }

    // ═══════════════════════════════════════════
    //  GOOGLE ACCOUNT PREFERENCES
    // ═══════════════════════════════════════════
    private static final String KEY_GOOGLE_SIGNED_IN   = "_google_signed_in";
    private static final String KEY_GOOGLE_DISPLAY_NAME= "_google_display_name";
    private static final String KEY_GOOGLE_EMAIL       = "_google_email";
    private static final String KEY_GOOGLE_PHOTO_URL   = "_google_photo_url";

    public boolean isGoogleSignedIn()   { return prefs.getBoolean(KEY_GOOGLE_SIGNED_IN, false); }
    public String  getGoogleName()      { return prefs.getString(KEY_GOOGLE_DISPLAY_NAME, ""); }
    public String  getGoogleEmail()     { return prefs.getString(KEY_GOOGLE_EMAIL, ""); }
    public String  getGooglePhotoUrl()  { return prefs.getString(KEY_GOOGLE_PHOTO_URL, ""); }

    public void saveGoogleAccount(String name, String email, String photoUrl) {
        prefs.edit()
            .putBoolean(KEY_GOOGLE_SIGNED_IN, true)
            .putString(KEY_GOOGLE_DISPLAY_NAME, name)
            .putString(KEY_GOOGLE_EMAIL, email)
            .putString(KEY_GOOGLE_PHOTO_URL, photoUrl)
            .apply();
    }

    public void clearGoogleAccount() {
        prefs.edit()
            .putBoolean(KEY_GOOGLE_SIGNED_IN, false)
            .putString(KEY_GOOGLE_DISPLAY_NAME, "")
            .putString(KEY_GOOGLE_EMAIL, "")
            .putString(KEY_GOOGLE_PHOTO_URL, "")
            .apply();
    }

    // ═══════════════════════════════════════════
    //  LOGIN / ONBOARDING STATE
    // ═══════════════════════════════════════════
    private static final String KEY_LOGIN_DONE = "_login_done";

    /** false = প্রথমবার → LoginActivity দেখাও */
    public boolean isLoginDone() {
        return prefs.getBoolean(KEY_LOGIN_DONE, false);
    }

    /** Login বা Skip সম্পন্ন হলে true সেট করুন */
    public void setLoginDone(boolean done) {
        prefs.edit().putBoolean(KEY_LOGIN_DONE, done).apply();
    }


    // ═══════════════════════════════════════════
    //  LOCAL PROFILE (custom name + photo path)
    // ═══════════════════════════════════════════
    private static final String KEY_LOCAL_PHOTO_PATH = "_local_profile_photo";
    private static final String KEY_CUSTOM_NAME      = "_custom_profile_name";

    /** Local storage থেকে profile photo path */
    public String getLocalPhotoPath() {
        return prefs.getString(KEY_LOCAL_PHOTO_PATH, "");
    }

    /** Local profile photo path সেভ */
    public void saveLocalPhotoPath(String path) {
        prefs.edit().putString(KEY_LOCAL_PHOTO_PATH, path).apply();
    }

    /** Custom name (Google name override) */
    public String getCustomName() {
        return prefs.getString(KEY_CUSTOM_NAME, "");
    }

    /** Custom profile name সেভ */
    public void saveCustomName(String name) {
        prefs.edit().putString(KEY_CUSTOM_NAME, name).apply();
        // getUserName()-এও সেভ করি consistency-র জন্য
        saveUserName(name);
    }

    /**
     * Profile-এ দেখানোর জন্য effective নাম:
     * Custom name → Google name → "ব্যবহারকারী"
     */
    public String getDisplayName() {
        String custom = getCustomName();
        if (custom != null && !custom.isEmpty()) return custom;
        String google = getGoogleName();
        if (google != null && !google.isEmpty()) return google;
        String user = getUserName();
        if (user != null && !user.isEmpty()) return user;
        return "ব্যবহারকারী";
    }

    /**
     * Profile photo URL/path (Google URL বা local path):
     * Local photo → Google photo URL → ""
     */
    public String getEffectivePhotoSource() {
        String local = getLocalPhotoPath();
        if (local != null && !local.isEmpty()) return local;
        return getGooglePhotoUrl();
    }


    // ═══════════════════════════════════════════
    //  PROFILE — USERNAME / PHONE / EMAIL
    // ═══════════════════════════════════════════
    private static final String KEY_PROFILE_USERNAME = "_profile_username";
    private static final String KEY_PROFILE_PHONE     = "_profile_phone";
    private static final String KEY_PROFILE_EMAIL     = "_profile_email";
    // 🔑 আলাদা explicit flag — email/phone দিয়ে সত্যিকারের Firebase Auth সাইন-ইন
    // করা আছে কিনা তা এখন এই flag দিয়ে ঠিক হয়, username/phone/email এর মান খালি
    // কিনা তা দিয়ে না। Google দিয়ে সাইন-ইন করা user-এর ক্ষেত্রেও (আগে থেকে একই
    // Gmail-এ username/phone সহ অ্যাকাউন্ট থাকলে) প্রোফাইল দেখানোর জন্য এই
    // ফিল্ডগুলো fill করা লাগে — সেটা করলে যেন ভুলবশত "phone/email সাইন-ইন"
    // অংশ UI-তে দেখানো শুরু না হয়ে যায়, তাই এই আলাদা flag।
    private static final String KEY_EMAIL_PHONE_AUTH_ACTIVE = "_email_phone_auth_active";

    public String getUsername() { return prefs.getString(KEY_PROFILE_USERNAME, ""); }
    public void saveUsername(String username) {
        prefs.edit().putString(KEY_PROFILE_USERNAME, username == null ? "" : username).apply();
    }

    /** সাইন-ইনের সময় সেট হওয়া ফোন নম্বর (email/phone login/sign-up থেকে, অথবা Google-লিংকড অ্যাকাউন্টের ফোন) */
    public String getPhoneNumber() { return prefs.getString(KEY_PROFILE_PHONE, ""); }
    public void savePhoneNumber(String phone) {
        prefs.edit().putString(KEY_PROFILE_PHONE, phone == null ? "" : phone).apply();
    }

    /** সাইন-ইনের সময় সেট হওয়া real email (Google email থেকে আলাদা, phone-pseudo email নয়) */
    public String getUserEmail() { return prefs.getString(KEY_PROFILE_EMAIL, ""); }
    public void saveUserEmail(String email) {
        prefs.edit().putString(KEY_PROFILE_EMAIL, email == null ? "" : email).apply();
    }

    /** নম্বর/ইমেইল/ইউজারনেম দিয়ে সত্যিকারের Firebase Auth সাইন-ইন করা আছে কিনা */
    public boolean isEmailPhoneSignedIn() {
        return prefs.getBoolean(KEY_EMAIL_PHONE_AUTH_ACTIVE, false);
    }

    /** Email/Phone/Username দিয়ে লগইন সম্পন্ন হওয়ার পরে call করুন (EmailLoginActivity) */
    public void setEmailPhoneSignedIn(boolean active) {
        prefs.edit().putBoolean(KEY_EMAIL_PHONE_AUTH_ACTIVE, active).apply();
    }

    public void clearEmailPhoneAccount() {
        prefs.edit()
            .putString(KEY_PROFILE_PHONE, "")
            .putString(KEY_PROFILE_EMAIL, "")
            .putString(KEY_PROFILE_USERNAME, "")
            .putBoolean(KEY_EMAIL_PHONE_AUTH_ACTIVE, false)
            .apply();
    }

    // ═══════════════════════════════════════════
    //  GOOGLE DRIVE SYNC STATE
    // ═══════════════════════════════════════════
    private static final String KEY_DRIVE_LAST_SYNC = "_drive_last_sync_time";
    private static final String KEY_DRIVE_AUTO_SYNC  = "_drive_auto_sync_enabled";

    public long getLastDriveSyncTime() {
        return prefs.getLong(KEY_DRIVE_LAST_SYNC, 0L);
    }

    public void saveLastDriveSyncTime(long time) {
        prefs.edit().putLong(KEY_DRIVE_LAST_SYNC, time).apply();
    }

    public boolean isDriveAutoSyncEnabled() {
        return prefs.getBoolean(KEY_DRIVE_AUTO_SYNC, true);
    }

    public void setDriveAutoSyncEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_DRIVE_AUTO_SYNC, enabled).apply();
    }

}
