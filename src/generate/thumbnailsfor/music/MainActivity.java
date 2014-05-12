package generate.thumbnailsfor.music;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.Menu;
import android.widget.Toast;
import android.provider.MediaStore;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		generateThumbs();
		
		Toast.makeText(getApplicationContext(), "Done!", Toast.LENGTH_SHORT).show();
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void generateThumbs() 
	{
		final String[] STAR = { "*" };        
		final Uri allsongsuri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		final String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
		
		SparseBooleanArray isItFound = new SparseBooleanArray(); 

		final Cursor cursor = getContentResolver().query(allsongsuri, STAR, selection, null, null);

		final File storage = Environment.getExternalStorageDirectory();
		//File albumthumbs = new File(storage.getAbsolutePath() + "/Android/com.android.providers.media/albumthumbs");
		final File log = new File(storage,"log.txt");
		FileOutputStream f = null;
		
		final Uri sArtworkUri = Uri
                .parse("content://media/external/audio/albumart");
		
		MediaMetadataRetriever mmr = new MediaMetadataRetriever();
		
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getRealSize(size);
		int width = size.x;
		int height = size.y;
		int dim = max(width,height);
		
		try {
			f = new FileOutputStream(log);

			if (cursor != null) {
				if (cursor.moveToFirst()) {
					do {
						//final String song_name = cursor
						//		.getString(cursor
						//				.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
						//final int song_id = cursor.getInt(cursor
						//		.getColumnIndex(MediaStore.Audio.Media._ID));

						final String fullpath = cursor.getString(cursor
								.getColumnIndex(MediaStore.Audio.Media.DATA));


						//final String album_name = cursor.getString(cursor
						//		.getColumnIndex(MediaStore.Audio.Media.ALBUM));
						final int album_id = cursor.getInt(cursor
								.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));

						//final String artist_name = cursor.getString(cursor
						//		.getColumnIndex(MediaStore.Audio.Media.ARTIST));
						//final int artist_id = cursor.getInt(cursor
						//		.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID));
						
						if(isItFound.get(album_id) == true){
							try {
								f.write(("SKIP: " + fullpath + " album already done\n#######\n").getBytes());
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							continue;
						}
						
						mmr.setDataSource(fullpath);
						byte[] artwork = mmr.getEmbeddedPicture();
						if(artwork == null) {
							try {
								f.write(("ERROR: " + fullpath + " skipped, no artwork\n#######\n").getBytes());
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
					
							continue;
						}
						
						String artpath = null;
						Uri albumart_uri = ContentUris.withAppendedId(sArtworkUri, album_id);
						Cursor c = getContentResolver().query(albumart_uri, new String [] { MediaStore.MediaColumns.DATA },
			                    null, null, null);
						if(c != null && c.moveToFirst()) {
							artpath = c.getString(0);
						}
						c.close();
						if(artpath == null) {
							artpath = "/storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/" + String.valueOf(System.currentTimeMillis());
							ContentValues values = new ContentValues();
                            values.put("album_id", album_id);
                            values.put("_data", artpath);
                            Uri newuri = getContentResolver().insert(sArtworkUri, values);
                            if(newuri == null) continue;
						}
						
						Bitmap original = BitmapFactory.decodeByteArray(artwork, 0, artwork.length);
						Bitmap resized = Bitmap.createScaledBitmap(original, dim, dim, false);
				
						try {
							FileOutputStream artfile = new FileOutputStream(artpath,false);
							//artfile.write(artwork);
							resized.compress(Bitmap.CompressFormat.JPEG, 90,artfile);
							artfile.close();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						
						
						try {
							f.write((fullpath + " succeded").getBytes());
							
							f.write("\n#######\n".getBytes());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						isItFound.put(album_id,true);

					} while (cursor.moveToNext());

				}
				cursor.close();
			}
		}
		catch(FileNotFoundException e)
		{}
		finally {
			if(f != null)
				try {
					f.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		mmr.release();
	}

	private int max(int width, int height) {
		if(width > height)
			return width;
		else
			return height;
	}
}
