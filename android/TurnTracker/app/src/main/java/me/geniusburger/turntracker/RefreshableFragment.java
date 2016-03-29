package me.geniusburger.turntracker;

import android.app.Fragment;
import android.content.Context;
import android.view.View;

public abstract class RefreshableFragment extends Fragment {
    public abstract void onRefresh(Context context);
    public abstract void onFabClick(View view);
}
