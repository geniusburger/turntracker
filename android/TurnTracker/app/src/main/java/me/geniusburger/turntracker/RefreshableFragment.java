package me.geniusburger.turntracker;

import android.app.Fragment;
import android.view.View;

public abstract class RefreshableFragment extends Fragment {
    public abstract void onRefresh();
    public abstract void onFabClick(View view);
}
