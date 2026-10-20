package org.tsg.android;

import android.content.Context;
import android.os.Build;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.*;

/** App-owned diagnostics only: no game files, save files or folder permissions. */
final class IssueReport {
    static final String ISSUES="https://github.com/codepdbh/the-simpsons-game-android/issues/new";
    static String summary(Context c) {
        GpuDrivers.Driver driver=GpuDrivers.selected(c);
        return "App: "+BuildConfig.VERSION_NAME+" ("+BuildConfig.VERSION_CODE+")\n"+
            "Device: "+Build.MANUFACTURER+" "+Build.MODEL+"\nAndroid: "+Build.VERSION.RELEASE+" (API "+Build.VERSION.SDK_INT+")\n"+
            "Hardware: "+Build.HARDWARE+"\nABI: "+String.join(", ",Build.SUPPORTED_ABIS)+"\n"+
            "Profile: "+GameOptions.get(c,"profile","balanced")+"\nFPS target: "+GameOptions.get(c,"fps","30")+
            "\nShader workers: "+GameOptions.get(c,"workers","2")+"\nAsync shaders: "+GameOptions.get(c,"async","false")+
            "\nEyes: "+GameOptions.get(c,"eyes","clean")+"\nOutlines: "+GameOptions.get(c,"outlines","original")+
            "\nDirect resolve: "+GameOptions.get(c,"direct_resolve","true")+
            "\nLanguage ID: "+GameOptions.prefs(c).getInt("language",5)+"\nDriver: "+(driver==null?"system":driver.name)+"\n";
    }
    static File create(Context c,String description) throws Exception {
        File folder=new File(c.getCacheDir(),"reports");
        if(!folder.isDirectory()&&!folder.mkdirs()) throw new IOException("No se pudo crear el informe");
        File[] old=folder.listFiles();if(old!=null) for(File f:old) if(f.isFile()) f.delete();
        String stamp=new SimpleDateFormat("yyyyMMdd-HHmmss",Locale.ROOT).format(new Date());
        File report=new File(folder,"TSG-report-"+stamp+".zip");
        try(ZipOutputStream zip=new ZipOutputStream(new FileOutputStream(report))) {
            entry(zip,"report.txt",(summary(c)+"\nProblem / Problema:\n"+description+"\n").getBytes(StandardCharsets.UTF_8));
            File[] logs=new File(c.getFilesDir(),"user/logs").listFiles(f->f.isFile()&&f.getName().matches("simpsons_[0-9]+\\.log"));
            if(logs!=null) {
                Arrays.sort(logs,(a,b)->Long.compare(b.lastModified(),a.lastModified()));
                for(int i=0;i<Math.min(3,logs.length);i++) entry(zip,"logs/"+logs[i].getName(),tail(logs[i],1024*1024));
            }
            entry(zip,"android-crash.txt",ownCrashLog().getBytes(StandardCharsets.UTF_8));
            if(Build.VERSION.SDK_INT>=30) try {
                android.app.ActivityManager manager=(android.app.ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE);
                StringBuilder exits=new StringBuilder();int traces=0;
                for(android.app.ApplicationExitInfo exit:manager.getHistoricalProcessExitReasons(c.getPackageName(),0,5)) {
                    String name=exit.getProcessName();if(name==null||!name.startsWith(c.getPackageName())) continue;
                    exits.append(name).append(" reason=").append(exit.getReason()).append(" status=").append(exit.getStatus())
                        .append(" timestamp=").append(exit.getTimestamp()).append(" description=").append(exit.getDescription()).append('\n');
                    try(InputStream trace=exit.getTraceInputStream()) {
                        if(trace!=null&&traces<2) {
                            ByteArrayOutputStream data=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
                            while(data.size()<1024*1024&&(n=trace.read(buffer,0,Math.min(buffer.length,1024*1024-data.size())))!=-1) data.write(buffer,0,n);
                            entry(zip,"exit-trace-"+(++traces)+".trace",data.toByteArray());
                        }
                    } catch(IOException|SecurityException ignored) { }
                }
                entry(zip,"process-exits.txt",exits.toString().getBytes(StandardCharsets.UTF_8));
            } catch(RuntimeException error) {
                entry(zip,"process-exits.txt","Process-exit history unavailable.\n".getBytes(StandardCharsets.UTF_8));
            }
        } catch(Exception error) {report.delete();throw error;}
        return report;
    }
    private static byte[] tail(File file,int limit) throws IOException {
        try(RandomAccessFile input=new RandomAccessFile(file,"r")) {
            long start=Math.max(0,input.length()-limit);input.seek(start);
            byte[] data=new byte[(int)Math.min(limit,input.length()-start)];input.readFully(data);return data;
        }
    }
    private static void entry(ZipOutputStream zip,String name,byte[] data) throws IOException {
        zip.putNextEntry(new ZipEntry(name));zip.write(data);zip.closeEntry();
    }
    private static String ownCrashLog() {
        File capture=null;
        Process process=null;
        try {
            capture=File.createTempFile("tsg-crash-",".txt");
            process=new ProcessBuilder("logcat","-d","-b","crash","-t","600").redirectOutput(capture).redirectErrorStream(true).start();
            if(!process.waitFor(5,TimeUnit.SECONDS)) {process.destroyForcibly();return "Crash log capture timed out.\n";}
            String all=new String(tail(capture,1024*1024),StandardCharsets.UTF_8);
            StringBuilder own=new StringBuilder();
            // Native tombstones identify the package in Cmdline. Do not include
            // crashes belonging to other apps from the shared logcat buffer.
            for(String block:all.split("(?m)(?=^.*F DEBUG\\s*: \\*\\*\\* \\*\\*\\*)"))
                if(block.matches("(?s).*Cmdline: org\\.tsg\\.android(?::game|:probe)?\\s.*"))
                    for(String line:block.split("\n")) if(line.matches(".*\\sF\\s+DEBUG\\s*:.*")) own.append(line).append('\n');
            return own.length()==0?"No native crash report available for org.tsg.android.\n":own.toString();
        } catch(Exception error) {return "Crash log unavailable; runtime logs are included when present.\n";}
        finally {if(process!=null&&process.isAlive()) process.destroyForcibly();if(capture!=null) capture.delete();}
    }
}
