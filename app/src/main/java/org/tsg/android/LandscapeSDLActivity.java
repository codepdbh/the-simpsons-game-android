package org.tsg.android;

import android.content.pm.ActivityInfo;
import org.libsdl.app.SDLActivity;

/** SDL's resizable window must not replace the manifest's landscape lock. */
public class LandscapeSDLActivity extends SDLActivity {
    @Override public void setRequestedOrientation(int requestedOrientation) {
        super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Override protected void onResume() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        super.onResume();
    }
}
