package net.nut.photosorganizer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

public class ImageAdapter extends BaseAdapter
{
    private static final String TAG = "ImageAdapter";

    private LayoutInflater inflater;
    private RelativeLayout.LayoutParams imageViewParams;
    private ImageManager ImageManager;
    private int itemHeight = 0;
    private int numCols = 0;

    /**
     * Initialize the ImageAdapter
     * @param mainActivity      The MainActivity of the app.
     * @param imageManager      A reference to the ImageManager instance.
     */
    public ImageAdapter(MainActivity mainActivity, ImageManager imageManager)
    {
        inflater = (LayoutInflater)mainActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        imageViewParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        this.ImageManager = imageManager;
    }

    /**
     * Set the number of columns in the grid.
     * @param _numCols  The number of columns.
     */
    public void setNumCols(int _numCols)
    {
        numCols = _numCols;
    }

    /**
     * Get the number of columns in the grid.
     * @return  The number of columns.
     */
    public int getNumCols()
    {
        return numCols;
    }

    /**
     * Set the height of each image item.
     * @param itemHeight    The item's height.
     */
    public void setItemHeight(int itemHeight)
    {
        if (this.itemHeight == itemHeight)
            return;

        this.itemHeight = itemHeight;
        imageViewParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, itemHeight);
        notifyDataSetChanged();
    }

    /**
     * Return the MediaItem corresponding to the item at the given position.
     * @param position  The position in the ArrayList.
     * @return          The found item.
     */
    public ImageManager.MediaItem getItem(int position)
    {
        return ImageManager.getImageFeed().get(position);
    }

    /**
     * Fetch the number of images in the results.
     * @return  The number of images.
     */
    public int getCount() { return ImageManager.getImageFeed().size(); }

    public int getItemHeight() {
        return itemHeight;
    }

    /**
     * Fetch the ID of the item in the given position.
     * @param position  The position of the item.
     * @return          The item's ID.
     */
    public long getItemId(int position) { return position; }

    /**
     * Create the thumbnail view that is to be shown in a given position
     * in the grid.
     * @param position  The position in the grid.
     * @param view      The current view.
     * @param parent    The current view's parent.
     * @return          The created view.
     */
    public View getView(final int position, View view, ViewGroup parent)
    {
        ViewHolder holder;

        if (view == null)
        {
            holder = new ViewHolder();
            view = inflater.inflate(R.layout.thumb, null);
            holder.thumb = view.findViewById(R.id.thumb);
            holder.title = view.findViewById(R.id.iTitle);

            view.setTag(holder);
        }
        else
        {
            holder = (ViewHolder) view.getTag();
        }

        holder.thumb.setLayoutParams(imageViewParams);

        if (holder.thumb.getLayoutParams().height != itemHeight)
        {
            holder.thumb.setLayoutParams(imageViewParams);
        }

        ImageManager.MediaItem item = getItem(position);
        Picasso.with(parent.getContext())
                .load(item.thumbLink)
                .placeholder(R.drawable.thumbnail_placeholder)
                .error(R.drawable.thumbnail_placeholder)
                .into(holder.thumb);
        holder.thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
        holder.title.setText(item.title);

        return view;
    }

    /**
     * A holder for the view. Currently only stores the thumbnail enabled.
     * The title can be used to handle the image title.
     */
    class ViewHolder
    {
        ImageView thumb;
        TextView title;
    }
}
