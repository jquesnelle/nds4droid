package com.opendoorstudios.ds4droid.NDSScanner;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

//==============================================================================
public class NdsRom {
	// Data from http://nocash.emubase.de/gbatek.htm
	//--------------------------------------------------------------------------
	
	// title, gamecode, makercode are tightly packed; no offset needed until info
	private static final int INFO_OFFSET = 0x56, INFO_ADDRESS = 0x68, ICON_OFFSET = 0x20;
	
	protected final byte[] TITLE_BYTES = new byte[12],
	                    GAMECODE_BYTES = new byte[ 4],
	                   MAKERCODE_BYTES = new byte[ 2],
	                        INFO_BYTES = new byte[ 4],
	                        
	                   // Below only used if INFO_BYTES > 0
	                        ICON_BYTES = new byte[512],
	                     PALETTE_BYTES = new byte[ 32],
	                    TITLE_JP_BYTES = new byte[256], // Japanese
	                    TITLE_EN_BYTES = new byte[256], // English
	                    TITLE_FR_BYTES = new byte[256], // French
	                    TITLE_DE_BYTES = new byte[256], // German
	    	            TITLE_IT_BYTES = new byte[256], // Italian
	                    TITLE_ES_BYTES = new byte[256]; // Spanish
	
	private long   game_code;
	private Bitmap icon;
	private String title_jp, title_en, title_fr, title_de, title_it, title_es;

	static final String ROM_PATTERN = "^.*\\.(NDS|nds)$",
	                    ZIP_PATTERN = "^.*\\.(ZIP|zip)$",
	                    RAR_PATTERN = "^.*\\.(RAR|rar)$",
						// We need to add 7-zip support ASAP.
						SEVENZ_PATTERN = "^.*\\.(7Z|7z)$";
	
	private final File FILE;
	
	//--------------------------------------------------------------------------

	public static NdsRom MakeRom(File f) {
		final InputStream stream = NdsRom.getRomStream(f);
		if(f == null)
			return null;
		return new NdsRom(f,stream);
	}
	
	private NdsRom(File file, InputStream stream ) {
		FILE = file;
		try {
			stream.read( TITLE_BYTES );
			stream.read( GAMECODE_BYTES );
			stream.read( MAKERCODE_BYTES );
			stream.skip( INFO_OFFSET );
			stream.read( INFO_BYTES );

			// Load info iff a valid address was provided in INFO_BYTES
			int info_skip = composeInt( INFO_BYTES ) - INFO_ADDRESS - INFO_BYTES.length;

			if ( info_skip > 0 ) {

				longSkip( stream, info_skip );

				stream.skip( ICON_OFFSET ); // Some junk data before the icon

				// Also iterate to read large sections
				int read_index = 0;
				while ( read_index < ICON_BYTES.length ) {
					read_index += stream.read( ICON_BYTES, read_index, ICON_BYTES.length - read_index );
				}

				stream.read( PALETTE_BYTES );
				stream.read( TITLE_JP_BYTES );
				stream.read( TITLE_EN_BYTES );
				stream.read( TITLE_FR_BYTES );
				stream.read( TITLE_DE_BYTES );
				stream.read( TITLE_IT_BYTES );
				stream.read( TITLE_ES_BYTES );
			}

		} catch ( IOException e ) {
			e.printStackTrace();
		}
		finally {
			try {
				stream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	//--------------------------------------------------------------------------

	// Iterate to skip large sections, in case we're reading an archive
	private void longSkip( InputStream stream, int skip ) throws IOException {
		long skipped_total = 0;
		while ( skipped_total < skip ) {
			skipped_total += stream.skip( skip - skipped_total );
		}
	}

	//--------------------------------------------------------------------------

	private static int composeInt( byte[] bytes ) {
		return ByteBuffer.wrap( bytes ).order( ByteOrder.LITTLE_ENDIAN ).getInt();
	}

	//--------------------------------------------------------------------------
	
	public long getGameCode() {
		if ( game_code == 0 ) game_code = composeInt( GAMECODE_BYTES );
		return game_code;
	}
	
	//--------------------------------------------------------------------------
	
	public Bitmap getIcon() {
		if ( icon == null && composeInt( INFO_BYTES ) > 0 ) {
			icon = Bitmap.createBitmap( 32, 32, Bitmap.Config.ARGB_8888 );
			Canvas canvas = new Canvas( icon );
			Paint paint = new Paint();
			
			int x, y, index_a, index_b, color_a, color_b;
			ByteBuffer palette = ByteBuffer.wrap( PALETTE_BYTES ).order( ByteOrder.LITTLE_ENDIAN );
			
			for ( int i = 0; i < 512; i ++ ) {
				index_a = ( ICON_BYTES[i]       ) & 15;
				index_b = ( ICON_BYTES[i] >>> 4 ) & 15;
				
				color_a = palette.getShort( index_a * 2 );
				color_b = palette.getShort( index_b * 2 );
				
				// Oh fuck me
				x = ( 2 * (  i         % 4 )) 
				  + ( 8 * (( i /  32 ) % 4 ));
				y = (     (  i /   4 ) % 8  )
				  + ( 8 * (  i / 128 )      );
				
				// X b b b b b g g|g g g r r r r r
				paint.setARGB( index_a != 0    ? 255 : 0,
					         ( color_a <<  3 ) & 255, 
					         ( color_a >>> 2 ) & 255,
					         ( color_a >>> 7 ) & 255 );
				canvas.drawPoint( x, y, paint );
				paint.setARGB( index_b != 0    ? 255 : 0,
						     ( color_b <<  3 ) & 255, 
						     ( color_b >>> 2 ) & 255,
						     ( color_b >>> 7 ) & 255 );
				canvas.drawPoint( x + 1, y, paint );
			}
		}
		return icon;
	}
	
	//--------------------------------------------------------------------------
	
	public boolean isIconLoaded() {
		return icon != null;
	}
	
	//--------------------------------------------------------------------------
	
	public String getTitle() {
		return getTitle( null );
	}
	
	//--------------------------------------------------------------------------
	
	public String getTitle( String lang ) {
		try {
			if ( Locale.JAPANESE.getLanguage().equals( lang )) {
				if ( title_jp == null ) title_jp = new String( TITLE_JP_BYTES, "UTF-8" );
				return title_jp;
				
			} else if ( Locale.FRENCH.getLanguage().equals( lang )) {
				if ( title_fr == null ) title_fr = new String( TITLE_FR_BYTES, "UTF-8" );
				return title_fr;
				
			} else if ( Locale.GERMAN.getLanguage().equals( lang )) {
				if ( title_de == null ) title_de = new String( TITLE_DE_BYTES, "UTF-8" );
				return title_de;
				
			} else if ( Locale.ITALIAN.getLanguage().equals( lang )) {
				if ( title_it == null ) title_it = new String( TITLE_IT_BYTES, "UTF-8" );
				return title_it;
				
			} else if ( "es".equals( lang )) { // Spanish not in Android?
				if ( title_es == null ) title_es = new String( TITLE_ES_BYTES, "UTF-8" );
				return title_es;
				
			} else {
				if ( title_en == null ) title_en = new String( TITLE_EN_BYTES, "UTF-8" );
				return title_en;
			}
			
		} catch ( UnsupportedEncodingException e ) {
			return null;
		}
	}
	
	//--------------------------------------------------------------------------
	
	public File getFile() { return FILE; }
	
	//--------------------------------------------------------------------------

	static boolean isRomArchive( ZipFile file ) {
		for ( ZipEntry entry : Collections.list( file.entries() )) {
			if ( entry.getName().matches( ROM_PATTERN )) return true;
		}
		return false;
	}
	
	//--------------------------------------------------------------------------
	/*
	private static boolean isRomArchive( RARFile file ) {
		for ( RARArchivedFile entry : file.getArchivedFiles() ) {
			if ( entry.getName().matches( ROM_PATTERN )) return true;
		}
		return false;
	}
	*/
	//--------------------------------------------------------------------------
	
	private static InputStream getRomStream( File file ) {
		if ( file.getName().matches( ROM_PATTERN )) {
			try { return new FileInputStream( file ); }
			catch ( FileNotFoundException e ) { return null; }
		}
		
		if ( file.getName().matches( ZIP_PATTERN )) {
			try { return getRomStream( new ZipFile( file )); }
			catch ( IOException e ) { return null; }
		}
		/*
		if ( file.getName().matches( RAR_PATTERN )) {
			Log.d( "NDS", "\nLoading RAR file " + file.getName() );
			try { return getRomStream( new RARFile( file )); }
			catch ( IOException e ) { return null; }
		}
		*/
		return null;
	}
	
	//--------------------------------------------------------------------------
	
	private static InputStream getRomStream( ZipFile file ) {
		for ( ZipEntry entry : Collections.list( file.entries() )){
			if ( entry.getName().matches( ROM_PATTERN )) {
				try { return file.getInputStream( entry ); }
				catch ( IOException e ) { return null; }
			}
		}
		return null;
	}
	
	//--------------------------------------------------------------------------
	/*
	private static InputStream getRomStream( RARFile file ) {
		for ( RARArchivedFile entry : file.getArchivedFiles() ) {
			if ( entry.getName().matches( ROM_PATTERN )) {
				return file.extract( entry );
			}
		}
		return null;
	}
	*/
	//--------------------------------------------------------------------------
}
//------------------------------------------------------------------------------