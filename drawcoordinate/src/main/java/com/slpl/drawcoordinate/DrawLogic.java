package com.slpl.drawcoordinate;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

import com.slpl.drawcoordinate.model.WordData;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

class DrawLogic {
    private List<WordData> mWordDataList = new ArrayList<>();
    private Mat mImg;

    DrawLogic(Mat img) {
        mImg = img;
    }

    void importWordData(AssetManager assetManager) {
        // AssetsからCSVファイルの読み込み
        try {
            InputStream is = assetManager.open("WordData_2_A.csv");
            InputStreamReader inputStreamReader = new InputStreamReader(is);
            BufferedReader bufferReader = new BufferedReader(inputStreamReader);
            String line;
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
                    throw new RuntimeException(e);
                }
            }
            bufferReader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    Bitmap createOutputBitmap(Bitmap bitmap, String fileName) {
        Utils.bitmapToMat(bitmap, mImg);
        fileName = fileName.replace(".png", "").replace(".jpg", "");
        for (WordData coo : mWordDataList) {
            if (coo.getDetectedWord().equals(fileName)) {
                plotCoordinate(coo);
            }
        }
        Utils.matToBitmap(mImg, bitmap);
        return bitmap;
    }

    private void plotCoordinate(WordData coo) {
        // TODO カーソルを移動させて削除し、またカーソルを移動して文字を入力した場合の処理が必要。カーソルの位置の比較でいける。
        // TODO 削除した後に、修正後の文字が間違えていた場合の処理が必要。
        plot(coo.getTouchUpX(), coo.getTouchUpY(), 0, 255, 255, 255);     // 指をあげたときの座標
        plot(coo.getTouchDownX(), coo.getTouchDownY(), 255, 255, 0, 255); // 文字を押したときの座標
        plot(coo.getMissTouchX(), coo.getMissTouchY(), 255, 0, 255, 255); // 誤って押されたときの座標
    }

    private void plot(float x, float y, int alpha, int red, int green, int blue) {
        if (x == 0.0 || y == 0.0) return;
        Imgproc.rectangle(mImg, new Point(x - 0.5, y - 0.5), new Point(x + 0.5, y + 0.5), new Scalar(alpha, red, green, blue), 2);
    }
}
