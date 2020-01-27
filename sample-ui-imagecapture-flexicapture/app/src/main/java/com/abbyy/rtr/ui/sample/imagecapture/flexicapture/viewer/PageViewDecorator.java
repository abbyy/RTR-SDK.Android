// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.flexicapture.viewer;

import android.graphics.Rect;
import android.view.View;

import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.R;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Decorates {@link PagesAdapter.PageViewHolder}.
 * It overrides the first and the last page's margins.
 */
public class PageViewDecorator extends RecyclerView.ItemDecoration {

	private final int spanCount;

	public PageViewDecorator( int spanCount )
	{
		this.spanCount = spanCount;
	}

	@Override
	public void getItemOffsets(
		@NonNull Rect outRect,
		@NonNull View view,
		@NonNull RecyclerView parent,
		@NonNull RecyclerView.State state
	)
	{
		int position = ( (RecyclerView.LayoutParams) view.getLayoutParams() ).getViewLayoutPosition();
		int margin = view.getResources().getDimensionPixelSize( R.dimen.list_item_page_margin );

		boolean isFirst = position % spanCount == 0;
		if( isFirst ) {
			outRect.left = margin;
		} else {
			outRect.left = margin / 2;
		}

		boolean isLast = position % spanCount == spanCount - 1;
		if( isLast ) {
			outRect.right = margin;
		} else {
			outRect.right = margin / 2;
		}
	}
}
