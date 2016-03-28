package me.geniusburger.turntracker.utilities;

import android.content.Context;
import android.content.res.Resources;

import me.geniusburger.turntracker.R;

public class UnitMapping {

    private static final int DEFAULT_MULTIPLIER = 1;

    private static UnitMapping instance;

    private String[] labels;
    private int[] multipliers;
    private int zeroIndex;
    private int defaultIndex;

    private UnitMapping(Context context) {
        Resources res = context.getResources();
        labels = res.getStringArray(R.array.unitLabels);
        multipliers = res.getIntArray(R.array.unitMultipliers);

        if (!(labels.length == multipliers.length && labels.length > 0)) {
            throw new AssertionError();
        }

        for(int i = 0; i < multipliers.length; i++) {
            int m = multipliers[i];
            if(m == 0) {
                zeroIndex = i;
            }
            if(m == DEFAULT_MULTIPLIER) {
                defaultIndex = i;
            }
        }
    }

    public static UnitMapping getInstance(Context context) {
        if(instance == null) {
            instance = new UnitMapping(context);
        }
        return instance;
    }

    public int getMatchingIndex(int hours) {
        if(hours == 0) {
            return zeroIndex;
        }

        // assume arrays are already in descending order by multiplier
        for(int i = multipliers.length - 1; i >= 0; i--) {
            if(hours % multipliers[i] == 0) {
                return i;
            }
        }
        return defaultIndex;
    }

    public String getMatchingText(int hours) {
        int i = getMatchingIndex(hours);
        hours /= multipliers[i];
        String label = labels[i].toLowerCase();
        if(hours == 1 && label.endsWith("s")) {
            label = label.substring(0,label.length()-1);
        }
        return String.format("%d %s", hours, label);
    }

    public int getMultiplier(int i) {
        return multipliers[i];
    }

    public String[] getLabels() {
        return labels;
    }

    public int getDefaultIndex() {
        return defaultIndex;
    }
}
