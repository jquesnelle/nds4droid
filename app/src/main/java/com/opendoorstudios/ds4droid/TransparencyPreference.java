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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.preference.DialogPreference;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class TransparencyPreference extends DialogPreference implements OnSeekBarChangeListener {

	final Drawable example;
	
	public TransparencyPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		example = context.getResources().getDrawable(R.drawable.dpad);
	}

	private SeekBar seek;
	private ImageView img;
	private DisplayMetrics metrics;

	static final int defaultValue = 78;
	private int currentValue;

	@Override
	protected View onCreateDialogView() {

		currentValue = getPersistedInt(defaultValue);

		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.dialog_slider, null);

		((TextView) view.findViewById(R.id.min_value)).setText("0");
		((TextView) view.findViewById(R.id.max_value)).setText("100");

		seek = (SeekBar) view.findViewById(R.id.seek_bar);
		seek.setMax(100);
		seek.setProgress(currentValue);
		seek.setOnSeekBarChangeListener(this);

		img = (ImageView) view.findViewById(R.id.current_value);
		img.setImageDrawable(example);
		img.setAlpha((int)(currentValue * 2.55f));

		return view;
	}
	


	

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

	@Override
	public CharSequence getSummary() {

		String summary = super.getSummary().toString();
		int value = getPersistedInt(currentValue);
		return summary + " (currently " + value + "%)";
	}

	public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
		currentValue = value;
		img.setAlpha((int)(currentValue * 2.55f));
	}

	@Override
	public void onStartTrackingTouch(SeekBar arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onStopTrackingTouch(SeekBar arg0) {
		// TODO Auto-generated method stub
	}

}