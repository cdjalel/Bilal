package com.djalel.android.bilal.helpers;

import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.djalel.android.bilal.R;

public class PermissionsDialog {
    private final AppCompatActivity mContext;

    private final ActivityResultLauncher<Intent> mAskNotifPermissionActivityLauncher;
    private final ActivityResultLauncher<Intent> mAskAlarmPermissionActivityLauncher;
    private final ActivityResultLauncher<Intent> mAskBatteryExemptionActivityLauncher;

    public PermissionsDialog(AppCompatActivity context,
                             ActivityResultLauncher<Intent> askNotifPermissionActivityLauncher,
                             ActivityResultLauncher<Intent> askAlarmPermissionActivityLauncher,
                             ActivityResultLauncher<Intent> askBatteryExemptionActivityLauncher) {
        mContext = context;
        mAskNotifPermissionActivityLauncher = askNotifPermissionActivityLauncher;
        mAskAlarmPermissionActivityLauncher = askAlarmPermissionActivityLauncher;
        mAskBatteryExemptionActivityLauncher = askBatteryExemptionActivityLauncher;
    }

    private void showMalFunctionToast() {
        String warning = mContext.getString(R.string.permission_warning);
        Toast.makeText(mContext, warning, Toast.LENGTH_SHORT).show();
    }

    public void showPermissionsDialog()
    {
        String title = mContext.getString(R.string.permission_title);
        String message = mContext.getString(R.string.permission_message);
        String grant = mContext.getString(R.string.permission_grant);
        String skip = mContext.getString(R.string.permission_skip);

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(message)
                .setTitle(title)
                .setCancelable(false)
                .setPositiveButton(grant, (dialog, id) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                        intent.putExtra(Settings.EXTRA_APP_PACKAGE, mContext.getPackageName());
                        mAskNotifPermissionActivityLauncher.launch(intent);
                    }
                    else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        mAskAlarmPermissionActivityLauncher.launch(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
                    }
                    else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        mAskBatteryExemptionActivityLauncher.launch(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                    }
                })
                .setNegativeButton(skip, (dialog, id) -> {
                    dialog.dismiss();
                    showMalFunctionToast();
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

}
