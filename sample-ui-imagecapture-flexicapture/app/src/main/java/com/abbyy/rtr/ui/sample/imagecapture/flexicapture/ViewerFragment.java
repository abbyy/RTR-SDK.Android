// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.flexicapture;

import android.app.Dialog;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.abbyy.mobile.rtr.Engine;
import com.abbyy.mobile.uicomponents.scenario.ImageCaptureScenario;
import com.abbyy.mobile.uicomponents.scenario.MultiPageImageCaptureScenario;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.capture.ScenarioPages;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.capture.SharedEngine;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.send.SendAsyncTask;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.send.SendPreferences;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.sign_in.SignInCredentialsPreferences;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.viewer.DeletePagesAsyncTask;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.viewer.LoadPagesAsyncTask;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.viewer.PageViewDecorator;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.viewer.PagesAdapter;
import com.google.android.material.snackbar.Snackbar;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * The fragment shows document pages preview
 * and buttons to delete the document, to send the document, to open sign-in screen, to open capture screen.
 */
public class ViewerFragment extends Fragment implements SendAsyncTask.Callback, LoadPagesAsyncTask.Callback,
	DeletePagesAsyncTask.Callback, PagesAdapter.Listener {

	private static final String LOG_TAG = "ViewerFragment";
	private static int DELETE_CLICK_REQUEST_CODE = 0;
	private static int DELETE_BEFORE_SCAN_NEW_DOC_REQUEST_CODE = 1;

	public static final String TAG = "ViewerFragment";

	public static Fragment newInstance()
	{
		return new ViewerFragment();
	}

	private View sendImageView;
	private TextView sendProjectTextView;
	private View sendProgressBar;
	private ImageView sendStateImageView;
	private View sendButton;
	private View logInButton;
	private View deleteButton;
	private View scanNewDocumentButton;
	private View addPageButton;
	private TextView pagesCountTextView;
	private View documentPlaceholderImageView;
	private Snackbar errorSnackbar;
	private RecyclerView pagesRecyclerView;
	private View pagesRecyclerViewDisableOverlayView;
	private PagesAdapter pagesAdapter;

	private SignInCredentialsPreferences signInCredentialsPreferences;
	private SendPreferences sendPreferences;
	private MultiPageImageCaptureScenario scenario;

	private SendAsyncTask sendAsyncTask;
	private LoadPagesAsyncTask loadPagesAsyncTask;

	private Dialog deleteDialog;
	private Dialog askPasswordDialog;

	@Nullable
	@Override
	public View onCreateView(
		@NonNull LayoutInflater inflater,
		@Nullable ViewGroup container,
		@Nullable Bundle savedInstanceState
	)
	{
		return inflater.inflate( R.layout.fragment_viewer, container, false );
	}

	@Override
	public void onViewCreated( @NonNull View view, @Nullable Bundle savedInstanceState )
	{
		super.onViewCreated( view, savedInstanceState );

		scenario = createScenario();
		if( scenario == null ) {
			return;
		}

		setupDependencies();
		setupViews( view );
		setupViewListeners( view );
		setupViewState();
		setupRecyclerView();
		loadScenarioImages();
	}

	private MultiPageImageCaptureScenario createScenario()
	{
		Engine engine = SharedEngine.get();
		try {
			return new MultiPageImageCaptureScenario.Builder( engine, requireContext() ).build();
		} catch( Exception e ) {
			Log.e( LOG_TAG, "Can't create scenario", e );
			requireActivity().finish();
			return null;
		}
	}

	private void setupDependencies()
	{
		signInCredentialsPreferences = new SignInCredentialsPreferences( requireContext() );
		sendPreferences = new SendPreferences( requireContext() );
	}

	private void setupViews( @NonNull View view )
	{
		sendImageView = view.findViewById( R.id.send_image_view );
		sendProjectTextView = view.findViewById( R.id.send_project_text_view );
		sendProgressBar = view.findViewById( R.id.send_progress_bar );
		sendStateImageView = view.findViewById( R.id.send_state_image_view );
		sendButton = view.findViewById( R.id.send_button_text_view );
		logInButton = view.findViewById( R.id.log_in_button_text_view );
		deleteButton = view.findViewById( R.id.delete_button_text_view );
		scanNewDocumentButton = view.findViewById( R.id.scan_new_document_button );
		addPageButton = view.findViewById( R.id.add_page_button );
		pagesCountTextView = view.findViewById( R.id.pages_count_text_view );
		documentPlaceholderImageView = view.findViewById( R.id.document_placeholder_image_view );
		pagesRecyclerView = view.findViewById( R.id.pages_recycler_view );
		pagesRecyclerViewDisableOverlayView = view.findViewById( R.id.pages_recycler_view_disable_overlay_view );
	}

	private void setupViewListeners( @NonNull View view )
	{
		view.findViewById( R.id.settings_button_image_view ).setOnClickListener( v -> onSettingsClick() );
		logInButton.setOnClickListener( v -> onSettingsClick() );
		scanNewDocumentButton.setOnClickListener( v -> onScanNewDocClick() );
		addPageButton.setOnClickListener( v -> onAddPagesClick() );
		sendButton.setOnClickListener( v -> onSendClick() );
		deleteButton.setOnClickListener( v -> onDeleteClick() );
	}

	private void setupViewState()
	{
		boolean isSignedIn = signInCredentialsPreferences.getSignInCredentials() != null;
		if( isSignedIn ) {
			sendImageView.setVisibility( View.VISIBLE );

			sendProjectTextView.setText( sendPreferences.getProject() );
			sendProjectTextView.setVisibility( View.VISIBLE );
		}
	}

	private void setupRecyclerView()
	{
		int spanCount = getResources().getInteger( R.integer.viewer_page_row_count );
		int pageHeight = getRecyclerPageHeight( spanCount );
		pagesAdapter = new PagesAdapter( this, pageHeight );
		pagesRecyclerView.setAdapter( pagesAdapter );
		pagesRecyclerView.setLayoutManager( new GridLayoutManager( requireContext(), spanCount ) );
		pagesRecyclerView.addItemDecoration( new PageViewDecorator( spanCount ) );
		pagesRecyclerView.getRecycledViewPool().setMaxRecycledViews( 0, spanCount * 2 );
		pagesRecyclerView.setItemViewCacheSize( spanCount * 2 );
		pagesRecyclerView.setHasFixedSize( true );
	}

	private int getRecyclerPageHeight( int spanCount )
	{
		DisplayMetrics displayMetrics = new DisplayMetrics();
		requireActivity().getWindowManager().getDefaultDisplay().getMetrics( displayMetrics );
		int screenWidth = displayMetrics.widthPixels;
		int listItemPageMargin = getResources().getDimensionPixelSize( R.dimen.list_item_page_margin );
		float listItemPageWidth = ( screenWidth - listItemPageMargin * ( spanCount + 1 ) ) / (float) spanCount;

		ImageCaptureScenario.DocumentSize a4 = ImageCaptureScenario.DocumentSize.A4;
		float a4AspectRatio = a4.getHeight() / a4.getWidth();
		return (int) ( listItemPageWidth * a4AspectRatio );
	}

	// LOADING START

	private void loadScenarioImages()
	{
		loadPagesAsyncTask = new LoadPagesAsyncTask(
			requireActivity().getApplication(),
			scenario.getResult(),
			new WeakReference<>( this )
		);
		loadPagesAsyncTask.execute();
	}

	@Override
	public void onNextPage( @NonNull ScenarioPages.Page page )
	{
		if( isViewNotDestroyed() ) {
			if( sendPreferences.isDocumentSent() ) {
				showRecyclerViewOverlay();
			}
			pagesRecyclerView.setVisibility( View.VISIBLE );
			pagesAdapter.addPage( page );
		}
	}

	@Override
	public void onLoadPagesComplete()
	{
		if( isViewNotDestroyed() ) {
			if( pagesAdapter.getItemCount() == 0 ) {
				showNoPagesLoaded();
			} else {
				showPagesLoaded();
			}
		}
	}

	private void showNoPagesLoaded()
	{
		scanNewDocumentButton.setVisibility( View.VISIBLE );
		addPageButton.setVisibility( View.GONE );

		sendButton.setVisibility( View.GONE );
		logInButton.setVisibility( View.GONE );
		deleteButton.setVisibility( View.GONE );

		documentPlaceholderImageView.setVisibility( View.VISIBLE );
		pagesRecyclerView.setVisibility( View.GONE );
		pagesRecyclerView.setAdapter( null );
		pagesCountTextView.setVisibility( View.GONE );
		sendStateImageView.setVisibility( View.GONE );
		hideRecyclerViewOverlay();
	}

	private void showPagesLoaded()
	{
		showCaptureButtonForNotEmptyDocument();
		showDeleteButton();
		showPagesCount();
		showSendAvailable();
		showSendSuccessIfNeeded();
	}

	private void showSendSuccessIfNeeded()
	{
		if( sendPreferences.isDocumentSent() && signInCredentialsPreferences.isSignedIn() ) {
			showSendSuccess();
		}
	}

	private void showCaptureButtonForNotEmptyDocument()
	{
		if( sendPreferences.isDocumentSent() ) {
			addPageButton.setVisibility( View.GONE );
			scanNewDocumentButton.setVisibility( View.VISIBLE );
		} else {
			scanNewDocumentButton.setVisibility( View.GONE );
			addPageButton.setVisibility( View.VISIBLE );
		}
	}

	private void showDeleteButton()
	{
		deleteButton.setVisibility( View.VISIBLE );
	}

	private void showSendAvailable()
	{
		if( signInCredentialsPreferences.isSignedIn() ) {
			sendButton.setVisibility( View.VISIBLE );
		} else {
			logInButton.setVisibility( View.VISIBLE );
		}
	}

	private void showPagesCount()
	{
		pagesCountTextView.setVisibility( View.VISIBLE );
		String pagesCount = getResources().getQuantityString(
			R.plurals.page_count,
			pagesAdapter.getItemCount(),
			pagesAdapter.getItemCount()
		);
		pagesCountTextView.setText( pagesCount );
	}

	@Override
	public void onLoadPagesError( @NonNull String error )
	{
		showErrorIfVisible( error );
	}

	private void cancelLoading()
	{
		if( loadPagesAsyncTask != null ) {
			loadPagesAsyncTask.cancel( false );
			loadPagesAsyncTask = null;
		}
	}

	// LOADING END

	// SENDING START

	private void onSendClick()
	{
		showSendingInProgress();
		send();
	}

	private void send()
	{
		sendWithPassword( null );
	}

	// If password is null, auth token will be used
	// otherwise password will be used and auth token stored.
	private void sendWithPassword( @Nullable String password )
	{
		sendAsyncTask = new SendAsyncTask(
			requireActivity().getApplication(),
			scenario.getResult(),
			new WeakReference<>( this ),
			password
		);
		sendAsyncTask.execute();
	}

	private void showSendingInProgress()
	{
		sendButton.setEnabled( false );
		deleteButton.setEnabled( false );
		sendProgressBar.setVisibility( View.VISIBLE );
		sendStateImageView.setVisibility( View.GONE );
		showRecyclerViewOverlay();
		hideErrorSnackbar();
	}

	private void showRecyclerViewOverlay()
	{
		pagesRecyclerViewDisableOverlayView.setVisibility( View.VISIBLE );
	}

	@Override
	public void onSendSuccess()
	{
		hideSendingInProgress();
		showCaptureButtonForNotEmptyDocument();
		showSendSuccess();
	}

	private void showSendSuccess()
	{
		sendStateImageView.setImageResource( R.drawable.ic_success );
		sendStateImageView.setVisibility( View.VISIBLE );
		sendButton.setEnabled( false );
	}

	@Override
	public void onSendError( @NonNull String error )
	{
		hideSendingInProgress();
		showErrorIfVisible( error );
	}

	@Override
	public void onSendTokenExpiredError()
	{
		showAskPasswordDialog();
	}

	private void showAskPasswordDialog()
	{
		LayoutInflater layoutInflater = LayoutInflater.from( requireContext() );
		FrameLayout frameLayout = (FrameLayout) layoutInflater.inflate( R.layout.dialog_ask_password, null, false );
		EditText editText = frameLayout.findViewById( R.id.ask_password_edit_text );

		askPasswordDialog = new AlertDialog.Builder( requireContext() )
			.setTitle( R.string.ask_password_dialog_title )
			.setMessage( R.string.ask_password_dialog_message )
			.setView( frameLayout )
			.setPositiveButton( R.string.ask_password_dialog_positive_button, ( dialog, which ) -> {
				onPasswordEntered( editText.getText().toString() );
			} )
			.setNegativeButton( R.string.ask_password_dialog_negative_button, ( dialog, which ) -> {
				onAskPasswordDialogCancel();
			} )
			.setOnDismissListener( dialog -> onAskPasswordDialogCancel() )
			.create();
		askPasswordDialog.show();
	}

	private void onPasswordEntered( @NonNull String password )
	{
		sendWithPassword( password );
	}

	private void onAskPasswordDialogCancel()
	{
		hideSendingInProgress();
		hideRecyclerViewOverlay();
	}

	private void showSendError()
	{
		sendStateImageView.setImageResource( R.drawable.ic_error );
		sendStateImageView.setVisibility( View.VISIBLE );
		hideRecyclerViewOverlay();
	}

	private void hideRecyclerViewOverlay()
	{
		pagesRecyclerViewDisableOverlayView.setVisibility( View.GONE );
	}

	private void hideSendingInProgress()
	{
		sendButton.setEnabled( true );
		deleteButton.setEnabled( true );
		sendProgressBar.setVisibility( View.GONE );
	}

	private void cancelSending()
	{
		if( sendAsyncTask != null ) {
			sendAsyncTask.cancel( true );
			sendAsyncTask = null;
		}
	}

	private void dismissAskPasswordDialog()
	{
		if( askPasswordDialog != null ) {
			askPasswordDialog.dismiss();
			askPasswordDialog = null;
		}
	}

	// SENDING END

	// DELETE START

	private void onDeleteClick()
	{
		if( sendPreferences.isDocumentSent() ) {
			deletePages( DELETE_CLICK_REQUEST_CODE );
		} else {
			showDeleteConfirmationDialog();
		}
	}

	private void showDeleteConfirmationDialog()
	{
		deleteDialog = new AlertDialog.Builder( requireContext() )
			.setTitle( R.string.delete_dialog_title )
			.setMessage( R.string.delete_dialog_message )
			.setPositiveButton( R.string.delete_dialog_positive_button, ( dialog, which ) -> onDeleteConfirmed() )
			.setNegativeButton( R.string.delete_dialog_positive_negative, ( dialog, which ) -> {
				// Do nothing
			} )
			.create();
		deleteDialog.show();
	}

	private void onDeleteConfirmed()
	{
		deletePages( DELETE_CLICK_REQUEST_CODE );
	}

	private void deletePages( int requestCode )
	{
		// We don't have to cancel it so there is no need to store reference to AsyncTask
		new DeletePagesAsyncTask(
			requireActivity().getApplication(),
			scenario.getResult(),
			new WeakReference<>( this ),
			requestCode
		).execute();

		showNoPagesLoaded();
		disableCaptureButtons();
	}

	private void disableCaptureButtons()
	{
		addPageButton.setEnabled( false );
		scanNewDocumentButton.setEnabled( false );
	}

	@Override
	public void onPagesDeleted( int requestCode )
	{
		enableCaptureButtons();
		if( requestCode == DELETE_BEFORE_SCAN_NEW_DOC_REQUEST_CODE ) {
			openCaptureScreen( null );
		}
	}

	@Override
	public void onPagesDeleteError( @NonNull Exception exception )
	{
		Log.e( LOG_TAG, "delete error", exception );
		enableCaptureButtons();
	}

	private void enableCaptureButtons()
	{
		addPageButton.setEnabled( true );
		scanNewDocumentButton.setEnabled( true );
	}

	private void dismissDeleteConfirmationDialog()
	{
		if( deleteDialog != null ) {
			deleteDialog.dismiss();
			deleteDialog = null;
		}
	}

	// DELETE END

	private void onSettingsClick()
	{
		( (AppActivity) requireActivity() ).openSignInSettings();
	}

	private void onAddPagesClick()
	{
		openCaptureScreen( null );
	}

	private void onScanNewDocClick()
	{
		deletePages( DELETE_BEFORE_SCAN_NEW_DOC_REQUEST_CODE );
	}

	@Override
	public void onPageClick( @NonNull String pageId )
	{
		openCaptureScreen( pageId );
	}

	private void openCaptureScreen( @Nullable String pageId )
	{
		( (AppActivity) requireActivity() ).openCaptureScreen( pageId );
	}

	@Override
	public void onDestroyView()
	{
		cancelSending();
		cancelLoading();
		hideErrorSnackbar();
		dismissDeleteConfirmationDialog();
		dismissAskPasswordDialog();
		super.onDestroyView();
	}

	private void showErrorIfVisible( @NonNull String error )
	{
		if( isViewNotDestroyed() ) {
			hideErrorSnackbar();
			showErrorSnackbar( error );
			showSendError();
		}
	}

	private void showErrorSnackbar( @NonNull String error )
	{
		errorSnackbar = Snackbar.make( requireView(), error, Snackbar.LENGTH_INDEFINITE );
		errorSnackbar.setAction( android.R.string.ok, v -> errorSnackbar.dismiss() );
		errorSnackbar.show();
	}

	private void hideErrorSnackbar()
	{
		if( errorSnackbar != null ) {
			errorSnackbar.dismiss();
			errorSnackbar = null;
		}
	}

	private boolean isViewNotDestroyed()
	{
		return getView() != null;
	}

}
