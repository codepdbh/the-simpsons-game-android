package org.tsg.android;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.List;
final class GameOptions {
    static SharedPreferences prefs(Context c) {return c.getSharedPreferences("tsg_settings",0);}
    static String get(Context c,String key,String fallback) {return prefs(c).getString(key,fallback);}
    static void arguments(Context c,List<String> args) {
        boolean compatibility=get(c,"profile","balanced").equals("compatibility");
        args.add("--frame_rate="+get(c,"fps","60"));
        args.add("--menu_frame_rate=30");
        args.add("--vulkan_pipeline_creation_threads="+get(c,"workers","2"));
        args.add("--async_shader_compilation="+get(c,"async","true"));
        args.add("--store_shaders=true");
        args.add("--render_target_path_vulkan=fbo");
        args.add("--vulkan_dynamic_rendering=false");
        args.add("--vulkan_native_shader_features=false");
        args.add("--vulkan_require_geometry_shader=false");
        args.add("--vulkan_require_fill_mode_non_solid=false");
        args.add("--vulkan_prefer_geometry_shader="+!compatibility);
        args.add("--vulkan_allow_present_mode_immediate=false");
        args.add("--vulkan_allow_present_mode_mailbox="+!compatibility);
        args.add("--vulkan_allow_present_mode_fifo_relaxed=false");
        args.add("--present_letterbox="+get(c,"letterbox","true"));
        GpuDrivers.arguments(c,args);
    }
}
