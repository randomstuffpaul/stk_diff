package com.android.stk;

import android.text.method.NumberKeyListener;

public class StkDigitsKeyListener extends NumberKeyListener {
    public static final char[] CHARACTERS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '*', '#', '+'};
    private static StkDigitsKeyListener sInstance;

    protected char[] getAcceptedChars() {
        return CHARACTERS;
    }

    public int getInputType() {
        return 3;
    }

    public static StkDigitsKeyListener getInstance() {
        if (sInstance != null) {
            return sInstance;
        }
        sInstance = new StkDigitsKeyListener();
        return sInstance;
    }
}
