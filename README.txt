=== IMPORTANT: এই ফাইলগুলো DELETE করুন (যদি থাকে) ===

মুছে ফেলুন এই folder/file গুলো:
- app/src/main/res/mipmap-anydpi-v26/  (পুরো ফোল্ডার ডিলিট করুন)
- app/src/main/res/mipmap-*/ic_launcher.webp  (যদি থাকে .webp ফাইল)
- app/src/main/res/mipmap-*/ic_launcher_round.webp

কারণ: নতুন icon .png ফরম্যাটে দেওয়া হয়েছে, পুরনো .webp/adaptive icon
থাকলে Android পুরনোটাই ব্যবহার করতে পারে।

=== BUILD স্টেপ ===
1. ফোন থেকে আগের app সম্পূর্ণ UNINSTALL করুন
2. উপরের ফাইলগুলো প্রজেক্টে replace করুন
3. Android Studio: Build → Clean Project
4. Build → Rebuild Project
5. Run ▶️

=== এই ZIP এ যা আছে ===
- activity_dashboard.xml (fixed margin bug)
- activity_splash.xml (নতুন লোগো + animation)
- splash_logo_animation.xml (rotate + zoom pulse, infinite loop)
- logo_splash.png (transparent bg লোগো — splash এর জন্য)
- SplashActivity.java (animation চালু করার কোড যুক্ত)
- mipmap-*/ic_launcher.png ও ic_launcher_round.png (নতুন app icon — কালো bg সহ)