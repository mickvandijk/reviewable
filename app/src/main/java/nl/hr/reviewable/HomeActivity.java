package nl.hr.reviewable;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;


public class HomeActivity extends ListActivity {

    protected List<ParseObject> mReview;
    protected SwipeRefreshLayout swipeLayout;
    protected Typeface face;
    protected ListView list;
    protected int reviewCount;
    protected boolean flag_loading;
    protected ReviewAdapter adapter;
    protected View footerView;
    protected int skipReviews = 0;
    protected int currentFirstVisibleItem = 0;
    protected int currentVisibleItemCount = 0;
    protected int currentTotalItemCount = 0;
    protected int currentScrollState = 0;
    static final int DIALOG_ERROR_CONNECTION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        getActionBar().setDisplayShowHomeEnabled(false);

        int titleId = getResources().getIdentifier("action_bar_title", "id", "android");
        TextView titleTextView = (TextView) findViewById(titleId);
        titleTextView.setTextSize(getResources().getDimension(R.dimen.title_size));
        titleTextView.setTextColor(getResources().getColor(R.color.white));
        face = Typeface.createFromAsset(getAssets(), "fonts/Pacifico.ttf");
        titleTextView.setTypeface(face);

        reviewCount = 6;

        footerView = ((LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.review_list_footer, null, false);
        getListView().addFooterView(footerView);


        if (!isOnline(this)) {

            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
            builder.setTitle("Error");
            builder.setMessage("There is no internet connection!")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            System.exit(0);
                            dialog.dismiss();
                        }
                    });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    System.exit(0);
                    dialog.dismiss();
                }
            });
            // Create the AlertDialog object and return it
            AlertDialog dialog = builder.create();
            dialog.show();

        } else {
            //Internet available. Do what's required when internet is available.

            mReview = new ArrayList<ParseObject>();
            adapter = new ReviewAdapter(getListView().getContext(), mReview);
            setListAdapter(adapter);

            Parse.initialize(this, "HS0km68yDCSvgftT2KILmFET7DFNESfH1rhVSmR2", "X4G5wb3DokD8aARe8lnLAk2HHDxdGTtsmhQQLw99");

            getReviews(0, reviewCount);

            list = getListView();
            swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
            swipeLayout.setColorSchemeResources(R.color.blue);
            swipeLayout.setEnabled(false);

            swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    swipeLayout.setRefreshing(true);
                    (new Handler()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            int listSize = mReview.size();
                            mReview.clear();
                            getReviews(0, listSize);
                            swipeLayout.setRefreshing(false);
                        }
                    }, 3000);
                }
            });

            list.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {
                    currentScrollState = scrollState;
                    isScrollCompleted();
                }

                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    int topRowVerticalPosition = (list == null || list.getChildCount() == 0) ? 0 : list.getChildAt(0).getTop();
                    swipeLayout.setEnabled(topRowVerticalPosition >= 0);

                    currentFirstVisibleItem = firstVisibleItem;
                    currentVisibleItemCount = visibleItemCount;
                    currentTotalItemCount = totalItemCount;

                }

                private void isScrollCompleted() {
                    if (currentVisibleItemCount > 0 && currentScrollState == SCROLL_STATE_IDLE && currentTotalItemCount == (currentFirstVisibleItem + currentVisibleItemCount)) {
                        if (!flag_loading) {

                            flag_loading = true;
                            getReviews(mReview.size(), reviewCount);
                        }
                    }
                }
            });

        }
    }

    public boolean isOnline(Context c) {
        ConnectivityManager cm = (ConnectivityManager) c
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();

        if (ni != null && ni.isConnected())
            return true;
        else
            return false;
    }

    public void getReviews(int skip, int limit) {
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser != null) {
            // Show home screen with reviews
            ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Review");
            query.orderByDescending("createdAt");
            query.setSkip(skip);
            query.setLimit(limit);
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> parseObjects, com.parse.ParseException e) {
                    if (e == null) {
                        // Success : list of reviews
                        if(parseObjects.size() == 0) {
                            getListView().removeFooterView(footerView);
                        }
                        else {
                            Log.d("list size", mReview.size() + "");
                            mReview.addAll(parseObjects);
                            adapter.notifyDataSetChanged();
                        }

                        flag_loading = false;
                    }
                    //else {
                        // Oops
                    //}
                }
            });

        } else {
            // Go to login screen
            Intent goToLogin = new Intent(HomeActivity.this, LoginActivity.class);
            startActivity(goToLogin);
            this.finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.new_review:
                Intent reviewIntent = new Intent(HomeActivity.this, ReviewActivity.class);
                startActivity(reviewIntent);

                break;
            case R.id.edit_account:
                Intent editAccountIntent = new Intent(HomeActivity.this, EditAccountActivity.class);
                startActivity(editAccountIntent);

                break;
            case R.id.logout:
                ParseUser currentUser = ParseUser.getCurrentUser();
                if (currentUser != null) {
                    ParseUser.logOut();
                }

                Intent loginIntent = new Intent(HomeActivity.this, LoginActivity.class);
                startActivity(loginIntent);
                this.finish();

                break;
        }

        return super.onOptionsItemSelected(item);
    }

}
