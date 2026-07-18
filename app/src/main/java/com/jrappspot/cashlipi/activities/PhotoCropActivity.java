package com.jrappspot.cashlipi.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.jrappspot.cashlipi.R;
import com.jrappspot.cashlipi.views.CropImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * গ্যালারি থেকে বাছাই করা ছবিকে বৃত্তাকার ফ্রেমের ভেতর টেনে/জুম করে ক্রপ করার স্ক্রিন।
 * ফলাফল হিসেবে internal storage-এ সেভ করা ছবির ফাইল-path রিটার্ন করা হয়।
 */
public class PhotoCropActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URI = "extra_image_uri";
    public static final String EXTRA_RESULT_PATH = "extra_result_path";
    private static final int OUTPUT_SIZE_PX = 480;
    private static final String TAG = "PhotoCropActivity";

    private CropImageView cropView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_crop);

        cropView = findViewById(R.id.cropView);
        findViewById(R.id.btnCropCancel).setOnClickListener(v -> { setResult(RESULT_CANCELED); finish(); });
        findViewById(R.id.btnCropSave).setOnClickListener(v -> saveCrop());

        String uriString = getIntent().getStringExtra(EXTRA_IMAGE_URI);
        if (uriString == null) { finish(); return; }

        Bitmap bitmap = decodeSampledAndRotated(Uri.parse(uriString), 1024);
        if (bitmap == null) {
            Toast.makeText(this, "ছবি লোড করা যায়নি", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        cropView.setImageBitmap(bitmap);
    }

    private void saveCrop() {
        Bitmap cropped = cropView.getCroppedBitmap(OUTPUT_SIZE_PX);
        if (cropped == null) { finish(); return; }

        File dir = new File(getFilesDir(), "person_photos");
        if (!dir.exists()) dir.mkdirs();
        File outFile = new File(dir, "person_" + System.currentTimeMillis() + ".jpg");

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            cropped.compress(Bitmap.CompressFormat.JPEG, 92, fos);
        } catch (Exception e) {
            Log.e(TAG, "crop save failed", e);
            Toast.makeText(this, "ছবি সেইভ করা যায়নি", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Intent result = new Intent();
        result.putExtra(EXTRA_RESULT_PATH, outFile.getAbsolutePath());
        setResult(RESULT_OK, result);
        finish();
    }

    /** মেমরি সাশ্রয়ে ডাউন-স্যাম্পল করে এবং EXIF অনুযায়ী সঠিক দিকে ঘুরিয়ে বিটম্যাপ ডিকোড করে। */
    @Nullable
    private Bitmap decodeSampledAndRotated(Uri uri, int reqSize) {
        try {
            // আগে EXIF পড়ার জন্য একটা টেম্প ফাইলে কপি করি (ContentResolver Uri থেকে সরাসরি path পাওয়া যায় না)
            File tempFile = File.createTempFile("pick_", ".jpg", getCacheDir());
            try (InputStream in = getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(tempFile)) {
                if (in == null) return null;
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) out.write(buffer, 0, len);
            }

            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(tempFile.getAbsolutePath(), bounds);

            int sample = 1;
            int halfW = bounds.outWidth / 2;
            int halfH = bounds.outHeight / 2;
            while ((halfW / sample) >= reqSize && (halfH / sample) >= reqSize) sample *= 2;

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            Bitmap decoded = BitmapFactory.decodeFile(tempFile.getAbsolutePath(), opts);
            if (decoded == null) return null;

            int rotation = readExifRotation(tempFile.getAbsolutePath());
            tempFile.delete();
            if (rotation == 0) return decoded;

            Matrix m = new Matrix();
            m.postRotate(rotation);
            Bitmap rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.getWidth(), decoded.getHeight(), m, true);
            if (rotated != decoded) decoded.recycle();
            return rotated;
        } catch (Exception e) {
            Log.e(TAG, "decode failed", e);
            return null;
        }
    }

    private int readExifRotation(String path) {
        try {
            ExifInterface exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90: return 90;
                case ExifInterface.ORIENTATION_ROTATE_180: return 180;
                case ExifInterface.ORIENTATION_ROTATE_270: return 270;
                default: return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }
}
