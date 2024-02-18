package com.djalel.android.bilal.helpers;

import static android.content.Context.ALARM_SERVICE;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.djalel.android.bilal.R;

import timber.log.Timber;

public class PermissionsDialog {
    private final AppCompatActivity mContext;
    private final ActivityResultLauncher<Intent> mAskAlarmPermissionActivityLauncher;
    private ActivityResultLauncher<Intent> mAskBatteryExemptionActivityLauncher;

    public PermissionsDialog(AppCompatActivity context) {
        mContext = context;

        mAskAlarmPermissionActivityLauncher = context.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Timber.d("AskAlarmPermission onActivityResult");
                    if  (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(ALARM_SERVICE);
                        if (null == alarmMgr || !alarmMgr.canScheduleExactAlarms()) {
                            showMalFunctionToast();
                            return;
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        mAskBatteryExemptionActivityLauncher.launch(
                                new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                    }
                }
        );

        mAskBatteryExemptionActivityLauncher = context.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Timber.d("AskBatteryExemption onActivityResult");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                        if (null == powerManager || !powerManager.isIgnoringBatteryOptimizations(context.getPackageName())) {
                            showMalFunctionToast();
                        }
                    }
                }
        );

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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
