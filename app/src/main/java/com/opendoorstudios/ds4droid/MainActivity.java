package com.opendoorstudios.ds4droid;

/*
	Copyright (C) 2012 Jeffrey Quesnelle

	This file is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 2 of the License, or
	(at your option) any later version.

	This file is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with the this software.  If not, see <http://www.gnu.org/licenses/>.
*/

import java.io.File;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.WindowManager;

public class MainActivity extends Activity implements OnSharedPreferenceChangeListener {

	static EmulatorThread coreThread;
	static Controls controls;
	NDSView view;
	static final String TAG = "nds4droid";
	Dialog loadingDialog = null;
	
	Handler msgHandler = new Handler() {
		
		@Override
		public
		void dispatchMessage(Message msg) {
			switch(msg.what) {
			case PICK_ROM:
				pickRom();
				break;
			case LOADING_START:
				if(loadingDialog == null) {
					final String loadingMsg = getResources().getString(R.string.loading);
					loadingDialog = ProgressDialog.show(MainActivity.this, null, loadingMsg, true);
					break;
				}
				break;
			case LOADING_END:
				if(loadingDialog != null) {
					loadingDialog.dismiss();
					loadingDialog = null;
				}
				break;
			case ROM_ERROR:
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				builder.setMessage(R.string.rom_error).setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						arg0.dismiss();
						pickRom();
					}
				}).setOnCancelListener(new OnCancelListener() {

					@Override
					public void onCancel(DialogInterface arg0) {
						arg0.dismiss();
						pickRom();
					}
					
				});
				builder.create().show();
			}
		}
		
	};
	
	public static final int PICK_ROM = 1338;
	public static final int LOADING_START = 1339;
	public static final int LOADING_END = 1340;
	public static final int ROM_ERROR = 1341;
	
	public static boolean IS_OUYA = tv.ouya.console.api.OuyaFacade.getInstance().isRunningOnOUYAHardware();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if(IS_OUYA)
			Log.i(TAG, "Starting in OUYA mode");

		view = new NDSView(this);
		setContentView(view);
		
		controls = new Controls(view);

		Settings.applyDefaults(this);
		prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
		prefs.registerOnSharedPreferenceChangeListener(this);
		loadJavaSettings(null);
		
		if(savedInstanceState != null) 
			timeAtLastAutosave = savedInstanceState.getLong(LASTSAVE_KEY);
		
		
		if(!DeSmuME.inited) 
			pickRom();
		
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}
	
	void runEmulation() {
		boolean created = false;
		if(coreThread == null) {
			coreThread = new EmulatorThread(this);
			created = true;
		}
		else
			coreThread.setCurrentActivity(this);
		coreThread.setPause(!DeSmuME.romLoaded);
		if(created)
			coreThread.start();
	}
	
	void pauseEmulation() {
		if(coreThread != null) {
			coreThread.setPause(true);
		}
	}
	
	void pickRom() {
		Intent i = new Intent(this, prefs.getBoolean(Settings.DISABLE_ROM_BROWSER, false) ? 
				FileDialog.class : com.opendoorstudios.ds4droid.NDSScanner.CollectionActivity.class);
		i.setAction(Intent.ACTION_PICK);
		
		String startPath = Environment.getExternalStorageDirectory().getPath();
		final File path = new File(prefs.getString(Settings.LAST_ROM_DIR, startPath));
		if(path.exists()) //make sure this path actually exists -- otherwise default back to /mnt/sdcard/
			startPath = path.getPath();
		
		i.putExtra(FileDialog.START_PATH, startPath);
		i.putExtra(FileDialog.FORMAT_FILTER, new String[] {".nds", ".zip", ".7z", ".rar"});
		startActivityForResult(i, PICK_ROM);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode != PICK_ROM || resultCode != Activity.RESULT_OK)
			return;
		String romPath = data.getStringExtra(FileDialog.RESULT_PATH);
		if(romPath != null) {
			final File romDir = new File(romPath);
			prefs.edit().putString(Settings.LAST_ROM_DIR, romDir.getParent()).apply();
			runEmulation();
			coreThread.loadRom(romPath);
			timeAtLastAutosave = System.currentTimeMillis();
			scheduleAutosave();
		}
			
	}
	
	private static final String LASTSAVE_KEY = "LASTSAVE_KEY";
	
	@Override
	public void onSaveInstanceState(Bundle out) {
		out.putLong(LASTSAVE_KEY, timeAtLastAutosave);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		runEmulation();
		startDrawTimer();
		if(DeSmuME.romLoaded)
			scheduleAutosave();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		pauseEmulation();
		stopDrawTimer();
		cancelAutosave();
	}
	
	Timer drawtimer;
	void startDrawTimer() { 
		drawtimer = new Timer();
		drawtimer.schedule(new TimerTask() {
			public void run(){
				if(coreThread.frameFinished) {
					coreThread.frameFinished = false;
					view.postInvalidate();
				}
					
			}
		}, 12, 12);
	}
	
	void stopDrawTimer() {
		if (drawtimer != null)
			drawtimer.cancel();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_main, menu);
	    return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		pauseEmulation();
		menu.findItem(R.id.cheats).setVisible(DeSmuME.romLoaded);
		menu.findItem(R.id.lid).setChecked(DeSmuME.lidOpen);
		
		final String defaultWorkingDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/nds4droid";
		final String statesPath = prefs.getString(Settings.DESMUME_PATH, defaultWorkingDir) + "/States/";
		
		String romSaveName = DeSmuME.loadedRom != null ? new File(DeSmuME.loadedRom).getName() : null;
		if(romSaveName != null)
			romSaveName = romSaveName.substring(0, romSaveName.lastIndexOf('.'));
		
		for(int i = 1 ; i <= 10 ; ++i) {
			
			MenuItem saveItem = null, loadItem = null;
			switch(i) {
			case 1: saveItem = menu.findItem(R.id.save1); loadItem = menu.findItem(R.id.restore1); break;
			case 2: saveItem = menu.findItem(R.id.save2); loadItem = menu.findItem(R.id.restore2); break;
			case 3: saveItem = menu.findItem(R.id.save3); loadItem = menu.findItem(R.id.restore3); break;
			case 4: saveItem = menu.findItem(R.id.save4); loadItem = menu.findItem(R.id.restore4); break;
			case 5: saveItem = menu.findItem(R.id.save5); loadItem = menu.findItem(R.id.restore5); break;
			case 6: saveItem = menu.findItem(R.id.save6); loadItem = menu.findItem(R.id.restore6); break;
			case 7: saveItem = menu.findItem(R.id.save7); loadItem = menu.findItem(R.id.restore7); break;
			case 8: saveItem = menu.findItem(R.id.save8); loadItem = menu.findItem(R.id.restore8); break;
			case 9: saveItem = menu.findItem(R.id.save9); loadItem = menu.findItem(R.id.restore9); break;
			case 10: loadItem = menu.findItem(R.id.restoreauto); break;
			}
			
			String newTitle = i == 10 ? "Autosave" : String.valueOf(i);
			
			if(romSaveName != null) {
				final File thisSave = new File(statesPath + romSaveName + ".ds" + i);
				if(thisSave.exists()) {
					final String date = new Date(thisSave.lastModified()).toString();
					if(i == 10)
						newTitle = "Autosave - " + date;
					else
						newTitle = String.format("%d - %s", i, date);
				}
			}
			
			if(i != 10)
				saveItem.setTitle(newTitle);
			loadItem.setTitle(newTitle);
		}
		
		return true;
	}
	
	@Override
	public void onOptionsMenuClosed(Menu menu) {
		runEmulation();
	}
	
	@Override
	public boolean onMenuItemSelected (int featureId, MenuItem item) {
		switch(item.getItemId()) {
		case R.id.load:
			pickRom();
			break;
		case R.id.quicksave:
			saveState(0);
			break;
		case R.id.quickrestore:
			restoreState(0);
			break;
		case R.id.restore1: case R.id.restore2: case R.id.restore3: case R.id.restore4: case R.id.restore5:
		case R.id.restore6: case R.id.restore7: case R.id.restore8: case R.id.restore9: case R.id.restoreauto:
		{
			String title = item.getTitle().toString();
			final int spacePos = title.indexOf(' ');
			if(spacePos != -1)
				title = title.substring(0, spacePos);
			final int state = title.equals("Autosave") ? 10 : Integer.valueOf(title);
			restoreState(state);
			
		}
			break;
		case R.id.save1: case R.id.save2: case R.id.save3: case R.id.save4: case R.id.save5:
		case R.id.save6: case R.id.save7: case R.id.save8: case R.id.save9:
		{
			String title = item.getTitle().toString();
			final int spacePos = title.indexOf(' ');
			if(spacePos != -1)
				title = title.substring(0, spacePos);
			final int state = Integer.valueOf(title);
			saveState(state);
		}
			break;
		case R.id.settings:
			startActivity(new Intent(this, Settings.class));
			break;
		case R.id.cheats:
			startActivity(new Intent(this, Cheats.class));
			break;
		case R.id.exit:
			DeSmuME.exit();
			finish();
			break;
		case R.id.lid:
			if(coreThread != null) {
				boolean newState = !DeSmuME.lidOpen;
				DeSmuME.lidOpen = newState;
				item.setChecked(newState);
			}
			break;
		case R.id.about:
			startActivity(new Intent(this, About.class));
			break;
		default:
			return false;
		}
		runEmulation();
		return true;
	}
	
	static String getStateName(int slot) {
		switch(slot) {
		case 0: return "quick state";
		case 10: return "autosave";
		default: return "state " + slot;
		}
	}
	
	void restoreState(int slot) {
		if(DeSmuME.romLoaded) {
			coreThread.inFrameLock.lock();
				DeSmuME.restoreState(slot);
			coreThread.inFrameLock.unlock();
			view.stateText = "Restored " + getStateName(slot);
			view.stateDrawStart = System.currentTimeMillis();
		}
	}
	
	void saveState(int slot) {
		if(DeSmuME.romLoaded) {
			coreThread.inFrameLock.lock();
				DeSmuME.saveState(slot);
			coreThread.inFrameLock.unlock();
			view.stateText = "Saved " + getStateName(slot);
			view.stateDrawStart = System.currentTimeMillis();
		}
	}

	
	SharedPreferences prefs = null;
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

		if(DeSmuME.inited)
			DeSmuME.loadSettings();
		loadJavaSettings(key);
			
	}
	
	void loadJavaSettings(String key)
	{
		if(key != null) {
			if(DeSmuME.inited && key.equals(Settings.LANGUAGE))
				DeSmuME.reloadFirmware();
		}
		
		if(view != null) {
			view.showfps = prefs.getBoolean(Settings.SHOW_FPS, false);
			view.showTouchMessage = prefs.getBoolean(Settings.SHOW_TOUCH_MESSAGE, true);
			view.showSoundMessage = prefs.getBoolean(Settings.SHOW_SOUND_MESSAGE, true);
			view.lcdSwap = prefs.getBoolean(Settings.LCD_SWAP, false);
			view.buttonAlpha = (int)(prefs.getInt(Settings.BUTTON_TRANSPARENCY, 78) * 2.55f);
			view.haptic = prefs.getBoolean(Settings.HAPTIC, false);
			view.dontRotate = prefs.getBoolean(Settings.DONT_ROTATE_LCDS, false);
			view.alwaysTouch = prefs.getBoolean(Settings.ALWAYS_TOUCH, false);
			
			controls.loadMappings(this);
			
			if(key != null) {
				if(key.equals(Settings.SCREEN_FILTER)) {
					int newFilter = DeSmuME.getSettingInt(Settings.SCREEN_FILTER, 0);
					DeSmuME.setFilter(newFilter);
					view.forceResize();
				}
				else if(key.equals(Settings.RENDERER)) {
					int new3D = DeSmuME.getSettingInt(Settings.RENDERER, 2);
					if(coreThread != null)
						coreThread.change3D(new3D);
				}
				else if(key.equals(Settings.ENABLE_SOUND)) {
					int newSound = DeSmuME.getSettingInt(Settings.ENABLE_SOUND, 0);
					if(coreThread != null)
						coreThread.changeSound(newSound);
				}
				else if(key.equals(Settings.CPU_MODE) || key.equals(Settings.JIT_SIZE)) {
					int newCpuMode = DeSmuME.getSettingInt(Settings.CPU_MODE, 1);
					if(coreThread != null)
						coreThread.changeCPUMode(newCpuMode);
				}
				else if(key.equals(Settings.SOUND_SYNC_MODE)) {
					int newSoundSyncMode = DeSmuME.getSettingInt(Settings.SOUND_SYNC_MODE, 0);
					if(coreThread != null)
						coreThread.changeSoundSyncMode(newSoundSyncMode);
				}
				else if(key.equals(Settings.ENABLE_AUTOSAVE) || key.equals(Settings.AUTOSVAE_FREQUENCY)) {
					cancelAutosave();
					if(coreThread != null) {
						if(DeSmuME.romLoaded)
							scheduleAutosave();
					}
				}
			}
		}
	}
	
	long timeAtLastAutosave = 0;
	Timer autosaveTask;
	
	void cancelAutosave() {
		if(autosaveTask != null) {
			autosaveTask.cancel();
			autosaveTask = null;
		}
	}
	
	void scheduleAutosave() {
		cancelAutosave();
		if(prefs.getBoolean(Settings.ENABLE_AUTOSAVE, true)) {
			final int freqSel = Integer.valueOf(prefs.getString(Settings.AUTOSVAE_FREQUENCY, "1"));
			long autosaveDelay;
			switch(freqSel) {
			case 0: autosaveDelay = 1 * 60 * 1000; break; //1 minute
			case 2: autosaveDelay = 15 * 60 * 1000; break; //15 minutes
			case 3: autosaveDelay = 30 * 60 * 1000; break; //30 minutes
			case 4: autosaveDelay = 60 * 60 * 1000; break; //1 hour
			default: autosaveDelay = 5 * 60 * 1000; break; //5 minutes
			}
			long timeSinceLastAuto = System.currentTimeMillis() - timeAtLastAutosave;
			if(timeSinceLastAuto >= autosaveDelay) 
				timeSinceLastAuto = autosaveDelay;
			autosaveTask = new Timer();
			autosaveTask.scheduleAtFixedRate(new TimerTask() {

				@Override
				public void run() {
					if(!coreThread.paused.get()) {
						saveState(10);
						timeAtLastAutosave = System.currentTimeMillis();
					}
				}
				
			}, autosaveDelay - timeSinceLastAuto, autosaveDelay);
		}
	}
	
	class NDSView extends SurfaceView implements Callback {
		Bitmap emuBitmapMain, emuBitmapTouch;
		
		final Paint emuPaint = new Paint();
		final Paint hudPaint = new Paint();
		final float defhudsize = 15;
		float curhudsize = defhudsize;
		public boolean showfps = false;
		public boolean showTouchMessage = false;
		public boolean showSoundMessage = false;
		public boolean lcdSwap = false;
		public boolean forceTouchScreen = false;
		public int buttonAlpha = 78;
		public boolean haptic = true;
		public boolean alwaysTouch = false;
		
		public NDSView(Context context) {
			super(context);
			getHolder().addCallback(this);
			setKeepScreenOn(true);
			setWillNotDraw(false);
			setFocusable(true);
			setFocusableInTouchMode(true);
			
			hudPaint.setColor(Color.WHITE);
			hudPaint.setShadowLayer(1, 0, 0, Color.BLACK);
			hudPaint.setTextSize(defhudsize);
			hudPaint.setAntiAlias(false);
		}
		
		boolean doForceResize = false;
		public void forceResize() {
			doForceResize = true;
		}
		
		
		
		@Override
		public void onDraw(Canvas canvas) {
			canvas.drawColor(Color.BLACK);
			
			if(showTouchMessage) {
				prefs.edit().putBoolean(Settings.SHOW_TOUCH_MESSAGE, showTouchMessage = false).apply();
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				builder.setPositiveButton(R.string.OK, null).setMessage(R.string.touchnotify).create().show();
			}
			
			if(showSoundMessage) {
				prefs.edit().putBoolean(Settings.SHOW_SOUND_MESSAGE, showSoundMessage = false).apply();
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				builder.setMessage(R.string.soundmsg).setPositiveButton(R.string.yes, new Dialog.OnClickListener() {

					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						prefs.edit().putBoolean(Settings.ENABLE_SOUND, true).apply();
						arg0.dismiss();
					}
					
				}).setNegativeButton(R.string.no, null).create().show();
			}

			synchronized(view) {
				if(!DeSmuME.inited)
					return;
				
				if(doForceResize)
					view.resize(width, height, pixelFormat);
				
				if(emuBitmapMain == null)
					return;
				
				
				int data = DeSmuME.fastForwardMode ? fpsData : DeSmuME.draw(emuBitmapMain, emuBitmapTouch, landscape && dontRotate);
				
				final boolean drawTouch = destTouch.left != 0 || destTouch.right != 0;

				if(lcdSwap) {
					if(drawTouch) {
						canvas.drawBitmap(emuBitmapTouch, srcMain, destMain, emuPaint);
						canvas.drawBitmap(emuBitmapMain, srcTouch, destTouch, emuPaint);
					}
					else
						canvas.drawBitmap(emuBitmapMain, srcMain, destMain, emuPaint);
				}
				else {
					canvas.drawBitmap(emuBitmapMain, srcMain, destMain, emuPaint);
					if(drawTouch)
						canvas.drawBitmap(emuBitmapTouch, srcTouch, destTouch, emuPaint);
				}
				
				controls.drawControls(canvas);
				
				if(stateText != null) {
					final long fadeHoldMs = 1000;
					final float fadeAwayMs = 750.0f;
					
					final long time = System.currentTimeMillis();
					final long fadeDiff = time - stateDrawStart;
					
					if(fadeDiff >= (fadeHoldMs + fadeAwayMs))
						stateText = null;
					else {
						final boolean fading = fadeDiff > fadeHoldMs;
						if(fading) {
							final float fadeTime = (fadeDiff - fadeHoldMs) / fadeAwayMs;
							final int fadeColor = Color.argb(255 - (int) (fadeTime * 255), 255, 255, 255);
							hudPaint.setColor(fadeColor);
						}
						canvas.drawText(stateText, 10, curhudsize * 2, hudPaint);
						if(fading)
							hudPaint.setColor(Color.WHITE);
					}
				}
				
				if (showfps)
				{
					if(data != fpsData || fpsText == null) {
						int fps = (data >> 24) & 0xFF;
						int fps3d = (data >> 16) & 0xFF;
						int cpuload0 = (data >> 8) & 0xFF;
						int cpuload1 = data & 0xFF;
						
						fpsText = "Fps:"+fps+"/"+fps3d+"("+cpuload0+"%/"+cpuload1+"%)";
						fpsData = data;
					}
					canvas.drawText(fpsText, 10, curhudsize, hudPaint);
				}
			}
			
		}
		
		
		@Override
		public boolean onTouchEvent(MotionEvent event) {
			return controls.onTouchEvent(event);
		}
		
		boolean resized = false;
		boolean sized = false;
		boolean landscape = false;
		boolean dontRotate = false;
		int sourceWidth;
		int sourceHeight;
		Rect srcMain, destMain, srcTouch, destTouch;
		int width = 0, height = 0, pixelFormat;
		String stateText = null;
		long stateDrawStart = 0;
		int fpsData = 0;
		int screenOption = 0;
		String fpsText = null;
		
		
		void resize(int newWidth, int newHeight, int newPixelFormat) {
			
			synchronized(view) {
				sourceWidth = DeSmuME.getNativeWidth();
				sourceHeight = DeSmuME.getNativeHeight();
				resized = true;
				
				screenOption = Integer.valueOf(prefs.getString(Settings.SPECIFIC_SCREEN_ONLY, "0"));
				final boolean hasScreenFilter = DeSmuME.getSettingInt(Settings.SCREEN_FILTER, 0) != 0;
				final boolean is565 = newPixelFormat == PixelFormat.RGB_565 && !hasScreenFilter;
				final boolean stretch = !prefs.getBoolean(Settings.MAINTAIN_ASPECT_RATIO, false);
				landscape = newWidth > newHeight;
				controls.setView(this);
				int xBlack = 0, yBlack = 0;
				
				forceTouchScreen = !prefs.getBoolean("Controls." + (landscape ? "Landscape." : "Portrait.") + "Draw", false);
				
				final Rect emptyRect = new Rect(0, 0, 0, 0);
				
				if(landscape) {
					if(stretch) {
						if(screenOption > 0) {
							final Rect full = new Rect(0, 0, newWidth, newHeight);
							destMain = screenOption == 1 ? full : emptyRect;
							destTouch = screenOption == 1 ? emptyRect : full;
						}
						else {
							destMain = new Rect(0, 0, newWidth / 2, newHeight);
							destTouch = new Rect(newWidth / 2, 0, newWidth, newHeight);
						}
					}
					else {
						final float dsAspect = (screenOption > 0) ? (float)(sourceWidth) / (float)(sourceHeight / 2) :
							(float)(sourceWidth * 2) / (float)(sourceHeight / 2);
						final float screenAspect = (float)newWidth / (float)newHeight;
						if(dsAspect > screenAspect) {
							//the DS is "wider" than our screen -- match to the width
							final int aspectHeight = (int)((float)newWidth / dsAspect);
							final int black = yBlack = (newHeight - aspectHeight) / 2;
							if(screenOption > 0) {
								final Rect full = new Rect(0,black,newWidth,newHeight - black);
								destMain = screenOption == 1 ? full : emptyRect;
								destTouch = screenOption == 1 ? emptyRect : full;
							}
							else {
								destMain = new Rect(0,black,newWidth / 2,newHeight - black);
								destTouch = new Rect(newWidth / 2,black,newWidth,newHeight-black);
							}
						}
						else {
							//match to the height
							final int aspectWidth = (int)((float)newHeight * dsAspect);
							final int black = xBlack = (newWidth - aspectWidth) / 2;
							if(screenOption > 0) {
								final Rect full = new Rect(black, 0, newWidth - black, newHeight);
								destMain = screenOption == 1 ? full : emptyRect;
								destTouch = screenOption == 1 ? emptyRect : full;
							}
							else {
								destMain = new Rect(black, 0, aspectWidth / 2 + black, newHeight);
								destTouch = new Rect(aspectWidth / 2 + black, 0, newWidth - black, newHeight);
							}
						}
					}
				}
				else {
					if(stretch) {
						if(screenOption > 0) {
							final Rect full = new Rect(0, 0, newWidth, newHeight);
							destMain = screenOption == 1 ? full : emptyRect;
							destTouch = screenOption == 1 ? emptyRect : full;
						}
						else {
							destMain = new Rect(0, 0, newWidth, newHeight / 2);
							destTouch = new Rect(0, newHeight / 2, newWidth, newHeight);
						}
					}
					else {
						final float dsAspect = (screenOption > 0) ? (float)sourceWidth / (float)(sourceHeight / 2) : 
							(float)sourceWidth / (float)sourceHeight;
						final float screenAspect = (float)newWidth / (float)newHeight;
						if(dsAspect > screenAspect) {
							//the DS is "wider" than our screen -- match to the width
							final int aspectHeight = (int)((float)newWidth / dsAspect);
							final int black = yBlack = (newHeight - aspectHeight) / 2;
							if(screenOption > 0) {
								final Rect full = new Rect(0, black, newWidth, newHeight - black);
								destMain = screenOption == 1 ? full : emptyRect;
								destTouch = screenOption == 1 ? emptyRect : full;
							}
							else {
								destMain = new Rect(0,black,newWidth,aspectHeight / 2 + black);
								destTouch = new Rect(0,aspectHeight / 2 + black,newWidth,newHeight-black);
							}
						}
						else {
							//match to the height
							final int aspectWidth = (int)((float)newHeight * dsAspect);
							final int black = xBlack = (newWidth - aspectWidth) / 2;
							if(screenOption > 0) {
								final Rect full = new Rect(black, 0, newWidth - black, newHeight);
								destMain = screenOption == 1 ? full : emptyRect;
								destTouch = screenOption == 1 ? emptyRect : full;
							}
							else {
								destMain = new Rect(black, 0, aspectWidth + black, newHeight / 2);
								destTouch = new Rect(black, newHeight / 2, aspectWidth + black, newHeight);
							}
						}
					}
				}
				
				controls.loadControls(MainActivity.this, newWidth, newHeight, xBlack, yBlack, screenOption, is565, landscape);
				
				if(landscape && dontRotate) {
					emuBitmapMain = Bitmap.createBitmap(sourceHeight / 2, sourceWidth, is565 ? Config.RGB_565 : Config.ARGB_8888);
					emuBitmapTouch = Bitmap.createBitmap(sourceHeight / 2, sourceWidth, is565 ? Config.RGB_565 : Config.ARGB_8888);
					srcMain = new Rect(0, 0, sourceHeight / 2, sourceWidth);
					srcTouch = new Rect(0, 0, sourceHeight / 2, sourceWidth);
				}
				else {
					emuBitmapMain = Bitmap.createBitmap(sourceWidth, sourceHeight / 2, is565 ? Config.RGB_565 : Config.ARGB_8888);
					emuBitmapTouch = Bitmap.createBitmap(sourceWidth, sourceHeight / 2, is565 ? Config.RGB_565 : Config.ARGB_8888);
					srcMain = new Rect(0, 0, sourceWidth, sourceHeight / 2);
					srcTouch = new Rect(0, 0, sourceWidth, sourceHeight / 2);
				}
				DeSmuME.resize(emuBitmapMain);
				
				requestFocus();
				
				width = newWidth;
				height = newHeight;
				pixelFormat = newPixelFormat;
				sized = true;
				doForceResize = false;
				
				float max_wh = newWidth > newHeight ? newWidth : newHeight;
				curhudsize = (max_wh / 384.0f) * defhudsize;
				hudPaint.setTextSize(curhudsize);
				
			}
		}


		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			synchronized(view) {
				if(width > 0 && height > 0) //not sure why, but some devices are sending 0s for these values sometimes
					view.resize(width, height, format);
			}
		}


		@Override
		public void surfaceCreated(SurfaceHolder arg0) {
		}


		@Override
		public void surfaceDestroyed(SurfaceHolder arg0) {
		}
		
		@Override
		public boolean onKeyDown(int keyCode, KeyEvent event) {
			return controls.onKeyDown(keyCode, event);
		}
		
		@Override
		public boolean onKeyUp(int keyCode, KeyEvent event) {
			return controls.onKeyUp(keyCode, event);
		}
		
		public void showMenu() {
			MainActivity.this.openOptionsMenu();
		}
		
	}
	
}
