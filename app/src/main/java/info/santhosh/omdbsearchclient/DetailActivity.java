package info.santhosh.omdbsearchclient;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.io.IOException;

import info.santhosh.omdbsearchclient.omdbApiRetrofitService.searchService;

public class DetailActivity extends AppCompatActivity {

    public static final String IMAGE_URL = "image_url";
    public static final String MOVIE_IMDB = "imdb";
    public static final String MOVIE_TITLE = "title";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        final String imageUrl = getIntent().getStringExtra(IMAGE_URL);
        final String movieTitle = getIntent().getStringExtra(MOVIE_TITLE);

        Glide.with(this).load(imageUrl).into( (ImageView) findViewById(R.id.main_backdrop));

        // set title for the appbar
        CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.main_collapsing);
        collapsingToolbarLayout.setTitle(movieTitle);

        final String imdbId = getIntent().getStringExtra(MOVIE_IMDB);
        ((TextView) findViewById(R.id.grid_title)).setText(movieTitle);

        /* Anonymous async-task to request movie detail */
        new AsyncTask<String, Void, searchService.Detail>() {
            @Override
            protected searchService.Detail doInBackground(final String... params ) {
                try {
                    return searchService.getDetail(params[0].trim());
                } catch(final IOException e) {
                    Log.e("DetailActivity", "Error from api access", e);
                }
                return null;
            }
            @Override
            protected void onPostExecute(final searchService.Detail detail) {
                ((TextView) findViewById(R.id.grid_writers)).setText(detail.Writer);
                ((TextView) findViewById(R.id.grid_actors)).setText(detail.Actors);
                ((TextView) findViewById(R.id.grid_director)).setText(detail.Director);
                ((TextView) findViewById(R.id.grid_genre)).setText(detail.Genre);
                ((TextView) findViewById(R.id.grid_released)).setText(detail.Released);
                ((TextView) findViewById(R.id.grid_plot)).setText(detail.Plot);
                ((TextView) findViewById(R.id.grid_runtime)).setText(detail.Runtime);
            }
        }.execute(imdbId);
    }
}
