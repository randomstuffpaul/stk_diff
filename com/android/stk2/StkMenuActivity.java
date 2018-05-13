package com.android.stk2;

import android.app.ActionBar;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings.System;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.telephony.cat.CatLog;
import com.android.internal.telephony.cat.Item;
import com.android.internal.telephony.cat.Menu;
import java.util.ArrayList;

public class StkMenuActivity extends ListActivity {
    private static String salesCode;
    StkAppService appService = StkAppService.getInstance();
    private boolean mAcceptUsersInput = true;
    private Context mContext;
    private boolean mEntered = false;
    private ProgressBar mProgressView = null;
    private BroadcastReceiver mReceiver = null;
    private int mState = 1;
    private Menu mStkMenu = null;
    Handler mTimeoutHandler = new C00071();
    private ImageView mTitleIconView = null;
    private TextView mTitleTextView = null;

    class C00071 extends Handler {
        C00071() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    StkMenuActivity.this.mAcceptUsersInput = false;
                    StkMenuActivity.this.sendResponse(20);
                    return;
                default:
                    return;
            }
        }
    }

    class C00082 extends BroadcastReceiver {
        C00082() {
        }

        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.AIRPLANE_MODE".equals(intent.getAction())) {
                int airPlaneEnabled = System.getInt(StkMenuActivity.this.mContext.getContentResolver(), "airplane_mode_on", 0);
                CatLog.d("SIM2", this, "Received  ACTION_AIRPLANE_MODE_CHANGED = " + airPlaneEnabled);
                if (airPlaneEnabled == 1) {
                    StkMenuActivity.this.finish();
                }
            }
        }
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        CatLog.d("SIM2", this, "onCreate");
        setContentView(R.layout.stk_menu_list);
        this.mTitleTextView = (TextView) findViewById(R.id.title_text);
        this.mTitleIconView = (ImageView) findViewById(R.id.title_icon);
        this.mProgressView = (ProgressBar) findViewById(R.id.progress_bar);
        this.mContext = getBaseContext();
        if (SystemProperties.get("gsm.STK_SETUP_MENU2").length() > 0) {
            setTitle(SystemProperties.get("gsm.STK_SETUP_MENU2"));
        }
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            salesCode = SystemProperties.get("ro.csc.sales_code");
            if ("CHU".equals(salesCode)) {
                actionBar.setDisplayOptions(11);
                actionBar.setLogo(R.drawable.logo_cu);
                actionBar.setTitle(R.string.app_name_cu);
                CatLog.d(this, "set cu feature for stk interface ");
            }
        }
        getListView().setOnCreateContextMenuListener(this);
        initFromIntent(getIntent());
        this.mAcceptUsersInput = true;
        this.mEntered = false;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.AIRPLANE_MODE");
        Context context = this.mContext;
        BroadcastReceiver c00082 = new C00082();
        this.mReceiver = c00082;
        context.registerReceiver(c00082, filter);
        if (this.appService == null) {
            CatLog.d(this, "appService is null in onCreate()");
            finish();
        } else if (this.appService.isAirplaneMode() || isSimDisabled()) {
            showStkDisabledMessage();
            finish();
        }
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        CatLog.d("SIM2", this, "onNewIntent");
        initFromIntent(intent);
        this.mAcceptUsersInput = true;
    }

    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        if (this.mAcceptUsersInput) {
            Item item = getSelectedItem(position);
            if (item == null) {
                return;
            }
            if (this.appService.mMenuItemBlock) {
                CatLog.d("SIM2", this, "menu blocked");
                return;
            }
            cancelTimeOut();
            sendResponse(11, item.id, false);
            this.mAcceptUsersInput = false;
            this.mProgressView.setVisibility(0);
            this.mProgressView.setIndeterminate(true);
            return;
        }
        CatLog.d("SIM2", this, "!mAcceptUsersInput");
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!this.mAcceptUsersInput) {
            return true;
        }
        if (this.appService.mMenuItemBlock) {
            CatLog.d("SIM2", this, "menu blocked");
            return true;
        }
        switch (keyCode) {
            case 4:
                switch (this.mState) {
                    case 2:
                        cancelTimeOut();
                        this.mAcceptUsersInput = false;
                        sendResponse(21);
                        return true;
                    default:
                        break;
                }
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onResume() {
        super.onResume();
        if (this.appService == null) {
            showStkDisabledMessage();
            finish();
        } else if (this.appService.isAirplaneMode() || isSimDisabled()) {
            showStkDisabledMessage();
            finish();
        } else {
            this.appService.indicateMenuVisibility(true);
            this.mStkMenu = this.appService.getMenu();
            if (this.mStkMenu == null) {
                CatLog.d("SIM2", this, "onDestroy, mStkMenu is null");
                finish();
                return;
            }
            displayMenu();
            if (!this.mAcceptUsersInput || this.appService.mIsMainMenu) {
                CatLog.d("SIM2", this, "onResume : It's STK MAIN MENU");
                this.mState = 1;
                this.mAcceptUsersInput = true;
                this.appService.mIsMainMenu = false;
            }
            invalidateOptionsMenu();
            this.mProgressView.setIndeterminate(false);
            this.mProgressView.setVisibility(8);
            this.mEntered = true;
            startTimeOut();
        }
    }

    public void onPause() {
        super.onPause();
        this.appService.indicateMenuVisibility(false);
        this.mAcceptUsersInput = false;
        if ("ZTR".equals(SystemProperties.get("ro.csc.sales_code"))) {
            cancelTimeOut();
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (this.appService != null) {
            this.appService.clearmIsStartedByUser();
        }
        CatLog.d(this, "onDestroy");
        if (this.mReceiver != null) {
            try {
                this.mContext.unregisterReceiver(this.mReceiver);
            } catch (IllegalArgumentException e) {
                CatLog.d(this, "Failed unregisterReceiver");
            }
        }
    }

    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, 1, 1, R.string.menu_end_session);
        menu.add(0, 3, 2, R.string.help);
        return true;
    }

    public boolean onPrepareOptionsMenu(android.view.Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean helpVisible = false;
        boolean mainVisible = false;
        Menu mTempStkMenu = this.appService.getMenu();
        Menu mTempMainStkMenu = this.appService.getMainMenu();
        if (!(mTempStkMenu == null || mTempMainStkMenu == null)) {
            ArrayList items_stk_temp = (ArrayList) ((ArrayList) mTempStkMenu.items).clone();
            items_stk_temp.removeAll(mTempMainStkMenu.items);
            if (items_stk_temp.size() == 0) {
                this.mState = 1;
            } else {
                this.mState = 2;
            }
        }
        if (this.mState == 2) {
            mainVisible = true;
        }
        if (this.mStkMenu != null) {
            helpVisible = this.mStkMenu.helpAvailable;
        }
        menu.findItem(1).setVisible(mainVisible);
        menu.findItem(3).setVisible(helpVisible);
        if (this.mAcceptUsersInput && !this.appService.mMenuItemBlock) {
            return true;
        }
        CatLog.d("SIM2", this, "onPrepareOptionsMenu mAcceptUsersInput:" + this.mAcceptUsersInput + ", " + "appService.mMenuItemBlock:" + this.appService.mMenuItemBlock);
        return false;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (!this.mAcceptUsersInput) {
            return true;
        }
        if (this.appService.mMenuItemBlock) {
            CatLog.d("SIM2", this, "menu blocked");
            return true;
        }
        switch (item.getItemId()) {
            case 1:
                cancelTimeOut();
                this.mAcceptUsersInput = false;
                sendResponse(22);
                return true;
            case 3:
                cancelTimeOut();
                this.mAcceptUsersInput = false;
                Item stkItem = getSelectedItem(getSelectedItemPosition());
                if (stkItem != null) {
                    sendResponse(11, stkItem.id, true);
                    return true;
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        if (this.mStkMenu != null) {
            CatLog.d("SIM2", this, "onCreateContextMenu helpAvailable = " + this.mStkMenu.helpAvailable);
            if (this.mStkMenu.helpAvailable) {
                menu.add(0, 3, 0, R.string.help);
                return;
            }
            return;
        }
        CatLog.d("SIM2", this, "onCreateContextMenu, mStkMenu is null");
    }

    public boolean onContextItemSelected(MenuItem item) {
        if (!this.mAcceptUsersInput) {
            return true;
        }
        if (this.appService.mMenuItemBlock) {
            CatLog.d("SIM2", this, "menu blocked");
            return true;
        }
        switch (item.getItemId()) {
            case 3:
                try {
                    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
                    cancelTimeOut();
                    this.mAcceptUsersInput = false;
                    Item stkItem = getSelectedItem(info.position);
                    if (stkItem != null) {
                        sendResponse(11, stkItem.id, true);
                        return true;
                    }
                } catch (ClassCastException e) {
                    CatLog.d("SIM2", this, "bad menuInfo");
                    break;
                }
                break;
        }
        return super.onContextItemSelected(item);
    }

    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("STATE", this.mState);
        outState.putParcelable("MENU", this.mStkMenu);
    }

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        this.mState = savedInstanceState.getInt("STATE");
        this.mStkMenu = (Menu) savedInstanceState.getParcelable("MENU");
    }

    private boolean isSimDisabled() {
        if (System.getInt(this.mContext.getContentResolver(), "phone2_on", 1) == 0) {
            return true;
        }
        return false;
    }

    private void showStkDisabledMessage() {
        if (this.appService == null) {
            CatLog.d(this, "appService is null for showStkDisabledMessage");
            return;
        }
        CharSequence message;
        int airmode = System.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0);
        CatLog.d(this, "device is in AirplaneMode:" + System.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) + "stk sart airmode = " + airmode);
        if (airmode == 1) {
            message = this.appService.getContext().getText(R.string.airplanemode);
        } else {
            message = this.appService.getContext().getText(R.string.stk_no_service);
        }
        Toast toast = Toast.makeText(this.appService.getContext().getApplicationContext(), message.toString(), 1);
        toast.setGravity(80, 0, 50);
        toast.show();
    }

    private void cancelTimeOut() {
        this.mTimeoutHandler.removeMessages(1);
    }

    private void startTimeOut() {
        if (this.mState == 2 || (this.mState == 1 && "KDI".equals(SystemProperties.get("ro.csc.sales_code")))) {
            int timeOut = 40000;
            if (this.mEntered) {
                timeOut = StkApp.getUItimeOutforSTKcmds("Select item");
                if (timeOut < 0) {
                    timeOut = 60000;
                }
            }
            CatLog.d("SIM2", this, "startTimeOut for => " + timeOut);
            cancelTimeOut();
            this.mTimeoutHandler.sendMessageDelayed(this.mTimeoutHandler.obtainMessage(1), (long) timeOut);
        }
    }

    private void displayMenu() {
        if (this.mStkMenu == null) {
            CatLog.d(this, "mStkMenu == null in displayMenu()");
            return;
        }
        if (this.mStkMenu.titleIcon != null) {
            this.mTitleIconView.setImageBitmap(this.mStkMenu.titleIcon);
            this.mTitleIconView.setVisibility(0);
        } else {
            this.mTitleIconView.setVisibility(8);
        }
        if (this.mStkMenu.titleIconSelfExplanatory) {
            this.mTitleTextView.setVisibility(4);
        } else {
            this.mTitleTextView.setVisibility(0);
            salesCode = SystemProperties.get("ro.csc.sales_code");
            if ("CHU".equals(salesCode)) {
                this.mTitleTextView.setText(R.string.app_name_cu);
            } else if (this.mStkMenu.title == null) {
                this.mTitleTextView.setText(R.string.app_name);
            } else {
                this.mTitleTextView.setText(this.mStkMenu.title);
            }
        }
        setListAdapter(new StkMenuAdapter(this, this.mStkMenu.items, this.mStkMenu.itemsIconSelfExplanatory));
        setSelection(this.mStkMenu.defaultItem);
    }

    private void initFromIntent(Intent intent) {
        if (intent != null) {
            this.mState = intent.getIntExtra("STATE", 1);
            invalidateOptionsMenu();
        } else {
            finish();
        }
        if (this.mState == 3) {
            CatLog.d("SIM2", this, "get end intent");
            finish();
        }
    }

    private Item getSelectedItem(int position) {
        Item item = null;
        if (this.mStkMenu == null) {
            return item;
        }
        try {
            return (Item) this.mStkMenu.items.get(position);
        } catch (IndexOutOfBoundsException e) {
            CatLog.d("SIM2", this, "Invalid menu");
            return item;
        } catch (NullPointerException e2) {
            CatLog.d("SIM2", this, "Invalid menu");
            return item;
        }
    }

    private void sendResponse(int resId) {
        sendResponse(resId, 0, false);
    }

    private void sendResponse(int resId, int itemId, boolean help) {
        Bundle args = new Bundle();
        args.putInt("op", 2);
        args.putInt("response id", resId);
        args.putInt("menu selection", itemId);
        args.putBoolean("help", help);
        this.mContext.startService(new Intent(this.mContext, StkAppService.class).putExtras(args));
    }
}
