// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.multipage.utils;

import android.os.AsyncTask;
import android.support.annotation.UiThread;

import java.lang.ref.WeakReference;

public class BackgroundWorker<Args, T> extends AsyncTask<Args, Void, T> {

	public interface Callback<Args, T> {
		T doWork( Args args ) throws Exception;

		@UiThread
		void onDone( T result );

		@UiThread
		void onError( Exception exception );
	}

	private WeakReference<Callback<Args, T>> callbackRef;
	private volatile Exception exception = null;

	public BackgroundWorker( WeakReference<Callback<Args, T>> callback )
	{
		this.callbackRef = callback;
	}

	@SafeVarargs @Override
	protected final T doInBackground( Args... args )
	{
		Callback<Args, T> callback = callbackRef.get();
		if( callback == null ) { return null; }
		try {
			return callback.doWork( args.length > 0 ? args[0] : null );
		} catch( Exception e ) {
			exception = e;
			return null;
		}
	}

	@Override
	protected void onPostExecute( T result )
	{
		Callback<Args, T> callback = callbackRef.get();
		if( callback == null ) { return; }
		if (exception != null) {
			callback.onError( exception );
		} else {
			callback.onDone( result );
		}
	}
}
