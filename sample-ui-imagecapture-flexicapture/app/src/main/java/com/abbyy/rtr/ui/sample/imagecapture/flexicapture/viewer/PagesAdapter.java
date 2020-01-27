// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.flexicapture.viewer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.R;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.capture.ScenarioPages;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * The recycler view adapter is used for document pages.
 * It supports adding pages one by one using {@link #addPage(ScenarioPages.Page)}.
 */
public class PagesAdapter extends RecyclerView.Adapter<PagesAdapter.PageViewHolder> {

	public interface Listener {
		void onPageClick( @NonNull String pageId );
	}

	private final List<ScenarioPages.Page> pages;
	private final Listener listener;
	private final int pageHeight;

	public PagesAdapter( @NonNull Listener listener, int pageHeight )
	{
		this.pages = new ArrayList<>();
		this.listener = listener;
		this.pageHeight = pageHeight;
	}

	public void addPage( @NonNull ScenarioPages.Page page )
	{
		pages.add( page );
		// It is necessary to invalidate the cache because page content may have changed
		Picasso.get().invalidate( page.getImageFile() );
		notifyItemInserted( pages.size() - 1 );
	}

	@NonNull
	@Override
	public PageViewHolder onCreateViewHolder( @NonNull ViewGroup parent, int viewType )
	{
		LayoutInflater layoutInflater = LayoutInflater.from( parent.getContext() );
		View view = layoutInflater.inflate( R.layout.list_item_page, parent, false );
		view.getLayoutParams().height = pageHeight;
		return new PageViewHolder( view );
	}

	@Override
	public void onBindViewHolder( @NonNull PageViewHolder holder, int position )
	{
		holder.bind( pages.get( position ) );
	}

	@Override
	public void onViewRecycled( @NonNull PageViewHolder holder )
	{
		holder.onRecycled();
	}

	@Override
	public int getItemCount()
	{
		return pages.size();
	}

	/**
	 * The view holder is used for a document page.
	 * It loads a page image and fits the view size.
	 */
	class PageViewHolder extends RecyclerView.ViewHolder {

		private final ImageView pageImageView;
		private ScenarioPages.Page page;

		public PageViewHolder( @NonNull View itemView )
		{
			super( itemView );
			this.pageImageView = itemView.findViewById( R.id.page_image_view );
			pageImageView.setOnClickListener( view -> listener.onPageClick( page.getId() ) );
		}

		public void bind( @NonNull ScenarioPages.Page page )
		{
			this.page = page;
			Picasso
				.get()
				.load( page.getImageFile() )
				.fit()
				.centerInside()
				.into( pageImageView );
		}

		public void onRecycled()
		{
			Picasso.get().cancelRequest( pageImageView );
			pageImageView.setImageBitmap( null );
		}
	}
}
