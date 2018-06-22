package net.nut.photosorganizer;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.bumptech.glide.Glide;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Utilities
{
    private static final String TAG = "Utilities";

    private Utilities() {}

    private static SharedPreferences preferences;
    public static Context context;
    private static GlideApp glide = null;

    static void init(Context ctx)
    {
        context = ctx.getApplicationContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static GlideApp getGlide()
    {
        return glide;
    }

    static class AccountManager
    {
        private AccountManager() {}

        private static String email = null;

        static void setEmail(String _email)
        {
            Utilities.preferences.edit()
                    .putString(Constants.ACC_NAME, (email = _email))
                    .apply();
        }

        static String getEmail()
        {
            return email != null ? email : (email = Utilities.preferences.getString(Constants.ACC_NAME, null));
        }
    }

    static ContentValues newVals(String title, String driveID, String mime)
    {
        ContentValues returnVal = new ContentValues();

        if (title != null)
            returnVal.put(Constants.TITLE, title);
        if (driveID != null)
            returnVal.put(Constants.DRIVE_ID, driveID);
        if (mime != null)
            returnVal.put(Constants.MIME, mime);

        return returnVal;
    }

    private static File cacheFile(String fileName)
    {
        File cache = Utilities.context.getExternalCacheDir();
        return (cache == null || fileName == null) ? null : new File(cache.getPath() + File.separator + fileName);
    }

    static File str2File(String str, String name)
    {
        if (str == null)
            return null;

        byte[] buffer = str.getBytes();

        File file = cacheFile(name);

        if (file == null)
            return null;

        BufferedOutputStream bufferedOutputStream = null;
        try
        {
            bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file));
            bufferedOutputStream.write(buffer);
        }
        catch (Exception e)
        {
            Log.e(TAG, e.getMessage(), e);
        }
        finally
        {
            if (bufferedOutputStream != null)
            {
                try
                {
                    bufferedOutputStream.close();
                }
                catch (Exception e)
                {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        }
        return file;
    }

    static byte[] getBytes(InputStream in)
    {
        byte[] buffer = null;
        BufferedInputStream bIn = null;

        if (in != null)
        {
            try
            {
                ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
                bIn = new BufferedInputStream(in);
                buffer = new byte[4096];

                int count;
                while ((count = bIn.read(buffer)) >= 0)
                {
                    byteBuffer.write(buffer, 0, count);
                }

                buffer = byteBuffer.size() > 0 ? byteBuffer.toByteArray() : null;
            }
            catch (Exception ignored) {}
            finally
            {
                try
                {
                    if (bIn != null)
                        bIn.close();
                }
                catch (Exception ignored) {}
            }
        }
        return buffer;
    }

    static String time2Title(Long millis)
    {
        Date date = (millis == null) ? new Date() : (millis >= 0) ? new Date(millis) : null;
        return (date == null) ? null : new SimpleDateFormat(Constants.TITLE_FORMAT, Locale.US).format(date);
    }

    static String title2Month(String title)
    {
        return title == null ? null : ("20" + title.substring(0, 2) + "-" + title.substring(2, 4));
    }

    // TODO: Either revisit or remove this.
    /*
    public static Picasso getAuthPicasso(Context context, final String authToken)
    {
        if (authPicasso == null)
        {
            Picasso.Builder builder = new Picasso.Builder(context);
            builder.downloader(new UrlConnectionDownloader(context) {
                @Override
                protected HttpURLConnection openConnection(Uri uri) throws IOException
                {
                    HttpURLConnection connection = super.openConnection(uri);
                    connection.setRequestProperty();
                }
            })
        }
    }*/
}
