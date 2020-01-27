// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.flexicapture.viewer;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import com.abbyy.mobile.uicomponents.scenario.MultiPageImageCaptureScenario;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.capture.ScenarioPages;

import java.io.IOException;
import java.lang.ref.WeakReference;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * AsyncTask that loads pages from a {@link MultiPageImageCaptureScenario.Result}.
 */
public class LoadPagesAsyncTask extends AsyncTask<Void, ScenarioPages.Page, Exception> {

	private static final String LOG_TAG = "LoadScenarioImages";

	public interface Callback {

		/**
		 * The method is called for every loaded page.
		 */
		@MainThread
		void onNextPage( @NonNull ScenarioPages.Page page );

		/**
		 * The method is called when all pages are loaded, even if no pages are loaded.
		 */
		@MainThread
		void onLoadPagesComplete();

		/**
		 * The method is called when an error has occurred.
		 *
		 * @param error A user-readable error text.
		 */
		@MainThread
		void onLoadPagesError( @NonNull String error );

	}

	private final LoadErrorDescription loadErrorDescription;
	private final ScenarioPages scenarioPages;
	private final WeakReference<Callback> callbackWeakReference;

	public LoadPagesAsyncTask(
		@NonNull Application application,
		@NonNull MultiPageImageCaptureScenario.Result scenarioResult,
		@NonNull WeakReference<Callback> callbackWeakReference
	)
	{
		this.loadErrorDescription = new LoadErrorDescription( application );
		this.scenarioPages = new ScenarioPages( application, scenarioResult, "load" );
		this.callbackWeakReference = callbackWeakReference;
	}

	@Override
	protected Exception doInBackground( Void... voids )
	{
		try {
			scenarioPages.loadPagesBlocking( this::publishProgress );
			return null;
		} catch( IOException exception ) {
			Log.e( LOG_TAG, "Load pages error", exception );
			return exception;
		}
	}

	@Override
	protected void onProgressUpdate( ScenarioPages.Page... values )
	{
		Callback callback = callbackWeakReference.get();
		if( callback != null ) {
			for( ScenarioPages.Page page : values ) {
				callback.onNextPage( page );
			}
		}
	}

	@Override
	protected void onPostExecute( @Nullable Exception exception )
	{
		Callback callback = callbackWeakReference.get();
		if( callback == null ) {
			return;
		}

		if( exception == null ) {
			callback.onLoadPagesComplete();
		} else {
			callback.onLoadPagesError( loadErrorDescription.getLoadDescription() );
		}
	}
}
