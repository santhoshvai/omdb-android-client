package info.santhosh.omdbsearchclient;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import info.santhosh.omdbsearchclient.Utils.CommonUtils;
import info.santhosh.omdbsearchclient.omdbApiRetrofitService.searchService;

public class MainActivity extends AppCompatActivity {

    private Button mSearchButton;
    private EditText mSearchEditText;
    private RecyclerView mMovieListRecyclerView;
    private MovieRecyclerViewAdapter mMovieAdapter;
    private searchService.Result mSearchResult;
    private List<searchService.Movie> mMovieList = new ArrayList<>();
    private Map<String, searchService.Detail> mMovieDetailMap = new HashMap<>();

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
        mMovieAdapter = new MovieRecyclerViewAdapter(mMovieList);
        mMovieListRecyclerView.setAdapter(mMovieAdapter);
        // First param is number of columns and second param is orientation i.e Vertical or Horizontal
        StaggeredGridLayoutManager gridLayoutManager =
                new StaggeredGridLayoutManager(getResources().getInteger(R.integer.grid_column_count), StaggeredGridLayoutManager.VERTICAL);
        mMovieListRecyclerView.setItemAnimator(null);
        // Attach the layout manager to the recycler view
        mMovieListRecyclerView.setLayoutManager(gridLayoutManager);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("mSearchResult", mSearchResult);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);
        mSearchResult = savedInstanceState.getParcelable("mSearchResult");
        if(mSearchResult.Search.size() > 0) {
            mMovieList.addAll(mSearchResult.Search);
            mMovieAdapter.notifyDataSetChanged();
        }
    }

    private class searchByMovieTitle extends AsyncTask<String, Void, searchService.Result> {
        private final String LOG_TAG = searchByMovieTitle.class.getSimpleName();

        @Override
        protected searchService.Result doInBackground(String... params) {
            try {
                return searchService.performSearch(params[0].trim());
            } catch(final IOException e) {
                Log.e(LOG_TAG, "Error from api access", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(searchService.Result searchResult) {
            mMovieList.clear();
            mMovieDetailMap.clear();
            if(searchResult.Response.equals("True")) {
                mSearchResult = searchResult;
                mMovieList.addAll(searchResult.Search);
            } else {
                Snackbar.make(mMovieListRecyclerView,
                        getResources().getString(R.string.snackbar_title_not_found), Snackbar.LENGTH_LONG).show();
            }
            mMovieAdapter.notifyDataSetChanged();
        }

    }

    public class MovieRecyclerViewAdapter
            extends RecyclerView.Adapter<MovieRecyclerViewAdapter.ViewHolder> {

        private List<searchService.Movie> mValues;

        public MovieRecyclerViewAdapter(List<searchService.Movie> items) {
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

            final String Title = mValues.get(position).Title;
            final String imdbId = mValues.get(position).imdbID;
            if(! mMovieDetailMap.containsKey(Title)) {
                /* Anonymous async-task to request movie detail */
                new AsyncTask<String, Void, searchService.Detail>() {
                    @Override
                    protected searchService.Detail doInBackground(final String... params ) {
                        try {
                            return searchService.getDetail(params[0].trim());
                        } catch(final IOException e) {
                            Log.e("onBindViewHolder", "Error from api access", e);
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(final searchService.Detail detail) {
                        mMovieDetailMap.put(Title, detail);
                        holder.mDirectorView.setText(detail.Director);
                    }
                }.execute(imdbId);
            } else {
                // Detail of the movie was already fetched
                holder.mDirectorView.setText(mMovieDetailMap.get(Title).Director);
            }

            holder.mTitleView.setText(Title);
            holder.mYearView.setText(mValues.get(position).Year);

            final String imageUrl;
            if (! mValues.get(position).Poster.equals("N/A")) {
                imageUrl = mValues.get(position).Poster;
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
                    intent.putExtra(DetailActivity.IMAGE_URL, imageUrl);
                    intent.putExtra(DetailActivity.MOVIE_TITLE, Title);
                    intent.putExtra(DetailActivity.MOVIE_IMDB, imdbId);

                    ActivityOptionsCompat options = ActivityOptionsCompat.
                            makeSceneTransitionAnimation(MainActivity.this,
                                    holder.mThumbImageView, "poster");
                    startActivity(intent, options.toBundle());
                }
            });
        }

        @Override
        public int getItemCount() {
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
    }

    private void startSearch() {
        if(CommonUtils.isNetworkAvailable(getApplicationContext())) {
            CommonUtils.hideSoftKeyboard(MainActivity.this);
            String movieTitle = mSearchEditText.getText().toString();
            if (!movieTitle.isEmpty())
                new searchByMovieTitle().execute(movieTitle);
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
