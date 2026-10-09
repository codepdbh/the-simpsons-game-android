package org.tsg.android;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.view.InputDevice;
import android.widget.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LauncherActivity extends Activity {
    private static final int SELECT_TREE = 10;
    // Local executable supplied for this port's first boot experiment. Boot
    // validation is recorded separately; a hash match alone is not game support.
    private static final String EXPECTED_SHA = "CD477E4DFD8EF35AEB380790DE90AB52130A118DC342E0650F32CC09F2C29704";
    private TextView status;
    private Button select, importButton, start;
    private Uri tree;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private volatile boolean busy;
    private LinearLayout options;
    private static final int DRIVER_ZIP=11, GPU_PROBE=12;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        LinearLayout columns=new LinearLayout(this);columns.setPadding(dp(24),dp(16),dp(24),dp(16));
        columns.setBackground(new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR,new int[]{0xff0b1018,0xff251c09,0xff0b1018}));
        LinearLayout left=new LinearLayout(this);left.setOrientation(1);
        ScrollView leftScroll=new ScrollView(this);leftScroll.setFillViewport(true);leftScroll.addView(left);
        columns.addView(leftScroll,new LinearLayout.LayoutParams(0,-1,1.05f));
        LinearLayout identity=new LinearLayout(this);identity.setGravity(android.view.Gravity.CENTER_VERTICAL);
        ImageView icon=new ImageView(this);icon.setImageResource(R.drawable.icon_art);identity.addView(icon,new LinearLayout.LayoutParams(dp(68),dp(68)));
        TextView title=label("THE SIMPSONS GAME\nANDROID EVOLVED",18,0xffffd83d);title.setPadding(dp(12),0,0,0);identity.addView(title,new LinearLayout.LayoutParams(0,-2,1));left.addView(identity);
        left.addView(label("Xbox 360 · recompilado para ARM64",12,0xffa7abb2));
        LinearLayout files=card();left.addView(files);files.addView(label("ARCHIVOS DEL JUEGO · TSG",12,0xffffd83d));
        status=label("",13,0xffdedfe4);files.addView(status);
        start=button(left,"JUGAR",this::startGame);start.setBackground(tint(0xffffd83d));start.setTextColor(0xff17191c);
        select = button(left, "Elegir carpeta TSG", () -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,
                Uri.parse("content://com.android.externalstorage.documents/document/primary%3ATSG"));
            startActivityForResult(intent, SELECT_TREE);
        });
        importButton = button(left, "Importar los datos seleccionados", this::importGame);
        left.addView(label("Mando virtual personalizable: pulsa EDITAR dentro del juego. También admite mando físico.",12,0xffa7abb2));
        LinearLayout right=card();LinearLayout.LayoutParams rightParams=new LinearLayout.LayoutParams(0,-1,1);rightParams.leftMargin=dp(24);columns.addView(right,rightParams);
        right.addView(label("GRÁFICOS Y COMPATIBILIDAD",12,0xffffd83d));right.addView(label("Los cambios se aplican al iniciar el juego.",12,0xffa7abb2));
        ScrollView scroll=new ScrollView(this);options=new LinearLayout(this);options.setOrientation(1);scroll.addView(options);right.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        button(right,"Prueba Vulkan 1.1",()->startActivity(new Intent(this,MainActivity.class)));
        setContentView(columns);
        String saved = getPreferences(MODE_PRIVATE).getString("tree", null);
        if (saved != null) tree = Uri.parse(saved);
        refresh();
    }
    private Button button(LinearLayout layout, String text, Runnable action) {
        Button result = new Button(this); result.setText(text); result.setAllCaps(false);result.setTextSize(14);result.setTextColor(-1);result.setBackground(tint(0xff272b33));result.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,dp(text.contains("\n")?60:48));params.topMargin=dp(8);layout.addView(result,params);return result;
    }
    private int dp(int value) {return Math.round(value*getResources().getDisplayMetrics().density);}
    private android.graphics.drawable.GradientDrawable tint(int color) {android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setCornerRadius(dp(12));return d;}
    private TextView label(String text,int size,int color) {TextView t=new TextView(this);t.setText(text);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(4),0,dp(4));return t;}
    private LinearLayout card() {LinearLayout c=new LinearLayout(this);c.setOrientation(1);c.setPadding(dp(16),dp(12),dp(16),dp(12));c.setBackground(tint(0xe61a1e25));return c;}
    private void choice(String key,String title,String[] values,String[] labels,String fallback) {
        String value=GameOptions.get(this,key,fallback);int selected=0;
        for(int i=0;i<values.length;i++) if(values[i].equals(value)) selected=i;
        final int checked=selected;
        button(options,title+"\n"+labels[selected],()->new android.app.AlertDialog.Builder(this).setTitle(title)
            .setSingleChoiceItems(labels,checked,(dialog,which)-> {GameOptions.prefs(this).edit().putString(key,values[which]).apply();dialog.dismiss();refreshOptions();}).setNegativeButton("Cancelar",null).show());
    }
    private void refreshOptions() {
        options.removeAllViews();
        choice("profile","Perfil gráfico",new String[]{"balanced","compatibility"},new String[]{"Equilibrado","Compatibilidad · Mali / Xclipse / otros"},"balanced");
        choice("fps","Límite de FPS del juego",new String[]{"30","60"},new String[]{"30 FPS · estabilidad","60 FPS · experimental"},"60");
        choice("workers","Compilación de shaders",new String[]{"1","2","4"},new String[]{"1 hilo · menor carga CPU","2 hilos · equilibrado","4 hilos · carga más rápida"},"2");
        choice("async","Shaders en segundo plano",new String[]{"true","false"},new String[]{"Activado · puede omitir frames nuevos","Desactivado · puede pausar al compilar"},"true");
        choice("letterbox","Formato de imagen",new String[]{"true","false"},new String[]{"16:9 original","Ocupar toda la pantalla"},"true");
        String[] names={"Español","Italiano","English","Français","Deutsch"};int[] ids={5,6,1,4,3};
        int language=GameOptions.prefs(this).getInt("language",5),selected=0;for(int i=0;i<ids.length;i++) if(ids[i]==language) selected=i;
        final int checked=selected;button(options,"Idioma del juego\n"+names[selected],()->new android.app.AlertDialog.Builder(this).setTitle("Idioma · requiere los datos correspondientes")
            .setSingleChoiceItems(names,checked,(dialog,which)->{GameOptions.prefs(this).edit().putInt("language",ids[which]).apply();dialog.dismiss();refreshOptions();}).setNegativeButton("Cancelar",null).show());
        GpuDrivers.Driver driver=GpuDrivers.selected(this);
        button(options,"Driver Vulkan\n"+(driver==null?"Del sistema":driver.name),this::chooseDriver);
        button(options,"Comprobar GPU y driver",()->startActivityForResult(new Intent(this,GpuProbeActivity.class),GPU_PROBE)).setEnabled(BuildConfig.WITH_GAME&&!busy);
        options.addView(label("Mali, Exynos y Xclipse usan el driver del sistema como base. Un driver externo requiere hardware y firmware compatibles; la prueba no certifica gameplay.",12,0xffa7abb2));
    }
    private void chooseDriver() {
        if(!BuildConfig.WITH_GAME) return;
        java.util.List<GpuDrivers.Driver> drivers=GpuDrivers.list(this);String[] names=new String[drivers.size()+1];names[0]="Driver del sistema";
        for(int i=0;i<drivers.size();i++) names[i+1]=drivers.get(i).name;
        new android.app.AlertDialog.Builder(this).setTitle("Driver Vulkan").setItems(names,(dialog,which)-> {GpuDrivers.select(this,which==0?"":drivers.get(which-1).id);refreshOptions();})
            .setPositiveButton("Importar ZIP",(dialog,which)-> {Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.setType("application/zip");intent.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(intent,DRIVER_ZIP);})
            .setNegativeButton("Cancelar",null).show();
    }
    private void refresh() {
        StringBuilder pads = new StringBuilder();
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice device = InputDevice.getDevice(id);
            if (device != null && (device.getSources() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) pads.append(device.getName()).append(" ");
        }
        boolean installed = new File(getFilesDir(), "game/default.xex").isFile();
        status.setText((installed?"✓ Datos importados · SHA al iniciar":"Elige TSG e importa tus datos")+"\nVulkan 1.1 · ARM64\n"+(BuildConfig.WITH_GAME?"Runtime experimental":"Build diagnóstico")+"\nMando: "+(pads.length()==0?"virtual":pads));
        select.setEnabled(!busy); importButton.setEnabled(!busy && tree != null);
        start.setEnabled(!busy && installed && BuildConfig.WITH_GAME);
        refreshOptions();
    }
    @Override protected void onResume() { super.onResume(); setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); if (status != null && !busy) refresh(); }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request,result,data);
        if(request==GPU_PROBE) {
            String report=result==RESULT_OK&&data!=null?data.getStringExtra("gpu_report"):"La prueba fue interrumpida";
            try {
                org.json.JSONObject gpu=new org.json.JSONObject(report);
                report=gpu.has("probeError")?gpu.getString("probeError"):
                    gpu.optString("gpu")+"\nVulkan "+gpu.optString("vulkan")+"\n\n"+
                    (gpu.optBoolean("compatible")?"Funciones básicas disponibles. Falta comprobar el juego en este dispositivo.":"Faltan funciones necesarias: "+gpu.optJSONArray("missing"));
            } catch(Exception ignored) { }
            new android.app.AlertDialog.Builder(this).setTitle("Diagnóstico Vulkan").setMessage(report).setPositiveButton("Aceptar",null).show();return;
        }
        if(request==DRIVER_ZIP&&result==RESULT_OK&&data!=null&&data.getData()!=null) {
            Uri zip=data.getData();busy=true;refresh();status.setText("Importando driver…");worker.execute(()-> {
                try {GpuDrivers.Driver driver=GpuDrivers.importZip(this,zip);GpuDrivers.select(this,driver.id);runOnUiThread(()-> {busy=false;refresh();status.setText("Driver importado. Compruébalo antes de jugar.");});}
                catch(Exception error) {showError(error);}
            });return;
        }
        if (request == SELECT_TREE && result == RESULT_OK && data != null) {
            tree = data.getData(); if (tree == null) return;
            try {
                getContentResolver().takePersistableUriPermission(tree, data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION);
                getPreferences(MODE_PRIVATE).edit().putString("tree",tree.toString()).apply(); refresh();
            } catch (SecurityException e) { status.setText("Folder permission failed: " + e.getMessage()); }
        }
    }
    private static String hash(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256"); byte[] buffer = new byte[1024*1024];
        try (InputStream input = new FileInputStream(file)) { int n; while ((n=input.read(buffer)) != -1) digest.update(buffer,0,n); }
        StringBuilder value = new StringBuilder(); for (byte b : digest.digest()) value.append(String.format(Locale.ROOT,"%02X",b)); return value.toString();
    }
    private void validate(File folder) throws Exception {
        File xex = new File(folder,"default.xex"); if (!xex.isFile()) throw new IOException("default.xex not found at folder root.");
        String sha = hash(xex);
        android.util.Log.i("TSGAndroid", "[GAME] SHA-256: " + sha);
        if (!EXPECTED_SHA.equals(sha)) throw new IOException("Unsupported game executable version.\nSHA-256: " + sha);
    }
    private void startGame() {
        busy = true; refresh(); status.setText("Validating default.xex...");
        worker.execute(() -> {
            try { validate(new File(getFilesDir(),"game")); runOnUiThread(() -> { busy=false; refresh(); startActivity(new Intent(this,GameActivity.class)); }); }
            catch (Exception e) { showError(e); }
        });
    }
    private void importGame() {
        busy = true; refresh(); status.setText("Importing selected folder to private storage... This may take several minutes.");
        Uri selected = tree;
        worker.execute(() -> {
            File staging = new File(getFilesDir(),"game-import"); File game = new File(getFilesDir(),"game"); File backup = new File(getFilesDir(),"game-previous");
            try {
                remove(staging); if (!staging.mkdirs()) throw new IOException("Cannot create import folder");
                copyDirectory(selected, DocumentsContract.getTreeDocumentId(selected), staging, 0);
                validate(staging);
                // Keep the previous installation until the validated replacement is complete.
                remove(backup);
                if (game.exists() && !game.renameTo(backup)) throw new IOException("Cannot preserve previous installation");
                if (!staging.renameTo(game)) { if (backup.exists()) backup.renameTo(game); throw new IOException("Cannot activate imported game"); }
                remove(backup);
                runOnUiThread(() -> { busy=false; refresh(); });
            } catch (Exception e) { remove(staging); showError(e); }
        });
    }
    private void copyDirectory(Uri selected, String id, File destination, int depth) throws Exception {
        if (depth > 64) throw new IOException("Folder nesting limit exceeded");
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(selected,id);
        String[] columns = { DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_SIZE };
        try (Cursor cursor = getContentResolver().query(children,columns,null,null,null)) {
            if (cursor == null) throw new IOException("Cannot enumerate selected folder");
            while (cursor.moveToNext()) {
                String childId=cursor.getString(0), name=cursor.getString(1), type=cursor.getString(2);
                if (name == null || name.isEmpty() || name.equals(".") || name.equals("..") || name.contains("/") || name.contains("\\")) throw new IOException("Invalid game path");
                File target = new File(destination,name);
                if (!target.getCanonicalPath().startsWith(new File(getFilesDir(),"game-import").getCanonicalPath()+File.separator)) throw new IOException("Game path escapes import folder");
                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(type)) {
                    if (!target.mkdirs()) throw new IOException("Cannot create " + name); copyDirectory(selected,childId,target,depth+1);
                } else {
                    long size=cursor.isNull(3) ? 0 : cursor.getLong(3);
                    if (size > 0 && getFilesDir().getUsableSpace() < size+64L*1024*1024) throw new IOException("Insufficient free space");
                    Uri document = DocumentsContract.buildDocumentUriUsingTree(selected,childId);
                    try (InputStream input = getContentResolver().openInputStream(document); OutputStream output = new FileOutputStream(target)) {
                        if (input == null) throw new IOException("Cannot open " + name);
                        byte[] buffer = new byte[1024*1024]; int n; while ((n=input.read(buffer)) != -1) output.write(buffer,0,n);
                    }
                }
            }
        }
    }
    private void remove(File file) {
        // Only app-owned import/backup folders are removed by callers.
        File[] children=file.listFiles(); if (children!=null) for(File child:children) remove(child);
        if(file.exists()) file.delete();
    }
    private void showError(Exception e) { runOnUiThread(() -> { busy=false; refresh(); status.setText(e.getMessage()); }); }
}
