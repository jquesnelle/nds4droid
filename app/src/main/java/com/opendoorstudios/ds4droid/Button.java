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

import java.util.HashMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.preference.PreferenceManager;

class Button {
	
	Button(int id) {
		this.id = id;
		this.position = null;
		this.bitmap = null;
	}
	
	Button(Rect position, int id) {
		this.position = position;
		this.id = id;
		this.bitmap = null;
	}
	
	Button(Rect position, int id, Bitmap bitmap) {
		this.position = position;
		this.id = id;
		this.bitmap = bitmap;
	}
	
	static final Button L_PORT_DEFAULT = new Button(new Rect(0, 590, 160, 680), Button.BUTTON_L);
	static final Button R_PORT_DEFAULT = new Button(new Rect(610, 590, 768, 680), Button.BUTTON_R);
	static final Button TOUCH_PORT_DEFAULT = new Button(new Rect(320, 590, 441, 659), Button.BUTTON_TOUCH);
	static final Button DPAD_PORT_DEFAULT = new Button(new Rect(0, 760, 334, 1074), Button.BUTTON_DPAD);
	static final Button ABXY_PORT_DEFAULT = new Button(new Rect(397, 755, 759, 1072), Button.BUTTON_ABXY);
	static final Button START_PORT_DEFAULT = new Button(new Rect(270, 1082, 366, 1147), Button.BUTTON_START);
	static final Button SELECT_PORT_DEFAULT = new Button(new Rect(400, 1082, 485, 1145), Button.BUTTON_SELECT);
	//static final Button FASTFORWARD_PORT_DEFAULT = new Button(new Rect(595, 1190, 713, 1281), Button.BUTTON_FASTFORWARD);
	static final Button OPTIONS_DEFAULT = new Button(new Rect(620, 1068, 768, 1152), Button.BUTTON_OPTIONS);
	
	static final Button L_LAND_DEFAULT = new Button(new Rect(0, 0, 160, 90), Button.BUTTON_L);
	static final Button R_LAND_DEFAULT = new Button(new Rect(994, 0, 1152, 90), Button.BUTTON_R);
	static final Button DPAD_LAND_DEFAULT = new Button(new Rect(0, 454, 334, 768), Button.BUTTON_DPAD);
	static final Button ABXY_LAND_DEFAULT = new Button(new Rect(770, 451, 1132, 768), Button.BUTTON_ABXY);
	static final Button START_LAND_DEFAULT = new Button(new Rect(380, 703, 476, 768), Button.BUTTON_START);
	static final Button TOUCH_LAND_DEFAULT = new Button(new Rect(506, 703, 627, 768), Button.BUTTON_TOUCH);
	static final Button SELECT_LAND_DEFAULT = new Button(new Rect(667, 703, 752, 768), Button.BUTTON_SELECT);
	//static final Button FASTFORWARD_LAND_DEFAULT = new Button(new Rect(487, 0, 645, 90), Button.BUTTON_FASTFORWARD);
	static final Button OPTIONS_LAND_DEFAULT = new Button(new Rect(502, 0, 650, 84), Button.BUTTON_OPTIONS);
	
	
	static final HashMap<Integer, Button> portraitToDefault;
	static final HashMap<Integer, Button> landscapeToDefault;
	
	static {
		portraitToDefault = new HashMap<Integer, Button>();
		portraitToDefault.put(Button.BUTTON_L, L_PORT_DEFAULT);
		portraitToDefault.put(Button.BUTTON_R, R_PORT_DEFAULT);
		portraitToDefault.put(Button.BUTTON_TOUCH, TOUCH_PORT_DEFAULT);
		portraitToDefault.put(Button.BUTTON_DPAD, DPAD_PORT_DEFAULT);
		portraitToDefault.put(Button.BUTTON_ABXY, ABXY_PORT_DEFAULT);
		portraitToDefault.put(Button.BUTTON_START, START_PORT_DEFAULT);
		portraitToDefault.put(Button.BUTTON_SELECT, SELECT_PORT_DEFAULT);
		//portraitToDefault.put(Button.BUTTON_FASTFORWARD, FASTFORWARD_PORT_DEFAULT);
		portraitToDefault.put(Button.BUTTON_OPTIONS, OPTIONS_DEFAULT);
		
		landscapeToDefault = new HashMap<Integer, Button>();
		landscapeToDefault.put(Button.BUTTON_L, L_LAND_DEFAULT);
		landscapeToDefault.put(Button.BUTTON_R, R_LAND_DEFAULT);
		landscapeToDefault.put(Button.BUTTON_TOUCH, TOUCH_LAND_DEFAULT);
		landscapeToDefault.put(Button.BUTTON_DPAD, DPAD_LAND_DEFAULT);
		landscapeToDefault.put(Button.BUTTON_ABXY, ABXY_LAND_DEFAULT);
		landscapeToDefault.put(Button.BUTTON_START, START_LAND_DEFAULT);
		landscapeToDefault.put(Button.BUTTON_SELECT, SELECT_LAND_DEFAULT);
		//landscapeToDefault.put(Button.BUTTON_FASTFORWARD, FASTFORWARD_LAND_DEFAULT);
		landscapeToDefault.put(Button.BUTTON_OPTIONS, OPTIONS_LAND_DEFAULT);
	}
	
	static Button load(Context context, int id, int resId, boolean landscape, boolean is565, Rect screen, Rect space, boolean forceLoad) {
		
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		final String prefLayoutBase = "Controls." + (landscape ? "Landscape." : "Portrait.");
		final String prefBase = prefLayoutBase + getButtonName(id) + "." ;
		final Button template = landscape ? landscapeToDefault.get(id) : portraitToDefault.get(id);
		if(forceLoad || prefs.getBoolean(prefLayoutBase + "Draw" , true)) {
			float controlxscale = (float)screen.width() / (float)space.width();
			float controlyscale = (float)screen.height() / (float)space.height();

			final Rect finalRect = new Rect(prefs.getInt(prefBase + "Left", (int) (template.position.left * controlxscale)),
					prefs.getInt(prefBase + "Top", (int) (template.position.top * controlyscale)),
					prefs.getInt(prefBase + "Right", (int) (template.position.right * controlxscale)),
					prefs.getInt(prefBase + "Bottom", (int) (template.position.bottom * controlyscale)));
	
			final BitmapFactory.Options controlOptions = new BitmapFactory.Options();
			if(is565)
				controlOptions.inPreferredConfig = Bitmap.Config.RGB_565;
			final Bitmap originalControls = BitmapFactory.decodeResource(context.getResources(), resId, controlOptions);
			final Bitmap controls = Bitmap.createScaledBitmap(originalControls, finalRect.width(), finalRect.height(), true);
			return new Button(finalRect, id, controls);
		}
		else
			return new Button(template.position, id);
		
	}
	
	static String getButtonName(int id) {
		switch(id) {
		case BUTTON_RIGHT: return "Right";
		case BUTTON_DOWN: return "Down";
		case BUTTON_UP: return "Up";
		case BUTTON_LEFT: return "Left";
		case BUTTON_A: return "A";
		case BUTTON_B: return "B";
		case BUTTON_X: return "X";
		case BUTTON_Y: return "Y";
		case BUTTON_L: return "L";
		case BUTTON_R: return "R";
		case BUTTON_START: return "Start";
		case BUTTON_SELECT: return "Select";
		case BUTTON_TOUCH: return "Touch";
		case BUTTON_DPAD: return "DPad";
		case BUTTON_ABXY: return "ABXY";
		case BUTTON_UPLEFT: return "UpLeft";
		case BUTTON_UPRIGHT: return "UpRight";
		case BUTTON_DOWNLEFT: return "DownLeft";
		case BUTTON_DOWNRIGHT: return "DownRight";
		case BUTTON_OPTIONS: return "Options";
		//case BUTTON_FASTFORWARD: return "Fast forward";
		default: return "Unknown";
		}
	}
	
	void apply(int[] states, boolean on) {
		final int val = on ? 1 : 0;
		switch(id) {
		case BUTTON_UPLEFT:
			states[Button.BUTTON_UP] = states[Button.BUTTON_LEFT] = val;
			break;
		case BUTTON_UPRIGHT:
			states[Button.BUTTON_UP] = states[Button.BUTTON_RIGHT] = val;
			break;
		case BUTTON_DOWNLEFT:
			states[Button.BUTTON_DOWN] = states[Button.BUTTON_LEFT] = val;
			break;
		case BUTTON_DOWNRIGHT:
			states[Button.BUTTON_DOWN] = states[Button.BUTTON_RIGHT] = val;
			break;
		default:
			if(id >= 0 && id < states.length)
				states[id] = val;
			break;
		}
	}
	
	void applyToPrefs(SharedPreferences prefs, boolean landscape, boolean overwrite) {
		final String prefLayoutBase = "Controls." + (landscape ? "Landscape." : "Portrait.");
		final String prefBase = prefLayoutBase + getButtonName(id) + "." ;
		if(prefs.contains(prefBase + "Left") && !overwrite)
			return;
		prefs.edit().putInt(prefBase + "Left", position.left).putInt(prefBase + "Top", position.top).putInt(prefBase + "Right", position.right).putInt(prefBase + "Bottom", position.bottom).apply();
	}
	
	static final int BUTTON_RIGHT = 0;
	static final int BUTTON_DOWN = 1;
	static final int BUTTON_UP = 2;
	static final int BUTTON_LEFT = 3;
	static final int BUTTON_A = 4;
	static final int BUTTON_B = 5;
	static final int BUTTON_X = 6;
	static final int BUTTON_Y = 7;
	static final int BUTTON_L = 8;
	static final int BUTTON_R = 9;
	static final int BUTTON_START = 10;
	static final int BUTTON_SELECT = 11;
	
	//meta buttons, aren't actually passed down
	static final int BUTTON_TOUCH = 12;
	static final int BUTTON_DPAD = 13;
	static final int BUTTON_ABXY = 14;
	static final int BUTTON_UPLEFT = 15;
	static final int BUTTON_UPRIGHT = 16;
	static final int BUTTON_DOWNLEFT = 17;
	static final int BUTTON_DOWNRIGHT = 18;
	static final int BUTTON_OPTIONS = 19;
	//static final int BUTTON_FASTFORWARD = 20;
	
	final Bitmap bitmap;
	Rect position;
	final int id;
}
