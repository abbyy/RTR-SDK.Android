// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.flexicapture.viewer;

import android.app.Application;
import android.os.AsyncTask;

import com.abbyy.mobile.uicomponents.scenario.MultiPageImageCaptureScenario;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.send.SendPreferences;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * AsyncTask that deletes a document from {@link MultiPageImageCaptureScenario.Result}.
 */
public class DeletePagesAsyncTask extends AsyncTask<Void, Void, Exception> {

	public interface Callback {

		void onPagesDeleted( int requestCode );

		void onPagesDeleteError( @NonNull Exception exception );

	}

	private final MultiPageImageCaptureScenario.Result scenarioResult;
	private final SendPreferences sendPreferences;
	private final WeakReference<Callback> callbackWeakReference;
	private final int requestCode;

	public DeletePagesAsyncTask(
		@NonNull Application application,
		@NonNull MultiPageImageCaptureScenario.Result scenarioResult,
		@NonNull WeakReference<Callback> callbackWeakReference,
		int requestCode
	)
	{
		this.scenarioResult = scenarioResult;
		this.sendPreferences = new SendPreferences( application );
		this.callbackWeakReference = callbackWeakReference;
		this.requestCode = requestCode;
	}

	@Override
	protected Exception doInBackground( Void... voids )
	{
		try {
			sendPreferences.setDocumentSent( false );
			scenarioResult.clear();
			return null;
		} catch( Exception exception ) {
			return exception;
		}
	}

	@Override
	protected void onPostExecute( @Nullable Exception exception )
	{
		Callback callback = callbackWeakReference.get();
		if( callback != null ) {
			if( exception == null ) {
				callback.onPagesDeleted( requestCode );
			} else {
				callback.onPagesDeleteError( exception );
			}
		}
	}
}
