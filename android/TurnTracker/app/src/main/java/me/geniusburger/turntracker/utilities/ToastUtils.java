package me.geniusburger.turntracker.utilities;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.util.function.BiFunction;

public class ToastUtils {

    public static void showToastOnUiThread(final Context context, final String text, final int duration, String tag, BiFunction<String,String,Integer> log) {
        log.apply(tag, text);
        showToastOnUiThread(context, text, duration);
    }

    public static void showToastOnUiThread(final Context context, final String text, final int duration) {
        Handler mainThread = new Handler(Looper.getMainLooper());
        mainThread.post(() -> Toast.makeText(context, text, duration).show());
    }
}
