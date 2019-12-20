package com.abbyy.mobile.sample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.abbyy.mobile.rtr.Engine;
import com.abbyy.mobile.rtr.IRecognitionCoreAPI;
import com.abbyy.mobile.rtr.IDataCaptureCoreAPI;
import com.abbyy.mobile.rtr.Language;

import java.lang.ref.WeakReference;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

	// Licensing
	private static final String licenseFileName = "license";

	private static final int OPEN_FILE_REQUEST_CODE = 42;
	private static final int READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 43;

	private TextView recognizedTextView; // TextView with recognition results and messages
	private Engine engine;    // The 'Abbyy RTR SDK Engine' to be used in this sample application
	private CoreAPITask activeRecognitionTask; // Latest recognition task. Can be completed

	// A subset of available languages shown in the UI. See all available languages in Language enum.
	// To show all languages in the UI you can substitute the list below with:
	// Language[] languages = Language.values();
	private Language[] languages = {
		Language.ChineseSimplified,
		Language.ChineseTraditional,
		Language.English,
		Language.French,
		Language.German,
		Language.Italian,
		Language.Japanese,
		Language.Korean,
		Language.Polish,
		Language.PortugueseBrazilian,
		Language.Russian,
		Language.Spanish,
	};

	// Type of image content
	enum ContentType {
		Text,
		BusinessCard
	}

	// Current recognition language
	private Language recognitionLanguage = Language.English;
	// Current type of image content
	private ContentType contentType = ContentType.Text;

	// Open image button click listener
	private View.OnClickListener openFileButtonClickListener = new View.OnClickListener() {
		@Override public void onClick( View v )
		{
			// If no permission to read file, first ask for it
			if( ContextCompat.checkSelfPermission( MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE )
				!= PackageManager.PERMISSION_GRANTED ) {
				ActivityCompat.requestPermissions( MainActivity.this, new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
					READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE );
				return;
			}

			openFilePickDialog();
		}
	};

	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_main );
		recognizedTextView = findViewById( R.id.recognized_text );
		recognizedTextView.setMovementMethod( new ScrollingMovementMethod() );

		ArrayAdapter<String> adapter = new ArrayAdapter<String>( this, android.R.layout.simple_spinner_item );
		adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );

		// Initialize language spinner
		final Spinner languagesSpinner = (Spinner) findViewById( R.id.languages );
		for( Language language : languages ) {
			String name = language.name();
			adapter.add( name );
		}
		languagesSpinner.setAdapter( adapter );
		languagesSpinner.setSelection( 2, true );

		// The callback to be called when a language is selected
		languagesSpinner.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected( AdapterView<?> parent, View view, int position, long id )
			{
				if( recognitionLanguage != languages[position] ) {
					recognitionLanguage = languages[position];
				}
			}

			@Override
			public void onNothingSelected( AdapterView<?> parent )
			{
			}
		} );

		// Initialize content type spinner
		final Spinner contentTypesSpinner = (Spinner) findViewById( R.id.contentTypes );
		adapter = new ArrayAdapter<String>( this, android.R.layout.simple_spinner_item );
		adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
		for( ContentType type : ContentType.values() ) {
			adapter.add( type.toString() );
		}
		contentTypesSpinner.setAdapter( adapter );
		contentTypesSpinner.setSelection( 0, true );

		// The callback to be called when a language is selected
		contentTypesSpinner.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected( AdapterView<?> parent, View view, int position, long id )
			{
				contentType = ContentType.values()[position];
			}

			@Override
			public void onNothingSelected( AdapterView<?> parent )
			{
			}
		} );

		try {
			engine = Engine.load( this, licenseFileName );
		} catch( Exception ex ) {
			Log.e( getString( R.string.app_name ), "Error loading engine", ex );
			recognizedTextView.setText( String.format( getString( R.string.error_loading_engine ), ex.getMessage() ) );
		}

		Button recognizeImageButton = findViewById( R.id.open_image_button );
		recognizeImageButton.setOnClickListener( openFileButtonClickListener );
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		// Stop active recognition task in case of no results handler
		cancelActiveRecognitionTask();
		activeRecognitionTask = null;
	}

	@Override
	public void onActivityResult( int requestCode, int resultCode, final Intent data )
	{
		if( resultCode == RESULT_OK && requestCode == OPEN_FILE_REQUEST_CODE ) {
			cancelActiveRecognitionTask();
			try {
				final Bitmap image = getPicture( data.getData() );
				switch( contentType ) {
					case Text:
						activeRecognitionTask = new TextRecognitionTask( engine, image, recognitionLanguage, this );
						break;
					case BusinessCard:
						activeRecognitionTask = new DataCaptureTask( engine, image, recognitionLanguage, this, "BusinessCards" );
						break;
				}
				activeRecognitionTask.execute();
			} catch( Exception e ) {
				Log.e( getString( R.string.app_name ), "Error: " + e.getMessage(), e );
			}
		}
	}

	@Override
	public void onRequestPermissionsResult( int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults )
	{
		if( requestCode == READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE ) {
			if( grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
				openFilePickDialog();
			} else {
				recognizedTextView.setText( R.string.no_permission_text );
			}
		}
	}

	private void cancelActiveRecognitionTask()
	{
		if( activeRecognitionTask != null ) {
			activeRecognitionTask.cancel( false );
		}
	}

	// Loads image
	private Bitmap getPicture( Uri selectedImage ) throws IOException
	{
		return MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
	}

	// Opens file pick dialog
	private void openFilePickDialog()
	{
		Intent intent = new Intent( Intent.ACTION_PICK );
		intent.setType( "image/*" );
		startActivityForResult( intent, OPEN_FILE_REQUEST_CODE );
	}

	// Async task for recognition or data extraction process
	// The recognition (data extraction) is not fast process that's why it have to be background
	private abstract static class CoreAPITask extends AsyncTask<Void, String, String> {
		protected Engine engine;
		protected Bitmap image;
		private WeakReference<MainActivity> activity;
		protected Language recognitionLanguage;

		CoreAPITask( Engine engine, Bitmap image, Language recognitionLanguage, MainActivity activity )
		{
			this.engine = engine;
			this.image = image;
			this.activity = new WeakReference<>( activity );
			this.recognitionLanguage = recognitionLanguage;
		}

		@Override
		protected void onProgressUpdate( String... values )
		{
			MainActivity mainActivity = activity.get();
			if( mainActivity != null ) {
				mainActivity.recognizedTextView.setText( values[0] );
			}
		}

		@Override
		protected void onPostExecute( String text )
		{
			MainActivity mainActivity = activity.get();
			if( mainActivity != null ) {
				if( text != null ) {
					mainActivity.recognizedTextView.setText( text );
				}
			}
		}

	}

	private class TextRecognitionTask extends CoreAPITask {

		// Callback for handling recognition-time events
		private IRecognitionCoreAPI.TextRecognitionCallback callback = new IRecognitionCoreAPI.TextRecognitionCallback() {
			@Override
			public boolean onProgress( int recognitionPercent, IRecognitionCoreAPI.Warning warning )
			{
				if( !isCancelled() ) {
					String progress = String.format( "Recognition progress %d%%.", recognitionPercent );

					if( warning != null ) {
						// It is useful to handle warnings instead of displaying it
						progress += " Warning: " + warning.name();
					}

					publishProgress( progress );
				}

				// Return true for interrupting recognition, false otherwise
				return isCancelled();
			}

			@Override
			public void onTextOrientationDetected( int orientation )
			{
				// Here you can handle information about the text orientation
				// E.g. you can rotate image in UI
			}

			@Override
			public void onError( Exception e )
			{
				// Recognition process errors handling
				Log.e( getString( R.string.app_name ), "Recognition error: " + e.getMessage(), e );
				publishProgress( "Recognition error: " + e.getMessage() );
			}
		};

		TextRecognitionTask( Engine engine, Bitmap image, Language recognitionLanguage, MainActivity activity )
		{
			super( engine, image, recognitionLanguage, activity );
		}

		@Override
		protected String doInBackground( Void... params )
		{
			// The 'Abbyy RTR SDK Core API' object to be used in this sample application
			IRecognitionCoreAPI recognitionCoreAPI = engine.createRecognitionCoreAPI();

			// Here you can configure the recognition languages and the area of interest
			recognitionCoreAPI.getTextRecognitionSettings().setRecognitionLanguage( recognitionLanguage );
			IRecognitionCoreAPI.TextBlock[] blocks = recognitionCoreAPI.recognizeText( image, callback );
			// Combing lines to string is complex process.
			// You may write another realisation
			StringBuilder resultText = new StringBuilder();
			for( int i = 0; i < blocks.length; i++ ) {
				for( int j = 0; j < blocks[i].TextLines.length; j++ ) {
					if( j > 0 ) {
						resultText.append( ' ' );
					}
					resultText.append( blocks[i].TextLines[j].Text );
				}
				resultText.append( System.lineSeparator() );
			}
			return resultText.toString();
		}
	}

	private class DataCaptureTask extends CoreAPITask {

		private String profile;

		// Callback for handling data extraction-time events (same as in recognition task)
		private IDataCaptureCoreAPI.Callback callback = new IDataCaptureCoreAPI.Callback() {
			@Override
			public boolean onProgress( int recognitionPercent, IDataCaptureCoreAPI.Warning warning )
			{
				if( !isCancelled() ) {
					String progress = String.format( "Recognition progress %d%%.", recognitionPercent );

					if( warning != null ) {
						// It is useful to handle warnings instead of displaying it
						progress += " Warning: " + warning.name();
					}

					publishProgress( progress );
				}

				// Return true for interrupting recognition, false otherwise
				return isCancelled();
			}

			@Override
			public void onTextOrientationDetected( int orientation )
			{
				// Here you can handle information about the text orientation
				// E.g. you can rotate image in UI
			}

			@Override
			public void onError( Exception e )
			{
				// Recognition process errors handling
				Log.e( getString( R.string.app_name ), "Recognition error: " + e.getMessage(), e );
				publishProgress( "Recognition error: " + e.getMessage() );
			}
		};

		DataCaptureTask( Engine engine, Bitmap image, Language recognitionLanguage, MainActivity activity, String profile )
		{
			super( engine, image, recognitionLanguage, activity );
			this.profile = profile;
		}

		@Override
		protected String doInBackground( Void... params )
		{
			// The 'Abbyy RTR SDK Core API' object to be used in this sample application
			IDataCaptureCoreAPI dataCaptureCoreAPI = engine.createDataCaptureCoreAPI();
			// Set recognition language
			dataCaptureCoreAPI.getDataCaptureSettings().setRecognitionLanguage( recognitionLanguage );
			// Set profile (for example "BusinessCards")
			dataCaptureCoreAPI.getDataCaptureSettings().setProfile( "BusinessCards" );
			// Do it!
			IDataCaptureCoreAPI.DataField[] dataFields = dataCaptureCoreAPI.extractDataFromImage( image, callback );
			// Write result to string
			StringBuilder resultText = new StringBuilder();
			for( IDataCaptureCoreAPI.DataField dataField : dataFields ) {
				resultText.append( dataField.Name ).append( ": " ).append( dataField.Text );
				resultText.append( System.lineSeparator() );
				for( IDataCaptureCoreAPI.DataField component : dataField.Components ) {
					if( !Objects.equals( component.Name, "" ) ) {
						resultText.append( "\t\t" ).append( component.Name ).append( ": " ).append( component.Text );
						resultText.append( System.lineSeparator() );
					}
				}
			}
			return resultText.toString();
		}
	}
}
