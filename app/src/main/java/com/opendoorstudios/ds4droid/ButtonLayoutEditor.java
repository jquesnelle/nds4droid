package com.opendoorstudios.ds4droid;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public class ButtonLayoutEditor extends Activity {
	
	final int[] buttonsToLoad = { Button.BUTTON_L, Button.BUTTON_R, Button.BUTTON_TOUCH, Button.BUTTON_SELECT,
			Button.BUTTON_START, Button.BUTTON_DPAD, Button.BUTTON_ABXY, Button.BUTTON_OPTIONS };
	final int[] buttonResIds = { R.drawable.l, R.drawable.r, R.drawable.touch, R.drawable.select,
			R.drawable.start, R.drawable.dpad, R.drawable.abxy, R.drawable.menu	};
	
	Bundle savedValues = null;
	ButtonEditorView view = null;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(view = new ButtonEditorView(this));
		savedValues = savedInstanceState;
	}
	
	@Override
	public void onStop() {
		super.onStop();
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		for(Editing button : view.buttons) {
			if(!button.position.equals(button.button.position)) {
				button.button.position = button.position;
				button.button.applyToPrefs(prefs, view.landscape, true);
			}
		}
	}
	
	private static class Editing {
		Editing(Button button) {
			this.button = button;
			sourceBitmapRect = new Rect(0, 0, button.bitmap.getWidth(), button.bitmap.getHeight());
			position = new Rect(button.position.left, button.position.top, button.position.right, button.position.bottom);
		}
		final Button button;
		int startX;
		int startY;
		final Rect sourceBitmapRect;
		Rect startRect;
		Rect position;
	}
	
	class ButtonEditorView extends SurfaceView implements Callback {

		public ButtonEditorView(Context context) {
			super(context);
			setWillNotDraw(false);
			getHolder().addCallback(this);
			
			scaleDetector = new ScaleGestureDetector(ButtonLayoutEditor.this, new ScaleListener());
		}
		
		final ScaleGestureDetector scaleDetector;
		
		class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
			
			@Override
			public boolean onScaleBegin(ScaleGestureDetector detector) {
				int x = (int)detector.getFocusX();
				int y = (int)detector.getFocusY();
				for(Editing button : buttons) {
					if(button.position.contains(x, y)) {
						scaling = button;
						invalidate();
						break;
					}
				}
				handledScaleEvent = true;
				return true;
			}
			
			@Override
			public boolean onScale(ScaleGestureDetector detector) {
				if(scaling == null)
					return false;
				//Log.i(MainActivity.TAG, "Scale factor: " + detector.getScaleFactor());
				
				final int scaleX = (int) (scaling.position.width() * (detector.getScaleFactor() - 1.0f));
				final int scaleY = (int) (scaling.position.height() * (detector.getScaleFactor() - 1.0f));
				//TODO: weigh the initial spot
				scaling.position.left -= (scaleX / 2);
				scaling.position.right += (scaleX / 2);
				scaling.position.top -= (scaleY / 2);
				scaling.position.bottom += (scaleY / 2);
				invalidate();
				handledScaleEvent = true;
				return true;
			}
			
			@Override
			public void onScaleEnd(ScaleGestureDetector detector) {
				scaling = null;
				handledScaleEvent = true;
			}
			
		}
		
		final ArrayList<Editing> buttons = new ArrayList<>();
		boolean landscape;
		
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			
			landscape = width > height;
			boolean is565 = format == PixelFormat.RGB_565;
			
			for(int i = 0 ; i < buttonsToLoad.length ; ++i) {
				final int id = buttonsToLoad[i];
				final Button button = Button.load(ButtonLayoutEditor.this, id, buttonResIds[i], 
						landscape, is565, new Rect(0, 0, width, height), landscape ? Controls.defaultLandSpace : Controls.defaultPortSpace, true);
				
				if(savedValues != null) {
					final Object saved = savedValues.get(Button.getButtonName(id));
					if(saved != null && saved instanceof Rect) 
						button.position = (Rect)saved;
				}
				buttons.add(new Editing(button));
			}
			savedValues = null;
			invalidate();
			
		}
		
		@Override
		public void onDraw(Canvas canvas) {
			for(Editing button : buttons) {
				if(button.button.bitmap != null)
					canvas.drawBitmap(button.button.bitmap, button.sourceBitmapRect, button.position, null);
			}
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
	
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
		}
		
		
		final HashMap<Integer, Editing> editing = new HashMap<>();
		Editing scaling = null;
		boolean handledScaleEvent = false;
		
		@Override
		public boolean onTouchEvent(MotionEvent event) {
			scaleDetector.onTouchEvent(event);
			if(handledScaleEvent) {
				handledScaleEvent = false;
				return true;
			}
				
			
			int i = event.getActionIndex();
			int id = event.getPointerId(i);
			int x = (int) event.getX(i);
			int y = (int) event.getY(i);
			final Editing edit = editing.get(id);
			switch(event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_POINTER_DOWN:
				for(Editing button : buttons) {
					if(button.position.contains(x, y)) {
						button.startX = x;
						button.startY = y;
						button.startRect = new Rect(button.position.left, button.position.top, button.position.right, button.position.bottom);
						editing.put(id, button);
						invalidate();
						break;
					}
				}
				break;
			case MotionEvent.ACTION_MOVE:
			case MotionEvent.ACTION_UP:
				if(edit != null) {
					final int deltaX = x - edit.startX;
					final int deltaY = y - edit.startY;
					edit.position.left = edit.startRect.left + deltaX;
					edit.position.right = edit.startRect.right + deltaX;
					edit.position.top = edit.startRect.top + deltaY;
					edit.position.bottom = edit.startRect.bottom + deltaY;
					if(event.getActionMasked() == MotionEvent.ACTION_UP) {
						editing.remove(id);
					}
					invalidate();
				}
				break;
			case MotionEvent.ACTION_CANCEL:
				if(edit != null)
					editing.remove(edit);
				invalidate();
				break;
			}
			return true;
		}

		
	}

}
