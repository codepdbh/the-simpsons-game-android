package org.tsg.android;
import org.libsdl.app.SDLActivity;
public class MainActivity extends SDLActivity {
    @Override protected String[] getLibraries() { return new String[] { "SDL3", "main" }; }
}
