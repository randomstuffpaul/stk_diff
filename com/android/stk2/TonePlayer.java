package com.android.stk2;

import android.media.ToneGenerator;
import com.android.internal.telephony.cat.Tone;
import java.util.HashMap;

public class TonePlayer {
    private static final HashMap<Tone, Integer> mToneMap = new HashMap();
    private ToneGenerator mToneGenerator;

    static {
        mToneMap.put(Tone.DIAL, Integer.valueOf(16));
        mToneMap.put(Tone.BUSY, Integer.valueOf(17));
        mToneMap.put(Tone.CONGESTION, Integer.valueOf(18));
        mToneMap.put(Tone.RADIO_PATH_ACK, Integer.valueOf(19));
        mToneMap.put(Tone.RADIO_PATH_NOT_AVAILABLE, Integer.valueOf(20));
        mToneMap.put(Tone.ERROR_SPECIAL_INFO, Integer.valueOf(21));
        mToneMap.put(Tone.CALL_WAITING, Integer.valueOf(22));
        mToneMap.put(Tone.RINGING, Integer.valueOf(23));
        mToneMap.put(Tone.GENERAL_BEEP, Integer.valueOf(24));
        mToneMap.put(Tone.POSITIVE_ACK, Integer.valueOf(25));
        mToneMap.put(Tone.NEGATIVE_ACK, Integer.valueOf(26));
    }

    TonePlayer() {
        this.mToneGenerator = null;
        this.mToneGenerator = new ToneGenerator(3, 100);
    }

    public void play(Tone tone, int timeout) {
        int toneId = getToneId(tone);
        if (toneId > 0 && this.mToneGenerator != null) {
            this.mToneGenerator.startTone(toneId, timeout);
        }
    }

    public void stop() {
        if (this.mToneGenerator != null) {
            this.mToneGenerator.stopTone();
        }
    }

    public void release() {
        if (this.mToneGenerator != null) {
            this.mToneGenerator.release();
            this.mToneGenerator = null;
        }
    }

    private int getToneId(Tone tone) {
        if (tone == null || !mToneMap.containsKey(tone)) {
            return 24;
        }
        return ((Integer) mToneMap.get(tone)).intValue();
    }
}
