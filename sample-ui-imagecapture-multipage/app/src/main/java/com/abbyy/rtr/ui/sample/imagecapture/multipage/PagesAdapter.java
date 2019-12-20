// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.multipage;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.abbyy.mobile.uicomponents.scenario.MultiPageImageCaptureScenario;
import com.abbyy.rtr.ui.sample.imagecapture.multipage.utils.BackgroundWorker;
import com.abbyy.rtr.ui.sample.imagecapture.multipage.utils.ImageUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for pages' recycler view, with memory cache and background loading of page thumbnails
 */
public class PagesAdapter extends RecyclerView.Adapter<PagesAdapter.ViewHolder> {
	private MultiPageImageCaptureScenario.Result pageResult;
	private List<String> pages = new ArrayList<>();
	private PageListener pageListener;
	// Page thumbnail actions
	private View.OnClickListener onThumbnailClickListener = new View.OnClickListener() {
		@Override
		public void onClick( View v )
		{
			Integer position = (Integer) v.getTag();
			if( position != null ) {
				pageListener.openPage( pages.get( position ) );
			}
		}
	};
	private View.OnClickListener onDeletePageClickListener = new View.OnClickListener() {
		@Override
		public void onClick( View v )
		{
			Integer position = (Integer) v.getTag();
			if( position != null ) {
				pageListener.deletePage( pages.get( position ) );
			}
		}
	};

	public void setPageResult( MultiPageImageCaptureScenario.Result pageResult )
	{
		this.pageResult = pageResult;
	}

	private static class MemoryCappedCache extends LruCache<Integer, Bitmap> {
		MemoryCappedCache( int maxSize )
		{
			super( maxSize );
		}

		protected int sizeOf( Integer key, Bitmap value )
		{
			return value.getByteCount();
		}
	}

	public interface PageListener {
		void openPage( String pageId );
		void deletePage( String pageId );
	}

	// Cache for page thumbnails
	private MemoryCappedCache thumbnailCache;

	public PagesAdapter( PageListener pageListener )
	{
		this.pageListener = pageListener;
		int maxMemory = (int) ( Runtime.getRuntime().maxMemory() );

		// Use 1/8th of the available memory for the memory cache.
		int cacheSize = maxMemory / 8;
		thumbnailCache = new MemoryCappedCache( cacheSize );
	}

	public void releasePages()
	{
		thumbnailCache.evictAll();
		notifyDataSetChanged();
	}

	public void updatePages( List<String> pages )
	{
		this.pages = pages;
		thumbnailCache.evictAll();
		notifyDataSetChanged();
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder( @NonNull ViewGroup parent, int viewType )
	{
		// Constructing thumbnail view
		LayoutInflater inflater = LayoutInflater.from( parent.getContext() );
		FrameLayout container = (FrameLayout) inflater.inflate( R.layout.page_thumbnail, parent, false );
		ImageView thumbnailView = container.findViewById( R.id.thumbnail );
		int thumbnailWidth = parent.getResources().getDimensionPixelSize( R.dimen.thumbnail_size );
		thumbnailView.setOnClickListener( onThumbnailClickListener );
		ImageButton deletePageView = container.findViewById( R.id.deletePage );
		deletePageView.setOnClickListener( onDeletePageClickListener );

		return new ViewHolder( container, thumbnailCache, thumbnailWidth );
	}

	@Override
	public void onBindViewHolder( @NonNull final ViewHolder viewHolder, int position )
	{
		viewHolder.bind( position );
	}

	@Override
	public int getItemCount()
	{
		return pages.size();
	}

	// Holder for thumbnail view
	class ViewHolder extends RecyclerView.ViewHolder {
		private View deletePageView;
		private ImageView thumbnail;
		// Asynchronous loader
		private BackgroundWorker<String, Bitmap> loader;
		// Bitmap cache held by weak reference
		private WeakReference<MemoryCappedCache> bitmapCacheRef;
		// Thumbnail is square, so one dimension is stored
		private int thumbnailDimension;
		private BackgroundWorker.Callback<String, Bitmap> callback = new BackgroundWorker.Callback<String, Bitmap>() {
			@Override
			public Bitmap doWork( String pageId ) throws Exception
			{
				Bitmap image = pageResult.loadImage( pageId );
				if( image != null ) {
					return ImageUtils.getMiniature( image, thumbnailDimension );
				}
				return null;
			}

			@MainThread
			@Override
			public void onDone( Bitmap thumbnail )
			{
				// Thumbnail loaded, update view and store in cache
				updateAndNotify( thumbnail );
			}

			@Override
			public void onError( Exception exception )
			{
				updateAndNotify( null );
			}
		};

		ViewHolder( @NonNull FrameLayout container, MemoryCappedCache bitmapCache, int thumbnailDimension )
		{
			super( container );
			thumbnail = container.findViewById( R.id.thumbnail );
			deletePageView = container.findViewById( R.id.deletePage );
			this.bitmapCacheRef = new WeakReference<>( bitmapCache );
			this.thumbnailDimension = thumbnailDimension;
		}

		void bind( int position )
		{
			MemoryCappedCache bitmapCache = bitmapCacheRef.get();
			if( bitmapCache == null ) {
				return;
			}
			Bitmap bitmap = bitmapCache.get( position );
			if( bitmap == null ) {
				if( loader != null ) {
					loader.cancel( true );
				}
				loader = getLoader();
				loader.executeOnExecutor( AsyncTask.THREAD_POOL_EXECUTOR, pages.get( position ) );
			} else {
				thumbnail.setImageBitmap( bitmap );
			}
			thumbnail.setTag( position );
			deletePageView.setTag( position );
		}

		private BackgroundWorker<String, Bitmap> getLoader()
		{
			return new BackgroundWorker<>( new WeakReference<>( callback ) );
		}

		private void updateAndNotify( Bitmap image )
		{
			thumbnail.setImageBitmap( image );
			MemoryCappedCache bitmapCache = bitmapCacheRef.get();
			if( bitmapCache != null && image != null ) {
				bitmapCache.put( getAdapterPosition(), image );
			}
		}
	}
}
