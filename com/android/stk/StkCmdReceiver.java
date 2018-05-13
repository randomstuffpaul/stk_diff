package com.android.stk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemProperties;
import com.android.internal.telephony.cat.CatEventDownload;
import com.android.internal.telephony.cat.CatLog;
import com.android.internal.telephony.cat.CatService;
import com.samsung.android.telephony.MultiSimManager;

public class StkCmdReceiver extends BroadcastReceiver {
    private String salesCode;

    public void onReceive(Context context, Intent intent) {
        CatLog.d(this, "onReceive - " + intent.getAction());
        String action = intent.getAction();
        this.salesCode = SystemProperties.get("ro.csc.sales_code");
        if (action.equals("android.intent.action.stk.command")) {
            handleCommandMessage(context, intent);
        } else if (action.equals("android.intent.action.stk.session_end")) {
            handleSessionEnd(context, intent);
        } else if (action.equals("android.intent.action.stk.icc_status_change")) {
            handleCardStatusChange(context, intent);
        } else if (action.equals("android.intent.action.stk.event")) {
            handleEvent(context, intent);
        } else if (action.equals("android.intent.action.stk.start_main_activity")) {
            handleStartMainActivity(context, intent);
        } else if (action.equals("com.sec.android.intent.action.HOME_RESUME")) {
            CatLog.d("StkCmdReceiver", "Received : HOME_RESUME intent");
            if ("1".equals(MultiSimManager.getTelephonyProperty("gsm.sim.screenEvent", 0, "0"))) {
                CatEventDownload catEventIdleScreen = new CatEventDownload(5);
                eventIntent = new Intent("android.intent.action.stk.event");
                eventIntent.putExtra("STK EVENT", catEventIdleScreen);
                context.sendBroadcast(eventIntent);
            }
        } else if (action.equals("android.intent.action.LOCALE_CHANGED")) {
            CatLog.d("StkCmdReceiver", "Received : ACTION_LOCALE_CHANGED intent");
            if ("1".equals(MultiSimManager.getTelephonyProperty("gsm.sim.lenguageEvent", 0, "0"))) {
                CatEventDownload catEventLanguage = new CatEventDownload(7, context.getResources().getConfiguration().locale.getLanguage());
                eventIntent = new Intent("android.intent.action.stk.event");
                eventIntent.putExtra("STK EVENT", catEventLanguage);
                context.sendBroadcast(eventIntent);
            }
        } else if (action.equals("android.intent.action.STK_BROWSER_HOMEPAGE")) {
            handleBrowserHomepage(context, intent);
        } else if ("android.intent.action.ACTION_SHUTDOWN".equals(action)) {
            handleActionShutdown(context, intent);
        } else if ("android.settings.SIMCARD_MGT_ACTIVATED".equals(action) && "CHU".equals(this.salesCode)) {
            args = new Bundle();
            args.putInt("op", 100);
            args.putInt("simcard_sim_activate", intent.getIntExtra("simcard_sim_activate", 1));
            CatLog.d("StkCmdReceiver", "Received : android.settings.SIMCARD_MGT_ACTIVATED  sim_id=" + intent.getIntExtra("simcard_sim_id", 0));
            if (intent.getIntExtra("simcard_sim_id", 0) == 0) {
                context.startService(new Intent(context, StkAppService.class).putExtras(args));
            } else {
                CatLog.d("StkCmdReceiver", "[STK]Received : android.settings.SIMCARD_MGT_ACTIVATED but SIM_ID!=0,IGNORE");
            }
        } else if (action.equals("android.intent.action.AIRPLANE_MODE") && "CHU".equals(this.salesCode)) {
            args = new Bundle();
            args.putInt("op", 100);
            if (intent.getBooleanExtra("state", false)) {
                args.putInt("simcard_sim_activate", 0);
            } else {
                args.putInt("simcard_sim_activate", 1);
            }
            context.startService(new Intent(context, StkAppService.class).putExtras(args));
        }
    }

    private void handleCommandMessage(Context context, Intent intent) {
        Bundle args = new Bundle();
        args.putInt("op", 1);
        args.putParcelable("cmd message", intent.getParcelableExtra("STK CMD"));
        context.startService(new Intent(context, StkAppService.class).putExtras(args));
        CatLog.d(this, "After StartService)");
    }

    private void handleStartMainActivity(Context context, Intent intent) {
        Bundle args = new Bundle();
        args.putInt("op", 3);
        context.startService(new Intent(context, StkAppService.class).putExtras(args));
    }

    private void handleEvent(Context context, Intent intent) {
        Bundle args = new Bundle();
        args.putInt("op", 7);
        args.putParcelable("event", intent.getParcelableExtra("STK EVENT"));
        context.startService(new Intent(context, StkAppService.class).putExtras(args));
    }

    private void handleSessionEnd(Context context, Intent intent) {
        Bundle args = new Bundle();
        args.putInt("op", 4);
        context.startService(new Intent(context, StkAppService.class).putExtras(args));
    }

    private void handleCardStatusChange(Context context, Intent intent) {
        if ((intent.getBooleanExtra("card_status", false) || StkAppService.getInstance() != null) && CatService.getPackageType(intent.getIntExtra("SLOT_ID", 0)) == 0) {
            Bundle args = new Bundle();
            args.putInt("op", 17);
            args.putBoolean("card_status", intent.getBooleanExtra("card_status", true));
            args.putInt("refresh_result", intent.getIntExtra("refresh_result", 0));
            context.startService(new Intent(context, StkAppService.class).putExtras(args));
        }
    }

    private void handleBrowserHomepage(Context context, Intent intent) {
        Bundle args = new Bundle();
        String homePage = intent.getStringExtra("homepage");
        args.putInt("op", 9);
        args.putString("homepage", homePage);
        context.startService(new Intent(context, StkAppService.class).putExtras(args));
    }

    private void handleActionShutdown(Context context, Intent intent) {
        Bundle args = new Bundle();
        args.putInt("op", 10);
        context.startService(new Intent(context, StkAppService.class).putExtras(args));
    }
}
