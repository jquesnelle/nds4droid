package com.opendoorstudios.ds4droid.NDSScanner;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.zip.ZipFile;

//==============================================================================
public class RomCollection {
	// NOTE: Rar archives weren't working for some reason. Probably need to plug in a new library.
	//--------------------------------------------------------------------------
	
	private static final FileFilter
		
		DIR_FILTER = new FileFilter() { @Override public boolean accept( File file ) {
			return file.isDirectory();
		}},

	NDS_FILTER = new FileFilter() {
		@Override
		public boolean accept( File file ) {
			return !file.isDirectory() && file.getName().matches( NdsRom.ROM_PATTERN );
		}
	},

	ZIP_FILTER = new FileFilter() {
		@Override
		public boolean accept( File file ) {
			if ( file.getName().matches( NdsRom.ZIP_PATTERN ) ) {
				try {
					return NdsRom.isRomArchive( new ZipFile( file ) );
				} catch ( IOException e ) { return false; }
			}
			return false;
		}
	},
	/*
	RAR_FILTER = new FileFilter() { @Override public boolean accept( File file ) {
		if ( file.getName().matches( "^.*\\.(RAR|rar)$" )) {
			try { return isRomArchive( new RARFile( file )); }
			catch ( IOException e ) { return false; }
		}
		return false;
	}},
	*/
	ALL_ROMS_FILTER = new FileFilter() {
		@Override
		public boolean accept( File file ) {
			return NDS_FILTER.accept( file )
					|| ZIP_FILTER.accept( file )
			    /*|| RAR_FILTER.accept( file )*/;
		}
	};

	private SQLiteDatabase DB;
	private final File           ROOT;

	private final Set<ScanListener> LISTENERS = new HashSet<ScanListener>();

	private NdsRom[] roms;

	//--------------------------------------------------------------------------

	public RomCollection( Context context, File root ) {
		try {
			DB = new RomDatabaseHelper( context ).getWritableDatabase();
		}
		catch(Exception e) {
			//for some reason, this is failing sometimes with a "SQLiteDatabaseLockedException".
			//I can't get this to happen on my device, but I see on the Google Play ANR reports.
			//I think it may have something to do with a previous instance of the activity not having
			//closed the connection yet. I made the actual collection instance static, but just as
			//an extra fallback we'll wait a bit here and try to reopen the DB
			try {
				Thread.sleep(500);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			DB = new RomDatabaseHelper( context ).getWritableDatabase();
		}
		ROOT = root;
	}

	//--------------------------------------------------------------------------

	public void update( final Context context ) {
		new AsyncTask<Void,Void,Void>() {
			
			@Override
			public void onPreExecute() {
				for ( ScanListener listener : LISTENERS ) {
					listener.onScanStarted( RomCollection.this );
				}
			}
			
			@Override
			public Void doInBackground( Void... x ) {
				SharedPreferences prefs = context.getSharedPreferences( "ndsscanner", Context.MODE_PRIVATE );
		
				// Delete first, ask questions later
				Cursor cursor = DB.query( "roms", new String[]{ "path" }, null, null, null, null, null );
				DB.beginTransaction();
				while ( cursor.moveToNext() ) {
					if ( !new File( cursor.getString( 0 ) ).exists() ) {
						DB.delete( "roms", "path=?", new String[]{ cursor.getString( 0 ) } );
					}
				}
				DB.setTransactionSuccessful();
				DB.endTransaction();
				cursor.close();
		
				long    last_scan = prefs.getLong(    "last_scan", 0 );
				boolean recursive = prefs.getBoolean( "recursive", true );
		
				DB.beginTransaction();
				scanDirectory( ROOT, last_scan, recursive, 0 );
				DB.setTransactionSuccessful();
				DB.endTransaction();
				prefs.edit().putLong( "last_scan", System.currentTimeMillis() ).commit();
				return null;
			}
			
			@Override
			public void onPostExecute( Void x ) {
				roms = null;
				for ( ScanListener listener : LISTENERS ) {
					listener.onScanFinished( RomCollection.this );
				}
			}
			
		}.execute();
	}
	
	public void clear() {
		DB.beginTransaction();
		DB.delete( "roms", null, null );
		DB.setTransactionSuccessful();
		DB.endTransaction();
	}
	
	//--------------------------------------------------------------------------
	
	private void scanDirectory( File file, long last_scan, boolean recursive, int recursionLevel ) {
		
		ContentValues vals = new ContentValues();
		NdsRom rom;
		InputStream stream;
		
		if(recursionLevel > 5)
			return; //if the ROMs are more than 5 layers deep... well... they'll just have to use the file browser
		
		try
		{
			// Insert DB entries for all matching rom files
			for ( File f : file.listFiles( ALL_ROMS_FILTER )) {
				vals.clear();
				
				rom = NdsRom.MakeRom(f);
				if(f == null)
					continue;
				
				vals.put( "path",     f.getAbsolutePath() );
				vals.put( "title",    rom.getTitle()      );
				vals.put( "gamecode", rom.getGameCode()   );
				
				DB.insertWithOnConflict( "roms", null, vals, SQLiteDatabase.CONFLICT_IGNORE );
			}
			
			// Subdirectories
			if ( recursive ) for ( File dir : file.listFiles( DIR_FILTER )) {
				if ( dir.lastModified() > last_scan ) scanDirectory( dir, last_scan, recursive, recursionLevel + 1 );
			}
		}
		catch(Exception e)
		{
			//on my x86 emulator I'm not allowed to read /mnt/sdcard/.android_secure
			//if we get this, just skip the directory
			Log.d("nds4droid", "Unable to scan " + file.getAbsolutePath());
		}
		
	}

	//--------------------------------------------------------------------------
	
	public void addListener( ScanListener listener ) {
		LISTENERS.add( listener );
	}
	
	//--------------------------------------------------------------------------
	
	public void removeListener( ScanListener listener ) {
		LISTENERS.remove( listener );
	}
	
	//--------------------------------------------------------------------------
	
	public void clearListeners() {
		LISTENERS.clear();
	}
	
	//--------------------------------------------------------------------------
	
	public NdsRom[] getRoms() {
		if ( roms == null ) {
			final LinkedList<NdsRom> list = new LinkedList<NdsRom>(); 
			Cursor c = DB.query( "roms", new String[]{ "path" },
			                     null, null, null, null, "title" );
			while ( c.moveToNext() ) {
				final NdsRom rom = NdsRom.MakeRom(new File( c.getString( 0 )));
				if(rom == null)
					continue;
				list.add(rom);
			}
			roms = new NdsRom[list.size()];
			list.toArray(roms);
			c.close();
		}
		return roms;
	}
	
	//--------------------------------------------------------------------------
	
	//==========================================================================
	public interface ScanListener {
		//----------------------------------------------------------------------
		
		public void onScanStarted(  RomCollection collection );
		public void onScanFinished( RomCollection collection );
		
		//----------------------------------------------------------------------
	}
	//--------------------------------------------------------------------------
}
//------------------------------------------------------------------------------
