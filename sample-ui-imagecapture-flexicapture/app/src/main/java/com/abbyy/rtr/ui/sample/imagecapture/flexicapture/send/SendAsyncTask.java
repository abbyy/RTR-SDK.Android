// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.flexicapture.send;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import com.abbyy.mobile.uicomponents.scenario.MultiPageImageCaptureScenario;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.api.Api;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.api.ApiFactory;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.net.NetworkConnectionCompat;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.sign_in.SignInCredentials;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.sign_in.SignInCredentialsPreferences;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import kotlin.jvm.Volatile;
import okhttp3.Credentials;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * AsyncTask that sends document to FlexiCapture server.
 */
public class SendAsyncTask extends AsyncTask<Void, Void, String> {

	private static final String LOG_TAG = "SendAsyncTask";

	public interface Callback {

		@MainThread
		void onSendSuccess();

		@MainThread
		void onSendTokenExpiredError();

		@MainThread
		void onSendError( @NonNull String error );

	}

	private final SendErrorDescription sendErrorDescription;
	private final SendBody sendBody;
	private final WeakReference<Callback> callbackWeakReference;
	private final SignInCredentialsPreferences signInCredentialsPreferences;
	private final NetworkConnectionCompat networkConnectionCompat;
	private final SendPreferences sendPreferences;
	@Nullable
	private final String password;

	@Volatile
	private boolean isTokenExpired;

	/**
	 * @param password If password is null, auth token will be used
	 * otherwise password will be used and auth token stored.
	 */
	public SendAsyncTask(
		@NonNull Application application,
		@NonNull MultiPageImageCaptureScenario.Result scenarioResult,
		@NonNull WeakReference<Callback> callbackWeakReference,
		@Nullable String password
	)
	{
		this.sendErrorDescription = new SendErrorDescription( application );
		this.sendBody = new SendBody( application, scenarioResult, new SendPreferences( application ) );
		this.callbackWeakReference = callbackWeakReference;
		this.signInCredentialsPreferences = new SignInCredentialsPreferences( application );
		this.networkConnectionCompat = new NetworkConnectionCompat( application );
		this.sendPreferences = new SendPreferences( application );
		this.password = password;
	}

	@Override
	protected String doInBackground( Void... voids )
	{
		SignInCredentials signInCredentials = signInCredentialsPreferences.getSignInCredentials();
		if( signInCredentials == null ) {
			return sendErrorDescription.getSignInDescription();
		}

		Api api = ApiFactory.create( signInCredentials.getUrl(), networkConnectionCompat );
		if( api == null ) {
			return sendErrorDescription.getSignInDescription();
		}

		MultipartBody body;
		try {
			body = sendBody.create();
		} catch( IOException e ) {
			return sendErrorDescription.getSendBodyCreationDescription();
		}

		if( isCancelled() ) {
			return null;
		}

		return send( signInCredentials, api, body );
	}

	// Returns error string if error occurred or null if document was sent successfully
	@Nullable
	private String send( SignInCredentials signInCredentials, Api api, MultipartBody body )
	{
		Call<Void> sendCall = createSignInCall( signInCredentials, api, body );

		try {
			Response<Void> response = sendCall.execute();
			if( response.isSuccessful() ) {
				onSuccess( response );
				return null;
			} else if( response.code() == HttpURLConnection.HTTP_UNAUTHORIZED ) {
				isTokenExpired = true;
				return null;
			} else {
				return sendErrorDescription.getUnknownErrorDescription();
			}
		} catch( Throwable throwable ) {
			return sendErrorDescription.getRequestThrowableDescription( throwable );
		}
	}

	private Call<Void> createSignInCall( SignInCredentials signInCredentials, Api api, MultipartBody body )
	{
		String authorization;
		if( password == null ) {
			authorization = Api.getBasic( signInCredentials.getAuthTicket() );
		} else {
			authorization = Credentials.basic( signInCredentials.getUsername(), password );
		}

		return api.sendDocument(
			signInCredentials.getTenant(),
			authorization,
			body
		);
	}

	private void onSuccess( Response<Void> response )
	{
		sendPreferences.setDocumentSent( true );

		String authTicket = response.headers().get( Api.AUTH_TICKET_HEADER );
		updateToken( authTicket );
	}

	// Update token to delay password asking
	private void updateToken( @Nullable String authToken )
	{
		if( authToken == null ) {
			Log.w( LOG_TAG, "Auth token is null" );
			return;
		}

		SignInCredentials signInCredentials = signInCredentialsPreferences.getSignInCredentials();
		if( signInCredentials == null ) {
			Log.w( LOG_TAG, "User is not signed in" );
			return;
		}

		signInCredentialsPreferences.setSignInCredentials( new SignInCredentials(
			signInCredentials.getUrl(),
			signInCredentials.getTenant(),
			signInCredentials.getUsername(),
			authToken
		) );
	}

	@Override
	protected void onPostExecute( @Nullable String error )
	{
		Callback callback = callbackWeakReference.get();
		if( callback == null ) {
			return;
		}

		if( isTokenExpired ) {
			callback.onSendTokenExpiredError();
		} else if( error == null ) {
			callback.onSendSuccess();
		} else {
			callback.onSendError( error );
		}
	}
}
