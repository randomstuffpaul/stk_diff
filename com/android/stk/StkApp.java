package com.android.stk;

import android.app.Application;
import com.android.internal.telephony.cat.Duration;
import com.android.internal.telephony.cat.Duration.TimeUnit;
import com.sec.android.app.CscFeature;

abstract class StkApp extends Application {

    static /* synthetic */ class C00001 {
        static final /* synthetic */ int[] $SwitchMap$com$android$internal$telephony$cat$Duration$TimeUnit = new int[TimeUnit.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$cat$Duration$TimeUnit[TimeUnit.MINUTE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$Duration$TimeUnit[TimeUnit.TENTH_SECOND.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$Duration$TimeUnit[TimeUnit.SECOND.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    public static int getUItimeOutforSTKcmds(String stkcmds) {
        String[] checkStkCmdArray = new String[8];
        checkStkCmdArray = CscFeature.getInstance().getString("CscFeature_RIL_StkCmdTimeOut").split(",");
        if (stkcmds == null || stkcmds.length() == 0) {
            return -1;
        }
        for (int i = 0; i < checkStkCmdArray.length; i += 2) {
            if (stkcmds.equals(checkStkCmdArray[i])) {
                return Integer.parseInt(checkStkCmdArray[i + 1]) * 1000;
            }
        }
        return -1;
    }

    public static int calculateDurationInMilis(Duration duration) {
        if (duration == null) {
            return 0;
        }
        int timeout;
        switch (C00001.$SwitchMap$com$android$internal$telephony$cat$Duration$TimeUnit[duration.timeUnit.ordinal()]) {
            case 1:
                timeout = 60000;
                break;
            case 2:
                timeout = 100;
                break;
            default:
                timeout = 1000;
                break;
        }
        return timeout * duration.timeInterval;
    }
}
