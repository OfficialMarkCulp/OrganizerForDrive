package net.nut.photosorganizer;

import java.util.ArrayList;

public class ImageManager
{
    private static final String TAG = "ImageManager";

    /** Holds the search results. */
    private ArrayList<MediaItem> imageFeed = new ArrayList<>();

    /** The current instance of the ImageManager. */
    private ImageManager instance = null;

    /** Token for the next page of results. */
    private String nextPage = null;

    /**
     * Private constructor to prevent accidental instantiation.
     */
    private ImageManager() {}

    /**
     * Get the ImageManager instance.
     * @return  The instance.
     */
    public ImageManager getInstance()
    {
        if (instance == null)
            instance = new ImageManager();

        return instance;
    }

    /**
     * Fetches the ImageFeed.
     * @return  The ImageFeed.
     */
    public ArrayList<MediaItem> getImageFeed()
    {
        return imageFeed;
    }

    /**
     * Have we reached the end of the results?
     * @return  True if we have, false if not.
     */
    public boolean isAtEnd() { return nextPage == null; }

    /** Template for a MediaItem. Represents any item fetched from Drive. */
    public class MediaItem
    {
        String thumbLink;
        String title;
    }
}
