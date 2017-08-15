package com.opendoorstudios.ds4droid;

import android.app.Activity;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.widget.TextView;

public class About extends Activity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.about);
		
		final TextView textVersion = (TextView)findViewById(R.id.about_versionstring);
		
		final StringBuilder build = new StringBuilder();
		
		String library;
		
		DeSmuME.load();
		
		switch(DeSmuME.getCPUType()) {
		case DeSmuME.CPUTYPE_V7: library = "v7"; break;
		case DeSmuME.CPUTYPE_NEON: library = "neon"; break;
		case DeSmuME.CPUTYPE_X86: library = "x86"; break;
		default: library = "unknown";
		}
		
		String version = null;
		try {
			version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			version = "unknown";
			e.printStackTrace();
		}
	
		build.append(getString(R.string.app_name)).append(" ").append(version).append(
						"/").append(library);
		
		textVersion.setText(build.toString());
	}

}
