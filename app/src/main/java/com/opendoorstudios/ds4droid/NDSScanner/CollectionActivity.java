package com.opendoorstudios.ds4droid.NDSScanner;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.opendoorstudios.ds4droid.R;
import com.opendoorstudios.ds4droid.FileDialog;
import com.opendoorstudios.ds4droid.Settings;

import java.io.File;

//==============================================================================
public class CollectionActivity extends Activity implements GridView.OnItemClickListener {
	//--------------------------------------------------------------------------

	private GridView      grid;
	private RomAdapter    adapter;
	private static RomCollection collection;
	
	private String browserRoot;
	private String[] browserFilter;

	//--------------------------------------------------------------------------

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );

		// nds4droid compatibility
		setResult( RESULT_CANCELED );
		

		browserFilter = getIntent().getStringArrayExtra(FileDialog.FORMAT_FILTER);
		browserRoot = getIntent().getStringExtra(FileDialog.START_PATH);

		setContentView( R.layout.fragment_list );
		grid = (GridView) findViewById( R.id.list );
		adapter = new RomAdapter();
		grid.setAdapter( adapter );
		grid.setOnItemClickListener( this );
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(adapter != null)
			collection.removeListener(adapter);
	}

	//--------------------------------------------------------------------------

	@Override
	public boolean onCreateOptionsMenu( Menu menu ) {
		getMenuInflater().inflate( R.menu.activity_collection, menu );
		return true;
	}
	
	private void showFileBrowser() {
		Intent i = new Intent(this, FileDialog.class);
		i.setAction(Intent.ACTION_PICK);
		i.putExtra(FileDialog.START_PATH, browserRoot);
		i.putExtra(FileDialog.FORMAT_FILTER, browserFilter);
		startActivityForResult(i, 0);
	}
	
	@Override
	public boolean onMenuItemSelected (int featureId, MenuItem item) {
		switch(item.getItemId()) {
		case R.id.scanner_rescan:
			collection.clear();
			adapter.onScanFinished(collection);
			collection.update( CollectionActivity.this );
			return true;
		case R.id.scanner_filebrowser:
			showFileBrowser();
			return true;
		case R.id.scanner_settings:
			startActivity(new Intent(this, Settings.class));
			break;
		}
		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if( resultCode != Activity.RESULT_OK)
			return;
		String romPath = data.getStringExtra(FileDialog.RESULT_PATH);
		if(romPath != null) {
			getIntent().putExtra(FileDialog.RESULT_PATH, romPath);
			setResult(RESULT_OK, getIntent());
			finish();
		}
			
	}
	
	//--------------------------------------------------------------------------

	@Override
	public void onItemClick( AdapterView<?> parent, View view, int index, long id ) {
		Log.d( "NDS", "onItemClick()" );

		File FILE = collection.getRoms()[index].getFile();
		
		getIntent().putExtra(FileDialog.RESULT_PATH, FILE.getAbsolutePath());
		setResult(RESULT_OK, getIntent());
		finish();
	}
	
	//--------------------------------------------------------------------------
	
	//==========================================================================
	private class RomAdapter extends BaseAdapter implements RomCollection.ScanListener {
		//----------------------------------------------------------------------
		
		public RomAdapter() {
			// TODO Check shared preference for user's desired scanner root
			File file = new File( Environment.getExternalStorageDirectory().getAbsolutePath() );
			if(collection == null)
				collection = new RomCollection( CollectionActivity.this, file );
			collection.addListener( this );
			collection.update( CollectionActivity.this );
		}
		
		//----------------------------------------------------------------------
		
		@Override
		public int getCount() {
			return collection.getRoms().length;
		}
		
		//----------------------------------------------------------------------
		
		@Override
		public Object getItem( int index ) {
			return collection.getRoms()[index];
		}
		
		//----------------------------------------------------------------------
		
		@Override
		public long getItemId( int index ) {
			return collection.getRoms()[index].getGameCode();
		}
		
		//----------------------------------------------------------------------
		
		@Override
		public View getView( final int index, View view, ViewGroup parent ) {
			final NdsRom rom = collection.getRoms()[index];
			
			if ( view == null ) {
				view = getLayoutInflater().inflate( R.layout.griditem_rom, parent, false );
			}
			
			final ImageView image_view = (ImageView) view.findViewById( R.id.icon );
			if ( rom.isIconLoaded() ) {
				image_view.setImageBitmap( rom.getIcon() );
				image_view.getDrawable().setFilterBitmap( false );
				
			// Delegate icon loading to an AsyncTask
			} else {
				image_view.setImageDrawable( null );
				new AsyncTask<Void,Void,Bitmap>() {
					protected Bitmap doInBackground( Void... xx ) {
						return rom.getIcon();
					}
					protected void onPostExecute( Bitmap result ) {
						Drawable[] transition = new Drawable[] { new ColorDrawable( 0 ), new BitmapDrawable( getResources(), result ) };
						transition[1].setFilterBitmap( false );
						TransitionDrawable drawable = new TransitionDrawable( transition );
						image_view.setImageDrawable( drawable );
						drawable.startTransition( 100 );
					}
				}.execute();
			}
			
			( (TextView )view.findViewById( R.id.title )).setText( rom.getTitle() );
			return view;
		}
		
		//----------------------------------------------------------------------
		
		private ProgressDialog pd;
		
		@Override
		public void onScanStarted( RomCollection collection ) {
			pd = new ProgressDialog(CollectionActivity.this);
			pd.setTitle(getString(R.string.ScanningForROMs));
			pd.setIndeterminate(true);
			pd.setCancelable(true);
			pd.show();
		}
		
		//----------------------------------------------------------------------
		
		@Override
		public void onScanFinished( RomCollection collection ) {
			pd.dismiss();
			if(collection.getRoms().length == 0)
				showFileBrowser();
			else
				notifyDataSetChanged();
		}

		//----------------------------------------------------------------------
	}
	//--------------------------------------------------------------------------
}
//------------------------------------------------------------------------------
