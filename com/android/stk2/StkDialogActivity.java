package com.android.stk2;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings.System;
import android.view.KeyEvent;
import com.android.internal.telephony.cat.CatLog;
import com.android.internal.telephony.cat.TextMessage;

public class StkDialogActivity extends Activity implements OnClickListener, OnKeyListener {
    StkAppService appService = StkAppService.getInstance();
    private Context mContext;
    boolean mPauseRelease;
    boolean mSentTerminalResponse;
    private final BroadcastReceiver mStateReceiver = new C00042();
    TextMessage mTextMsg;
    Handler mTimeoutHandler = new C00031();

    class C00031 extends Handler {
        C00031() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (StkDialogActivity.this.mTextMsg.userClear) {
                        StkDialogActivity.this.sendResponse(20);
                    } else {
                        StkDialogActivity.this.sendResponse(14);
                    }
                    StkDialogActivity.this.finish();
                    return;
                case 2:
                    CatLog.d("SIM2", this, "MSG_ID_PAUSE_TIMEOUT!!!!! ");
                    StkDialogActivity.this.mPauseRelease = false;
                    return;
                default:
                    return;
            }
        }
    }

    class C00042 extends BroadcastReceiver {
        C00042() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.android.samsungtest.EVENTHANDLE_BUTTON")) {
                String inputString = "";
                CatLog.d("SIM2", this, "got intent");
                int button = intent.getIntExtra("Button", -1);
                if (button == 0) {
                    StkDialogActivity.this.sendResponse(21);
                    StkDialogActivity.this.finish();
                } else if (button == 1) {
                    StkDialogActivity.this.sendResponse(13, true);
                    StkDialogActivity.this.finish();
                } else if (button == 2) {
                    StkDialogActivity.this.sendResponse(13, false);
                    StkDialogActivity.this.finish();
                }
            } else if ("android.intent.action.AIRPLANE_MODE".equals(intent.getAction())) {
                int airPlaneEnabled = System.getInt(StkDialogActivity.this.mContext.getContentResolver(), "airplane_mode_on", 0);
                CatLog.d("SIM2", this, "Received  ACTION_AIRPLANE_MODE_CHANGED = " + airPlaneEnabled);
                if (airPlaneEnabled == 1) {
                    StkDialogActivity.this.finish();
                }
            }
        }
    }

    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (!this.appService.mWakeLock.isHeld()) {
            this.appService.mWakeLock.acquire();
        }
        this.appService.disableKeyguard();
        this.mSentTerminalResponse = false;
        initFromIntent(getIntent());
        if (this.mTextMsg == null) {
            finish();
            return;
        }
        this.mTextMsg.text = this.mTextMsg.text.replaceAll("\r\n", "\n");
        this.mTextMsg.text = this.mTextMsg.text.replaceAll("\r", "\n");
        showDialog(0);
        this.mContext = getBaseContext();
        IntentFilter filter = new IntentFilter("com.android.samsungtest.EVENTHANDLE_BUTTON");
        filter.addAction("android.intent.action.AIRPLANE_MODE");
        this.mContext.registerReceiver(this.mStateReceiver, filter);
        this.mPauseRelease = true;
        startPauseTimeOut();
    }

    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case 0:
                Builder builder = new Builder(this, 5);
                builder.setTitle(this.mTextMsg.title);
                builder.setMessage(this.mTextMsg.text);
                builder.setPositiveButton(17039370, this);
                builder.setNegativeButton(17039360, this);
                builder.setOnKeyListener(this);
                return builder.create();
            default:
                return super.onCreateDialog(id);
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case -2:
                sendResponse(13, false);
                break;
            case -1:
                sendResponse(13, true);
                break;
        }
        finish();
    }

    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        CatLog.d("SIM2", this, "onKeyDown : " + keyCode);
        switch (keyCode) {
            case 4:
                sendResponse(21);
                finish();
                break;
        }
        return false;
    }

    public void onResume() {
        CatLog.d("SIM2", this, "onResume startTimeOut");
        super.onResume();
        startTimeOut();
    }

    public void onPause() {
        CatLog.d("SIM2", this, "onPause time out cancel");
        super.onPause();
        if (!this.mPauseRelease || this.mSentTerminalResponse) {
            this.appService.lock.lock();
            try {
                if (this.appService.mWakeLock.isHeld()) {
                    CatLog.d("SIM2", this, "before release wakeup");
                    this.appService.mWakeLock.release();
                }
                this.appService.lock.unlock();
                this.appService.enableKeyguard();
                if ("KDI".equals(SystemProperties.get("ro.csc.sales_code"))) {
                    CatLog.d(this, "mTextMsg.responseNeeded : " + this.mTextMsg.responseNeeded);
                    if (!this.mTextMsg.responseNeeded) {
                        return;
                    }
                }
                if (!this.mSentTerminalResponse) {
                    sendResponse(22);
                }
                if (this.mStateReceiver != null) {
                    try {
                        this.mContext.unregisterReceiver(this.mStateReceiver);
                    } catch (IllegalArgumentException e) {
                        CatLog.d(this, "Failed unregisterReceiver");
                    }
                }
                cancelTimeOut();
                finish();
            } catch (Throwable th) {
                this.appService.lock.unlock();
            }
        }
    }

    public void onStop() {
        CatLog.d("SIM2", this, "onStop finish dialog and send TR as END SESSION");
        super.onStop();
    }

    public void finish() {
        overridePendingTransition(-1, 0);
        super.finish();
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        CatLog.d("SIM2", this, "onConfigurationChanged");
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("text", this.mTextMsg);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        this.mTextMsg = (TextMessage) savedInstanceState.getParcelable("text");
    }

    private void sendResponse(int resId, boolean confirmed) {
        Bundle args = new Bundle();
        args.putInt("op", 2);
        args.putInt("response id", resId);
        args.putBoolean("confirm", confirmed);
        startService(new Intent(this, StkAppService.class).putExtras(args));
        this.mSentTerminalResponse = true;
    }

    private void sendResponse(int resId) {
        sendResponse(resId, true);
    }

    private void initFromIntent(Intent intent) {
        if (intent != null) {
            this.mTextMsg = (TextMessage) intent.getParcelableExtra("TEXT");
        } else {
            finish();
        }
    }

    private void cancelTimeOut() {
        this.mTimeoutHandler.removeMessages(1);
    }

    private void startTimeOut() {
        cancelTimeOut();
        int dialogDuration = StkApp.calculateDurationInMilis(this.mTextMsg.duration);
        CatLog.d("SIM2", this, "raw data dialogDuration = " + dialogDuration);
        if (dialogDuration == 0) {
            if (this.mTextMsg.userClear) {
                dialogDuration = 40000;
            } else {
                dialogDuration = 5000;
            }
        }
        CatLog.d("SIM2", this, "final dialogDuration = " + dialogDuration);
        this.mTimeoutHandler.sendMessageDelayed(this.mTimeoutHandler.obtainMessage(1), (long) dialogDuration);
    }

    private void startPauseTimeOut() {
        this.mTimeoutHandler.sendMessageDelayed(this.mTimeoutHandler.obtainMessage(2), 500);
    }
}
