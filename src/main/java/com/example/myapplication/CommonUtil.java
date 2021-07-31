package com.example.myapplication;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.Player;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Locale;

public class CommonUtil {

    @NonNull
    public static String humanReadableByteCountSI(double bytes) {
        if (bytes < 1000) {
            return ((int) bytes) + "";
        }
        CharacterIterator ci = new StringCharacterIterator(" KMGTPE");
        while (Math.abs(bytes) >= 1000) {
            bytes = bytes / 1000.0;
            ci.next();
        }
        Log.d("Util", "humanReadableByteCountSI: " + bytes);
        return String.format(Locale.ENGLISH, "%.1f%c", bytes, ci.current());
    }

    @NonNull
    public static String getStateString(int state) {
        switch (state) {
            case Player.STATE_BUFFERING:
                return "BUFFERING";
            case Player.STATE_ENDED:
                return "ENDED";
            case Player.STATE_IDLE:
                return "IDLE";
            case Player.STATE_READY:
                return "READY";
            default:
                return "?";
        }
    }
}
