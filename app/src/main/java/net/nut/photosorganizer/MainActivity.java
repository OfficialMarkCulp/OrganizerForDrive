package net.nut.photosorganizer;

import android.accounts.AccountManager;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

public class MainActivity extends AppCompatActivity implements DriveController.ConnectionCallback
{
    private static final String TAG = "MainActivity";

    protected static TextView dispText;
    protected static boolean isBusy;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Test.ctx = this;

        dispText = findViewById(R.id.dispText);

        if (savedInstanceState == null)
        {
            Utilities.init(this);

            if (!DriveController.init(this))
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

        DriveController.connect();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        DriveController.disconnect();
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
                Test.createTree(Utilities.time2Title(null));
                return true;
            case R.id.action_list:
                Test.testTree();
                return true;
            case R.id.action_delete:
                Test.deleteTree();
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
                    DriveController.connect();
                else
                    suicide("err_nogo");
                break;
            case Constants.REQ_CODE_SIGN_IN:
                if (data != null && data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME) != null)
                {
                    Utilities.AccountManager.setEmail(data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
                }
                if (!DriveController.init(this))
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