package org.tsg.android;
import android.content.Context;
final class GpuDiagnostics {
    private static native String nativeReport(String hooks,String temp,String directory,String library);
    static String probe(Context c) {
        System.loadLibrary("tsg_gpu_probe");
        GpuDrivers.Driver driver=GpuDrivers.selected(c);
        return nativeReport(c.getApplicationInfo().nativeLibraryDir,c.getCacheDir().getAbsolutePath(),
            driver==null?"":driver.directory.getAbsolutePath(),driver==null?"":driver.library);
    }
}
