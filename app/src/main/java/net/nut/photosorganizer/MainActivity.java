package net.nut.photosorganizer;

import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.drive.model.File;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements RESTController.ConnectionCallback
{
    private static final String TAG = "MainActivity";

    private static TextView dispText;
    private static boolean isBusy;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dispText = findViewById(R.id.dispText);

        if (savedInstanceState == null)
        {
            Utilities.init(this);

            if (!RESTController.init(this))
            {
                startActivityForResult(AccountPicker.newChooseAccountIntent(null,
                        null, new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, true,
                        null, null, null, null), Constants.REQ_CODE_SIGN_IN);
            }
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        RESTController.connect();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        RESTController.disconnect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_create:
                createTree(Utilities.time2Title(null));
                return true;
            case R.id.action_list:
                testTree();
                return true;
            case R.id.action_delete:
                deleteTree();
                return true;
            case R.id.action_account:
                dispText.setText("");
                startActivityForResult(AccountPicker.newChooseAccountIntent(
                        null, null, new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE},
                        true, null, null, null, null), Constants.REQ_CODE_SIGN_IN);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode)
        {
            case Constants.REQ_CODE_CONNECT:
                if (resultCode == RESULT_OK)
                    RESTController.connect();
                else
                    suicide("err_nogo");
                break;
            case Constants.REQ_CODE_SIGN_IN:
                if (data != null && data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME) != null)
                {
                    Utilities.AccountManager.setEmail(data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
                }
                if (!RESTController.init(this))
                {
                    suicide("err_auth_accpick");
                }
                break;
        }
    }

    @Override
    public void onConnectionSuccess()
    {
        dispText.append("\n\nCONNECTED TO: " + Utilities.AccountManager.getEmail());
    }

    @Override
    public void onConnectionFail(Exception e) {
        if (e == null)
        {
            suicide("err_auth_dono");
            return;
        }

        if (e instanceof UserRecoverableAuthIOException)
        {
            startActivityForResult((((UserRecoverableAuthIOException) e).getIntent()), Constants.REQ_CODE_CONNECT);
        }
        else if (e instanceof GoogleAuthIOException)
        {
            if (e.getMessage() != null)
                suicide(e.getMessage());
            else
                suicide("err_auth_sha");
        }
        else
            suicide("err_auth_dono");
    }

    @SuppressLint("StaticFieldLeak")
    private void createTree(final String title)
    {
        if (title != null && !isBusy)
        {
            dispText.setText("UPLOADING\n");

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
                    isBusy = true;
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
                    dispText.append("\n" + strings[0]);
                }
                @Override
                protected void onPostExecute(Void nada) { super.onPostExecute(nada);
                    dispText.append("\n\nDONE");
                    isBusy = false;
                }
            }.execute();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void testTree() {
        if (!isBusy) {
            dispText.setText("DOWNLOADING\n");
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
                    isBusy = true;
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
                    dispText.append("\n" + strings[0]);
                }

                @Override
                protected void onPostExecute(Void nada) {
                    super.onPostExecute(nada);
                    dispText.append("\n\nDONE");
                    isBusy = false;
                }
            }.execute();
        }
    }

    /**
     *  scans folder tree created by this app deleting folders / files in the process
     */
    @SuppressLint("StaticFieldLeak")
    private void deleteTree() {
        if (!isBusy) {
            dispText.setText("DELETING\n");
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
                    isBusy = true;
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
                    dispText.append("\n" + strings[0]);
                }

                @Override
                protected void onPostExecute(Void nada) {
                    super.onPostExecute(nada);
                    dispText.append("\n\nDONE");
                    isBusy = false;
                }
            }.execute();
        }
    }

    private void suicide(int rid)
    {
        Utilities.AccountManager.setEmail(null);
        Toast.makeText(this, rid, Toast.LENGTH_LONG).show();
        finish();
    }

    private void suicide(String msg)
    {
        Utilities.AccountManager.setEmail(null);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        finish();
    }
}