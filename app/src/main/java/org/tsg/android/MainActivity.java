package org.tsg.android;
import org.libsdl.app.SDLActivity;
public class MainActivity extends LandscapeSDLActivity {
    @Override protected String[] getLibraries() { return new String[] { "SDL3", "main" }; }
}
