package com.android.stk;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.Settings.System;
import android.widget.Toast;
import com.android.internal.telephony.cat.CatLog;
import com.sec.android.app.CscFeature;

public class StkLauncherActivity extends Activity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (CscFeature.getInstance().getEnableStatus("CscFeature_RIL_FixedStkMenu") && SystemProperties.get("gsm.STK_SETUP_MENU").length() <= 0) {
            Toast.makeText(this, R.string.stk_no_service, 0).show();
        }
        int mStatus = System.getInt(getBaseContext().getContentResolver(), "phone1_on", 1);
        CatLog.d(this, "isSimDisabled PHONE1_ON mStatus = " + mStatus);
        if ("CTC".equals(SystemProperties.get("ro.csc.sales_code", ""))) {
            mStatus = System.getInt(getBaseContext().getContentResolver(), "phone2_on", 1);
            CatLog.d(this, "isSimDisabled PHONE2_ON mStatus = " + mStatus);
        }
        if (mStatus == 0) {
            Toast toast = Toast.makeText(this, (String) getText(R.string.stk_no_service), 0);
            toast.setGravity(80, 0, 50);
            toast.show();
            finish();
            return;
        }
        Bundle args = new Bundle();
        args.putInt("op", 3);
        startService(new Intent(this, StkAppService.class).putExtras(args));
        finish();
    }
}
