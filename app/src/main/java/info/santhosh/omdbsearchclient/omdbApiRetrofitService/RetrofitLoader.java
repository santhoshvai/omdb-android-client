package info.santhosh.omdbsearchclient.omdbApiRetrofitService;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import java.io.IOException;

/**
 * Created by santhoshvai on 27/04/16.
 */
public class RetrofitLoader extends AsyncTaskLoader<searchService.ResultWithDetail> {

    private static final String LOG_TAG = "RetrofitLoader";

    private final String mTitle;

    private searchService.ResultWithDetail mData;

    public RetrofitLoader(Context context, String title) {
        super(context);
        mTitle = title;
    }

    @Override
    public searchService.ResultWithDetail loadInBackground() {
        Log.d(LOG_TAG, "loadInBackground");
        // get results from calling API
        try {
            searchService.Result result =  searchService.performSearch(mTitle);
            searchService.ResultWithDetail resultWithDetail = new searchService.ResultWithDetail(result);
            for(searchService.Movie movie: result.Search) {
                resultWithDetail.addToList(searchService.getDetail(movie.imdbID));
            }
            return  resultWithDetail;
        } catch(final IOException e) {
            Log.e(LOG_TAG, "Error from api access", e);
        }
        return null;
    }

    @Override
    protected void onStartLoading() {
        if (mData != null) {
            // Deliver any previously loaded data immediately.
            deliverResult(mData); // makes loaderManager call onLoadFinished
        } else {
            forceLoad(); // calls the loadInBackground
        }
    }


    @Override
    protected void onReset() {
        super.onReset();
        mData = null;
    }
}
