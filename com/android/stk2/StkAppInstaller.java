package com.android.stk2;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import com.android.internal.telephony.cat.CatLog;

abstract class StkAppInstaller {
    static void install(Context context) {
        setAppState(context, true);
    }

    static void unInstall(Context context) {
        setAppState(context, false);
    }

    private static void setAppState(Context context, boolean install) {
        int state = 1;
        if (context != null) {
            PackageManager pm = context.getPackageManager();
            if (pm != null) {
                ComponentName cName = new ComponentName("com.android.stk2", "com.android.stk2.StkLauncherActivity");
                if (!install) {
                    state = 2;
                }
                try {
                    pm.setComponentEnabledSetting(cName, state, 1);
                } catch (Exception e) {
                    CatLog.d("SIM2", "StkAppInstaller", "Could not change STK app state");
                }
            }
        }
    }
}
