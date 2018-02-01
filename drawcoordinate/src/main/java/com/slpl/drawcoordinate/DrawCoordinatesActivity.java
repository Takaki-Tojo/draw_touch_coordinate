package com.slpl.drawcoordinate;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.slpl.drawcoordinate.model.WordData;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

/**
 * Created by Takaki on 2017/06/03.
 */

public class DrawCoordinatesActivity extends Activity {
    private static final int RESULT_PICK_IMAGE_FILE = 1000;
    private static final int REQUEST_PERMISSION = 1001;
    private Mat mImg;
    private List<WordData> mWordDataList = new ArrayList<>();
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
        importCoordinates();
        // Android 6, API 23以上でパーミッシンの確認
        if (Build.VERSION.SDK_INT >= 23) {
            checkPermission();
        } else {
            request();
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

    private void importCoordinates() {
        // AssetsからCSVファイルの読み込み
        AssetManager assetManager = getResources().getAssets();
        try {
            InputStream is = assetManager.open("WordData_2_A.csv");
            InputStreamReader inputStreamReader = new InputStreamReader(is);
            BufferedReader bufferReader = new BufferedReader(inputStreamReader);
            String line = "";
            while ((line = bufferReader.readLine()) != null) {
                WordData data = new WordData();
                StringTokenizer st = new StringTokenizer(line, ",");
                try {
                    data.setCursorPosition(Integer.parseInt(st.nextToken()));
                    data.setDate(st.nextToken());
                    data.setDeleteCount(Integer.parseInt(st.nextToken()));
                    data.setDeleteWordId(Integer.parseInt(st.nextToken()));
                    data.setDetectedWord(st.nextToken());
                    data.setInputText(st.nextToken());
                    data.setInputTime(Double.parseDouble(st.nextToken()));
                    data.setMissDelete(Integer.parseInt(st.nextToken()));
                    data.setMissTouchX(Integer.parseInt(st.nextToken()));
                    data.setMissTouchY(Integer.parseInt(st.nextToken()));
                    data.setTouchDownX(Integer.parseInt(st.nextToken()));
                    data.setTouchDownY(Integer.parseInt(st.nextToken()));
                    data.setTouchUpX(Integer.parseInt(st.nextToken()));
                    data.setTouchUpY(Integer.parseInt(st.nextToken()));
                    data.setWordId(Integer.parseInt(st.nextToken()));
                    mWordDataList.add(data);
                } catch (RuntimeException e) {
                    Log.e(getLocalClassName(), "代入できないパラメータです");
                }
            }
            bufferReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // permissionの確認
    public void checkPermission() {
        // 既に許可している
        if (ActivityCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            request();
        } else { // 拒否していた場合
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, WRITE_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(DrawCoordinatesActivity.this, new String[]{WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
            } else {
                Toast.makeText(this, "許可してください", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE,}, REQUEST_PERMISSION);
            }
        }
    }

    private void request() {
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
            Bitmap bitmap = createOutputBitmap(uri, fileName);
            ContentValues values = createContentValues(bitmap, fileName);
            saveValuesList(values);
        } else {
            // 複数選択
            ClipData clipData = resultData.getClipData();
            ContentValues[] valuesList = new ContentValues[clipData.getItemCount()];
            for (int i = 0; i < clipData.getItemCount(); i++) {
                Uri uri = clipData.getItemAt(i).getUri();
                String fileName = fileNameFrom(uri);
                Bitmap bitmap = createOutputBitmap(uri, fileName);
                valuesList[i] = createContentValues(bitmap, fileName);
            }
            saveValuesList(valuesList);
        }
    }

    private ContentValues createContentValues(Bitmap bitmap, String oldName) {
        File file = new File("sdcard/");
        String fileName = "coo_2_" + oldName;
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

    private Bitmap createOutputBitmap(Uri uri, String fileName) {
        Bitmap bitmap = bitmapFrom(uri).copy(Bitmap.Config.ARGB_8888, true);// formatをARGB_8888にしないとMatで読み込めないらしい
        Utils.bitmapToMat(bitmap, mImg);
        fileName = fileName.replace(".png", "").replace(".jpg", "");
        for (WordData coo : mWordDataList) {
            if (coo.getDetectedWord().equals(fileName)) {
                plotCoordinate(mImg, coo);
            }
        }
        Utils.matToBitmap(mImg, bitmap);
        return bitmap;
    }

    private void plotCoordinate(Mat img, WordData coo) {
        // TODO カーソルを移動させて削除し、またカーソルを移動して文字を入力した場合の処理が必要。カーソルの位置の比較でいける。
        // TODO 削除した後に、修正後の文字が間違えていた場合の処理が必要。
        // 指をあげたときの座標
        float uX = coo.getTouchUpX();
        float uY = coo.getTouchUpY();
        Imgproc.rectangle(img, new Point(uX - 0.5, uY - 0.5), new Point(uX + 0.5, uY + 0.5), new Scalar(0, 255, 255, 255), 2);

        // 文字を押したときの座標
        float dX = coo.getTouchDownX();
        float dY = coo.getTouchDownY();
        Imgproc.rectangle(img, new Point(dX - 0.5, dY - 0.5), new Point(dX + 0.5, dY + 0.5), new Scalar(255, 255, 0, 255), 2);

        // 誤って押されたときの座標
        float mX = coo.getMissTouchX();
        float mY = coo.getMissTouchY();
        if (mX == 0.0 || mY == 0.0) return; // ミスタッチがない
        Imgproc.rectangle(img, new Point(mX - 0.5, mY - 0.5), new Point(mX + 0.5, mY + 0.5), new Scalar(255, 0, 0, 255), 2);
    }
}
