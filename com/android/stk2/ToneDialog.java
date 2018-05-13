package com.android.stk2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.telephony.cat.CatLog;
import com.android.internal.telephony.cat.TextMessage;
import com.android.internal.telephony.cat.ToneSettings;

public class ToneDialog extends Activity {
    StkAppService appService = StkAppService.getInstance();
    boolean mSentTerminalResponse;
    Handler mToneStopper = new C00091();
    Vibrator mVibrator = null;
    TonePlayer player = null;
    ToneSettings settings = null;
    TextMessage toneMsg = null;

    class C00091 extends Handler {
        C00091() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 218:
                    ToneDialog.this.player.release();
                    ToneDialog.this.mVibrator.cancel();
                    ToneDialog.this.sendResponse(14);
                    ToneDialog.this.finish();
                    return;
                default:
                    return;
            }
        }
    }

    class C00102 implements Runnable {
        C00102() {
        }

        public void run() {
            try {
                ToneDialog.this.appService.disableKeyguard();
            } catch (Exception e) {
            }
        }
    }

    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        initFromIntent(getIntent());
        new Thread(new C00102()).start();
        findViewById(16908310).setVisibility(8);
        setContentView(R.layout.stk_tone_dialog);
        ImageView iv = (ImageView) findViewById(R.id.icon);
        ((TextView) findViewById(R.id.message)).setText(this.toneMsg.text);
        if (this.toneMsg.icon == null) {
            iv.setImageResource(17302912);
        } else {
            iv.setImageBitmap(this.toneMsg.icon);
        }
        this.player = new TonePlayer();
        this.mVibrator = (Vibrator) getSystemService("vibrator");
        int timeout = StkApp.calculateDurationInMilis(this.settings.duration);
        if (timeout == 0) {
            timeout = 2000;
        } else if (timeout < 600) {
            timeout = 2000;
        }
        this.mToneStopper.sendEmptyMessageDelayed(218, (long) timeout);
        if (this.settings.vibrate) {
            this.mVibrator.vibrate((long) timeout);
        }
        if (!this.appService.mWakeLock.isHeld()) {
            this.appService.mWakeLock.acquire();
        }
        try {
            Thread.sleep(500);
        } catch (Exception e) {
        }
        this.mSentTerminalResponse = false;
        this.player.play(this.settings.tone, timeout);
    }

    public void onStop() {
        CatLog.d("SIM2", this, "onStop");
        super.onStop();
        this.appService.lock.lock();
        try {
            if (this.appService.mWakeLock.isHeld()) {
                CatLog.d("SIM2", this, "before release wakeup");
                this.appService.mWakeLock.release();
            }
            this.appService.lock.unlock();
            this.appService.enableKeyguard();
        } catch (Throwable th) {
            this.appService.lock.unlock();
        }
    }

    public void onResume() {
        CatLog.d("SIM2", this, "onResume");
        super.onResume();
    }

    public void onPause() {
        CatLog.d("SIM2", this, "onPause");
        super.onPause();
    }

    protected void onDestroy() {
        CatLog.d("SIM2", this, "onDestroy");
        if (!this.mSentTerminalResponse) {
            sendResponse(14);
        }
        super.onDestroy();
        this.mToneStopper.removeMessages(218);
        this.player.release();
        this.mVibrator.cancel();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case 4:
                this.player.stop();
                this.player.release();
                this.mVibrator.cancel();
                sendResponse(22);
                finish();
                break;
        }
        return false;
    }

    private void initFromIntent(Intent intent) {
        if (intent == null) {
            finish();
        }
        this.toneMsg = (TextMessage) intent.getParcelableExtra("TEXT");
        this.settings = (ToneSettings) intent.getParcelableExtra("TONE");
    }

    private void sendResponse(int resId) {
        Bundle args = new Bundle();
        this.mSentTerminalResponse = true;
        args.putInt("op", 2);
        args.putInt("response id", resId);
        startService(new Intent(this, StkAppService.class).putExtras(args));
    }
}
