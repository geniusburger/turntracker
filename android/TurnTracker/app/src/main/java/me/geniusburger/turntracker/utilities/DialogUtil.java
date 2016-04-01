package me.geniusburger.turntracker.utilities;

import android.app.AlertDialog;
import android.content.Context;

import me.geniusburger.turntracker.R;

public class DialogUtil {

    public static void displayErrorDialog(Context context, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setIcon(R.drawable.ic_warning_24dp);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setNegativeButton("Dismiss", null);
        builder.create().show();
    }

    public static void displayInfoDialog(Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message);
        builder.setNegativeButton("OK", null);
        builder.create().show();
    }

}
