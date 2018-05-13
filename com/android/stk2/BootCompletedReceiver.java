package com.android.stk2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class BootCompletedReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                Log.e("STK2", "onReceive " + action);
                if (action.equals("android.intent.action.BOOT_COMPLETED")) {
                    Bundle args = new Bundle();
                    args.putInt("op", 5);
                    context.startService(new Intent(context, StkAppService.class).putExtras(args));
                }
            }
        }
    }
}
