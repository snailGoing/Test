package com.sprd.generalsecurity.battery;

import java.util.ArrayList;
import java.util.HashMap;

import com.android.internal.os.BatterySipper;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserManager;
import android.util.Log;
import android.content.pm.UserInfo;

import android.app.AppGlobals;
import android.os.UserHandle;

import com.sprd.generalsecurity.R;

public class BatteryEntry {
    private static final String TAG = "BatteryEntry";
    public static final int MSG_UPDATE_NAME_ICON = 1;
    public static final int MSG_REPORT_FULLY_DRAWN = 2;

    public final Context context;
    public final BatterySipper sipper;

    public String name;
    public Drawable icon;
    public int iconId; // For passing to the detail screen.
    public String defaultPackageName;

    public static final HashMap<String,UidToDetail> sUidCache = new HashMap<String,UidToDetail>();

    public static final ArrayList<BatteryEntry> mRequestQueue = new ArrayList<BatteryEntry>();
    public static Handler sHandler;

    static private class NameAndIconLoader extends Thread {
        private boolean mAbort = false;

        public NameAndIconLoader() {
            super("BatteryUsage Icon Loader");
        }

        public void abort() {
            mAbort = true;
        }

        @Override
        public void run() {
            while (true) {
                BatteryEntry be;
                synchronized (mRequestQueue) {
                    if (mRequestQueue.isEmpty() || mAbort) {
                        if (sHandler != null) {
                            sHandler.sendEmptyMessage(MSG_REPORT_FULLY_DRAWN);
                        }
                        mRequestQueue.clear();
                        return;
                    }
                    be = mRequestQueue.remove(0);
                }
                be.loadNameAndIcon();
            }
        }
    }

    private static NameAndIconLoader mRequestThread;

    public static void startRequestQueue() {
        if (sHandler != null) {
            synchronized (mRequestQueue) {
                if (!mRequestQueue.isEmpty()) {
                    if (mRequestThread != null) {
                        mRequestThread.abort();
                    }
                    mRequestThread = new NameAndIconLoader();
                    mRequestThread.setPriority(Thread.MIN_PRIORITY);
                    mRequestThread.start();
                    mRequestQueue.notify();
                }
            }
        }
    }

    public static void stopRequestQueue() {
        synchronized (mRequestQueue) {
            if (mRequestThread != null) {
                mRequestThread.abort();
                mRequestThread = null;
                sHandler = null;
            }
        }
    }

    public static void clearUidCache() {
        sUidCache.clear();
    }

    static class UidToDetail {
        String name;
        String packageName;
        Drawable icon;
    }

    public BatteryEntry(Context context, Handler handler, UserManager um, BatterySipper sipper) {
        sHandler = handler;
        this.context = context;
        this.sipper = sipper;

        switch (sipper.drainType) {
            case IDLE:
                name =  context.getResources().getString(R.string.power_idle);
                iconId = R.drawable.ic_settings_phone_idle;
                break;
            case CELL:
                name =  context.getResources().getString(R.string.power_cell);
                iconId = R.drawable.ic_settings_cell_standby;
                break;
            case PHONE:
                name =  context.getResources().getString(R.string.power_phone);
                iconId = R.drawable.ic_settings_voice_calls;
                break;
            case WIFI:
                name =  context.getResources().getString(R.string.power_wifi);
                iconId = R.drawable.ic_settings_wireless_alpha;
                break;
            case BLUETOOTH:
                name =  context.getResources().getString(R.string.power_bluetooth);
                iconId = R.drawable.ic_settings_bluetooth;
                break;
            case SCREEN:
                name =  context.getResources().getString(R.string.power_screen);
                iconId = R.drawable.ic_settings_display;
                break;
            case FLASHLIGHT:
                name =  context.getResources().getString(R.string.power_flashlight);
                iconId = R.drawable.ic_settings_display;
                break;
            case APP:
                name = sipper.packageWithHighestDrain;
                break;
            case USER:
            break;
            case UNACCOUNTED:
                name =  context.getResources().getString(R.string.power_unaccounted);
                iconId = R.drawable.ic_power_system;
                break;
            case OVERCOUNTED:
                name =  context.getResources().getString(R.string.power_overcounted);
                iconId = R.drawable.ic_power_system;
                break;
            case CAMERA:
                name =  context.getResources().getString(R.string.power_camera);
                iconId = R.drawable.ic_settings_camera;
                break;
        }
        if (iconId > 0) {
            icon = context.getDrawable(iconId);
        }
        if ((name == null || iconId == 0) && this.sipper.uidObj != null) {
            Log.i(TAG, "iconId>>"+iconId);
            //name = null
            getQuickNameIconForUid(this.sipper.uidObj.getUid());
        }
    }

    public int getIconId() {
        return (iconId == 0) ? R.drawable.battery_9:iconId;
    }

    public Drawable getIcon() {
        return icon;
    }

    /**
     * Gets the application name
     */
    public String getName() {
        return name;
    }

    void getQuickNameIconForUid(final int uid) {
        final String uidString = Integer.toString(uid);
        if (sUidCache.containsKey(uidString)) {
            UidToDetail utd = sUidCache.get(uidString);
            defaultPackageName = utd.packageName;
            name = utd.name;
            icon = utd.icon;
            return;
        }
        PackageManager pm = context.getPackageManager();
        icon = pm.getDefaultActivityIcon();
        if (pm.getPackagesForUid(uid) == null) {
            if (uid == 0) {
                name = context.getResources().getString(R.string.process_kernel_label);
            } else if ("mediaserver".equals(name)) {
                name = context.getResources().getString(R.string.process_mediaserver_label);
            } else if ("dex2oat".equals(name)) {
                name = context.getResources().getString(R.string.process_dex2oat_label);
            } else if ("drmserver".equals(name)) {
                name = context.getResources().getString(R.string.process_drmserver_label);
            }
            iconId = R.drawable.ic_power_system;
            icon = context.getDrawable(iconId);
        }

        if (sHandler != null) {
            synchronized (mRequestQueue) {
                mRequestQueue.add(this);
            }
        }
    }

    /**
     * Loads the app label and icon image and stores into the cache.
     */
    public void loadNameAndIcon() {
        // Bail out if the current sipper is not an App sipper.
        if (sipper.uidObj == null) {
            return;
        }

        PackageManager pm = context.getPackageManager();
        final int uid = sipper.uidObj.getUid();
        sipper.mPackages = pm.getPackagesForUid(uid);
        if (sipper.mPackages != null) {
            String[] packageLabels = new String[sipper.mPackages.length];
            System.arraycopy(sipper.mPackages, 0, packageLabels, 0, sipper.mPackages.length);

            // Convert package names to user-facing labels where possible
            IPackageManager ipm = AppGlobals.getPackageManager();
            final int userId = UserHandle.getUserId(uid);
            for (int i = 0; i < packageLabels.length; i++) {
                try {
                    final ApplicationInfo ai = ipm.getApplicationInfo(packageLabels[i],
                            0 /* no flags */, userId);
                    if (ai == null) {
                        Log.d(TAG, "Retrieving null app info for package "
                                + packageLabels[i] + ", user " + userId);
                        continue;
                    }
                    CharSequence label = ai.loadLabel(pm);
                    if (label != null) {
                        packageLabels[i] = label.toString();
                    }
                    if (ai.icon != 0) {
                        name = label.toString();
                        defaultPackageName = sipper.mPackages[i];
                        icon = ai.loadIcon(pm);
                        break;
                    }
                } catch (RemoteException e) {
                    Log.d(TAG, "Error while retrieving app info for package "
                            + packageLabels[i] + ", user " + userId, e);
                }
            }

            if (packageLabels.length == 1) {
                name = packageLabels[0];
            } else {
                // Look for an official name for this UID.
                for (String pkgName : sipper.mPackages) {
                    try {
                        final PackageInfo pi = ipm.getPackageInfo(pkgName, 0/* no flags */, userId);
                        if (pi == null) {
                            Log.d(TAG, "Retrieving null package info for package "
                                    + pkgName + ", user " + userId);
                            continue;
                        }
                        if (pi.sharedUserLabel != 0) {
                            final CharSequence nm = pm.getText(pkgName,
                                    pi.sharedUserLabel, pi.applicationInfo);
                            if (nm != null) {
                                name = nm.toString();
                                if (pi.applicationInfo.icon != 0) {
                                    defaultPackageName = pkgName;
                                    icon = pi.applicationInfo.loadIcon(pm);
                                }
                                break;
                            }
                        }
                    } catch (RemoteException e) {
                        Log.d(TAG, "Error while retrieving package info for package "
                                + pkgName + ", user " + userId, e);
                    }
                }
            }
        }

        final String uidString = Integer.toString(uid);
        if (name == null) {
            name = "Uid:"+uidString;
        }

        if (icon == null) {
            icon = pm.getDefaultActivityIcon();
        }

        UidToDetail utd = new UidToDetail();
        utd.name = name;
        utd.icon = icon;
        utd.packageName = defaultPackageName;
        sUidCache.put(uidString, utd);
        if (sHandler != null) {
            sHandler.sendMessage(sHandler.obtainMessage(MSG_UPDATE_NAME_ICON, this));
        }
    }
}
