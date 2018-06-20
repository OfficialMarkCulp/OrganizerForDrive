package net.nut.photosorganizer;

import android.app.Activity;
import android.content.ContentValues;
import android.os.AsyncTask;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class RESTController
{
    private static final String TAG = "RESTController";

    private RESTController() {}

    interface ConnectionCallback
    {
        void onConnectionFail(Exception e);
        void onConnectionSuccess();
    }

    private static com.google.api.services.drive.Drive driveService;
    private static boolean isConnected;
    private static ConnectionCallback callback;

    static boolean init(Activity activity)
    {
        if (activity == null)
            return false;

        try
        {
            String email = Utilities.AccountManager.getEmail();

            if (email != null)
            {
                callback = (ConnectionCallback)activity;
                driveService = new Drive.Builder(AndroidHttp.newCompatibleTransport(),
                        new GsonFactory(), GoogleAccountCredential.usingOAuth2(Utilities.context,
                            Collections.singletonList(DriveScopes.DRIVE_FILE))
                            .setSelectedAccountName(email)
                ).build();
                return true;
            }
        }
        catch (Exception e)
        {
            Log.e(TAG, e.getMessage(), e);
        }
        return false;
    }

    static void connect()
    {
        if (Utilities.AccountManager.getEmail() != null && driveService != null)
        {
            isConnected = false;

            new AsyncTask<Void, Void, Exception>() {
                @Override
                protected Exception doInBackground(Void... voids){
                    try {
                        driveService.files().get("root")
                                .setFields("title")
                                .execute();
                        isConnected = true;
                    }
                    catch (UserRecoverableAuthIOException e) // Authorization failed, but it's user-recoverable.
                    {
                        return e;
                    }
                    catch (GoogleAuthIOException e) // Usually caused by a PackageName/SHA1 mismatch in the Developer Console.
                    {
                        return e;
                    }
                    catch (IOException e) // Usually caused by a 404.
                    {
                        if (e instanceof GoogleJsonResponseException)
                            if (((GoogleJsonResponseException)e).getStatusCode() == 404)
                                isConnected = true;
                    }
                    catch (Exception e) // Any other type of error.
                    {
                        Log.e(TAG, e.getMessage(), e);
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Exception e)
                {
                    super.onPostExecute(e);

                    if (isConnected)
                        callback.onConnectionSuccess();
                    else
                        callback.onConnectionFail(e);
                }
            }.execute();
        }
    }

    static void disconnect() {}

    /**
     * Searches for a given file/folder in Drive.
     * @param parent Parent ID (optional). null to search full drive, "root" to search root directory.
     * @param title The name of the thing we're looking for (optional).
     * @param mime The mime type of the thing we're looking for (optional).
     * @return An ArrayList containing the found objects.
     */
    static ArrayList<ContentValues> search(String parent, String title, String mime)
    {
        ArrayList<ContentValues> matches = new ArrayList<>();

        if (driveService != null && isConnected)
        {
            try
            {
                // Add query conditions & build the query.
                String queryClause = "'me' in owners and ";
                if (parent != null)
                    queryClause += "'" + parent + "' in parents and ";
                if (title != null)
                    queryClause += "title = '" + title + "' and ";
                if (mime != null)
                    queryClause += "mimeType = '" + mime + "' and ";
                queryClause = queryClause.substring(0, queryClause.length() - " and ".length());

                Drive.Files.List query = driveService.files().list()
                        .setQ(queryClause)
                        .setFields("items(id,mimeType,labels/trashed,title),nextPageToken");
                String nextPage = null;

                if (query != null)
                {
                    do {
                        FileList list = query.execute();

                        if (list != null)
                        {
                            for (File file : list.getItems())
                            {
                                if (file.getLabels().getTrashed())
                                    continue;
                                matches.add(Utilities.newVals(file.getTitle(), file.getId(), file.getMimeType()));
                            }
                            nextPage = list.getNextPageToken();
                            query.setPageToken(nextPage);
                        }
                    } while (nextPage != null && nextPage.length() > 0);
                }
            }
            catch (Exception e)
            {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        return matches;
    }

    /**
     * Creates a folder in the user's Drive.
     * @param parent The ID of the parent; null or "root" for root.
     * @param title Folder name
     * @return ID of the created folder, or null if the operation fails.
     */
    static String createFolder(String parent, String title)
    {
        String resourceID = null;

        if (driveService != null && isConnected && title != null)
        {
            File meta = new File();
            meta.setParents(Collections.singletonList(new ParentReference().setId(parent == null ? "root" : parent)));
            meta.setTitle(title);
            meta.setMimeType(Constants.MIME_FLDR);

            File file = null;
            try
            {
                file = driveService.files().insert(meta).execute();
            }
            catch (Exception e)
            {
                Log.e(TAG, e.getMessage(), e);
            }

            if (file != null && file.getId() != null)
            {
                resourceID = file.getId();
            }
        }
        return resourceID;
    }

    /**
     * Creates a file in the user's Drive.
     * @param parent The ID of the parent; null or "root" for root.
     * @param title File name
     * @return ID of the created file, or null if the operation fails.
     */
    static String createFile(String parent, String title, String mime, java.io.File file)
    {
        String resourceID = null;

        if (driveService != null && isConnected && title != null && mime != null && file != null)
        {
            try
            {
                File meta = new File()
                        .setParents(Collections.singletonList(new ParentReference().setId(parent == null ? "root" : parent)))
                        .setTitle(title)
                        .setMimeType(mime);
                File gFile = driveService.files().insert(meta, new FileContent(mime, file))
                        .execute();
                if (gFile != null)
                    resourceID = gFile.getId();
            }
            catch (Exception e)
            {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        return resourceID;
    }

    /**
     * Gets the contents of the given file.
     * @param resourceID The file to retrieve the contents of.
     * @return The file's contents.
     */
    static byte[] read(String resourceID)
    {
        if (driveService != null && isConnected && resourceID != null)
        {
            try
            {
                File file = driveService.files().get(resourceID)
                        .setFields("downloadUrl")
                        .execute();
                if (file != null)
                {
                    String url = file.getDownloadUrl();
                    return Utilities.getBytes(driveService.getRequestFactory()
                            .buildGetRequest(new GenericUrl(url))
                            .execute()
                            .getContent());
                }
            }
            catch (Exception e)
            {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * Update a file in Drive.
     * @param resourceID    The file's ID.
     * @param title         New file name (optional).
     * @param mime          New mime type (optional).
     * @param description   New file description (optional).
     * @param file          New file content (optional).
     * @return              The file's ID, or null if the operation fails.
     */
    static String update(String resourceID, String title, String mime, String description, java.io.File file)
    {
        File gFile = null;

        if (driveService != null && isConnected && resourceID != null)
        {
            try
            {
                File meta = new File();
                if (title != null)
                    meta.setTitle(title);
                if (mime != null)
                    meta.setMimeType(mime);
                if (description != null)
                    meta.setDescription(description);

                if (file == null)
                    gFile = driveService.files().patch(resourceID, meta).execute();
                else
                    gFile = driveService.files().update(resourceID, meta, new FileContent(mime, file)).execute();
            }
            catch (Exception e)
            {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        return gFile == null ? null : gFile.getId();
    }

    /**
     * Move an item to trash in Drive.
     * @param resourceID    The item to trash.
     * @return              Success status.
     */
    static boolean trash(String resourceID)
    {
        if (driveService != null && isConnected && resourceID != null)
        {
            try
            {
                return null != driveService.files().trash(resourceID).execute();
            }
            catch (Exception e)
            {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        return false;
    }

    /**
     * Is this item a file?
     * @param cv    The ContentValue we're inquiring about.
     * @return      True if it's a folder, false if not.
     */
    static boolean isFolder(ContentValues cv)
    {
        String mime = cv.getAsString(Constants.MIME);
        return mime != null && Constants.MIME_FLDR.equalsIgnoreCase(mime);
    }
}
