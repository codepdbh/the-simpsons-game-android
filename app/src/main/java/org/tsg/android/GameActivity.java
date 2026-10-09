package org.tsg.android;
import org.libsdl.app.SDLActivity;
import android.os.Bundle;
import android.system.Os;
import android.system.ErrnoException;
import java.io.File;
public class GameActivity extends LandscapeSDLActivity {
    private TouchControlsView controls;
    private android.widget.LinearLayout editor;
    private android.widget.TextView editorLabel;
    private android.widget.SeekBar sizeSlider;
    private boolean updatingEditor;
    @Override protected void onCreate(Bundle state) {
        File user = new File(getFilesDir(), "user"); user.mkdirs();
        try { Os.setenv("REX_APP_FOLDER", user.getAbsolutePath(), true); }
        catch (ErrnoException e) { throw new IllegalStateException("Cannot configure runtime folder", e); }
        super.onCreate(state);
        controls = new TouchControlsView(this);
        ((android.view.ViewGroup)findViewById(android.R.id.content)).addView(controls,
            new android.widget.FrameLayout.LayoutParams(-1,-1));
        buildEditor();
    }
    private void buildEditor() {
        editor=new android.widget.LinearLayout(this); editor.setOrientation(1);
        int pad=(int)(12*getResources().getDisplayMetrics().density); editor.setPadding(pad,pad,pad,pad);
        editor.setBackgroundColor(0xee252525);
        editorLabel=new android.widget.TextView(this); editorLabel.setTextColor(android.graphics.Color.WHITE); editor.addView(editorLabel);
        sizeSlider=new android.widget.SeekBar(this); sizeSlider.setMax(195); editor.addView(sizeSlider);
        sizeSlider.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(android.widget.SeekBar bar,int progress,boolean fromUser) {
                if(fromUser&&!updatingEditor) controls.resizeSelected((progress+25)/1000f);
            }
            public void onStartTrackingTouch(android.widget.SeekBar bar) {}
            public void onStopTrackingTouch(android.widget.SeekBar bar) {}
        });
        android.widget.LinearLayout buttons=new android.widget.LinearLayout(this); editor.addView(buttons);
        android.widget.Button reset=new android.widget.Button(this); reset.setText("Restablecer");
        reset.setOnClickListener(v->controls.resetLayout()); buttons.addView(reset,new android.widget.LinearLayout.LayoutParams(0,-2,1));
        android.widget.Button done=new android.widget.Button(this); done.setText("Guardar");
        done.setOnClickListener(v->controls.setEditing(false)); buttons.addView(done,new android.widget.LinearLayout.LayoutParams(0,-2,1));
        android.widget.FrameLayout.LayoutParams params=new android.widget.FrameLayout.LayoutParams(
            (int)(320*getResources().getDisplayMetrics().density),-2,android.view.Gravity.CENTER);
        ((android.view.ViewGroup)findViewById(android.R.id.content)).addView(editor,params);
        controls.setEditorListener(()-> {
            editor.setVisibility(controls.isEditing()?android.view.View.VISIBLE:android.view.View.GONE);
            editorLabel.setText("Arrastra para mover. Tamaño: "+controls.selectedLabel());
            updatingEditor=true; sizeSlider.setEnabled(controls.hasSelection());
            sizeSlider.setProgress(Math.round(controls.selectedSize()*1000)-25); updatingEditor=false;
        });
        editor.setVisibility(android.view.View.GONE);
    }
    @Override protected void onPause() { if(controls != null) {controls.release();controls.saveLayout();} super.onPause(); }
    @Override protected String[] getLibraries() { return new String[] { "SDL3", "tsg_game" }; }
    @Override protected String[] getArguments() {
        int language=getSharedPreferences("tsg_settings",MODE_PRIVATE).getInt("language",5);
        java.util.List<String> args=new java.util.ArrayList<>(java.util.Arrays.asList("--user_language="+language,"--game_data_root="+new File(getFilesDir(),"game"),"--user_data_root="+new File(getFilesDir(),"user"),"--cache_root="+new File(getFilesDir(),"cache"),"--gpu=vulkan","--gpu_plugin=xenos"));
        GameOptions.arguments(this,args);return args.toArray(new String[0]);
    }
}
