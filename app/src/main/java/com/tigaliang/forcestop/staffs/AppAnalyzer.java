package com.tigaliang.forcestop.staffs;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tigaliang on 2016/10/3.
 * <p>
 * AppAnalyzer:
 */

public final class AppAnalyzer {
    private static final String TAG = "AppAnalyzer";

    private AppAnalyzer() {
    }

    public static List<AppBean> getInstalledApps(@NonNull Context context) {
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> installedPkgList = pm.getInstalledPackages(PackageManager.GET_META_DATA);
        List<AppBean> installedApps = new ArrayList<>();
        for (PackageInfo pkgInfo : installedPkgList) {
            AppBean bean = new AppBean();
            bean.pkgName = pkgInfo.packageName;
            if (isUnconcernedPkg(bean.pkgName)) {
                continue;
            }
            bean.uid = getAppUid(pm, bean.pkgName);
            bean.appName = getAppName(pm, bean.pkgName);
            bean.icLauncher = getIcLauncher(pm, bean.pkgName);
            if (!TextUtils.isEmpty(bean.pkgName) && bean.uid > 0) {
                installedApps.add(bean);
            }
        }
        return installedApps;
    }

    public static List<AppBean> getRunningApps(@NonNull Context context) {
        List<Integer> runningUids = getRunningUids(context);

        PackageManager pm = context.getPackageManager();

        List<AppBean> runningApps = new ArrayList<>();

        for (int uid : runningUids) {
            AppBean bean = new AppBean();
            bean.uid = uid;
            bean.pkgName = pm.getNameForUid(bean.uid);
            if (!TextUtils.isEmpty(bean.pkgName) && bean.pkgName.contains(":")) {
                bean.pkgName = bean.pkgName.substring(0, bean.pkgName.indexOf(':'));
            }
            if (isUnconcernedPkg(bean.pkgName)) {
                continue;
            }
            bean.appName = getAppName(pm, bean.pkgName);
            bean.icLauncher = getIcLauncher(pm, bean.pkgName);

            Log.d(TAG, "running:" + uid + "," + bean.pkgName + "," + bean.appName);

            runningApps.add(bean);
        }

        return runningApps;
    }

    private static int getAppUid(PackageManager pm, String pkgName) {
        try {
            return pm.getApplicationInfo(pkgName, PackageManager.GET_META_DATA).uid;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return -1;
    }

    private static String getAppName(PackageManager pm, String pkgName) {
        try {
            return pm.getApplicationLabel(
                    pm.getApplicationInfo(pkgName, PackageManager.GET_META_DATA)).toString();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    private static List<Integer> getRunningUids(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<Integer> uids = new ArrayList<>();
        List<RunningAppProcessInfo> rais = am.getRunningAppProcesses();
        List<RunningServiceInfo> rsis = am.getRunningServices(Integer.MAX_VALUE);

        List<Integer> pidsTmp = new ArrayList<>();

        for (RunningAppProcessInfo rai : rais) {
            if (pidsTmp.contains(rai.pid)) {
                continue;
            }
            uids.add(rai.uid);
            pidsTmp.add(rai.pid);
        }
        for (RunningServiceInfo rsi : rsis) {
            if (pidsTmp.contains(rsi.pid)) {
                continue;
            }
            uids.add(rsi.uid);
            pidsTmp.add(rsi.pid);
        }
        return uids;
    }

    private static Drawable getIcLauncher(PackageManager pm, String pkgName) {
        try {
            return pm.getApplicationIcon(pkgName);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    private static boolean isUnconcernedPkg(String pkgName) {
        if (TextUtils.isEmpty(pkgName)) {
            return true;
        }

        // for 'Nexus 5X'
        return pkgName.startsWith("android.")
                || pkgName.startsWith("com.android.")
                || (pkgName.startsWith("com.google.android.") && !pkgName.startsWith("com.google.android.app."))
                || pkgName.startsWith("com.qualcomm.")
                || pkgName.startsWith("com.qti.qualcomm.")
                || pkgName.equals("com.google.uid.shared")
                || pkgName.equals("android")
                || pkgName.equals("com.google.vr.vrcore")
                || pkgName.equals("com.lge.lifetimer")
                || pkgName.equals("com.lge.entitlement")
                || pkgName.equals("com.lge.HiddenMenu")
                || pkgName.equals("com.quicinc.cne.CNEService")
                || pkgName.equals("org.codeaurora.ims")
                || pkgName.equals("com.verizon.omadm");
    }
}
