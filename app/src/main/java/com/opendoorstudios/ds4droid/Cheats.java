package com.opendoorstudios.ds4droid;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

public class Cheats extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.cheats);
		
		inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
		
		cheatList = (ListView) findViewById(R.id.cheatList);
		cheatList.setAdapter(adapter = new CheatAdapter());
		
		final android.widget.Button addButton = (Button) findViewById(R.id.addcheat);
		addButton.setOnClickListener(new android.view.View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				currentlyEditing = -1;
				showEditDialog();
			}
		});
		
		
	}
	
	CheatAdapter adapter = null;
	ListView cheatList = null;
	LayoutInflater inflater;
	int currentlyEditing = -1;
	TextView editingDescription, editingCode;
	boolean editingCheckedState = false;
	
	void showEditDialog() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		View cheatEditView;
		final AlertDialog dialog = builder.setPositiveButton(R.string.OK, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(currentlyEditing == -1) 
					DeSmuME.addCheat(editingDescription.getText().toString(), editingCode.getText().toString());
				else
					DeSmuME.updateCheat(editingDescription.getText().toString(), editingCode.getText().toString(), currentlyEditing);
				DeSmuME.saveCheats();
				adapter.notifyDataSetChanged();
				dialog.dismiss();
			}
			
		}).setNegativeButton(R.string.cancel, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
			
		}).setView(cheatEditView = inflater.inflate(R.layout.cheatedit, null)).create();
		
		editingDescription = (TextView)cheatEditView.findViewById(R.id.cheatDesc);
		editingCode = (TextView)cheatEditView.findViewById(R.id.cheatCode);
		
		if(currentlyEditing != -1) {
			
			editingDescription.setText(DeSmuME.getCheatName(currentlyEditing));
			editingCode.setText(DeSmuME.getCheatCode(currentlyEditing));
		}
		dialog.show();
	}
	
	class CheatAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return DeSmuME.getNumberOfCheats();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if(convertView == null) {
				convertView = inflater.inflate(R.layout.cheatrow, null);
				final android.widget.Button edit = (Button)convertView.findViewById(R.id.cheatEdit);
				final android.widget.Button delete = (Button)convertView.findViewById(R.id.cheatDelete);
				edit.setOnClickListener(new android.view.View.OnClickListener() {

					@Override
					public void onClick(View v) {
						final Object tag = v.getTag();
						if(tag != null && tag instanceof Integer) {
							currentlyEditing = (Integer)tag;
							showEditDialog();
						}
					}
	
				});
				delete.setOnClickListener(new android.view.View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						final Object tag = v.getTag();
						if(tag != null && tag instanceof Integer) {
							DeSmuME.deleteCheat((Integer)tag);
							notifyDataSetChanged();
						}
					}
				});
			}
			
			final CheckBox cheatEnabled = (CheckBox)convertView.findViewById(R.id.cheatEnabled);
			cheatEnabled.setText(DeSmuME.getCheatName(position));
			editingCheckedState = true;
			cheatEnabled.setChecked(DeSmuME.getCheatEnabled(position));
			editingCheckedState = false; //dunno if we need to do this or not
			cheatEnabled.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if(editingCheckedState)
						return;
					final Object tag = buttonView.getTag();
					if(tag != null && tag instanceof Integer)
						DeSmuME.setCheatEnabled((Integer)tag, isChecked);					
				}
				
			});
			cheatEnabled.setTag(position);
			
			final android.widget.Button edit = (Button)convertView.findViewById(R.id.cheatEdit);
			edit.setTag(position);
			edit.setEnabled(DeSmuME.getCheatType(position) == 1); //only support editing AR codes for now
			
			final android.widget.Button delete = (Button)convertView.findViewById(R.id.cheatDelete);
			delete.setTag(position);
			
			return convertView;
		}
		
	}
	
	@Override
	public void onStop() {
		super.onStop();
		DeSmuME.saveCheats();
	}
	
	
}
