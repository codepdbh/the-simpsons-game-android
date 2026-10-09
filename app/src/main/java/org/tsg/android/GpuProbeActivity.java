package org.tsg.android;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
public final class GpuProbeActivity extends Activity {
    private boolean finished;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        TextView text=new TextView(this); text.setText("Comprobando GPU y driver Vulkan…");
        text.setTextColor(-1); text.setBackgroundColor(0xff10141b); text.setGravity(17); setContentView(text);
        Handler handler=new Handler(Looper.getMainLooper());
        handler.postDelayed(()->finishReport("{\"probeError\":\"La prueba excedió 20 segundos\"}"),20000);
        new Thread(()-> {
            String report;
            try {report=GpuDiagnostics.probe(this);}
            catch(Exception|LinkageError e) {report="{\"probeError\":\"No se pudo cargar el diagnóstico\"}";}
            String result=report; handler.post(()->finishReport(result));
        },"TSG GPU probe").start();
    }
    private void finishReport(String report) {
        if(finished) return; finished=true; setResult(RESULT_OK,new Intent().putExtra("gpu_report",report)); finish();
    }
    @Override protected void onDestroy() {super.onDestroy(); android.os.Process.killProcess(android.os.Process.myPid());}
}
