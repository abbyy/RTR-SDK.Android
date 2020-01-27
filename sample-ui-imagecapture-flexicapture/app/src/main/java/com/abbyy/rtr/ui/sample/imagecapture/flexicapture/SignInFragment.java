// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.flexicapture;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.abbyy.mobile.uicomponents.BuildConfig;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.api.Api;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.api.ApiFactory;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.api.Parameters;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.api.Project;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.net.NetworkConnectionCompat;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.send.SendPreferences;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.sign_in.SignInCredentials;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.sign_in.SignInCredentialsPreferences;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.sign_in.SignInErrorDescription;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.widgets.KeyboardCloseHelper;
import com.google.android.material.snackbar.Snackbar;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import okhttp3.Credentials;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * The fragment shows UI to sign in to a FlexiCapture server and to chose a project.
 */
public class SignInFragment extends Fragment {

	public static final String TAG = "SignInFragment";

	public static Fragment newInstance()
	{
		return new SignInFragment();
	}

	private EditText urlEditText;
	private EditText tenantEditText;
	private EditText usernameEditText;
	private EditText passwordEditText;
	private View connectButton;
	private View reconnectButton;
	private View progressBar;
	private Spinner projectsSpinner;
	private View signOutButton;
	private ImageView connectionStateImageView;
	private TextView connectionStateTextView;
	private View closeButton;
	private View connectionDivider;
	private View projectDivider;
	private View projectTitleTextView;
	private Snackbar errorSnackbar;
	private View urlLockImageView;
	private View tenantLockImageView;
	private View usernameLockImageView;
	private TextView buildNumberTextView;

	private Dialog signOutConfirmationDialog;

	private SendPreferences sendPreferences;
	private SignInCredentialsPreferences signInCredentialsPreferences;

	private SignInErrorDescription signInErrorDescription;

	private Call<Parameters> signInCall;

	@Nullable
	@Override
	public View onCreateView(
		@NonNull LayoutInflater inflater,
		@Nullable ViewGroup container,
		@Nullable Bundle savedInstanceState
	)
	{
		return inflater.inflate( R.layout.fragment_sign_in, container, false );
	}

	@Override
	public void onViewCreated( @NonNull View view, @Nullable Bundle savedInstanceState )
	{
		super.onViewCreated( view, savedInstanceState );

		setupDependencies();
		setupViews( view );
		setupViewListeners();
		showInitialViewState();
	}

	private void setupDependencies()
	{
		sendPreferences = new SendPreferences( requireContext() );
		signInCredentialsPreferences = new SignInCredentialsPreferences( requireContext() );
		signInErrorDescription = new SignInErrorDescription( requireContext() );
	}

	private void setupViews( @NonNull View view )
	{
		urlEditText = view.findViewById( R.id.url_edit_text );
		tenantEditText = view.findViewById( R.id.tenant_edit_text );
		usernameEditText = view.findViewById( R.id.username_edit_text );
		passwordEditText = view.findViewById( R.id.password_edit_text );
		connectButton = view.findViewById( R.id.connect_button );
		reconnectButton = view.findViewById( R.id.reconnect_button );
		projectsSpinner = view.findViewById( R.id.projects_spinner );
		signOutButton = view.findViewById( R.id.sign_out_button );
		progressBar = view.findViewById( R.id.sing_in_progress_bar );
		connectionStateTextView = view.findViewById( R.id.connection_state_text_view );
		connectionStateImageView = view.findViewById( R.id.connection_state_image_view );
		closeButton = view.findViewById( R.id.close_button );
		connectionDivider = view.findViewById( R.id.connection_divider );
		projectDivider = view.findViewById( R.id.project_divider );
		projectTitleTextView = view.findViewById( R.id.project_title );
		urlLockImageView = view.findViewById( R.id.url_lock_image_view );
		tenantLockImageView = view.findViewById( R.id.tenant_lock_image_view );
		usernameLockImageView = view.findViewById( R.id.username_lock_image_view );
		buildNumberTextView = view.findViewById( R.id.build_number_text_view );
	}

	private void setupViewListeners()
	{
		connectButton.setOnClickListener( v -> onSignInClick() );
		reconnectButton.setOnClickListener( v -> onSignInClick() );
		signOutButton.setOnClickListener( v -> onSignOutClick() );
		passwordEditText.setOnEditorActionListener( ( v, actionId, event ) -> {
			if( actionId == EditorInfo.IME_ACTION_DONE && isSignInEnabled() ) {
				onSignInClick();
				return true;
			} else {
				return false;
			}
		} );
		closeButton.setOnClickListener( v -> onCloseClick() );
		setupTextChangeListeners();
	}

	private void setupTextChangeListeners()
	{
		// Listen to text changes to disable sign in button if some fields are empty
		TextWatcher textWatcher = new TextWatcher() {
			@Override
			public void beforeTextChanged( CharSequence s, int start, int count, int after ) { }

			@Override
			public void onTextChanged( CharSequence s, int start, int before, int count ) { }

			@Override
			public void afterTextChanged( Editable s )
			{
				SignInFragment.this.afterTextChanged();
			}
		};
		urlEditText.addTextChangedListener( textWatcher );
		usernameEditText.addTextChangedListener( textWatcher );
		passwordEditText.addTextChangedListener( textWatcher );
	}

	private void afterTextChanged()
	{
		boolean isConnectEnabled = isSignInEnabled();

		connectButton.setEnabled( isConnectEnabled );
		reconnectButton.setEnabled( isConnectEnabled );
	}

	private boolean isSignInEnabled()
	{
		SignInParams signInParams = getSignInParams();
		boolean isUrlEmpty = signInParams.url.isEmpty();
		boolean isUsernameEmpty = signInParams.username.isEmpty();
		boolean isPasswordEmpty = signInParams.password.isEmpty();
		// Tenant may be empty
		return !( isUrlEmpty || isUsernameEmpty || isPasswordEmpty );
	}

	private void showInitialViewState()
	{
		if( signInCredentialsPreferences.getSignInCredentials() != null ) {
			showSignedInInitialViewState();
		} else {
			showSignedOutViewState();
		}

		buildNumberTextView.setText( getString( R.string.build_number, BuildConfig.VERSION_NAME ) );
	}

	// We don't store password therefore we ask to enter password to change projects if needed.
	private void showSignedInInitialViewState()
	{
		showReconnectButton();
		showConnectionState( R.drawable.ic_warning, getString( R.string.enter_password_to_change_project ) );
		showSignOutButton();
		showProjects( Collections.singletonList( sendPreferences.getProject() ), /* isChangeEnabled */ false );
		showSignInHints();
		disableEditTexts();
	}

	private void showSignOutButton()
	{
		signOutButton.setVisibility( View.VISIBLE );
	}

	private void showReconnectButton()
	{
		connectButton.setVisibility( View.GONE );
		reconnectButton.setVisibility( View.VISIBLE );
	}

	private void showConnectionState( @DrawableRes int imageRes, String stateText )
	{
		connectionStateImageView.setImageResource( imageRes );
		connectionStateImageView.setVisibility( View.VISIBLE );
		connectionStateTextView.setText( stateText );
		connectionStateTextView.setVisibility( View.VISIBLE );
	}

	private void showSignInHints()
	{
		SignInCredentials signInCredentials = signInCredentialsPreferences.getSignInCredentials();
		if( signInCredentials != null ) {
			urlEditText.setText( signInCredentials.getUrl() );
			tenantEditText.setText( signInCredentials.getTenant() );
			usernameEditText.setText( signInCredentials.getUsername() );
		}
	}

	private void showProjects( @NonNull List<String> projects, boolean isChangeEnabled )
	{
		ArrayAdapter<String> adapter = new ArrayAdapter<>(
			requireContext(),
			android.R.layout.simple_spinner_item,
			projects
		);
		projectsSpinner.setAdapter( adapter );
		projectsSpinner.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected( AdapterView<?> parent, View view, int position, long id )
			{
				saveProject( projects.get( position ) );
			}

			@Override
			public void onNothingSelected( AdapterView<?> parent )
			{
				// Do nothing
			}
		} );

		projectsSpinner.setVisibility( View.VISIBLE );
		connectionDivider.setVisibility( View.VISIBLE );
		projectDivider.setVisibility( View.VISIBLE );
		projectTitleTextView.setVisibility( View.VISIBLE );
		projectsSpinner.setEnabled( isChangeEnabled );
	}

	private void disableEditTexts()
	{
		disableEditText( urlEditText, urlLockImageView );
		disableEditText( tenantEditText, tenantLockImageView );
		disableEditText( usernameEditText, usernameLockImageView );
		// Don't disable password. User may want to change project
	}

	private void disableEditText( @NonNull EditText editText, @NonNull View lockImageView )
	{
		editText.setEnabled( false );
		editText.setPadding(
			editText.getPaddingLeft(),
			editText.getPaddingTop(),
			// Padding prevents text overlaying on lock image view
			getResources().getDimensionPixelOffset( R.dimen.settings_locked_edit_text_padding ),
			editText.getPaddingBottom()
		);
		lockImageView.setVisibility( View.VISIBLE );
	}

	private void showSignedOutViewState()
	{
		signOutButton.setVisibility( View.GONE );
		connectButton.setVisibility( View.VISIBLE );
		reconnectButton.setVisibility( View.GONE );
		connectionStateTextView.setVisibility( View.GONE );
		connectionStateImageView.setVisibility( View.GONE );

		hideProjects();
		enableEditTexts();
	}

	private void hideProjects()
	{
		projectsSpinner.setVisibility( View.GONE );
		projectsSpinner.setAdapter( null );
		connectionDivider.setVisibility( View.GONE );
		projectDivider.setVisibility( View.GONE );
		projectTitleTextView.setVisibility( View.GONE );
	}

	private void enableEditTexts()
	{
		enableEditText( urlEditText, urlLockImageView );
		enableEditText( tenantEditText, tenantLockImageView );
		enableEditText( usernameEditText, usernameLockImageView );
	}

	private void enableEditText( @NonNull EditText editText, @NonNull View lockImageView )
	{
		editText.setEnabled( true );
		editText.setPadding(
			editText.getPaddingLeft(),
			editText.getPaddingTop(),
			0,
			editText.getPaddingBottom()
		);
		lockImageView.setVisibility( View.GONE );
	}

	// SIGN IN START

	private void onSignInClick()
	{
		KeyboardCloseHelper.close( requireActivity() );
		signIn();
	}

	private SignInParams getSignInParams()
	{
		return new SignInParams(
			urlEditText.getText().toString().trim(),
			tenantEditText.getText().toString().trim(),
			usernameEditText.getText().toString().trim(),
			passwordEditText.getText().toString() // We don't trim password intentionally
		);
	}

	private void signIn()
	{
		signOut();
		hideErrorSnackbar();
		showProgress();

		NetworkConnectionCompat networkConnectionCompat = new NetworkConnectionCompat( requireContext() );
		SignInParams signInParams = getSignInParams();
		Api api = ApiFactory.create( signInParams.url, networkConnectionCompat );
		if( api == null ) {
			hideProgress();
			showSignInErrorSnackbar( signInErrorDescription.getIncorrectUrlDescription() );
		} else {
			signInWithApi( signInParams, api );
		}
	}

	private void signInWithApi( @NonNull SignInParams signInParams, @NonNull Api api )
	{
		String basicAuthString = Credentials.basic( signInParams.username, signInParams.password );
		signInCall = api.getProjects( signInParams.tenant, basicAuthString );

		signInCall.enqueue( new Callback<Parameters>() {
			@Override
			public void onResponse( @NonNull Call<Parameters> call, @NonNull Response<Parameters> response )
			{
				if( isViewNotDestroyed() ) {
					onSignInResponse( response, signInParams );
				}
			}

			@Override
			public void onFailure( @NonNull Call<Parameters> call, @NonNull Throwable throwable )
			{
				if( isViewNotDestroyed() ) {
					onSignInFailure( throwable );
				}
			}
		} );
	}

	private boolean isViewNotDestroyed()
	{
		return getView() != null;
	}

	private void onSignInResponse( @NonNull Response<Parameters> response, @NonNull SignInParams signInParams )
	{
		hideProgress();

		if( !response.isSuccessful() ) {
			showSignInErrorSnackbar( signInErrorDescription.getRequestCodeDescription( response.code() ) );
			return;
		}

		if( !areProjectsNotEmpty( response.body() ) ) {
			showSignInErrorWarningView( signInErrorDescription.getEmptyProjectsDescription() );
			return;
		}
		List<String> projects = getProjects( response.body() );

		String authTicket = response.headers().get( Api.AUTH_TICKET_HEADER );
		if( authTicket == null ) {
			showSignInErrorSnackbar( signInErrorDescription.getRequestCodeDescription( HttpURLConnection.HTTP_UNAUTHORIZED ) );
			return;
		}

		saveSignInCredentials( signInParams, authTicket );
		saveProject( projects.get( 0 ) );

		showSignedInViewState( projects );
	}

	private void saveSignInCredentials( @NonNull SignInParams signInParams, String authTicket )
	{
		SignInCredentials credentials = new SignInCredentials(
			signInParams.url,
			signInParams.tenant,
			signInParams.username,
			authTicket
		);
		signInCredentialsPreferences.setSignInCredentials( credentials );
	}

	private boolean areProjectsNotEmpty( @Nullable Parameters parameters )
	{
		return parameters != null && parameters.getProjects() != null && !parameters.getProjects().isEmpty();
	}

	private void saveProject( @NonNull String project )
	{
		sendPreferences.setProject( project );
	}

	private List<String> getProjects( @NonNull Parameters parameters )
	{
		List<String> strings = new ArrayList<>();
		for( Project project : parameters.getProjects() ) {
			strings.add( project.getName() );
		}
		return strings;
	}

	private void showSignedInViewState( @NonNull List<String> projects )
	{
		showReconnectButton();
		showConnectionState( R.drawable.ic_success, getString( R.string.connection_to_server ) );
		showSignOutButton();
		showProjects( projects, /* isChangeEnabled */ true );
		showSignInHints();
		disableEditTexts();
	}

	private void onSignInFailure( @NonNull Throwable throwable )
	{
		hideProgress();
		showSignInErrorSnackbar( signInErrorDescription.getRequestThrowableDescription( throwable ) );
	}

	private void showProgress()
	{
		connectButton.setVisibility( View.GONE );
		reconnectButton.setVisibility( View.GONE );
		progressBar.setVisibility( View.VISIBLE );
		connectionStateImageView.setVisibility( View.GONE );
		connectionStateTextView.setVisibility( View.VISIBLE );
		connectionStateTextView.setText( R.string.connecting_to_server );
	}

	private void hideProgress()
	{
		progressBar.setVisibility( View.GONE );
	}

	private void showSignInErrorSnackbar( @NonNull String error )
	{
		showErrorSnackbar( error );
		showConnectionState( R.drawable.ic_error, getString( R.string.no_connection_to_server ) );
		showReconnectButton();
	}

	private void showErrorSnackbar( @NonNull String error )
	{
		errorSnackbar = Snackbar.make( requireView(), error, Snackbar.LENGTH_INDEFINITE );
		errorSnackbar.setAction( android.R.string.ok, v -> errorSnackbar.dismiss() );
		errorSnackbar.show();
	}

	private void showSignInErrorWarningView( @NonNull String error )
	{
		showConnectionState( R.drawable.ic_warning, error );
		showReconnectButton();
	}

	private void hideErrorSnackbar()
	{
		if( errorSnackbar != null ) {
			errorSnackbar.dismiss();
			errorSnackbar = null;
		}
	}

	private void cancelSigningIn()
	{
		if( signInCall != null ) {
			signInCall.cancel();
			signInCall = null;
		}
	}

	// SIGN IN END

	// SIGN OUT START

	private void onSignOutClick()
	{
		signOutConfirmationDialog = new AlertDialog.Builder( requireContext() )
			.setTitle( R.string.sign_out_confirmation_dialog_title )
			.setMessage( R.string.sign_out_confirmation_dialog_message )
			.setPositiveButton( android.R.string.ok, ( dialog, which ) -> onSignOutConfirmed() )
			.setNegativeButton( android.R.string.cancel, ( dialog, which ) -> { /* Do nothing */ } )
			.create();
		signOutConfirmationDialog.show();
	}

	private void onSignOutConfirmed()
	{
		signOut();
		clearEditTextViews();
	}

	private void clearEditTextViews()
	{
		urlEditText.setText( null );
		tenantEditText.setText( null );
		usernameEditText.setText( null );
		passwordEditText.setText( null );
	}

	private void signOut()
	{
		signInCredentialsPreferences.resetSignInCredentials();
		sendPreferences.resetProject();

		showSignedOutViewState();
	}

	private void hideSignOutDialog()
	{
		if( signOutConfirmationDialog != null ) {
			signOutConfirmationDialog.dismiss();
			signOutConfirmationDialog = null;
		}
	}

	// SIGN OUT END

	private void onCloseClick()
	{
		requireFragmentManager().popBackStack();
	}

	@Override
	public void onDestroyView()
	{
		super.onDestroyView();
		cancelSigningIn();

		hideErrorSnackbar();
		hideSignOutDialog();
	}

	private static class SignInParams {
		final String url;
		final String tenant;
		final String username;
		final String password;

		public SignInParams( String url, String tenant, String username, String password )
		{
			this.url = url;
			this.tenant = tenant;
			this.username = username;
			this.password = password;
		}
	}

}
