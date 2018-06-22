package net.nut.photosorganizer;

import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements DriveController.ConnectionCallback
{
    private static final String TAG = "MainActivity";

    /** UI Elements. */
    protected TextView txtNoAlbums;
    protected ProgressBar progressLoadMore;
    protected GridView photoGrid;
    protected TextView statusText;
    protected View expandedImageContainer;
    protected ImageView expandedImageView;
    protected TextView expandedImageTitle;
    protected TextView expandedImagePrompt;
    protected Button searchBtn;

    private ImageAdapter imageAdapter;
    private Animator animator;

    public static int currPage = 1;
    private int lastItem = 0;
    private boolean isAtEnd = false;

    /** Instance of the ImageManager. */
    private ImageManager ImageManager;

    protected static boolean isBusy;
    private int animationDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Test.ctx = this;

        txtNoAlbums = findViewById(R.id.tvNoAlbums);
        progressLoadMore = findViewById(R.id.progress);
        photoGrid = findViewById(R.id.photoGrid);
        statusText = findViewById(R.id.statusText);
        expandedImageContainer = findViewById(R.id.full_image_box);
        expandedImageView = findViewById(R.id.full_image);
        expandedImageTitle = findViewById(R.id.full_image_title);
        expandedImagePrompt = findViewById(R.id.info);
        searchBtn = findViewById(R.id.btnSearch);

        animationDuration = 10;

        // Initialize UI elements.
        initUIElements();

        // Get a reference to the ImageManager.
        ImageManager = ImageManager.getInstance();

        // Initialize the image handling stuff (Picasso).
        initImageHandling();

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

        setReadyToLoadImages();
    }

    private void initUIElements()
    {
        progressLoadMore.setVisibility(View.GONE);
        statusText.setText("Preparing API parameters...");

        searchBtn.setEnabled(false);
    }

    private void initImageHandling()
    {
        imageAdapter = new ImageAdapter(this, ImageManager);
        photoGrid.setAdapter(imageAdapter);
        photoGrid.setFastScrollEnabled(true);

        final int imageThumbSize = getResources().getDimensionPixelSize(R.dimen.photo_thumbnail_size);
        final int imageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.photo_thumbnail_spacing);

        // Determine the final width of the GridView and how many columns it should contain.
        photoGrid.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (imageAdapter.getNumCols() == 0)
                {
                    final int numCols = (int) Math.floor(photoGrid.getWidth() / (imageThumbSize + imageThumbSpacing));

                    if (numCols > 0)
                    {
                        final int colWidth = (photoGrid.getWidth() / numCols) - imageThumbSpacing;
                        imageAdapter.setNumCols(numCols);
                        imageAdapter.setItemHeight(colWidth);
                    }
                }
            }
        });

        photoGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
                // Zoom the image to fullscreen.
                performImageZoom(view, pos);
            }
        });

        photoGrid.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                /**************************************************************************
                 * Pausing of image download (to ensure smoother scrolling) when flinging
                 * is implicitly taken care in Picasso, so no custom handling required here
                 **************************************************************************/
            }

            @Override
            public void onScroll(AbsListView absListView, int first, int visCount, int totalCount) {
                final int _lastItem = first + visCount;
                if (_lastItem > 0 && totalCount > 0)
                {
                    if (_lastItem == ImageManager.getImageFeed().size() && !isAtEnd && lastItem != _lastItem)
                    {
                        lastItem = _lastItem;
                        fetchAndShowImages();
                    }
                }
            }
        });
    }

    /**
     * Invoked when the 'SEARCH' button is tapped.
     * @param view the current view.
     */
    public  void fireSearch(View view) { startNewSearch(); }

    /**
     * Reset any data and start a new search.
     */
    private void startNewSearch()
    {
        ImageManager.getImageFeed().clear();
        currPage = 1;
        fetchAndShowImages();
    }

    /**
     * Does the actual heavy-lifting of displaying the search results on-screen.
     */
    @SuppressLint("StaticFieldLeak")
    private void fetchAndShowImages()
    {
        statusText.setVisibility(View.GONE);
        searchBtn.setVisibility(View.GONE);

        if (currPage == 1)
        {
            ImageManager.getImageFeed().clear();
            isAtEnd = false;
            lastItem = 0;
            progressLoadMore.setVisibility(View.VISIBLE);
        }
        else
        {
            progressLoadMore.setVisibility(View.VISIBLE);
        }

        new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected Void doInBackground(Void... voids)
            {
                if (currPage == 1 || (currPage > 1 && !ImageManager.isAtEnd()))
                {
                    ArrayList<ImageManager.MediaItem> photoResults = ImageManager.getPhotos();

                    if (photoResults.size() > 0)
                        ImageManager.getImageFeed().addAll(photoResults);
                    else
                        isAtEnd = true;

                    currPage++;
                }
                else
                {
                    isAtEnd = true;
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid)
            {
                if (ImageManager.getImageFeed().size() > 0)
                {
                    imageAdapter.notifyDataSetChanged();

                    int currentPosition = photoGrid.getFirstVisiblePosition();
                    photoGrid.smoothScrollToPosition(currentPosition + 1, 0);
                }
                else
                {
                    txtNoAlbums.setVisibility(View.VISIBLE);
                }

                progressLoadMore.setVisibility(View.GONE);
            }
        }.execute();
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
                //Test.createTree(Utilities.time2Title(null));
                return true;
            case R.id.action_list:
                //Test.testTree();
                return true;
            case R.id.action_delete:
                //Test.deleteTree();
                return true;
            case R.id.action_account:
                //dispText.setText("");
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

    private void setReadyToLoadImages()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusText.setText("Ready!");
                searchBtn.setEnabled(true);
            }
        });
    }

    private void performImageZoom(final View thumbView, int pos)
    {
        // Cancel any currently running animations.
        if (animator != null)
        {
            animator.cancel();
        }

        Log.d(TAG, "Loading full image: " + ImageManager.getImageFeed().get(pos).embedLink);
        progressLoadMore.setVisibility(View.VISIBLE);

        // TODO: Change back to embed link.
        GlideApp.with(this)
                .load(ImageManager.getImageFeed().get(pos).thumbLink)
                .centerCrop()
                .fallback(R.drawable.thumbnail_placeholder)
                .placeholder(R.drawable.thumbnail_placeholder)
                .into(expandedImageView);
        expandedImageTitle.setText(ImageManager.getImageFeed().get(pos).title);

        // Calculate the starting and ending bounds for the zoomed-in image.
        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        /**************************************************************************
         * The start bounds are the global visible rectangle of the thumbnail,
         * and final bounds are the global visible rectangle of the container view.
         * Set the container view's offset as the origin for the bounds, since
         * that's the origin for the positioning animation properties (X, Y).
         **************************************************************************/
        thumbView.getGlobalVisibleRect(startBounds);
        findViewById(R.id.container).getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x+32, -globalOffset.y+32);

        /**************************************************************************
         * Adjust the start bounds to be the same aspect ratio as the final bounds
         * using the "center crop" technique. This prevents undesirable stretching
         * during the animation. Also calculate the start scaling factor (the end
         * scaling factor is always 1.0).
         **************************************************************************/
        float startScale;
        if ((float) finalBounds.width() / finalBounds.height() > (float) startBounds.width() / startBounds.height()) {
            // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        // Hide the thumbnail and show the zoomed-in view.
        thumbView.setAlpha(0f);
        expandedImageContainer.setVisibility(View.VISIBLE);

        // Construct and run the animation
        expandedImageContainer.setPivotX(0f);
        expandedImageContainer.setPivotY(0f);
        AnimatorSet set = new AnimatorSet();
        set.play(ObjectAnimator.ofFloat(expandedImageContainer, View.X, startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(expandedImageContainer, View.Y, startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(expandedImageContainer, View.SCALE_X, startScale, 1f))
                .with(ObjectAnimator.ofFloat(expandedImageContainer, View.SCALE_Y, startScale, 1f));
        set.setDuration(animationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                expandedImageTitle.setVisibility(View.VISIBLE);
                expandedImagePrompt.setVisibility(View.VISIBLE);
                animator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                animator = null;
            }
        });
        set.start();
        animator = set;

        // Upon clicking the zoomed-in image, it zooms back down to the
        // original bounds and show the thumbnail instead of the expanded image.
        final float startScaleFinal = startScale;
        expandedImageContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (animator != null) {
                    animator.cancel();
                }

                // Animate the four positioning/sizing properties in parallel,
                // back to their original values.
                AnimatorSet set = new AnimatorSet();
                set.play(ObjectAnimator.ofFloat(expandedImageContainer, View.X, startBounds.left))
                        .with(ObjectAnimator.ofFloat(expandedImageContainer, View.Y, startBounds.top))
                        .with(ObjectAnimator.ofFloat(expandedImageContainer, View.SCALE_X, startScaleFinal))
                        .with(ObjectAnimator.ofFloat(expandedImageContainer, View.SCALE_Y, startScaleFinal));
                set.setDuration(animationDuration);
                set.setInterpolator(new DecelerateInterpolator());
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        thumbView.setAlpha(1f);
                        expandedImageContainer.setVisibility(View.GONE);
                        animator = null;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        thumbView.setAlpha(1f);
                        expandedImageContainer.setVisibility(View.GONE);
                        animator = null;
                    }
                });
                expandedImageTitle.setVisibility(View.GONE);
                expandedImagePrompt.setVisibility(View.GONE);
                set.start();
                animator = set;
            }
        });
    }

    @Override
    public void onConnectionSuccess()
    {
        //dispText.append("\n\nCONNECTED TO: " + Utilities.AccountManager.getEmail());
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