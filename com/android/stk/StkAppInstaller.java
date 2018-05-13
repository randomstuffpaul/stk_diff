package com.android.stk;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import com.android.internal.telephony.cat.CatLog;
import com.sec.android.app.CscFeature;

abstract class StkAppInstaller {
    static void install(Context context) {
        setAppState(context, true);
    }

    static void unInstall(Context context) {
        if (CscFeature.getInstance().getEnableStatus("CscFeature_RIL_FixedStkMenu")) {
            setAppState(context, true);
        } else {
            setAppState(context, false);
        }
    }

    private static void setAppState(Context context, boolean install) {
        int state = 1;
        if (context != null) {
            PackageManager pm = context.getPackageManager();
            if (pm != null) {
                ComponentName cName = new ComponentName("com.android.stk", "com.android.stk.StkLauncherActivity");
                if (!install) {
                    state = 2;
                }
                try {
                    pm.setComponentEnabledSetting(cName, state, 1);
                } catch (Exception e) {
                    CatLog.d("StkAppInstaller", "Could not change STK app state");
                }
            }
        }
    }
}
