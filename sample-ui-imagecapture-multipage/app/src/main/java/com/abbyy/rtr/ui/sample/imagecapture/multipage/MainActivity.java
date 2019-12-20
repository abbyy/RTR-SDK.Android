// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.multipage;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.abbyy.mobile.rtr.Engine;
import com.abbyy.mobile.uicomponents.scenario.MultiPageImageCaptureScenario;
import com.abbyy.rtr.ui.sample.imagecapture.multipage.utils.BackgroundWorker;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Main activity of the app displays already captured pages.
 */
public class MainActivity extends AppCompatActivity {
	// This flag is used to request pages from CaptureActivity
	private static final int PAGES_REQUEST_CODE = 0;
	private static final String TAG = MainActivity.class.getName();

	// Adapter for pages' recycler view
	private PagesAdapter pagesAdapter;

	private ImageView emptyDocumentView;
	private Button captureAddPages;
	private TabLayout scenarioTabs;
	private MultiPageImageCaptureScenario.Result result;

	private int tabIndex;

	// Working with the result on a background thread
	private BackgroundWorker.Callback<Void, List<String>> pagesListCallback = new BackgroundWorker.Callback<Void, List<String>>() {
		private boolean isResultEmpty = false;

		@Override
		public List<String> doWork( Void none ) throws Exception
		{
			List<String> pageIds = result.getPages();
			isResultEmpty = pageIds.isEmpty();
			return pageIds;
		}

		@UiThread
		@Override
		public void onDone( List<String> pages )
		{
			if( pages == null ) {
				return;
			}
			pagesAdapter.updatePages( pages );
			if( isResultEmpty ) {
				emptyDocumentView.setVisibility( View.VISIBLE );
			} else {
				emptyDocumentView.setVisibility( View.GONE );
			}
			if( !isResultEmpty ) {
				captureAddPages.setVisibility( View.VISIBLE );
			} else {
				captureAddPages.setVisibility( View.GONE );
			}
		}

		@Override
		public void onError( Exception exception )
		{
			Toast.makeText( MainActivity.this, R.string.unknown_error, Toast.LENGTH_LONG ).show();
		}
	};

	private BackgroundWorker.Callback<String, Void> deletePageCallback = new BackgroundWorker.Callback<String, Void>() {
		@Override
		public Void doWork( String pageId ) throws Exception
		{
			result.delete( pageId );
			return null;
		}

		@UiThread
		@Override
		public void onDone( Void none )
		{
			loadPages();
		}

		@Override
		public void onError( Exception exception )
		{
			Toast.makeText( MainActivity.this, R.string.unknown_error, Toast.LENGTH_LONG ).show();
		}
	};

	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_main );

		Engine engine = EngineFactory.createEngine( this );
		if( engine == null ) {
			finish();
			return;
		}

		emptyDocumentView = findViewById( R.id.emptyDocument );
		TextView buildNumberView = findViewById( R.id.buildNumber );
		// retrieve build number of UI Components SDK
		buildNumberView.setText( getString( R.string.build_number, com.abbyy.mobile.uicomponents.BuildConfig.VERSION_NAME ) );

		Button captureNewPages = findViewById( R.id.captureNew );
		captureNewPages.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick( View v )
			{
				startNewCapture();
			}
		} );
		captureAddPages = findViewById( R.id.captureAdd );
		captureAddPages.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick( View v )
			{
				startCapture( null );
			}
		} );
		initPagesView();

		initScenarios();
	}

	private void initScenarios()
	{
		scenarioTabs = findViewById( R.id.scenarioTabs );
		for( CaptureProfile documentProfile : CaptureProfile.values() ) {
			scenarioTabs.addTab( scenarioTabs.newTab().setText( documentProfile.displayText ) );
		}
		scenarioTabs.addOnTabSelectedListener( new TabLayout.OnTabSelectedListener() {
			@Override
			public void onTabSelected( TabLayout.Tab tab )
			{
				changeScenario( tab.getPosition() );
			}

			@Override
			public void onTabUnselected( TabLayout.Tab tab )
			{
			}

			@Override
			public void onTabReselected( TabLayout.Tab tab )
			{
			}
		} );
		scenarioTabs.getTabAt( 0 ).select();
		changeScenario( 0 );
	}

	private void changeScenario( int tabIndex )
	{
		this.tabIndex = tabIndex;
		result = ScenarioFactory.create(
			this,
			EngineFactory.getEngine(),
			CaptureProfile.values()[tabIndex],
			null
		).getResult();
		pagesAdapter.setPageResult( result );
		loadPages();
	}

	private void initPagesView()
	{
		RecyclerView pagesView = findViewById( R.id.pages );
		pagesView.setHasFixedSize( true );

		RecyclerView.LayoutManager layoutManager = new GridLayoutManager( this,
			getResources().getInteger( R.integer.columns_count ) );
		pagesView.setLayoutManager( layoutManager );

		pagesAdapter = new PagesAdapter( new PagesAdapter.PageListener() {
			@Override
			public void openPage( String pageId )
			{
				startCapture( pageId );
			}

			@Override
			public void deletePage( String pageId )
			{
				new BackgroundWorker<>( new WeakReference<>( deletePageCallback ) ).execute( pageId );
			}
		} );
		pagesView.setAdapter( pagesAdapter );
	}

	private void startNewCapture()
	{
		AsyncTask.execute( new Runnable() {
			@Override
			public void run()
			{
				try {
					result.clear();
				} catch( Exception e ) {
					Log.e( TAG, "Failed to clear pages", e );
				}
			}
		} );
		pagesAdapter.releasePages();
		emptyDocumentView.setVisibility( View.VISIBLE );
		captureAddPages.setVisibility( View.GONE );
		startCapture( null );
	}

	private void startCapture( String pageId )
	{
		Intent intent = CaptureActivity.getIntent( this, CaptureProfile.values()[tabIndex], pageId );
		startActivityForResult( intent, PAGES_REQUEST_CODE );
	}

	@Override
	protected void onActivityResult( int requestCode, int resultCode, @Nullable Intent data )
	{
		if( requestCode == PAGES_REQUEST_CODE ) {
			loadPages();
		}
	}

	@Override
	protected void onDestroy()
	{
		pagesListCallback = null;
		deletePageCallback = null;
		super.onDestroy();
	}

	private void loadPages()
	{
		// Loading pages in background
		new BackgroundWorker<>( new WeakReference<>( pagesListCallback ) ).execute();
	}
}
