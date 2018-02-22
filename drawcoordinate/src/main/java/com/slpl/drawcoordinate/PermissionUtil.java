package com.slpl.drawcoordinate;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

class PermissionUtil {
    private static final int REQUEST_PERMISSION = 1001;

    // permissionの確認
    static boolean checkPermission(Activity activity) {
        Context context = activity.getBaseContext();

        // 既に許可している
        if (ActivityCompat.checkSelfPermission(context, WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else { // 拒否していた場合
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, WRITE_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(activity, new String[]{WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
            } else {
                Toast.makeText(context, "許可してください", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(activity, new String[]{WRITE_EXTERNAL_STORAGE,}, REQUEST_PERMISSION);
            }
            return false;
        }
    }
}
