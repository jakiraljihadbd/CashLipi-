# Auto Backup কিভাবে ব্যবহার করবেন

## Income যোগের পর (AddIncomeActivity.java)
```java
// Save এর পর এই লাইন যোগ করুন:
BackupActivity.triggerAutoBackup(this, "income");
```

## Expense যোগের পর (AddExpenseActivity.java)
```java
BackupActivity.triggerAutoBackup(this, "expense");
```

## Ledger যোগের পর (AddLedgerActivity.java)
```java
BackupActivity.triggerAutoBackup(this, "debt");
```

## সেভ, আপডেট, ডিলিট এর পর
```java
BackupActivity.triggerAutoBackup(this, "update");
BackupActivity.triggerAutoBackup(this, "delete");
```

## Backup হবে:
- ✅ Downloads/CashLipi/CashLipi_AUTO_yyyyMMdd_HHmmss.json
- ✅ Telegram (যদি Bot Token set থাকে)
