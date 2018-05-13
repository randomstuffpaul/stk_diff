package com.android.stk2;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings.System;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import com.android.internal.telephony.cat.CatLog;
import com.android.internal.telephony.cat.Input;

public class StkInputActivity extends Activity implements TextWatcher, OnClickListener {
    StkAppService appService = StkAppService.getInstance();
    private Context mContext;
    private View mNormalLayout = null;
    boolean mPauseRelease;
    private TextView mPromptView = null;
    boolean mSentTerminalResponse;
    private int mState;
    private final BroadcastReceiver mStateReceiver = new C00062();
    private Input mStkInput = null;
    private EditText mTextIn = null;
    Handler mTimeoutHandler = new C00051();
    private View mYesNoLayout = null;

    class C00051 extends Handler {
        C00051() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    StkInputActivity.this.sendResponse(20);
                    StkInputActivity.this.finish();
                    return;
                case 2:
                    CatLog.d("SIM2", this, "MSG_ID_PAUSE_TIMEOUT!!!!! ");
                    StkInputActivity.this.mPauseRelease = false;
                    return;
                default:
                    return;
            }
        }
    }

    class C00062 extends BroadcastReceiver {
        C00062() {
        }

        public void onReceive(Context context, Intent intent) {
            String inputString;
            int button;
            if (intent.getAction().equals("com.android.samsungtest.EVENTHANDLE_TEXTANDBUTTON")) {
                CatLog.d("SIM2", this, "got intent");
                inputString = intent.getStringExtra("Value");
                button = intent.getIntExtra("Button", -1);
                if (inputString != null) {
                    StkInputActivity.this.mTextIn.setText(inputString);
                    StkInputActivity.this.mTextIn.setSelection(StkInputActivity.this.mTextIn.getText().length());
                }
                if (button == 0) {
                    StkInputActivity.this.sendResponse(21, null, false);
                    StkInputActivity.this.finish();
                } else if (button == 1 && StkInputActivity.this.verfiyTypedText()) {
                    StkInputActivity.this.sendResponse(12, StkInputActivity.this.mTextIn.getText().toString(), false);
                    StkInputActivity.this.finish();
                }
            } else if (intent.getAction().equals("com.android.samsungtest.EVENTHANDLE_BUTTON")) {
                inputString = "";
                CatLog.d("SIM2", this, "got intent");
                button = intent.getIntExtra("Button", -1);
                if (button == 0) {
                    StkInputActivity.this.sendResponse(21, null, false);
                    StkInputActivity.this.finish();
                } else if (button == 1) {
                    if (StkInputActivity.this.verfiyTypedText()) {
                        StkInputActivity.this.sendResponse(12, StkInputActivity.this.mTextIn.getText().toString(), false);
                        StkInputActivity.this.finish();
                    }
                } else if (button == 2) {
                    StkInputActivity.this.sendResponse(22, null, false);
                    StkInputActivity.this.finish();
                }
            } else if ("android.intent.action.AIRPLANE_MODE".equals(intent.getAction())) {
                int airPlaneEnabled = System.getInt(StkInputActivity.this.mContext.getContentResolver(), "airplane_mode_on", 0);
                CatLog.d("SIM2", this, "Received  ACTION_AIRPLANE_MODE_CHANGED = " + airPlaneEnabled);
                if (airPlaneEnabled == 1) {
                    StkInputActivity.this.finish();
                }
            }
        }
    }

    public void onClick(View v) {
        String input = null;
        switch (v.getId()) {
            case R.id.button_ok:
                if (verfiyTypedText()) {
                    input = this.mTextIn.getText().toString();
                    break;
                }
                return;
            case R.id.button_yes:
                input = "YES";
                break;
            case R.id.button_no:
                input = "NO";
                break;
        }
        sendResponse(12, input, false);
        finish();
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.stk_input);
        this.mSentTerminalResponse = false;
        this.mTextIn = (EditText) findViewById(R.id.in_text);
        this.mPromptView = (TextView) findViewById(R.id.prompt);
        this.mTextIn.setImeOptions(33554432);
        Button yesButton = (Button) findViewById(R.id.button_yes);
        Button noButton = (Button) findViewById(R.id.button_no);
        ((Button) findViewById(R.id.button_ok)).setOnClickListener(this);
        yesButton.setOnClickListener(this);
        noButton.setOnClickListener(this);
        this.mYesNoLayout = findViewById(R.id.yes_no_layout);
        this.mNormalLayout = findViewById(R.id.normal_layout);
        IntentFilter filter = new IntentFilter("com.android.samsungtest.EVENTHANDLE_TEXTANDBUTTON");
        filter.addAction("com.android.samsungtest.EVENTHANDLE_BUTTON");
        filter.addAction("android.intent.action.AIRPLANE_MODE");
        Intent intent = getIntent();
        if (intent != null) {
            this.mStkInput = (Input) intent.getParcelableExtra("INPUT");
            if (this.mStkInput == null) {
                finish();
            } else {
                this.mState = this.mStkInput.yesNo ? 2 : 1;
                configInputDisplay();
                if (!this.appService.mWakeLock.isHeld()) {
                    this.appService.mWakeLock.acquire();
                }
                this.appService.disableKeyguard();
            }
        } else {
            finish();
        }
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(false);
        }
        this.mContext = getBaseContext();
        this.mContext.registerReceiver(this.mStateReceiver, filter);
        this.mPauseRelease = true;
        startPauseTimeOut();
    }

    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        this.mTextIn.addTextChangedListener(this);
    }

    public void onResume() {
        super.onResume();
        CatLog.d("SIM2", this, "onResume startTimeOut");
        startTimeOut();
    }

    public void onPause() {
        super.onPause();
        CatLog.d("SIM2", this, "onPause finish activity");
        if (!"China".equals(SystemProperties.get("ro.csc.country_code"))) {
            cancelTimeOut();
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        CatLog.d("SIM2", this, "onConfigurationChanged");
        super.onConfigurationChanged(newConfig);
    }

    public void onStop() {
        super.onStop();
        CatLog.d("SIM2", this, "onStop");
        if (!this.mPauseRelease || this.mSentTerminalResponse) {
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
    }

    public void onDestroy() {
        super.onDestroy();
        CatLog.d("SIM2", this, "onDestroy activity");
        if (!this.mPauseRelease || this.mSentTerminalResponse) {
            if (!this.mSentTerminalResponse) {
                sendResponse(22);
            }
            this.mContext.unregisterReceiver(this.mStateReceiver);
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case 4:
                sendResponse(21, null, false);
                finish();
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void sendResponse(int resId) {
        sendResponse(resId, null, false);
    }

    private void sendResponse(int resId, String input, boolean help) {
        Bundle args = new Bundle();
        args.putInt("op", 2);
        args.putInt("response id", resId);
        if (input != null) {
            args.putString("input", input);
        }
        args.putBoolean("help", help);
        this.mContext.startService(new Intent(this.mContext, StkAppService.class).putExtras(args));
        this.mSentTerminalResponse = true;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, 1, 1, R.string.menu_end_session);
        menu.add(0, 3, 2, R.string.help);
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(1).setVisible(true);
        menu.findItem(3).setVisible(this.mStkInput.helpAvailable);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                sendResponse(22);
                finish();
                return true;
            case 3:
                sendResponse(12, "", true);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        startTimeOut();
    }

    public void afterTextChanged(Editable s) {
    }

    private boolean verfiyTypedText() {
        if (this.mTextIn.getText().length() < this.mStkInput.minLen) {
            return false;
        }
        return true;
    }

    private void cancelTimeOut() {
        this.mTimeoutHandler.removeMessages(1);
    }

    private void startTimeOut() {
        cancelTimeOut();
        int duration = StkApp.calculateDurationInMilis(this.mStkInput.duration);
        if (duration == 0) {
            duration = 40000;
        }
        this.mTimeoutHandler.sendMessageDelayed(this.mTimeoutHandler.obtainMessage(1), (long) duration);
    }

    private void startPauseTimeOut() {
        this.mTimeoutHandler.sendMessageDelayed(this.mTimeoutHandler.obtainMessage(2), 500);
    }

    private void configInputDisplay() {
        TextView numOfCharsView = (TextView) findViewById(R.id.num_of_chars);
        TextView inTypeView = (TextView) findViewById(R.id.input_type);
        int inTypeId = R.string.alphabet;
        this.mStkInput.text = this.mStkInput.text.replaceAll("\r\n", "\n");
        this.mStkInput.text = this.mStkInput.text.replaceAll("\r", "\n");
        if ("white".equals(SystemProperties.get("ro.build.scafe.cream"))) {
            numOfCharsView.setTextColor(-16777216);
            inTypeView.setTextColor(-16777216);
        }
        this.mPromptView.setText(this.mStkInput.text);
        if (this.mStkInput.digitOnly) {
            this.mTextIn.setKeyListener(StkDigitsKeyListener.getInstance());
            inTypeId = R.string.digits;
        }
        inTypeView.setText(inTypeId);
        if (this.mStkInput.icon != null) {
            setFeatureDrawable(3, new BitmapDrawable(this.mStkInput.icon));
        }
        switch (this.mState) {
            case 1:
                int maxLen = this.mStkInput.maxLen;
                int minLen = this.mStkInput.minLen;
                this.mTextIn.setFilters(new InputFilter[]{new LengthFilter(maxLen)});
                String lengthLimit = String.valueOf(minLen);
                if (maxLen != minLen) {
                    lengthLimit = minLen + " - " + maxLen;
                }
                numOfCharsView.setText(lengthLimit);
                if (!this.mStkInput.echo) {
                    this.mTextIn.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
                if (this.mStkInput.defaultText != null) {
                    this.mTextIn.setText(this.mStkInput.defaultText);
                    this.mTextIn.setSelection(this.mTextIn.getText().length());
                    return;
                }
                this.mTextIn.setText("", BufferType.EDITABLE);
                return;
            case 2:
                this.mYesNoLayout.setVisibility(0);
                this.mNormalLayout.setVisibility(8);
                return;
            default:
                return;
        }
    }
}
