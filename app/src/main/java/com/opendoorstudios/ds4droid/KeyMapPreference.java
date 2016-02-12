package com.opendoorstudios.ds4droid;


import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

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

public class KeyMapPreference extends DialogPreference implements OnKeyListener {

	public KeyMapPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		currentValuePreface = context.getResources().getString(R.string.KeymapValuePreface);
	}
	
	int currentValue;
	TextView currentValueDesc;
	final String currentValuePreface;
	
	static String getKeyDesc(int value) {
		if(value == 0)
			return "(none)";
		else {
			final String ret = KEYCODE_SYMBOLIC_NAMES.get(value, "(none)");
			return ret == null ? "(unknown)" : ret;
		}
	}
	
	void sync() {
		currentValueDesc.setText(currentValuePreface + " " + getKeyDesc(currentValue));
	}
	
	@Override
	protected View onCreateDialogView() {
		currentValue = getPersistedInt(0);
		
		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		RelativeLayout layout = (RelativeLayout)inflater.inflate(R.layout.keymap, null);
		layout.setOnKeyListener(this);
		layout.requestFocus();
		currentValueDesc = (TextView)layout.findViewById(R.id.keymap_value);
		sync();
		
		return layout;
	}
	
	@Override
	public CharSequence getSummary() {
		String summary = super.getSummary().toString();
		int value = getPersistedInt(currentValue);
		return summary + " " + getKeyDesc(value);
	}
	
	
	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		/*switch(keyCode) {
		case KeyEvent.KEYCODE_HOME:
		case KeyEvent.KEYCODE_BACK:
		case KeyEvent.KEYCODE_SETTINGS:
		case KeyEvent.KEYCODE_SEARCH:
			return false;
		}*/
		currentValue = keyCode;
		sync();
		return true;
	};
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if (!positiveResult) {
			return;
		}

		if (shouldPersist()) {
			persistInt(currentValue);
		}

		notifyChanged();
	}
	
	static {
		KEYCODE_SYMBOLIC_NAMES = new SparseArray<String>();
		populateKeycodeSymbolicNames();
	}
	
	//This actually comes straight from the Android source code (android.view.KeyEvent) but it isn't in until ICS
	private static final SparseArray<String> KEYCODE_SYMBOLIC_NAMES;
    private static void populateKeycodeSymbolicNames() {
        SparseArray<String> names = KEYCODE_SYMBOLIC_NAMES;
        names.append(KeyEvent.KEYCODE_UNKNOWN, "UNKNOWN");
        names.append(KeyEvent.KEYCODE_SOFT_LEFT, "SOFT_LEFT");
        names.append(KeyEvent.KEYCODE_SOFT_RIGHT, "SOFT_RIGHT");
        names.append(KeyEvent.KEYCODE_HOME, "HOME");
        names.append(KeyEvent.KEYCODE_BACK, "BACK");
        names.append(KeyEvent.KEYCODE_CALL, "CALL");
        names.append(KeyEvent.KEYCODE_ENDCALL, "ENDCALL");
        names.append(KeyEvent.KEYCODE_0, "0");
        names.append(KeyEvent.KEYCODE_1, "1");
        names.append(KeyEvent.KEYCODE_2, "2");
        names.append(KeyEvent.KEYCODE_3, "3");
        names.append(KeyEvent.KEYCODE_4, "4");
        names.append(KeyEvent.KEYCODE_5, "5");
        names.append(KeyEvent.KEYCODE_6, "6");
        names.append(KeyEvent.KEYCODE_7, "7");
        names.append(KeyEvent.KEYCODE_8, "8");
        names.append(KeyEvent.KEYCODE_9, "9");
        names.append(KeyEvent.KEYCODE_STAR, "STAR");
        names.append(KeyEvent.KEYCODE_POUND, "POUND");
        names.append(KeyEvent.KEYCODE_DPAD_UP, "DPAD_UP");
        names.append(KeyEvent.KEYCODE_DPAD_DOWN, "DPAD_DOWN");
        names.append(KeyEvent.KEYCODE_DPAD_LEFT, "DPAD_LEFT");
        names.append(KeyEvent.KEYCODE_DPAD_RIGHT, "DPAD_RIGHT");
        names.append(KeyEvent.KEYCODE_DPAD_CENTER, "DPAD_CENTER");
        names.append(KeyEvent.KEYCODE_VOLUME_UP, "VOLUME_UP");
        names.append(KeyEvent.KEYCODE_VOLUME_DOWN, "VOLUME_DOWN");
        names.append(KeyEvent.KEYCODE_POWER, "POWER");
        names.append(KeyEvent.KEYCODE_CAMERA, "CAMERA");
        names.append(KeyEvent.KEYCODE_CLEAR, "CLEAR");
        names.append(KeyEvent.KEYCODE_A, "A");
        names.append(KeyEvent.KEYCODE_B, "B");
        names.append(KeyEvent.KEYCODE_C, "C");
        names.append(KeyEvent.KEYCODE_D, "D");
        names.append(KeyEvent.KEYCODE_E, "E");
        names.append(KeyEvent.KEYCODE_F, "F");
        names.append(KeyEvent.KEYCODE_G, "G");
        names.append(KeyEvent.KEYCODE_H, "H");
        names.append(KeyEvent.KEYCODE_I, "I");
        names.append(KeyEvent.KEYCODE_J, "J");
        names.append(KeyEvent.KEYCODE_K, "K");
        names.append(KeyEvent.KEYCODE_L, "L");
        names.append(KeyEvent.KEYCODE_M, "M");
        names.append(KeyEvent.KEYCODE_N, "N");
        names.append(KeyEvent.KEYCODE_O, "O");
        names.append(KeyEvent.KEYCODE_P, "P");
        names.append(KeyEvent.KEYCODE_Q, "Q");
        names.append(KeyEvent.KEYCODE_R, "R");
        names.append(KeyEvent.KEYCODE_S, "S");
        names.append(KeyEvent.KEYCODE_T, "T");
        names.append(KeyEvent.KEYCODE_U, "U");
        names.append(KeyEvent.KEYCODE_V, "V");
        names.append(KeyEvent.KEYCODE_W, "W");
        names.append(KeyEvent.KEYCODE_X, "X");
        names.append(KeyEvent.KEYCODE_Y, "Y");
        names.append(KeyEvent.KEYCODE_Z, "Z");
        names.append(KeyEvent.KEYCODE_COMMA, "COMMA");
        names.append(KeyEvent.KEYCODE_PERIOD, "PERIOD");
        names.append(KeyEvent.KEYCODE_ALT_LEFT, "ALT_LEFT");
        names.append(KeyEvent.KEYCODE_ALT_RIGHT, "ALT_RIGHT");
        names.append(KeyEvent.KEYCODE_SHIFT_LEFT, "SHIFT_LEFT");
        names.append(KeyEvent.KEYCODE_SHIFT_RIGHT, "SHIFT_RIGHT");
        names.append(KeyEvent.KEYCODE_TAB, "TAB");
        names.append(KeyEvent.KEYCODE_SPACE, "SPACE");
        names.append(KeyEvent.KEYCODE_SYM, "SYM");
        names.append(KeyEvent.KEYCODE_EXPLORER, "EXPLORER");
        names.append(KeyEvent.KEYCODE_ENVELOPE, "ENVELOPE");
        names.append(KeyEvent.KEYCODE_ENTER, "ENTER");
        names.append(KeyEvent.KEYCODE_DEL, "DEL");
        names.append(KeyEvent.KEYCODE_GRAVE, "GRAVE");
        names.append(KeyEvent.KEYCODE_MINUS, "MINUS");
        names.append(KeyEvent.KEYCODE_EQUALS, "EQUALS");
        names.append(KeyEvent.KEYCODE_LEFT_BRACKET, "LEFT_BRACKET");
        names.append(KeyEvent.KEYCODE_RIGHT_BRACKET, "RIGHT_BRACKET");
        names.append(KeyEvent.KEYCODE_BACKSLASH, "BACKSLASH");
        names.append(KeyEvent.KEYCODE_SEMICOLON, "SEMICOLON");
        names.append(KeyEvent.KEYCODE_APOSTROPHE, "APOSTROPHE");
        names.append(KeyEvent.KEYCODE_SLASH, "SLASH");
        names.append(KeyEvent.KEYCODE_AT, "AT");
        names.append(KeyEvent.KEYCODE_NUM, "NUM");
        names.append(KeyEvent.KEYCODE_HEADSETHOOK, "HEADSETHOOK");
        names.append(KeyEvent.KEYCODE_FOCUS, "FOCUS");
        names.append(KeyEvent.KEYCODE_PLUS, "PLUS");
        names.append(KeyEvent.KEYCODE_MENU, "MENU");
        names.append(KeyEvent.KEYCODE_NOTIFICATION, "NOTIFICATION");
        names.append(KeyEvent.KEYCODE_SEARCH, "SEARCH");
        names.append(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, "MEDIA_PLAY_PAUSE");
        names.append(KeyEvent.KEYCODE_MEDIA_STOP, "MEDIA_STOP");
        names.append(KeyEvent.KEYCODE_MEDIA_NEXT, "MEDIA_NEXT");
        names.append(KeyEvent.KEYCODE_MEDIA_PREVIOUS, "MEDIA_PREVIOUS");
        names.append(KeyEvent.KEYCODE_MEDIA_REWIND, "MEDIA_REWIND");
        names.append(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, "MEDIA_FAST_FORWARD");
        names.append(KeyEvent.KEYCODE_MUTE, "MUTE");
        names.append(KeyEvent.KEYCODE_PAGE_UP, "PAGE_UP");
        names.append(KeyEvent.KEYCODE_PAGE_DOWN, "PAGE_DOWN");
        names.append(KeyEvent.KEYCODE_PICTSYMBOLS, "PICTSYMBOLS");
        names.append(KeyEvent.KEYCODE_SWITCH_CHARSET, "SWITCH_CHARSET");
        
        if(!MainActivity.IS_OUYA) {
	        names.append(KeyEvent.KEYCODE_BUTTON_A, "BUTTON_A");
	        names.append(KeyEvent.KEYCODE_BUTTON_B, "BUTTON_B");
	        names.append(KeyEvent.KEYCODE_BUTTON_X, "BUTTON_X");
	        names.append(KeyEvent.KEYCODE_BUTTON_Y, "BUTTON_Y");
	        names.append(KeyEvent.KEYCODE_BUTTON_L1, "BUTTON_L1");
	        names.append(KeyEvent.KEYCODE_BUTTON_R1, "BUTTON_R1");
	        names.append(KeyEvent.KEYCODE_BUTTON_L2, "BUTTON_L2");
	        names.append(KeyEvent.KEYCODE_BUTTON_R2, "BUTTON_R2");
	        names.append(KeyEvent.KEYCODE_BUTTON_THUMBL, "BUTTON_THUMBL");
	        names.append(KeyEvent.KEYCODE_BUTTON_THUMBR, "BUTTON_THUMBR");
        }
        else {
        	//The OUYA interface guidelines wants us to call the buttons by these names
        	names.append(KeyEvent.KEYCODE_BUTTON_A, "O");
	        names.append(KeyEvent.KEYCODE_BUTTON_B, "A");
	        names.append(KeyEvent.KEYCODE_BUTTON_X, "U");
	        names.append(KeyEvent.KEYCODE_BUTTON_Y, "Y");
	        names.append(KeyEvent.KEYCODE_BUTTON_L1, "L1");
	        names.append(KeyEvent.KEYCODE_BUTTON_R1, "R1");
	        names.append(KeyEvent.KEYCODE_BUTTON_L2, "L2");
	        names.append(KeyEvent.KEYCODE_BUTTON_R2, "R2");
	        names.append(KeyEvent.KEYCODE_BUTTON_THUMBL, "L3");
	        names.append(KeyEvent.KEYCODE_BUTTON_THUMBR, "R3");
        }
        
        names.append(KeyEvent.KEYCODE_BUTTON_START, "BUTTON_START");
        names.append(KeyEvent.KEYCODE_BUTTON_SELECT, "BUTTON_SELECT");
        names.append(KeyEvent.KEYCODE_BUTTON_MODE, "BUTTON_MODE");
        names.append(KeyEvent.KEYCODE_BUTTON_Z, "BUTTON_Z");
        names.append(KeyEvent.KEYCODE_BUTTON_C, "BUTTON_C");
	    names.append(188, "KEYCODE_BUTTON_1"); //KEYCODE_BUTTON_1
    }



}
