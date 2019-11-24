package com.samourai.wallet.permissions;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.samourai.wallet.R;

public class PermissionsUtil {

    private Context context;

    private PermissionsUtil(Context context) {
        this.context = context;
    }

    public static PermissionsUtil getInstance(Context context) {
        return new PermissionsUtil(context);
    }

    public boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public void showRequestPermissionsInfoAlertDialog(final SamouraiPermissions code) {

        int title = -1;
        int message = -1;

        switch (code) {
            case READ_WRITE_EXTERNAL_STORAGE:
                title = R.string.permission_alert_dialog_title_external;
                message = R.string.permission_dialog_message_external;
                break;
            case CAMERA:
                title = R.string.permission_alert_dialog_title_camera;
                message = R.string.permission_dialog_message_camera;
                break;
        }


        showDialog(title, message, R.string.ok, (dialog, which) -> {

            switch (code) {
                case READ_WRITE_EXTERNAL_STORAGE:
                    requestPermissions(
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            SamouraiPermissions.READ_WRITE_EXTERNAL_STORAGE.ordinal());
                    break;

                case CAMERA:
                    requestPermissions(
                            new String[]{Manifest.permission.CAMERA},
                            SamouraiPermissions.CAMERA.ordinal());
                    break;
            }
            dialog.dismiss();
        });
    }

    private void showDialog(int titleStringId, int messageStringId, int positiveButtonStringId, DialogInterface.OnClickListener onClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(titleStringId);
        builder.setCancelable(false);
        builder.setMessage(messageStringId);
        builder.setPositiveButton(positiveButtonStringId, onClickListener);
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

        if (!((Activity) context).isFinishing()) {
            builder.show();
        }
    }

    private void requestPermissions(String[] permissions, int code) {

        for (int i = 0; i < permissions.length; i++) {
            if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, permissions[i])) {
                Log.d("PermissionsUtil", "shouldShowRequestPermissionRationale(), no permission requested");
            } else {
                ActivityCompat.requestPermissions((Activity) context, permissions, code);
                break;
            }
        }
    }

    public void showRepeatedCameraPermissionRequestDialog() {
        Activity activity = (Activity) context;
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)) {
            showDialog(
                    R.string.permission_alert_dialog_title_camera_repeated,
                    R.string.permission_dialog_message_camera_repeated,
                    R.string.ok,
                    (dialog, which) -> {
                        dialog.dismiss();
                        ActivityCompat.requestPermissions(
                                (Activity) context,
                                new String[]{Manifest.permission.CAMERA}, SamouraiPermissions.CAMERA.ordinal());
                    }
            );
        } else {
            showDialog(
                    R.string.permission_alert_dialog_title_camera,
                    R.string.permission_dialog_message_camera_repeated_denied_permanently,
                    R.string.permission_dialog_button_camera_go_to_settings,
                    (dialog, which) -> {
                        dialog.dismiss();
                        Intent settingsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                        settingsIntent.setData(uri);
                        activity.startActivityForResult(settingsIntent, SamouraiPermissions.CAMERA.ordinal());
                    }
            );
        }
    }

}
