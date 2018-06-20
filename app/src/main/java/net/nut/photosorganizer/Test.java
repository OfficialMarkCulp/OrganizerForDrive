package net.nut.photosorganizer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.os.AsyncTask;

import java.util.ArrayList;

public class Test
{
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
                    ArrayList<ContentValues> cvs = RESTController.search(parent, title, Constants.MIME_FLDR);
                    String id, txt;

                    if (cvs.size() > 0)
                    {
                        txt = "found ";
                        id = cvs.get(0).getAsString(Constants.DRIVE_ID);
                    }
                    else
                    {
                        id = RESTController.createFolder(parent, title);
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
                            java.io.File fl = Utilities.str2File("content of " + title, "tmp" );
                            String id = null;
                            if (fl != null) {
                                id = RESTController.createFile(rsid, title, Constants.MIME_TEXT, fl);
                                fl.delete();
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
    protected static void testTree() {
        if (!MainActivity.isBusy) {
            MainActivity.dispText.setText("DOWNLOADING\n");
            new AsyncTask<Void, String, Void>() {

                private void iterate(ContentValues gfParent) {
                    ArrayList<ContentValues> cvs = RESTController.search(gfParent.getAsString(Constants.DRIVE_ID), null, null);
                    if (cvs != null) for (ContentValues cv : cvs) {
                        String gdid = cv.getAsString(Constants.DRIVE_ID);
                        String titl = cv.getAsString("title");

                        if (RESTController.isFolder(cv)) {
                            publishProgress(titl);
                            iterate(cv);
                        } else {
                            byte[] buf = RESTController.read(gdid);
                            if (buf == null)
                                titl += " failed";
                            publishProgress(titl);
                            String str = buf == null ? "" : new String(buf);
                            java.io.File fl = Utilities.str2File(str + "\n updated " + Utilities.time2Title(null), "tmp" );
                            if (fl != null) {
                                String desc = "seen " + Utilities.time2Title(null);
                                RESTController.update(gdid, null, null, desc, fl);
                                fl.delete();
                            }
                        }
                    }
                }

                @Override
                protected Void doInBackground(Void... params) {
                    MainActivity.isBusy = true;
                    ArrayList<ContentValues> gfMyRoot = RESTController.search("root", Constants.MYROOT, null);
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
                    ArrayList<ContentValues> cvs = RESTController.search(gfParent.getAsString(Constants.DRIVE_ID), null, null);
                    if (cvs != null) for (ContentValues cv : cvs) {
                        String titl = cv.getAsString("title");
                        String gdid = cv.getAsString(Constants.DRIVE_ID);
                        if (RESTController.isFolder(cv))
                            iterate(cv);
                        publishProgress("  " + titl + (RESTController.trash(gdid) ? " OK" : " FAIL"));
                    }
                }

                @Override
                protected Void doInBackground(Void... params) {
                    MainActivity.isBusy = true;
                    ArrayList<ContentValues> gfMyRoot = RESTController.search("root", Constants.MYROOT, null);
                    if (gfMyRoot != null && gfMyRoot.size() == 1 ){
                        ContentValues cv = gfMyRoot.get(0);
                        iterate(cv);
                        String titl = cv.getAsString("title");
                        String gdid = cv.getAsString(Constants.DRIVE_ID);
                        publishProgress("  " + titl + (RESTController.trash(gdid) ? " OK" : " FAIL"));
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
