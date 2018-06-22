package net.nut.photosorganizer;

import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.ArrayList;
import java.util.List;

public class ImageManager
{
    private static final String TAG = "ImageManager";

    /** Holds the search results. */
    private ArrayList<MediaItem> imageFeed = new ArrayList<>();

    /** The current instance of the ImageManager. */
    private static ImageManager instance = null;

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
    public static ImageManager getInstance()
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

    public ArrayList<MediaItem> getPhotos()
    {
        ArrayList<MediaItem> images = new ArrayList<>();

        FileList results = DriveController.paginatedSearch(null, null, Constants.MIME_IMG, nextPage);

        nextPage = results.getNextPageToken();
        List<File> files = results.getItems();

        for (File file : files)
        {
            MediaItem newItem = new MediaItem();

            newItem.embedLink = file.getEmbedLink();
            newItem.id = file.getId();
            newItem.thumbLink = file.getThumbnailLink();
            newItem.title = file.getTitle();

            images.add(newItem);
        }

        return images;
    }

    /** Template for a MediaItem. Represents any item fetched from Drive. */
    public class MediaItem
    {
        String id, title, thumbLink, embedLink;

    }
}
