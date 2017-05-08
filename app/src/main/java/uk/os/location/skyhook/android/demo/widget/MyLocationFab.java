package uk.os.location.skyhook.android.demo.widget;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;

import uk.os.location.skyhook.android.demo.R;

public class MyLocationFab extends FloatingActionButton {

    private boolean isEnabled = false;

    public MyLocationFab(Context context) {
        this(context, null);
    }

    public MyLocationFab(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyLocationFab(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        updateIcon();
    }

    public void disabled() {
        enabled(false);
    }

    public void enabled() {
        enabled(true);
    }

    public void enabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
        updateIcon();
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("superState", super.onSaveInstanceState());
        bundle.putBoolean("enabled", isEnabled);
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            isEnabled = bundle.getBoolean("enabled");
            state = bundle.getParcelable("superState");
        }
        updateIcon();
        super.onRestoreInstanceState(state);
    }

    private void updateIcon() {
        if (isEnabled) {
            setImageResource(R.drawable.ic_location_disabled);
        } else {
            setImageResource(R.drawable.ic_my_location);
        }
    }
}
