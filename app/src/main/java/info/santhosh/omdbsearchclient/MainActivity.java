package info.santhosh.omdbsearchclient;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

import info.santhosh.omdbsearchclient.Utils.CommonUtils;
import info.santhosh.omdbsearchclient.omdbApiRetrofitService.RetrofitLoader;
import info.santhosh.omdbsearchclient.omdbApiRetrofitService.searchService;

public class MainActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<searchService.ResultWithDetail>{


    private Button mSearchButton;
    private EditText mSearchEditText;
    private RecyclerView mMovieListRecyclerView;
    private MovieRecyclerViewAdapter mMovieAdapter;
    private String mMovieTitle;

    private static final int LOADER_ID = 1;

    private static final String LOG_TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSearchEditText = (EditText) findViewById(R.id.search_edittext);
        // set action for pressing search button on keyboard
        mSearchEditText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || (event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))) {
                    startSearch();
                    handled = true;
                }
                return handled;
            }
        });
        mSearchButton = (Button) findViewById(R.id.search_button);
        mMovieListRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSearch();
            }
        });
        mMovieAdapter = new MovieRecyclerViewAdapter(null);
        mMovieListRecyclerView.setAdapter(mMovieAdapter);
        // First param is number of columns and second param is orientation i.e Vertical or Horizontal
        StaggeredGridLayoutManager gridLayoutManager =
                new StaggeredGridLayoutManager(getResources().getInteger(R.integer.grid_column_count), StaggeredGridLayoutManager.VERTICAL);
        mMovieListRecyclerView.setItemAnimator(null);
        // Attach the layout manager to the recycler view
        mMovieListRecyclerView.setLayoutManager(gridLayoutManager);
        getSupportLoaderManager().enableDebugLogging(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("mMovieTitle", mMovieTitle);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);
        // init the loader, so that the onLoadFinished is called
        mMovieTitle = savedInstanceState.getString("mMovieTitle");
        if (mMovieTitle != null) {
            Bundle args = new Bundle();
            args.putString("movieTitle", mMovieTitle);
            getSupportLoaderManager().initLoader(LOADER_ID, args, this);
        }
    }

    @Override
    public Loader<searchService.ResultWithDetail> onCreateLoader(int id, Bundle args) {
        Log.d(LOG_TAG, "onCreateLoader");
        return new RetrofitLoader(MainActivity.this, args.getString("movieTitle"));
    }

    @Override
    public void onLoadFinished(Loader<searchService.ResultWithDetail> loader, searchService.ResultWithDetail resultWithDetail) {
        Log.d(LOG_TAG, "onLoadFinished");
        if(resultWithDetail.getResponse().equals("True")) {
            mMovieAdapter.swapData(resultWithDetail.getMovieDetailList());
        } else {
            Snackbar.make(mMovieListRecyclerView,
                    getResources().getString(R.string.snackbar_title_not_found), Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onLoaderReset(Loader<searchService.ResultWithDetail> loader) {
        Log.d(LOG_TAG, "onLoaderReset");
        mMovieAdapter.swapData(null);
    }

    public class MovieRecyclerViewAdapter
            extends RecyclerView.Adapter<MovieRecyclerViewAdapter.ViewHolder> {

        private List<searchService.Detail> mValues;

        public MovieRecyclerViewAdapter(List<searchService.Detail> items) {
            mValues = items;
        }


        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_movie, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {

            final searchService.Detail detail = mValues.get(position);
            final String title = detail.Title;
            final String imdbId = detail.imdbID;
            final String director = detail.Director;
            final String year = detail.Year;
            holder.mDirectorView.setText(director);
            holder.mTitleView.setText(title);
            holder.mYearView.setText(year);

            final String imageUrl;
            if (! detail.Poster.equals("N/A")) {
                imageUrl = detail.Poster;
            } else {
                // default image if there is no poster available
                imageUrl = getResources().getString(R.string.default_poster);
            }

            Glide.with(MainActivity.this).load(imageUrl).into(holder.mThumbImageView);

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                    // Pass data object in the bundle and populate details activity.
                    intent.putExtra(DetailActivity.MOVIE_DETAIL, detail);
                    intent.putExtra(DetailActivity.IMAGE_URL, imageUrl);

                    ActivityOptionsCompat options = ActivityOptionsCompat.
                            makeSceneTransitionAnimation(MainActivity.this,
                                    holder.mThumbImageView, "poster");
                    startActivity(intent, options.toBundle());
                }
            });
        }

        @Override
        public int getItemCount() {
            if(mValues == null) {
                return 0;
            }
            return mValues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mTitleView;
            public final TextView mYearView;
            public final TextView mDirectorView;
            public final ImageView mThumbImageView;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mTitleView = (TextView) view.findViewById(R.id.movie_title);
                mYearView = (TextView) view.findViewById(R.id.movie_year);
                mThumbImageView = (ImageView) view.findViewById(R.id.thumbnail);
                mDirectorView = (TextView) view.findViewById(R.id.movie_director);
            }

        }

        @Override
        public void onViewRecycled(ViewHolder holder) {
            super.onViewRecycled(holder);
            Glide.clear(holder.mThumbImageView);
        }

        public void swapData(List<searchService.Detail> items) {
            if(items != null) {
                mValues = items;
                notifyDataSetChanged();

            } else {
                mValues = null;
            }
        }
    }

    private void startSearch() {
        if(CommonUtils.isNetworkAvailable(getApplicationContext())) {
            CommonUtils.hideSoftKeyboard(MainActivity.this);
            String movieTitle = mSearchEditText.getText().toString().trim();
            if (!movieTitle.isEmpty()) {
                Bundle args = new Bundle();
                args.putString("movieTitle", movieTitle);
                getSupportLoaderManager().restartLoader(LOADER_ID, args, this);
                mMovieTitle = movieTitle;
            }
            else
                Snackbar.make(mMovieListRecyclerView,
                        getResources().getString(R.string.snackbar_title_empty),
                        Snackbar.LENGTH_LONG).show();
        } else {
            Snackbar.make(mMovieListRecyclerView,
                    getResources().getString(R.string.network_not_available),
                    Snackbar.LENGTH_LONG).show();
        }
    }
}
