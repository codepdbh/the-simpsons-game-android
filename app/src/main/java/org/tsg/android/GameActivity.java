package org.tsg.android;
import org.libsdl.app.SDLActivity;
import android.os.Bundle;
import android.system.Os;
import android.system.ErrnoException;
import java.io.File;
public class GameActivity extends SDLActivity {
    @Override protected void onCreate(Bundle state) {
        File user = new File(getFilesDir(), "user"); user.mkdirs();
        try { Os.setenv("REX_APP_FOLDER", user.getAbsolutePath(), true); }
        catch (ErrnoException e) { throw new IllegalStateException("Cannot configure runtime folder", e); }
        super.onCreate(state);
    }
    @Override protected String[] getLibraries() { return new String[] { "SDL3", "tsg_game" }; }
    @Override protected String[] getArguments() {
        return new String[] { "--frame_rate=30", "--menu_frame_rate=30", "--gpu=vulkan", "--gpu_plugin=xenos", "--vulkan_dynamic_rendering=false", "--vulkan_native_shader_features=false" };
    }
}
