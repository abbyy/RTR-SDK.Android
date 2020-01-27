// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.flexicapture;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.abbyy.mobile.rtr.Engine;
import com.abbyy.mobile.uicomponents.CaptureView;
import com.abbyy.mobile.uicomponents.scenario.MultiPageImageCaptureScenario;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.capture.SharedEngine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * The fragment uses {@link MultiPageImageCaptureScenario} to capture a document.
 */
public class CaptureFragment extends Fragment implements MultiPageImageCaptureScenario.Callback {

	public static final String TAG = "CaptureFragment";

	private static final String LOG_TAG = "CaptureFragment";
	private static final String START_PAGE_ID_KEY = "start_page_id_key";

	/**
	 * Creates an instance of {@link CaptureFragment}.
	 *
	 * @param startPageIdKey If the value is not null,
	 * the page editor will be opened at a page with the specified id.
	 */
	public static Fragment newInstance( @Nullable String startPageIdKey )
	{
		CaptureFragment captureFragment = new CaptureFragment();

		Bundle arguments = new Bundle();
		arguments.putString( START_PAGE_ID_KEY, startPageIdKey );
		captureFragment.setArguments( arguments );

		return captureFragment;
	}

	private CaptureView captureView;

	@Nullable
	@Override
	public View onCreateView(
		@NonNull LayoutInflater inflater,
		@Nullable ViewGroup container,
		@Nullable Bundle savedInstanceState
	)
	{
		return inflater.inflate( R.layout.fragment_capture, container, false );
	}

	@Override
	public void onViewCreated( @NonNull View view, @Nullable Bundle savedInstanceState )
	{
		super.onViewCreated( view, savedInstanceState );

		captureView = view.findViewById( R.id.capture_view );
		captureView.getUISettings().setTheme( CaptureView.UISettings.Theme.DARK );

		Bundle arguments = getArguments();
		if( arguments == null ) {
			throw new IllegalStateException( "Arguments is null. Use newInstance method for Fragment creation" );
		}

		String startPageId = arguments.getString( START_PAGE_ID_KEY );
		// We should use start page id only once.
		// Otherwise editor will be opened again after fragment recreation.
		arguments.putString( START_PAGE_ID_KEY, null );

		MultiPageImageCaptureScenario scenario = createScenario( startPageId );
		if( scenario != null ) {
			scenario.setCallback( this );
			captureView.setCaptureScenario( scenario );
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();
		captureView.startCamera();
	}

	@Override
	public void onPause()
	{
		super.onPause();
		captureView.stopCamera();
	}

	private MultiPageImageCaptureScenario createScenario( @Nullable String startPageId )
	{
		Engine engine = SharedEngine.get();
		try {
			return new MultiPageImageCaptureScenario.Builder( engine, requireContext() )
				.setStartAsEditorAtPage( startPageId )
				.build();
		} catch( Exception e ) {
			Log.e( LOG_TAG, "Can't create scenario", e );
			return null;
		}
	}

	@Override
	public void onClose( @NonNull MultiPageImageCaptureScenario.Result result )
	{
		close();
	}

	@Override
	public void onError( @NonNull Exception e, @NonNull MultiPageImageCaptureScenario.Result result )
	{
		Log.e( LOG_TAG, "error during capture", e );
		close();
	}

	@Override
	public void onFinished( @NonNull MultiPageImageCaptureScenario.Result result )
	{
		close();
	}

	private void close()
	{
		( (AppActivity) requireActivity() ).closeCaptureScreen();
	}
}
