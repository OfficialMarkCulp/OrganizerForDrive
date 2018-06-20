package net.nut.photosorganizer;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class Test
{
    protected static Context ctx;

    @SuppressLint("StaticFieldLeak")
    protected static void createTree(final String title)
    {
        if (title != null && !MainActivity.isBusy)
        {
            MainActivity.dispText.setText("UPLOADING\n");

            new AsyncTask<Void, String, Void>()
            {
                private String findOrCreateFolder(String parent, String title)
                {
                    DriveController.search(parent, title, Constants.MIME_FLDR);
                    ArrayList<ContentValues> cvs = DriveController.getSearchResults();

                    String id, txt;

                    if (cvs.size() > 0)
                    {
                        txt = "found ";
                        id = cvs.get(0).getAsString(Constants.DRIVE_ID);
                    }
                    else
                    {
                        id = DriveController.createFolder(parent, title);
                        txt = "created ";
                    }
                    if (id != null)
                    {
                        txt += title;
                    }
                    else
                    {
                        txt = "failed " + title;
                    }
                    publishProgress(txt);
                    return id;
                }

                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                protected Void doInBackground(Void... params)
                {
                    MainActivity.isBusy = true;
                    String rsid = findOrCreateFolder("root", Constants.MYROOT);
                    if (rsid != null)
                    {
                        rsid = findOrCreateFolder(rsid, Utilities.title2Month(title));

                        if (rsid != null)
                        {
                            Bitmap bm = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.test);
                            File filesDir = ctx.getApplicationContext().getFilesDir();
                            File imgFile = new File(filesDir, "test.jpg");
                            OutputStream os;

                            try
                            {
                                os = new FileOutputStream(imgFile);
                                bm.compress(Bitmap.CompressFormat.JPEG, 100, os);
                                os.flush();
                                os.close();
                            }
                            catch (Exception e)
                            {
                                Log.e("Test", e.getMessage(), e);
                            }

                            Log.d("Test", "file: " + imgFile.getAbsolutePath());
                            //java.io.File fl = Utilities.str2File("content of " + title, "tmp" );
                            String id = null;
                            if (imgFile != null) {
                                id = DriveController.createFile(rsid, title, Constants.MIME_JPG, imgFile);
                                imgFile.delete();
                            }
                            if (id != null)
                                publishProgress("created " + title);
                            else
                                publishProgress("failed " + title);
                        }
                    }
                    return null;
                }
                @Override
                protected void onProgressUpdate(String... strings) { super.onProgressUpdate(strings);
                    MainActivity.dispText.append("\n" + strings[0]);
                }
                @Override
                protected void onPostExecute(Void nada) { super.onPostExecute(nada);
                    MainActivity.dispText.append("\n\nDONE");
                    MainActivity.isBusy = false;
                }
            }.execute();
        }
    }

    @SuppressLint("StaticFieldLeak")
    protected static void stressTestTree()
    {
        if (!MainActivity.isBusy)
        {
            MainActivity.dispText.setText("DOWNLOADING\n");
            new AsyncTask<Void, String, Void>()
            {
                private void iterate(ContentValues parent)
                {
                    DriveController.search(null, null, Constants.MIME_JPG);
                    ArrayList<ContentValues> cvs = DriveController.getSearchResults();

                    if (cvs != null)
                        for (ContentValues cv : cvs)
                        {
                            String driveID = cv.getAsString(Constants.DRIVE_ID);
                            String title = cv.getAsString("title");

                            if (DriveController.isFolder(cv))
                            {
                                publishProgress(title);
                                iterate(cv);
                            }
                            else
                            {
                                byte[] buffer = DriveController.read(driveID);
                                if (buffer == null)
                                {
                                    title += " failed";
                                }
                                publishProgress(title);
                                String string = buffer == null ? "" : new String(buffer);
                                File file = Utilities.str2File(string + "\n updated " + Utilities.time2Title(null), "tmp");
                                if (file != null)
                                {
                                    String desc = "seen " + Utilities.time2Title(null);
                                    DriveController.update(driveID, null, null, desc, file);
                                    file.delete();
                                }
                            }
                        }
                }

                @Override
                protected Void doInBackground(Void... params) {
                    MainActivity.isBusy = true;

                    DriveController.search(null, null, Constants.MIME_JPG);
                    ArrayList<ContentValues> gfMyRoot = DriveController.getSearchResults();

                    if (gfMyRoot != null && gfMyRoot.size() == 1 ){
                        publishProgress(gfMyRoot.get(0).getAsString("title"));
                        iterate(gfMyRoot.get(0));
                    }
                    return null;
                }

                @Override
                protected void onProgressUpdate(String... strings) {
                    super.onProgressUpdate(strings);
                    MainActivity.dispText.append("\n" + strings[0]);
                }

                @Override
                protected void onPostExecute(Void nada) {
                    super.onPostExecute(nada);
                    MainActivity.dispText.append("\n\nDONE");
                    MainActivity.isBusy = false;
                }
            }.execute();
        }
    }

    @SuppressLint("StaticFieldLeak")
    protected static void testTree() {
        if (!MainActivity.isBusy) {
            MainActivity.dispText.setText("DOWNLOADING\n");
            new AsyncTask<Void, String, Void>() {

                private void iterate(ContentValues gfParent) {

                    DriveController.search(gfParent.getAsString(Constants.DRIVE_ID), null, null);
                    ArrayList<ContentValues> cvs = DriveController.getSearchResults();

                    if (cvs != null) for (ContentValues cv : cvs) {
                        String gdid = cv.getAsString(Constants.DRIVE_ID);
                        String titl = cv.getAsString("title");

                        if (DriveController.isFolder(cv)) {
                            publishProgress(titl);
                            iterate(cv);
                        } else {
                            byte[] buf = DriveController.read(gdid);
                            if (buf == null)
                                titl += " failed";
                            publishProgress(titl);
                            String str = buf == null ? "" : new String(buf);
                            java.io.File fl = Utilities.str2File(str + "\n updated " + Utilities.time2Title(null), "tmp" );
                            if (fl != null) {
                                String desc = "seen " + Utilities.time2Title(null);
                                DriveController.update(gdid, null, null, desc, fl);
                                fl.delete();
                            }
                        }
                    }
                }

                @Override
                protected Void doInBackground(Void... params) {
                    MainActivity.isBusy = true;

                    DriveController.search("root", Constants.MYROOT, null);
                    ArrayList<ContentValues> gfMyRoot = DriveController.getSearchResults();

                    if (gfMyRoot != null && gfMyRoot.size() == 1 ){
                        publishProgress(gfMyRoot.get(0).getAsString("title"));
                        iterate(gfMyRoot.get(0));
                    }
                    return null;
                }

                @Override
                protected void onProgressUpdate(String... strings) {
                    super.onProgressUpdate(strings);
                    MainActivity.dispText.append("\n" + strings[0]);
                }

                @Override
                protected void onPostExecute(Void nada) {
                    super.onPostExecute(nada);
                    MainActivity.dispText.append("\n\nDONE");
                    MainActivity.isBusy = false;
                }
            }.execute();
        }
    }

    /**
     *  scans folder tree created by this app deleting folders / files in the process
     */
    @SuppressLint("StaticFieldLeak")
    protected static void deleteTree() {
        if (!MainActivity.isBusy) {
            MainActivity.dispText.setText("DELETING\n");
            new AsyncTask<Void, String, Void>() {

                private void iterate(ContentValues gfParent) {

                    DriveController.search(gfParent.getAsString(Constants.DRIVE_ID), null, null);
                    ArrayList<ContentValues> cvs = DriveController.getSearchResults();

                    if (cvs != null) for (ContentValues cv : cvs) {
                        String titl = cv.getAsString("title");
                        String gdid = cv.getAsString(Constants.DRIVE_ID);
                        if (DriveController.isFolder(cv))
                            iterate(cv);
                        publishProgress("  " + titl + (DriveController.trash(gdid) ? " OK" : " FAIL"));
                    }
                }

                @Override
                protected Void doInBackground(Void... params) {
                    MainActivity.isBusy = true;

                    DriveController.search("root", Constants.MYROOT, null);
                    ArrayList<ContentValues> gfMyRoot = DriveController.getSearchResults();

                    if (gfMyRoot != null && gfMyRoot.size() == 1 ){
                        ContentValues cv = gfMyRoot.get(0);
                        iterate(cv);
                        String titl = cv.getAsString("title");
                        String gdid = cv.getAsString(Constants.DRIVE_ID);
                        publishProgress("  " + titl + (DriveController.trash(gdid) ? " OK" : " FAIL"));
                    }
                    return null;
                }

                @Override
                protected void onProgressUpdate(String... strings) {
                    super.onProgressUpdate(strings);
                    MainActivity.dispText.append("\n" + strings[0]);
                }

                @Override
                protected void onPostExecute(Void nada) {
                    super.onPostExecute(nada);
                    MainActivity.dispText.append("\n\nDONE");
                    MainActivity.isBusy = false;
                }
            }.execute();
        }
    }
}
