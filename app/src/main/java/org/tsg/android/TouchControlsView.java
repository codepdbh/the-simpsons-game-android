package org.tsg.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.HashMap;

/** A multitouch Xbox pad. Axes/buttons reach the real SDK input driver. */
public final class TouchControlsView extends View {
    static native void nativeSetTouchState(int buttons,int lx,int ly,int rx,int ry,int lt,int rt);
    private static final class Control {
        final String label; float x,y,r; final float defaultX,defaultY,defaultR; final int mask,kind;
        float ax,ay; boolean held;
        Control(String label,float x,float y,float r,int mask,int kind) {
            this.label=label; this.x=x; this.y=y; this.r=r; defaultX=x; defaultY=y; defaultR=r; this.mask=mask; this.kind=kind;
        }
    }
    private final ArrayList<Control> controls=new ArrayList<>();
    private final HashMap<Integer,Control> fingers=new HashMap<>();
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Bitmap artwork;
    private final RectF bounds=new RectF();
    interface EditorListener { void changed(); }
    private EditorListener editorListener;
    private final SharedPreferences layoutPrefs;
    private boolean editing;
    private Control selected;
    private int dragPointer=-1;
    private float dragX,dragY;
    private int lastButtons=-1,lastLX,lastLY,lastRX,lastRY,lastLT,lastRT;
    public TouchControlsView(Context context) {
        super(context); setContentDescription("Mando virtual de The Simpsons Game");
        artwork=BitmapFactory.decodeResource(getResources(),R.drawable.control_button);
        layoutPrefs=context.getSharedPreferences("tsg_touch_layout",Context.MODE_PRIVATE);
        // 1/2 are analog sticks; 3/4 are analog triggers; 0 is a digital button.
        add("MOVER",.12f,.76f,.125f,0,1); add("CÁMARA",.69f,.77f,.095f,0,2);
        add("A",.88f,.79f,.066f,0x1000,0); add("B",.95f,.64f,.066f,0x2000,0);
        add("X",.81f,.64f,.066f,0x4000,0); add("Y",.88f,.49f,.066f,0x8000,0);
        add("↑",.12f,.31f,.044f,1,0); add("↓",.12f,.49f,.044f,2,0);
        add("←",.08f,.40f,.044f,4,0); add("→",.16f,.40f,.044f,8,0);
        add("LB",.07f,.12f,.051f,0x100,0); add("LT",.15f,.12f,.051f,0,3);
        add("RB",.85f,.12f,.051f,0x200,0); add("RT",.93f,.12f,.051f,0,4);
        add("BACK",.43f,.10f,.047f,0x20,0); add("START",.57f,.10f,.047f,0x10,0);
        add("EDITAR",.50f,.10f,.047f,0,5);
        add("L3",.27f,.86f,.04f,0x40,0); add("R3",.57f,.86f,.04f,0x80,0);
        paint.setTypeface(Typeface.DEFAULT_BOLD); paint.setTextAlign(Paint.Align.CENTER);
        for(Control c:controls) {
            c.x=clamp(layoutPrefs.getFloat(c.label+"_x",c.x),.02f,.98f);
            c.y=clamp(layoutPrefs.getFloat(c.label+"_y",c.y),.02f,.98f);
            c.r=clamp(layoutPrefs.getFloat(c.label+"_r",c.r),.025f,.22f);
        }
    }
    private static float clamp(float value,float min,float max) {return Math.max(min,Math.min(max,value));}
    void setEditorListener(EditorListener listener) {editorListener=listener;}
    boolean isEditing() {return editing;}
    String selectedLabel() {return selected==null?"Selecciona un botón o stick":selected.label;}
    float selectedSize() {return selected==null?.06f:selected.r;}
    boolean hasSelection() {return selected!=null;}
    void setEditing(boolean value) {
        release(); editing=value; dragPointer=-1;
        if(!editing) saveLayout();
        if(editorListener!=null) editorListener.changed(); invalidate();
    }
    void resizeSelected(float size) {
        if(selected==null) return; selected.r=clamp(size,.025f,.22f); constrain(selected); invalidate();
    }
    private void constrain(Control c) {
        if(getWidth()==0||getHeight()==0) return;
        float rx=radius(c)/getWidth(),ry=c.r;
        c.x=clamp(c.x,rx,1-rx); c.y=clamp(c.y,ry,1-ry);
    }
    void saveLayout() {
        SharedPreferences.Editor writer=layoutPrefs.edit();
        for(Control c:controls) writer.putFloat(c.label+"_x",c.x).putFloat(c.label+"_y",c.y).putFloat(c.label+"_r",c.r);
        writer.apply();
    }
    void resetLayout() {
        for(Control c:controls) {c.x=c.defaultX;c.y=c.defaultY;c.r=c.defaultR;}
        if(editorListener!=null) editorListener.changed(); invalidate();
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh) {
        super.onSizeChanged(w,h,oldw,oldh); for(Control c:controls) constrain(c);
    }
    private void add(String label,float x,float y,float r,int mask,int kind) {
        controls.add(new Control(label,x,y,r,mask,kind));
    }
    private float radius(Control c) { return c.r*getHeight(); }
    private float distance(Control c,float x,float y) {
        return (float)Math.hypot(x-c.x*getWidth(),y-c.y*getHeight());
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(editing) canvas.drawColor(0x55000000);
        for(Control c:controls) {
            float x=c.x*getWidth(),y=c.y*getHeight(),r=radius(c);
            bounds.set(x-r,y-r,x+r,y+r);
            paint.setAlpha(c.held?235:150); canvas.drawBitmap(artwork,null,bounds,paint);
            paint.setAlpha(255);
            if(c.kind==1||c.kind==2) {
                paint.setColor(c.held?0xffffdf38:0xffc9c9c9);
                canvas.drawCircle(x+c.ax*r*.55f,y-c.ay*r*.55f,r*.30f,paint);
                paint.setColor(Color.WHITE); paint.setTextSize(r*.24f);
                paint.setShadowLayer(3,0,1,Color.BLACK); canvas.drawText(c.label,x,y+r+paint.getTextSize(),paint);
            } else {
                int color=c.mask==0x1000?0xff7cff79:c.mask==0x2000?0xffff7979:
                    c.mask==0x4000?0xff82bfff:c.mask==0x8000?0xffffe952:Color.WHITE;
                paint.setColor(color); paint.setTextSize(r*(c.label.length()>2?.38f:.66f));
                paint.setShadowLayer(3,0,1,Color.BLACK);
                String label=c.kind==5&&editing?"LISTO":c.label;
                canvas.drawText(label,x,y-(paint.ascent()+paint.descent())/2,paint);
            }
            paint.clearShadowLayer(); paint.setColor(Color.WHITE);
            if(editing&&c==selected) {
                paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(3);
                canvas.drawCircle(x,y,r+4,paint); paint.setStyle(Paint.Style.FILL);
            }
        }
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        int action=event.getActionMasked(),index=event.getActionIndex();
        if(action==MotionEvent.ACTION_DOWN) {
            for(Control c:controls) if(c.kind==5&&distance(c,event.getX(),event.getY())<=radius(c)*1.15f) {
                setEditing(!editing); return true;
            }
        }
        if(editing) {
            if(action==MotionEvent.ACTION_DOWN) {
                selected=null; float best=Float.MAX_VALUE;
                for(Control c:controls) {
                    float d=distance(c,event.getX(),event.getY())/radius(c);
                    if(d<=1.2f&&d<best) {selected=c;best=d;}
                }
                if(selected!=null) {
                    dragPointer=event.getPointerId(0);
                    dragX=event.getX()-selected.x*getWidth(); dragY=event.getY()-selected.y*getHeight();
                }
                if(editorListener!=null) editorListener.changed();
            } else if(action==MotionEvent.ACTION_MOVE&&selected!=null&&dragPointer!=-1) {
                int pointer=event.findPointerIndex(dragPointer);
                if(pointer>=0) {
                    selected.x=(event.getX(pointer)-dragX)/getWidth(); selected.y=(event.getY(pointer)-dragY)/getHeight();
                    constrain(selected);
                }
            } else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL) dragPointer=-1;
            invalidate(); return true;
        }
        if(action==MotionEvent.ACTION_CANCEL) { release(); return true; }
        if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN) {
            float x=event.getX(index),y=event.getY(index);
            Control hit=null; float best=Float.MAX_VALUE;
            for(Control c:controls) {
                float d=distance(c,x,y)/radius(c);
                if(c.kind!=5&&d<=1.15f&&d<best&&!fingers.containsValue(c)) {hit=c;best=d;}
            }
            if(hit!=null) fingers.put(event.getPointerId(index),hit);
            else if(action==MotionEvent.ACTION_DOWN) return false;
        }
        if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP) {
            Control c=fingers.remove(event.getPointerId(index));
            if(c!=null) {c.held=false;c.ax=0;c.ay=0;}
        }
        for(int i=0;i<event.getPointerCount();i++) {
            Control c=fingers.get(event.getPointerId(i)); if(c==null) continue;
            if(c.kind==1||c.kind==2) {
                float x=(event.getX(i)-c.x*getWidth())/radius(c);
                float y=(c.y*getHeight()-event.getY(i))/radius(c);
                float scale=Math.max(1,(float)Math.hypot(x,y)); c.ax=x/scale;c.ay=y/scale;c.held=true;
            } else c.held=distance(c,event.getX(i),event.getY(i))<=radius(c)*1.4f;
        }
        send(); invalidate(); return true;
    }
    private void send() {
        int buttons=0,lx=0,ly=0,rx=0,ry=0,lt=0,rt=0;
        for(Control c:controls) {
            if(c.held) buttons|=c.mask;
            if(c.kind==1) {lx=(int)(c.ax*32767);ly=(int)(c.ay*32767);}
            if(c.kind==2) {rx=(int)(c.ax*32767);ry=(int)(c.ay*32767);}
            if(c.kind==3&&c.held) lt=255;
            if(c.kind==4&&c.held) rt=255;
        }
        if(buttons!=lastButtons||lx!=lastLX||ly!=lastLY||rx!=lastRX||ry!=lastRY||lt!=lastLT||rt!=lastRT) {
            nativeSetTouchState(buttons,lx,ly,rx,ry,lt,rt);
            lastButtons=buttons;lastLX=lx;lastLY=ly;lastRX=rx;lastRY=ry;lastLT=lt;lastRT=rt;
        }
    }
    void release() {
        fingers.clear(); for(Control c:controls) {c.held=false;c.ax=0;c.ay=0;} send(); invalidate();
    }
    @Override protected void onDetachedFromWindow() { release(); super.onDetachedFromWindow(); }
}
