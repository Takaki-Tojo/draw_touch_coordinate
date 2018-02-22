package com.slpl.drawcoordinate;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class DrawCoordinatesActivity extends Activity {
    private static final int RESULT_PICK_IMAGE_FILE = 1000;
    private DrawLogic mLogic;
    private Mat mImg;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (mImg != null) return;
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    mImg = new Mat();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw);
        mLogic = new DrawLogic(mImg);
        mLogic.importWordData(getResources().getAssets());

        // Android 6, API 23以上でパーミッシンの確認
        if (Build.VERSION.SDK_INT >= 23) {
            if (PermissionUtil.checkPermission(this)) requestImage();
        } else {
            requestImage();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private void requestImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "画像を選択"), RESULT_PICK_IMAGE_FILE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode != RESULT_PICK_IMAGE_FILE || resultCode != RESULT_OK || resultData == null) {
            return;
        }
        if (resultData.getData() != null) {
            // 単体選択
            Uri uri = resultData.getData();
            String fileName = fileNameFrom(uri);
            Bitmap bitmap = bitmapFrom(uri).copy(Bitmap.Config.ARGB_8888, true);// formatをARGB_8888にしないとMatで読み込めないらしい
            bitmap = mLogic.createOutputBitmap(bitmap, fileName);
            ContentValues values = createContentValues(bitmap, fileName);
            saveValuesList(values);
        } else {
            // 複数選択
            ClipData clipData = resultData.getClipData();
            ContentValues[] valuesList = new ContentValues[clipData.getItemCount()];
            for (int i = 0; i < clipData.getItemCount(); i++) {
                Uri uri = clipData.getItemAt(i).getUri();
                String fileName = fileNameFrom(uri);
                Bitmap bitmap = bitmapFrom(uri).copy(Bitmap.Config.ARGB_8888, true);// formatをARGB_8888にしないとMatで読み込めないらしい
                bitmap = mLogic.createOutputBitmap(bitmap, fileName);
                valuesList[i] = createContentValues(bitmap, fileName);
            }
            saveValuesList(valuesList);
        }
    }

    private Bitmap bitmapFrom(Uri uri) {
        InputStream in = null;
        try {
            in = getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return BitmapFactory.decodeStream(in);
    }

    // ContentProviderからUriに紐づくファイル名を取得
    private String fileNameFrom(Uri uri) {
        String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
        Cursor cursor = this.getContentResolver().query(uri, projection, null, null, null);
        if (cursor == null) return null;

        String fileName = null;
        if (cursor.moveToFirst()) {
            fileName = cursor.getString(0);
        }
        cursor.close();
        return fileName;
    }

    private ContentValues createContentValues(Bitmap bitmap, String oldName) {
        File file = new File("sdcard/");
        String fileName = "coo_" + oldName;
        String path = file.getAbsolutePath() + "/" + fileName;
        try {
            FileOutputStream out = new FileOutputStream(path, true);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.TITLE, fileName);
        values.put("_data", path);
        return values;
    }

    private void saveValuesList(ContentValues... valuesList) {
        getContentResolver().bulkInsert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, valuesList);
    }
}
