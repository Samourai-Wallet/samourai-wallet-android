package com.samourai.wallet.permissions;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import com.samourai.wallet.R;

public class PermissionsUtil {

    public final static int READ_WRITE_EXTERNAL_PERMISSION_CODE = 0;
    public static final int SMS_PERMISSION_CODE = 1;
    public static final int OUTGOING_CALL_PERMISSION_CODE = 2;
    public static final int CAMERA_PERMISSION_CODE = 3;

    private static PermissionsUtil instance = null;
    private static Context context = null;

    private PermissionsUtil()   { ; }

    public static PermissionsUtil getInstance(Context ctx) {

        context = ctx;

        if(instance == null)    {
            instance = new PermissionsUtil();
        }

        return instance;
    }

    public boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public void showRequestPermissionsInfoAlertDialog(final int code) {

        String title = "";
        String message = "";

        switch(code)    {
            case READ_WRITE_EXTERNAL_PERMISSION_CODE:
                title = context.getString(R.string.permission_alert_dialog_title_external);
                message = context.getString(R.string.permission_dialog_message_external);
                break;
            case SMS_PERMISSION_CODE:
                title = context.getString(R.string.permission_alert_dialog_title_sms);
                message = context.getString(R.string.permission_dialog_message_sms);
                break;
            case OUTGOING_CALL_PERMISSION_CODE:
                title = context.getString(R.string.permission_alert_dialog_title_outgoing);
                message = context.getString(R.string.permission_dialog_message_outgoing);
                break;
            case CAMERA_PERMISSION_CODE:
                title = context.getString(R.string.permission_alert_dialog_title_camera);
                message = context.getString(R.string.permission_dialog_message_camera);
                break;
            default:
                break;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setCancelable(false);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                switch(code)    {
                    case READ_WRITE_EXTERNAL_PERMISSION_CODE:
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, READ_WRITE_EXTERNAL_PERMISSION_CODE);
                        break;
                    case SMS_PERMISSION_CODE:
                        requestPermissions(new String[]{Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_PHONE_STATE}, SMS_PERMISSION_CODE);
                        break;
                    case OUTGOING_CALL_PERMISSION_CODE:
                        requestPermissions(new String[]{Manifest.permission.PROCESS_OUTGOING_CALLS}, OUTGOING_CALL_PERMISSION_CODE);
                        break;
                    case CAMERA_PERMISSION_CODE:
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
                        break;
                    default:
                        break;
                }

                dialog.dismiss();

            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        if(!((Activity)context).isFinishing())    {
            builder.show();
        }

    }

    private void requestPermissions(String[] permissions, int code) {

        for(int i = 0; i < permissions.length; i++)   {
            if (ActivityCompat.shouldShowRequestPermissionRationale((Activity)context, permissions[i])) {
                Log.d("PermissionsUtil", "shouldShowRequestPermissionRationale(), no permission requested");
            }
            else    {
                ActivityCompat.requestPermissions((Activity)context, permissions, code);
                break;
            }
        }

    }

}
