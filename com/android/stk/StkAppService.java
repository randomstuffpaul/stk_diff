package com.android.stk;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.ProxyInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.System;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.telephony.cat.AppInterface;
import com.android.internal.telephony.cat.AppInterface.CommandType;
import com.android.internal.telephony.cat.CatCmdMessage;
import com.android.internal.telephony.cat.CatCmdMessage.BrowserSettings;
import com.android.internal.telephony.cat.CatEnvelopeMessage;
import com.android.internal.telephony.cat.CatEventDownload;
import com.android.internal.telephony.cat.CatLog;
import com.android.internal.telephony.cat.CatResponseMessage;
import com.android.internal.telephony.cat.CatService;
import com.android.internal.telephony.cat.Input;
import com.android.internal.telephony.cat.Item;
import com.android.internal.telephony.cat.LaunchBrowserMode;
import com.android.internal.telephony.cat.Menu;
import com.android.internal.telephony.cat.ResultCode;
import com.android.internal.telephony.cat.TextMessage;
import com.android.internal.telephony.uicc.IccRefreshResponse;
import com.samsung.android.multiwindow.MultiWindowStyle;
import com.samsung.android.telephony.MultiSimManager;
import com.sec.android.app.CscFeature;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

public class StkAppService extends Service implements Runnable {
    static StkAppService sInstance = null;
    private final int DELAY_TIME_FOR_RESET = 5000;
    private String lastSelectedItem = null;
    private boolean launchBrowser = false;
    public final ReentrantLock lock = new ReentrantLock();
    ProxyInfo mBackupProxy;
    private BrowserSettings mBrowserSettings = null;
    private boolean mCmdInProgress = false;
    private LinkedList<DelayedCmd> mCmdsQ = null;
    private Context mContext = null;
    private CatCmdMessage mCurrentCmd = null;
    private Menu mCurrentMenu = null;
    public boolean mIsMainMenu = false;
    boolean mIsProxyChanged = false;
    private boolean mIsStartedByUser = false;
    boolean mIsSystemShutdown = false;
    private KeyguardLock mKeyguardLock;
    private KeyguardManager mKeyguardManager;
    private CatCmdMessage mMainCmd = null;
    private boolean mMenuIsVisibile = false;
    public boolean mMenuItemBlock = false;
    private Handler mMessageHandler = null;
    private NotificationManager mNotificationManager = null;
    private volatile ServiceHandler mServiceHandler;
    private volatile Looper mServiceLooper;
    private boolean[] mSetEventList = null;
    private AppInterface mStkService;
    WakeLock mWakeLock;
    private boolean responseNeeded = true;
    private String salesCode;

    class C00011 implements Runnable {
        final /* synthetic */ StkAppService this$0;

        public void run() {
            StkAppService stkAppService = this.this$0;
            this.this$0.mContext;
            ((PowerManager) stkAppService.getSystemService("power")).reboot(null);
        }
    }

    static /* synthetic */ class C00022 {
        static final /* synthetic */ int[] f0xca33cf42 = new int[CommandType.values().length];
        static final /* synthetic */ int[] $SwitchMap$com$android$internal$telephony$cat$LaunchBrowserMode = new int[LaunchBrowserMode.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$cat$LaunchBrowserMode[LaunchBrowserMode.USE_EXISTING_BROWSER.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$LaunchBrowserMode[LaunchBrowserMode.LAUNCH_NEW_BROWSER.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$LaunchBrowserMode[LaunchBrowserMode.LAUNCH_IF_NOT_ALREADY_LAUNCHED.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                f0xca33cf42[CommandType.SEND_DTMF.ordinal()] = 1;
            } catch (NoSuchFieldError e4) {
            }
            try {
                f0xca33cf42[CommandType.SEND_SMS.ordinal()] = 2;
            } catch (NoSuchFieldError e5) {
            }
            try {
                f0xca33cf42[CommandType.SEND_SS.ordinal()] = 3;
            } catch (NoSuchFieldError e6) {
            }
            try {
                f0xca33cf42[CommandType.SEND_USSD.ordinal()] = 4;
            } catch (NoSuchFieldError e7) {
            }
            try {
                f0xca33cf42[CommandType.SET_UP_IDLE_MODE_TEXT.ordinal()] = 5;
            } catch (NoSuchFieldError e8) {
            }
            try {
                f0xca33cf42[CommandType.SET_UP_MENU.ordinal()] = 6;
            } catch (NoSuchFieldError e9) {
            }
            try {
                f0xca33cf42[CommandType.REFRESH.ordinal()] = 7;
            } catch (NoSuchFieldError e10) {
            }
            try {
                f0xca33cf42[CommandType.DISPLAY_TEXT.ordinal()] = 8;
            } catch (NoSuchFieldError e11) {
            }
            try {
                f0xca33cf42[CommandType.SELECT_ITEM.ordinal()] = 9;
            } catch (NoSuchFieldError e12) {
            }
            try {
                f0xca33cf42[CommandType.GET_INPUT.ordinal()] = 10;
            } catch (NoSuchFieldError e13) {
            }
            try {
                f0xca33cf42[CommandType.GET_INKEY.ordinal()] = 11;
            } catch (NoSuchFieldError e14) {
            }
            try {
                f0xca33cf42[CommandType.LAUNCH_BROWSER.ordinal()] = 12;
            } catch (NoSuchFieldError e15) {
            }
            try {
                f0xca33cf42[CommandType.SET_UP_CALL.ordinal()] = 13;
            } catch (NoSuchFieldError e16) {
            }
            try {
                f0xca33cf42[CommandType.PLAY_TONE.ordinal()] = 14;
            } catch (NoSuchFieldError e17) {
            }
            try {
                f0xca33cf42[CommandType.SET_UP_EVENT_LIST.ordinal()] = 15;
            } catch (NoSuchFieldError e18) {
            }
            try {
                f0xca33cf42[CommandType.LANGUAGE_NOTIFICATION.ordinal()] = 16;
            } catch (NoSuchFieldError e19) {
            }
            try {
                f0xca33cf42[CommandType.OPEN_CHANNEL.ordinal()] = 17;
            } catch (NoSuchFieldError e20) {
            }
            try {
                f0xca33cf42[CommandType.CLOSE_CHANNEL.ordinal()] = 18;
            } catch (NoSuchFieldError e21) {
            }
            try {
                f0xca33cf42[CommandType.RECEIVE_DATA.ordinal()] = 19;
            } catch (NoSuchFieldError e22) {
            }
            try {
                f0xca33cf42[CommandType.SEND_DATA.ordinal()] = 20;
            } catch (NoSuchFieldError e23) {
            }
        }
    }

    private class DelayedCmd {
        int id;
        CatCmdMessage msg;

        DelayedCmd(int id, CatCmdMessage msg) {
            this.id = id;
            this.msg = msg;
        }
    }

    private enum InitiatedByUserAction {
        yes,
        unknown
    }

    private final class MessageHandler extends Handler {
        private MessageHandler() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    StkAppService.this.mMenuItemBlock = false;
                    CatLog.d(this, "It is weird. It should be unlocked by END_SESSION. Please check if we got END_SESSION from modem.");
                    return;
                default:
                    return;
            }
        }
    }

    private final class ServiceHandler extends Handler {

        class C00041 implements Runnable {

            class C00031 implements Runnable {
                C00031() {
                }

                public void run() {
                    Intent rebootIntent = new Intent("android.intent.action.ACTION_REQUEST_SHUTDOWN");
                    rebootIntent.setAction("android.intent.action.REBOOT");
                    rebootIntent.putExtra("android.intent.extra.KEY_CONFIRM", false);
                    rebootIntent.setFlags(268435456);
                    StkAppService.this.mContext.startActivityAsUser(rebootIntent, UserHandle.CURRENT);
                }
            }

            C00041() {
            }

            public void run() {
                new Thread(new C00031()).start();
            }
        }

        private ServiceHandler() {
        }

        public void handleMessage(Message msg) {
            switch (msg.arg1) {
                case 1:
                    CatCmdMessage cmdMsg = msg.obj;
                    if (!StkAppService.this.isCmdInteractive(cmdMsg)) {
                        StkAppService.this.handleCmd(cmdMsg);
                        return;
                    } else if (StkAppService.this.mCmdInProgress) {
                        StkAppService.this.mCmdsQ.addLast(new DelayedCmd(1, (CatCmdMessage) msg.obj));
                        return;
                    } else {
                        StkAppService.this.mCmdInProgress = true;
                        StkAppService.this.handleCmd((CatCmdMessage) msg.obj);
                        return;
                    }
                case 2:
                    if (StkAppService.this.responseNeeded) {
                        StkAppService.this.handleCmdResponse((Bundle) msg.obj);
                    }
                    if (StkAppService.this.mCmdsQ.size() != 0) {
                        StkAppService.this.callDelayedMsg();
                    } else {
                        StkAppService.this.mCmdInProgress = false;
                    }
                    StkAppService.this.responseNeeded = true;
                    return;
                case 3:
                    if (StkAppService.this.mMainCmd == null) {
                        CatLog.d(this, "Invalid mMainCmd");
                        return;
                    }
                    StkAppService.this.mIsStartedByUser = true;
                    StkAppService.this.launchMenuActivity(null);
                    return;
                case 4:
                    if (StkAppService.this.mCmdInProgress) {
                        StkAppService.this.mCmdsQ.addLast(new DelayedCmd(4, null));
                        return;
                    }
                    StkAppService.this.mCmdInProgress = true;
                    StkAppService.this.handleSessionEnd();
                    return;
                case 5:
                    CatLog.d(this, "OP_BOOT_COMPLETED");
                    if (StkAppService.this.mMainCmd == null) {
                        StkAppInstaller.unInstall(StkAppService.this.mContext);
                        return;
                    }
                    return;
                case 6:
                    StkAppService.this.handleDelayedCmd();
                    return;
                case 7:
                    StkAppService.this.handleEvent(msg.obj);
                    return;
                case 8:
                    CatLog.d(this, "before ckecking timed release wakeup");
                    StkAppService.this.lock.lock();
                    try {
                        if (StkAppService.this.mWakeLock.isHeld()) {
                            CatLog.d(this, "before timed release wakeup");
                            StkAppService.this.mWakeLock.release();
                        }
                        StkAppService.this.lock.unlock();
                        return;
                    } catch (Throwable th) {
                        StkAppService.this.lock.unlock();
                    }
                case 9:
                    StkAppService.this.mBrowserSettings.url = ((Bundle) msg.obj).getString("homepage");
                    StkAppService.this.launchBrowser(StkAppService.this.mBrowserSettings);
                    return;
                case 10:
                    CatLog.d(this, "Uninstall App for ACTION_SHUTDOWN");
                    StkAppService.this.mIsSystemShutdown = true;
                    StkAppInstaller.unInstall(StkAppService.this.mContext);
                    return;
                case 17:
                    CatLog.d(this, "Card/Icc Status change received");
                    handleCardStatusChangeAndIccRefresh((Bundle) msg.obj);
                    return;
                case 100:
                    if (((Bundle) msg.obj).getInt("simcard_sim_activate") == 1) {
                        int mSimActive = System.getInt(StkAppService.this.mContext.getContentResolver(), "phone1_on", 1);
                        CatLog.d(this, "Install App by user activation. mSimActive=" + mSimActive + " mMainCmd=" + StkAppService.this.mMainCmd);
                        if (StkAppService.this.mMainCmd != null && 1 == mSimActive) {
                            StkAppInstaller.install(StkAppService.this.mContext);
                            return;
                        }
                        return;
                    }
                    if (StkAppService.this.mCmdInProgress) {
                        StkAppService.this.mCmdsQ.addLast(new DelayedCmd(4, null));
                    } else {
                        StkAppService.this.mCmdInProgress = true;
                        StkAppService.this.handleSessionEnd();
                    }
                    CatLog.d(this, "Uninstall App by user deactivation");
                    StkAppInstaller.unInstall(StkAppService.this.mContext);
                    return;
                default:
                    return;
            }
        }

        private void handleCardStatusChangeAndIccRefresh(Bundle args) {
            if ("CTC".equals(SystemProperties.get("ro.csc.sales_code", ""))) {
                IccRefreshResponse state = new IccRefreshResponse();
                state.refreshResult = args.getInt("refresh_result");
                CatLog.d(this, "Icc Refresh Result: " + state.refreshResult);
                if (state.refreshResult == 2) {
                    StkAppService.this.mMessageHandler.postDelayed(new C00041(), 5000);
                }
            }
        }
    }

    public void onCreate() {
        if ("DCG".equals("DGG") || "DCGG".equals("DGG") || "DCGS".equals("DGG") || "DCGGS".equals("DGG") || "CG".equals("DGG")) {
            this.mStkService = CatService.getInstance(1);
        } else {
            this.mStkService = CatService.getInstance();
        }
        this.mCmdsQ = new LinkedList();
        Thread serviceThread = new Thread(null, this, "Stk App Service");
        this.salesCode = SystemProperties.get("ro.csc.sales_code");
        serviceThread.start();
        this.mContext = getBaseContext();
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        sInstance = this;
        this.mWakeLock = ((PowerManager) getSystemService("power")).newWakeLock(805306394, "STK");
        this.mKeyguardManager = (KeyguardManager) getSystemService("keyguard");
        this.mSetEventList = new boolean[20];
        for (int i = 0; i < 20; i++) {
            this.mSetEventList[i] = false;
        }
        this.mMessageHandler = new MessageHandler();
    }

    public void onStart(Intent intent, int startId) {
        CatLog.d(this, "onStart)");
        waitForLooper();
        if ("DCG".equals("DGG") || "DCGG".equals("DGG") || "DCGS".equals("DGG") || "DCGGS".equals("DGG") || "CG".equals("DGG")) {
            this.mStkService = CatService.getInstance(1);
        } else {
            this.mStkService = CatService.getInstance();
        }
        if (intent != null) {
            Bundle args = intent.getExtras();
            if (args != null) {
                Message msg = this.mServiceHandler.obtainMessage();
                msg.arg1 = args.getInt("op");
                switch (msg.arg1) {
                    case 1:
                        msg.obj = args.getParcelable("cmd message");
                        break;
                    case 2:
                    case 9:
                    case 17:
                        msg.obj = args;
                        break;
                    case 3:
                    case 4:
                    case 5:
                    case 10:
                        break;
                    case 7:
                        msg.obj = args.getParcelable("event");
                        break;
                    case 100:
                        msg.obj = args;
                        break;
                    default:
                        return;
                }
                CatLog.d(this, "Before SendMessage to ServiceHandler)");
                this.mServiceHandler.sendMessage(msg);
                CatLog.d(this, "After SendMessage to ServiceHandler)");
            }
        }
    }

    public void onDestroy() {
        waitForLooper();
        this.mServiceLooper.quit();
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    public void run() {
        Looper.prepare();
        this.mServiceLooper = Looper.myLooper();
        this.mServiceHandler = new ServiceHandler();
        Looper.loop();
    }

    void indicateMenuVisibility(boolean visibility) {
        this.mMenuIsVisibile = visibility;
    }

    Menu getMenu() {
        return this.mCurrentMenu;
    }

    Menu getMainMenu() {
        if (this.mMainCmd != null) {
            return this.mMainCmd.getMenu();
        }
        return null;
    }

    static StkAppService getInstance() {
        return sInstance;
    }

    private void waitForLooper() {
        while (this.mServiceHandler == null) {
            synchronized (this) {
                try {
                    wait(100);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    private boolean isCmdInteractive(CatCmdMessage cmd) {
        switch (C00022.f0xca33cf42[cmd.getCmdType().ordinal()]) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
                CatLog.d(this, "Command is Informative!");
                return false;
            default:
                return true;
        }
    }

    private void handleDelayedCmd() {
        if (this.mCmdsQ.size() != 0) {
            DelayedCmd cmd = (DelayedCmd) this.mCmdsQ.poll();
            switch (cmd.id) {
                case 1:
                    handleCmd(cmd.msg);
                    return;
                case 4:
                    handleSessionEnd();
                    return;
                default:
                    return;
            }
        }
    }

    private void handleEvent(CatEventDownload event) {
        if (event.getEvent() == 8 && this.mIsProxyChanged) {
            CatLog.d(this, "received browser termination event and proxy was chaned, restore proxy");
            ((ConnectivityManager) this.mContext.getSystemService("connectivity")).setGlobalProxy(this.mBackupProxy);
            this.mBackupProxy = null;
            this.mIsProxyChanged = false;
        }
        if (this.mSetEventList[event.getEvent()]) {
            switch (event.getEvent()) {
                case 4:
                    CatLog.d(this, "send user activity event to RIL");
                    this.mStkService.onEventDownload(new CatEnvelopeMessage(4, 130, 129, null));
                    MultiSimManager.setTelephonyProperty("gsm.sim.userEvent", 0, "0");
                    this.mSetEventList[event.getEvent()] = false;
                    return;
                case 5:
                    CatLog.d(this, "send Idle screen event to RIL");
                    this.mStkService.onEventDownload(new CatEnvelopeMessage(5, 2, 129, null));
                    MultiSimManager.setTelephonyProperty("gsm.sim.screenEvent", 0, "0");
                    this.mSetEventList[event.getEvent()] = false;
                    return;
                case 7:
                    CatLog.d(this, "send setting language event to RIL, language = " + event.getLanguage());
                    this.mStkService.onEventDownload(new CatEnvelopeMessage(7, 130, 129, new byte[]{(byte) 45, (byte) 2, (byte) event.getLanguage().codePointAt(0), (byte) event.getLanguage().codePointAt(1)}));
                    return;
                case 8:
                    CatLog.d(this, "browser termination event to RIL, cause = " + event.getBrowserTerminationCause());
                    this.mStkService.onEventDownload(new CatEnvelopeMessage(8, 130, 129, new byte[]{(byte) -76, (byte) 1, (byte) event.getBrowserTerminationCause()}));
                    return;
                default:
                    return;
            }
        }
    }

    private void callDelayedMsg() {
        Message msg = this.mServiceHandler.obtainMessage();
        msg.arg1 = 6;
        this.mServiceHandler.sendMessage(msg);
    }

    private void handleSessionEnd() {
        CatLog.d(this, "handleSessionEnd()");
        this.mCurrentCmd = this.mMainCmd;
        this.lastSelectedItem = null;
        CatLog.d(this, "unlockMenuActivityBlock()");
        unlockMenuActivityBlock();
        if (!(this.mCurrentMenu == null || this.mMainCmd == null)) {
            this.mCurrentMenu = this.mMainCmd.getMenu();
            this.mIsMainMenu = true;
        }
        if (!this.mIsStartedByUser && isRunningStk()) {
            CatLog.d(this, "distroyMenuActivity");
            distroyMenuActivity();
        } else if (this.mMenuIsVisibile) {
            launchMenuActivity(null);
        }
        if (this.mCmdsQ.size() != 0) {
            callDelayedMsg();
        } else {
            this.mCmdInProgress = false;
        }
        if (this.launchBrowser) {
            this.launchBrowser = false;
            launchBrowser(this.mBrowserSettings);
        }
        this.lock.lock();
        try {
            if (this.mWakeLock.isHeld()) {
                CatLog.d(this, "before release wakeup");
                this.mWakeLock.release();
            }
            this.lock.unlock();
            enableKeyguard();
        } catch (Throwable th) {
            this.lock.unlock();
        }
    }

    private void handleCmd(CatCmdMessage cmdMsg) {
        if (cmdMsg != null) {
            Bundle args;
            ResultCode resCode;
            if (this.mStkService == null) {
                if ("DCG".equals("DGG") || "DCGG".equals("DGG") || "DCGS".equals("DGG") || "DCGGS".equals("DGG") || "CG".equals("DGG")) {
                    this.mStkService = CatService.getInstance(1);
                } else {
                    this.mStkService = CatService.getInstance();
                }
                if (this.mStkService == null) {
                    args = new Bundle();
                    args.putInt("op", 2);
                    resCode = ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS;
                    args.putInt("response id", 24);
                    args.putInt("error code", resCode.value());
                    args.putBoolean("additional info", false);
                    startService(new Intent(this, StkAppService.class).putExtras(args));
                }
            }
            this.mCurrentCmd = cmdMsg;
            boolean waitForUsersResponse = true;
            CatLog.d(this, cmdMsg.getCmdType().name());
            if (cmdMsg.getCmdType() == CommandType.SET_UP_MENU || cmdMsg.getCmdType() == CommandType.SELECT_ITEM) {
                unlockMenuActivityBlock();
            } else {
                lockMenuActivityBlock();
            }
            switch (C00022.f0xca33cf42[cmdMsg.getCmdType().ordinal()]) {
                case 1:
                case 2:
                case 3:
                case 4:
                    waitForUsersResponse = false;
                    launchEventMessage();
                    break;
                case 5:
                    waitForUsersResponse = false;
                    break;
                case 6:
                    this.mMainCmd = this.mCurrentCmd;
                    this.mCurrentMenu = cmdMsg.getMenu();
                    if ("DCG".equals("DGG") || "DCGG".equals("DGG") || "DCGS".equals("DGG") || "DCGGS".equals("DGG") || "CG".equals("DGG")) {
                        this.mStkService = CatService.getInstance(1);
                    } else {
                        this.mStkService = CatService.getInstance();
                    }
                    if (removeMenu()) {
                        CatLog.d(this, "Uninstall App");
                        this.mCurrentMenu = null;
                        StkAppInstaller.unInstall(this.mContext);
                    } else {
                        CatLog.d(this, "Install App");
                        if ("CHU".equals(this.salesCode)) {
                            int mSimActive = System.getInt(this.mContext.getContentResolver(), "phone1_on", 1);
                            CatLog.d(this, "[STK]DB in Settings, mSimActive=" + mSimActive);
                            if (this.mMainCmd != null && 1 == mSimActive) {
                                StkAppInstaller.install(this.mContext);
                            }
                        } else {
                            StkAppInstaller.install(this.mContext);
                        }
                    }
                    if (this.mMenuIsVisibile) {
                        launchMenuActivity(null);
                    }
                    args = new Bundle();
                    args.putInt("op", 2);
                    args.putInt("response id", 14);
                    startService(new Intent(this, StkAppService.class).putExtras(args));
                    if (!(this.mStkService == null || this.mIsSystemShutdown)) {
                        this.mStkService.sentTerminalResponseForSetupMenu(true);
                        break;
                    }
                    break;
                case 7:
                    waitForUsersResponse = false;
                    if (CscFeature.getInstance().getEnableStatus("CscFeature_RIL_RemoveToastDuringStkRefresh")) {
                        CatLog.d(this, "Do not display a toast for SIM Refresh");
                        break;
                    }
                    break;
                case 8:
                    TextMessage msg = cmdMsg.geTextMessage();
                    this.responseNeeded = msg.responseNeeded;
                    if (this.lastSelectedItem != null) {
                        msg.title = this.lastSelectedItem;
                    } else if (this.mMainCmd != null) {
                        msg.title = this.mMainCmd.getMenu().title;
                    } else {
                        msg.title = "";
                    }
                    if (!msg.isHighPriority) {
                        if (!msg.isHighPriority) {
                            if (!canLaunchDisptextDialog()) {
                                CatLog.d(this, "Can not display Normal Priority text");
                                args = new Bundle();
                                args.putInt("op", 2);
                                resCode = ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS;
                                args.putInt("response id", 24);
                                args.putInt("error code", resCode.value());
                                args.putBoolean("additional info", true);
                                args.putInt("additional info data", 1);
                                startService(new Intent(this, StkAppService.class).putExtras(args));
                                break;
                            }
                            launchTextDialog();
                            break;
                        }
                    }
                    launchTextDialog();
                    break;
                    break;
                case 9:
                    this.mCurrentMenu = cmdMsg.getMenu();
                    launchMenuActivity(cmdMsg.getMenu());
                    break;
                case 10:
                case 11:
                    launchInputActivity();
                    break;
                case 12:
                    if (this.mCurrentCmd.getBrowserSettings().mode == LaunchBrowserMode.LAUNCH_IF_NOT_ALREADY_LAUNCHED && !canLaunchBrowser()) {
                        CatLog.d(this, "Launch Browser mode is LAUNCH_IF_NOT_ALREADY_LAUNCHED and browser is already launched");
                        Bundle argsBrowser = new Bundle();
                        argsBrowser.putInt("op", 2);
                        resCode = ResultCode.LAUNCH_BROWSER_ERROR;
                        argsBrowser.putInt("response id", 24);
                        argsBrowser.putInt("error code", resCode.value());
                        argsBrowser.putBoolean("additional info", true);
                        argsBrowser.putInt("additional info data", 2);
                        startService(new Intent(this, StkAppService.class).putExtras(argsBrowser));
                        break;
                    }
                    this.mCurrentCmd.geTextMessage().userClear = true;
                    launchConfirmationDialog(this.mCurrentCmd.geTextMessage());
                    break;
                    break;
                case 13:
                    if (this.mCurrentCmd.getCallSettings().confirmMsg.text == null) {
                        CharSequence message = this.mContext.getText(R.string.default_call_setup_msg);
                        this.mCurrentCmd.getCallSettings().confirmMsg.text = message.toString();
                    }
                    launchConfirmationDialog(this.mCurrentCmd.getCallSettings().confirmMsg);
                    break;
                case 14:
                    launchToneDialog();
                    break;
                case 15:
                    processSetEventList(cmdMsg.getNumberOfEventList(), cmdMsg.getEventList());
                    break;
                case 16:
                    processLanguageNotification(cmdMsg.getLanguage(), cmdMsg.getinitLanguage());
                    break;
                case 17:
                    launchConfirmationDialog(this.mCurrentCmd.geTextMessage());
                    CatLog.d(this, "OPEN CHANNEL");
                    break;
                case 18:
                    launchConfirmationDialog(this.mCurrentCmd.geTextMessage());
                    CatLog.d(this, "CLOSE CHANNEL");
                    break;
                case 19:
                case 20:
                    waitForUsersResponse = false;
                    launchEventMessage();
                    CatLog.d(this, cmdMsg.getCmdType().name());
                    break;
            }
            if (!waitForUsersResponse) {
                if (this.mCmdsQ.size() != 0) {
                    callDelayedMsg();
                } else {
                    this.mCmdInProgress = false;
                }
            }
        }
    }

    private boolean canLaunchDisptextDialog() {
        CatLog.d(this, "canLaunchDisptextDialog");
        Context iContext = getBaseContext();
        PackageManager pm = iContext.getPackageManager();
        if (pm == null) {
            CatLog.d(this, "Package Manager is NULL");
            return true;
        }
        ActivityManager am = (ActivityManager) iContext.getSystemService("activity");
        if (am == null) {
            CatLog.d(this, "Activity Manager is NULL");
            return true;
        }
        ResolveInfo homeInfo = pm.resolveActivity(new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME"), 0);
        if (homeInfo == null) {
            CatLog.d(this, "homdInfo is NULL");
            return true;
        }
        CatLog.d(this, "Value of original Packgage name" + homeInfo.activityInfo.packageName);
        List<RunningTaskInfo> runningTaskInfo = am.getRunningTasks(1);
        if (runningTaskInfo == null) {
            CatLog.d(this, "runningTaskInfo == null");
            return true;
        }
        RunningTaskInfo runInfo = (RunningTaskInfo) runningTaskInfo.get(0);
        CatLog.d(this, "Getting first Running task info");
        CatLog.d(this, "Value of package name" + runInfo.topActivity.getPackageName());
        if (!runInfo.topActivity.getPackageName().equals(homeInfo.activityInfo.packageName) && !runInfo.topActivity.getPackageName().equals("com.android.stk")) {
            return false;
        }
        CatLog.d(this, "Package Name matches");
        return true;
    }

    private boolean canLaunchBrowser() {
        CatLog.d(this, "canLaunchBrowser");
        ActivityManager am = (ActivityManager) getBaseContext().getSystemService("activity");
        if (am == null) {
            CatLog.d(this, "Activity Manager is NULL");
            return true;
        }
        List<RunningTaskInfo> runningTaskInfo = am.getRunningTasks(1);
        if (runningTaskInfo == null) {
            CatLog.d(this, "runningTaskInfo == null");
            return true;
        }
        RunningTaskInfo runInfo = (RunningTaskInfo) runningTaskInfo.get(0);
        CatLog.d(this, "Getting first Running task info");
        CatLog.d(this, "Value of package name" + runInfo.topActivity.getPackageName());
        if (!runInfo.topActivity.getPackageName().equals("com.android.browser") && !runInfo.topActivity.getPackageName().equals("com.sec.android.app.sbrowser")) {
            return true;
        }
        CatLog.d(this, "Package Name matches");
        return false;
    }

    private boolean isRunningStk() {
        CatLog.d(this, "isRunningStk");
        ActivityManager am = (ActivityManager) getBaseContext().getSystemService("activity");
        if (am == null) {
            CatLog.d(this, "Activity Manager is NULL");
            return false;
        }
        List<RunningTaskInfo> runningTaskInfo = am.getRunningTasks(1);
        if (runningTaskInfo == null) {
            CatLog.d(this, "runningTaskInfo == null");
            return false;
        } else if (runningTaskInfo.isEmpty()) {
            return false;
        } else {
            RunningTaskInfo runInfo = (RunningTaskInfo) runningTaskInfo.get(0);
            CatLog.d(this, "Getting first Running task info");
            CatLog.d(this, "Value of class name : " + runInfo.topActivity.getClassName());
            if (!runInfo.topActivity.getClassName().equals("com.android.stk.StkMenuActivity")) {
                return false;
            }
            CatLog.d(this, "Class Name matches");
            return true;
        }
    }

    private void handleCmdResponse(Bundle args) {
        if (this.mCurrentCmd != null) {
            CatResponseMessage resMsg = new CatResponseMessage(this.mCurrentCmd);
            boolean helpRequired = args.getBoolean("help", false);
            switch (args.getInt("response id")) {
                case 11:
                    CatLog.d(this, "RES_ID_MENU_SELECTION");
                    int menuSelection = args.getInt("menu selection");
                    switch (C00022.f0xca33cf42[this.mCurrentCmd.getCmdType().ordinal()]) {
                        case 6:
                        case 9:
                            this.lastSelectedItem = getItemName(menuSelection);
                            if (helpRequired) {
                                resMsg.setResultCode(ResultCode.HELP_INFO_REQUIRED);
                            } else {
                                resMsg.setResultCode(ResultCode.OK);
                            }
                            resMsg.setMenuSelection(menuSelection);
                            break;
                    }
                    break;
                case 12:
                    CatLog.d(this, "RES_ID_INPUT");
                    String input = args.getString("input");
                    Input cmdInput = this.mCurrentCmd.geInput();
                    if (cmdInput == null || !cmdInput.yesNo) {
                        if (!helpRequired) {
                            resMsg.setResultCode(ResultCode.OK);
                            resMsg.setInput(input);
                            break;
                        }
                        resMsg.setResultCode(ResultCode.HELP_INFO_REQUIRED);
                        break;
                    }
                    resMsg.setYesNo(input.equals("YES"));
                    break;
                    break;
                case 13:
                    CatLog.d(this, "RES_ID_CONFIRM");
                    boolean confirmed = args.getBoolean("confirm");
                    switch (C00022.f0xca33cf42[this.mCurrentCmd.getCmdType().ordinal()]) {
                        case 8:
                            resMsg.setResultCode(confirmed ? ResultCode.OK : ResultCode.UICC_SESSION_TERM_BY_USER);
                            break;
                        case 12:
                            resMsg.setResultCode(confirmed ? ResultCode.OK : ResultCode.UICC_SESSION_TERM_BY_USER);
                            if (confirmed) {
                                this.launchBrowser = true;
                                this.mBrowserSettings = this.mCurrentCmd.getBrowserSettings();
                                break;
                            }
                            break;
                        case 13:
                            resMsg.setResultCode(ResultCode.OK);
                            resMsg.setConfirmation(confirmed);
                            if (confirmed) {
                                launchCallMsg();
                                break;
                            }
                            break;
                        case 17:
                            CatLog.d(this, "Open Channel Command response");
                            resMsg.setResultCode(ResultCode.OK);
                            resMsg.setConfirmation(confirmed);
                            if (!confirmed) {
                                CatLog.d(this, "User did not Confirm!");
                                break;
                            }
                            CatLog.d(this, "User Confirmed");
                            launchBIPChannel("OPEN CHANNEL");
                            break;
                        case 18:
                            CatLog.d(this, "Close Channel Command response");
                            resMsg.setResultCode(confirmed ? ResultCode.OK : ResultCode.UICC_SESSION_TERM_BY_USER);
                            resMsg.setConfirmation(confirmed);
                            if (!confirmed) {
                                CatLog.d(this, "User did not Confirm!");
                                break;
                            }
                            CatLog.d(this, "User Confirmed");
                            launchBIPChannel("CLOSE CHANNEL");
                            break;
                        default:
                            break;
                    }
                case 14:
                    resMsg.setResultCode(ResultCode.OK);
                    break;
                case 20:
                    CatLog.d(this, "RES_ID_TIMEOUT");
                    if (this.mCurrentCmd.getCmdType().value() == CommandType.DISPLAY_TEXT.value() && !this.mCurrentCmd.geTextMessage().userClear) {
                        resMsg.setResultCode(ResultCode.OK);
                        break;
                    } else {
                        resMsg.setResultCode(ResultCode.NO_RESPONSE_FROM_USER);
                        break;
                    }
                case 21:
                    CatLog.d(this, "RES_ID_BACKWARD");
                    resMsg.setResultCode(ResultCode.BACKWARD_MOVE_BY_USER);
                    break;
                case 22:
                    CatLog.d(this, "RES_ID_END_SESSION");
                    resMsg.setResultCode(ResultCode.UICC_SESSION_TERM_BY_USER);
                    break;
                case 24:
                    CatLog.d(this, "RES_ID_GENERAL_ERROR");
                    int errorCode = args.getInt("error code");
                    boolean additionalInfo = args.getBoolean("additional info");
                    resMsg.setResultCode(ResultCode.fromInt(errorCode));
                    resMsg.setAdditionalInfo(additionalInfo);
                    if (additionalInfo) {
                        resMsg.setAdditionalInfoData(args.getInt("additional info data"));
                        break;
                    }
                    break;
                default:
                    CatLog.d(this, "Unknown result id");
                    return;
            }
            if (this.mStkService == null) {
                if ("DCG".equals("DGG") || "DCGG".equals("DGG") || "DCGS".equals("DGG") || "DCGGS".equals("DGG") || "CG".equals("DGG")) {
                    this.mStkService = CatService.getInstance(1);
                } else {
                    this.mStkService = CatService.getInstance();
                }
            }
            if (this.mStkService != null) {
                this.mStkService.onCmdResponse(resMsg);
            }
        }
    }

    private int getFlagActivityNoUserAction(InitiatedByUserAction userAction) {
        return ((userAction == InitiatedByUserAction.yes ? 1 : 0) | this.mMenuIsVisibile) != 0 ? 0 : 262144;
    }

    private void launchMenuActivity(Menu menu) {
        int intentFlags;
        Intent newIntent = new Intent("android.intent.action.VIEW");
        newIntent.setClassName("com.android.stk", "com.android.stk.StkMenuActivity");
        if (menu == null) {
            intentFlags = 335544320 | getFlagActivityNoUserAction(InitiatedByUserAction.yes);
            newIntent.putExtra("STATE", 1);
        } else {
            intentFlags = 335544320 | getFlagActivityNoUserAction(InitiatedByUserAction.unknown);
            newIntent.putExtra("STATE", 2);
        }
        newIntent.setFlags(intentFlags);
        this.mContext.startActivity(newIntent);
    }

    private void distroyMenuActivity() {
        Intent newIntent = new Intent("android.intent.action.VIEW");
        newIntent.setClassName("com.android.stk", "com.android.stk.StkMenuActivity");
        int intentFlags = 335544320 | getFlagActivityNoUserAction(InitiatedByUserAction.unknown);
        newIntent.putExtra("STATE", 3);
        newIntent.setFlags(intentFlags);
        this.mContext.startActivity(newIntent);
    }

    private void launchInputActivity() {
        Intent newIntent = new Intent("android.intent.action.VIEW");
        newIntent.setFlags(268435456 | getFlagActivityNoUserAction(InitiatedByUserAction.unknown));
        newIntent.setClassName("com.android.stk", "com.android.stk.StkInputActivity");
        newIntent.putExtra("INPUT", this.mCurrentCmd.geInput());
        this.mContext.startActivity(newIntent);
    }

    private void launchTextDialog() {
        Intent newIntent = new Intent(this, StkDialogActivity.class);
        newIntent.setFlags(1484783616 | getFlagActivityNoUserAction(InitiatedByUserAction.unknown));
        newIntent.putExtra("TEXT", this.mCurrentCmd.geTextMessage());
        MultiWindowStyle style = new MultiWindowStyle();
        style.setType(0);
        newIntent.setMultiWindowStyle(style);
        startActivity(newIntent);
        if (StkApp.calculateDurationInMilis(this.mCurrentCmd.geTextMessage().duration) == 6000000) {
            CatLog.d(this, "sustained text. It'll be delete with new display text");
            this.mCmdInProgress = false;
        }
    }

    private void launchEventMessage() {
        TextMessage msg = this.mCurrentCmd.geTextMessage();
        if (msg != null && msg.text != null) {
            if (!this.mWakeLock.isHeld()) {
                this.mWakeLock.acquire();
                Message wakelockMsg = this.mServiceHandler.obtainMessage();
                wakelockMsg.arg1 = 8;
                this.mServiceHandler.sendMessageDelayed(wakelockMsg, 10000);
            }
            disableKeyguard();
            Toast toast = new Toast(this.mContext.getApplicationContext());
            View v = ((LayoutInflater) this.mContext.getSystemService("layout_inflater")).inflate(R.layout.stk_event_msg, null);
            TextView tv = (TextView) v.findViewById(16908299);
            ImageView iv = (ImageView) v.findViewById(16908294);
            if (msg.icon != null) {
                iv.setImageBitmap(msg.icon);
            } else {
                iv.setVisibility(8);
            }
            tv.setText(msg.text);
            toast.setView(v);
            toast.setDuration(1);
            toast.setGravity(80, 0, 50);
            toast.show();
        }
    }

    private void launchConfirmationDialog(TextMessage msg) {
        switch (C00022.f0xca33cf42[this.mCurrentCmd.getCmdType().ordinal()]) {
            case 17:
                msg.title = "OPEN CHANNEL";
                break;
            case 18:
                msg.title = "CLOSE CHANNEL";
                break;
            default:
                msg.title = this.lastSelectedItem;
                break;
        }
        CatLog.d(this, "Launch Dialog");
        Intent newIntent = new Intent(this, StkDialogActivity.class);
        newIntent.setFlags(1350565888 | getFlagActivityNoUserAction(InitiatedByUserAction.unknown));
        newIntent.putExtra("TEXT", msg);
        startActivity(newIntent);
    }

    private void launchBrowser(BrowserSettings settings) {
        if (settings != null) {
            if (settings.url == null) {
                CatLog.d(this, "url is null, so try to get default url from browser");
                this.mContext.sendBroadcast(new Intent("android.intent.action.STK_BROWSER_GET_HOMEPAGE"));
                return;
            }
            Uri data;
            String gatewayProxy = settings.gatewayProxy;
            if (gatewayProxy != null) {
                CatLog.d(this, "gateway/proxy informaion is in launch browser cmd, change proxy");
                ConnectivityManager cm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
                this.mBackupProxy = cm.getGlobalProxy();
                ProxyInfo tempProxy = cm.getProxy();
                int port = 80;
                String exclusionList = "";
                if (tempProxy != null) {
                    port = tempProxy.getPort();
                    exclusionList = tempProxy.getExclusionListAsString();
                }
                cm.setGlobalProxy(new ProxyInfo(gatewayProxy, port, exclusionList));
                this.mIsProxyChanged = true;
                MultiSimManager.setTelephonyProperty("gsm.sim.browserEvent", 0, "1");
            }
            Intent intent = new Intent("android.intent.action.VIEW");
            CatLog.d(this, "settings.url = " + settings.url);
            if (settings.url.startsWith("http://") || settings.url.startsWith("https://") || settings.url.startsWith("dss://")) {
                data = Uri.parse(settings.url);
            } else {
                String modifiedUrl = "http://" + settings.url;
                CatLog.d(this, "modifiedUrl = " + modifiedUrl);
                data = Uri.parse(modifiedUrl);
            }
            intent.setData(data);
            intent.addFlags(268435456);
            switch (C00022.$SwitchMap$com$android$internal$telephony$cat$LaunchBrowserMode[settings.mode.ordinal()]) {
                case 1:
                    intent.addFlags(67108864);
                    break;
                case 2:
                    intent.addFlags(134217728);
                    break;
                case 3:
                    intent.addFlags(67108864);
                    break;
            }
            startActivity(intent);
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
            }
        }
    }

    private void launchCallMsg() {
        TextMessage msg = this.mCurrentCmd.getCallSettings().callMsg;
        if (msg.text == null || msg.text.length() == 0) {
            msg.text = this.mContext.getText(R.string.default_call_setup_msg).toString();
        }
        msg.title = this.lastSelectedItem;
        Toast toast = Toast.makeText(this.mContext.getApplicationContext(), msg.text, 1);
        toast.setGravity(80, 0, 50);
        toast.show();
    }

    private void processSetEventList(int numberOfEvents, int[] events) {
        int i;
        CatLog.d(this, "processSetEventList");
        boolean allHandledEvent = true;
        for (i = 0; i < numberOfEvents; i++) {
            switch (events[i]) {
                case 4:
                    MultiSimManager.setTelephonyProperty("gsm.sim.userEvent", 0, "1");
                    this.mSetEventList[4] = true;
                    break;
                case 5:
                    MultiSimManager.setTelephonyProperty("gsm.sim.screenEvent", 0, "1");
                    this.mSetEventList[5] = true;
                    break;
                case 7:
                    MultiSimManager.setTelephonyProperty("gsm.sim.lenguageEvent", 0, "1");
                    this.mSetEventList[7] = true;
                    break;
                case 8:
                    MultiSimManager.setTelephonyProperty("gsm.sim.browserEvent", 0, "1");
                    this.mSetEventList[8] = true;
                    break;
                case 9:
                    CatLog.d(this, "processSetEventList data");
                    if (this.mStkService == null) {
                        break;
                    }
                    this.mStkService.setEventListDataAvailable(true);
                    break;
                case 10:
                    CatLog.d(this, "processSetEventList channel");
                    if (this.mStkService == null) {
                        break;
                    }
                    this.mStkService.setEventListChannelStatus(true);
                    break;
                case 255:
                    for (int j = 0; j < 20; j++) {
                        this.mSetEventList[j] = false;
                    }
                    MultiSimManager.setTelephonyProperty("gsm.sim.userEvent", 0, "0");
                    MultiSimManager.setTelephonyProperty("gsm.sim.screenEvent", 0, "0");
                    MultiSimManager.setTelephonyProperty("gsm.sim.lenguageEvent", 0, "0");
                    MultiSimManager.setTelephonyProperty("gsm.sim.browserEvent", 0, "0");
                    break;
                default:
                    break;
            }
        }
        Bundle args = new Bundle();
        args.putInt("op", 2);
        if ("SKT".equals("") || "LGT".equals("")) {
            i = 0;
            while (i < numberOfEvents) {
                if (events[i] == 9 || events[i] == 10 || events[i] == 255) {
                    i++;
                } else {
                    allHandledEvent = false;
                }
            }
        } else {
            i = 0;
            while (i < numberOfEvents) {
                if (events[i] == 0 || events[i] == 1 || events[i] == 2 || events[i] == 3 || events[i] == 4 || events[i] == 5 || events[i] == 7 || events[i] == 8 || events[i] == 11 || events[i] == 9 || events[i] == 10 || events[i] == 255 || events[i] == 18) {
                    i++;
                } else {
                    allHandledEvent = false;
                }
            }
        }
        if (allHandledEvent) {
            args.putInt("response id", 14);
        } else {
            ResultCode resCode = ResultCode.BEYOND_TERMINAL_CAPABILITY;
            args.putInt("response id", 24);
            args.putInt("error code", resCode.value());
            args.putBoolean("additional info", false);
        }
        startService(new Intent(this, StkAppService.class).putExtras(args));
    }

    private void processLanguageNotification(String language, boolean initLanguage) {
        CatLog.d(this, "processLanguageNotification language = " + language + ", init = " + initLanguage);
        Boolean handled = Boolean.valueOf(true);
        Bundle args = new Bundle();
        args.putInt("op", 2);
        if (initLanguage) {
            CatLog.d(this, "Language info is null, and init language");
            language = "en";
        }
        if (initLanguage || language != null) {
            try {
                IActivityManager am = ActivityManagerNative.getDefault();
                if (am == null) {
                    handled = Boolean.valueOf(false);
                } else {
                    Configuration config = am.getConfiguration();
                    Locale locale = new Locale(language);
                    if (config == null || locale == null) {
                        handled = Boolean.valueOf(false);
                    } else {
                        config.locale = locale;
                        config.userSetLocale = true;
                        am.updateConfiguration(config);
                    }
                }
            } catch (RemoteException e) {
                handled = Boolean.valueOf(false);
            }
            if (handled.booleanValue()) {
                args.putInt("response id", 14);
            } else {
                ResultCode resCode = ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS;
                args.putInt("response id", 24);
                args.putInt("error code", resCode.value());
                args.putBoolean("additional info", false);
            }
            startService(new Intent(this, StkAppService.class).putExtras(args));
            return;
        }
        CatLog.d(this, "Language info is null, and not init language");
    }

    private void launchToneDialog() {
        Intent newIntent = new Intent(this, ToneDialog.class);
        newIntent.setFlags(1350565888 | getFlagActivityNoUserAction(InitiatedByUserAction.unknown));
        newIntent.putExtra("TEXT", this.mCurrentCmd.geTextMessage());
        newIntent.putExtra("TONE", this.mCurrentCmd.getToneSettings());
        startActivity(newIntent);
    }

    private String getItemName(int itemId) {
        Menu menu = this.mCurrentCmd.getMenu();
        if (menu == null) {
            return null;
        }
        for (Item item : menu.items) {
            if (item.id == itemId) {
                return item.text;
            }
        }
        return null;
    }

    private boolean removeMenu() {
        try {
            if (this.mCurrentMenu.items.size() == 1 && this.mCurrentMenu.items.get(0) == null) {
                return true;
            }
            return false;
        } catch (NullPointerException e) {
            CatLog.d(this, "Unable to get Menu's items size");
            return true;
        }
    }

    synchronized void enableKeyguard() {
        if (this.mKeyguardLock != null) {
            this.mKeyguardLock.reenableKeyguard();
            this.mKeyguardLock = null;
        }
    }

    synchronized void disableKeyguard() {
        if (this.mKeyguardLock == null) {
            this.mKeyguardLock = this.mKeyguardManager.newKeyguardLock("STK");
            this.mKeyguardLock.disableKeyguard();
        }
    }

    private void unlockMenuActivityBlock() {
        if (this.mMenuItemBlock) {
            CatLog.d(this, "unlockMenuActivityBlock");
            this.mMenuItemBlock = false;
            this.mMessageHandler.removeMessages(1);
            CatLog.d(this, "has message! : " + this.mMessageHandler.hasMessages(1));
        }
    }

    void lockMenuActivityBlock() {
        if (!this.mMenuItemBlock) {
            CatLog.d(this, "lockMenuActivityBlock");
            this.mMessageHandler.removeMessages(1);
            this.mMenuItemBlock = true;
            this.mMessageHandler.sendMessageDelayed(this.mMessageHandler.obtainMessage(1), 60000);
        }
    }

    private void launchBIPChannel(String Str) {
        TextMessage msg = this.mCurrentCmd.geTextMessage();
        if (msg.text == null || msg.text.length() == 0) {
            msg.text = "Sending...";
        }
        msg.title = Str;
        CatLog.d(this, "Launch BIP Channel");
        Toast toast = Toast.makeText(this.mContext.getApplicationContext(), msg.text, 1);
        toast.setGravity(80, 0, 50);
        toast.show();
    }

    public void clearmIsStartedByUser() {
        this.mIsStartedByUser = false;
    }

    public boolean isAirplaneMode() {
        if (this.mStkService == null) {
            return false;
        }
        return this.mStkService.isAirplaneMode();
    }

    public Context getContext() {
        return this.mContext;
    }
}
