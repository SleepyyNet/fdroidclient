package org.fdroid.fdroid.views.hiding;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AlertDialog;

import org.fdroid.fdroid.AppUpdateStatusService;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.CleanCacheService;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.data.InstalledAppProviderService;
import org.fdroid.fdroid.installer.InstallHistoryService;
import org.fdroid.fdroid.installer.InstallManagerService;
import org.fdroid.fdroid.installer.InstallerService;
import org.fdroid.fdroid.localrepo.CacheSwapAppsService;
import org.fdroid.fdroid.localrepo.SwapService;
import org.fdroid.fdroid.net.DownloaderService;
import org.fdroid.fdroid.net.WifiStateChangeService;
import org.fdroid.fdroid.views.main.MainActivity;

/**
 * This class is encapsulating all methods related to hiding the app from the launcher
 * and restoring it.
 *
 * It can tell you whether the app is hidden, what the PIN to restore is
 * and show confirmation dialogs before hiding.
 */
public class HidingManager {

    private static final ComponentName LAUNCHER_NAME =
            new ComponentName(BuildConfig.APPLICATION_ID, MainActivity.class.getName());

    private static final ComponentName CALCULATOR_NAME =
            new ComponentName(BuildConfig.APPLICATION_ID, CalculatorActivity.class.getName());

    public static int getUnhidePin(Context context) {
        return context.getResources().getInteger(R.integer.unhidePin);
    }

    public static boolean isHidden(Context context) {
        PackageManager pm = context.getPackageManager();
        int state = pm.getComponentEnabledSetting(LAUNCHER_NAME);
        return state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
    }

    public static void showHideDialog(final Context context) {
        String appName = context.getString(R.string.app_name);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.hiding_dialog_title, appName));
        builder.setMessage(context.getString(R.string.hiding_dialog_message, appName,
                HidingManager.getUnhidePin(context), context.getString(R.string.hiding_calculator)));
        builder.setPositiveButton(context.getString(R.string.panic_hide_title, appName),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        hide(context);
                    }
                });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        builder.setView(R.layout.dialog_app_hiding);
        builder.create().show();
    }

    public static void hide(Context context) {
        stopServices(context);
        removeNotifications(context);

        PackageManager pm = context.getPackageManager();
        // hide launcher icon
        pm.setComponentEnabledSetting(LAUNCHER_NAME,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        // show calculator icon
        pm.setComponentEnabledSetting(CALCULATOR_NAME,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                0); // please kill app (faster and safer, because it also stops services)
    }

    public static void show(Context context) {
        PackageManager pm = context.getPackageManager();
        // show launcher icon
        pm.setComponentEnabledSetting(LAUNCHER_NAME,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
        // hide calculator icon
        pm.setComponentEnabledSetting(CALCULATOR_NAME,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                0); // please kill app (faster)
    }

    private static void removeNotifications(Context context) {
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        nm.cancelAll();
    }

    /**
     * Stops all running services, so nothing can pop up and reveal F-Droid's existence on the system
     */
    private static void stopServices(Context context) {
        context.stopService(new Intent(context, UpdateService.class));
        context.stopService(new Intent(context, DownloaderService.class));
        context.stopService(new Intent(context, InstallerService.class));
        context.stopService(new Intent(context, CleanCacheService.class));
        context.stopService(new Intent(context, WifiStateChangeService.class));
        context.stopService(new Intent(context, SwapService.class));
        context.stopService(new Intent(context, InstallManagerService.class));
        context.stopService(new Intent(context, InstallHistoryService.class));
        context.stopService(new Intent(context, CacheSwapAppsService.class));
        context.stopService(new Intent(context, InstalledAppProviderService.class));
        context.stopService(new Intent(context, AppUpdateStatusService.class));
    }

}