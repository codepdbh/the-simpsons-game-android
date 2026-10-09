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

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(20 * getResources().getDisplayMetrics().density); layout.setPadding(pad,pad,pad,pad);
        TextView title = new TextView(this); title.setText("The Simpsons Game Android"); title.setTextSize(24); layout.addView(title);
        status = new TextView(this); status.setTextSize(16); layout.addView(status);
        select = button(layout, "Select TSG folder", () -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,
                Uri.parse("content://com.android.externalstorage.documents/document/primary%3ATSG"));
            startActivityForResult(intent, SELECT_TREE);
        });
        importButton = button(layout, "Import selected game files", this::importGame);
        start = button(layout, "Start Game", this::startGame);
        button(layout, "Vulkan 1.1 test (blue screen)", () -> startActivity(new Intent(this, MainActivity.class)));
        TextView controls = new TextView(this);
        controls.setText("Controls: physical gamepad via SDL. Touch overlay is not implemented yet."); layout.addView(controls);
        ScrollView scroll = new ScrollView(this); scroll.addView(layout); setContentView(scroll);
        String saved = getPreferences(MODE_PRIVATE).getString("tree", null);
        if (saved != null) tree = Uri.parse(saved);
        refresh();
    }
    private Button button(LinearLayout layout, String text, Runnable action) {
        Button result = new Button(this); result.setText(text); result.setOnClickListener(v -> action.run()); layout.addView(result); return result;
    }
    private void refresh() {
        StringBuilder pads = new StringBuilder();
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice device = InputDevice.getDevice(id);
            if (device != null && (device.getSources() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) pads.append(device.getName()).append(" ");
        }
        boolean installed = new File(getFilesDir(), "game/default.xex").isFile();
        status.setText("Renderer: Vulkan 1.1\nGame files: " + (installed ? "Imported (hash checked before start)" : "Not imported")
            + "\nFolder: " + (tree == null ? "Not selected" : tree.toString())
            + "\nRuntime: " + (BuildConfig.WITH_GAME ? "Experimental recompilation build; boot not guaranteed" : "Not included in this diagnostic build")
            + "\nGamepad: " + (pads.length() == 0 ? "Not connected" : pads));
        select.setEnabled(!busy); importButton.setEnabled(!busy && tree != null);
        start.setEnabled(!busy && installed && BuildConfig.WITH_GAME);
    }
    @Override protected void onResume() { super.onResume(); if (status != null && !busy) refresh(); }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request,result,data);
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
