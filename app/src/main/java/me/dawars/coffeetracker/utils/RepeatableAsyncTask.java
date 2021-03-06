package me.dawars.coffeetracker.utils;

import android.os.AsyncTask;

public abstract class RepeatableAsyncTask<A, B, C> extends AsyncTask<A, B, C> {
    public static final int DEFAULT_MAX_RETRY = 5;
    private static final String TAG = "RepeatableAsyncTask";
    private int mMaxRetries = DEFAULT_MAX_RETRY;
    private Exception mException = null;

    /**
     * Default constructor
     */
    public RepeatableAsyncTask() {
        super();
    }

    /**
     * Constructs an AsyncTask that will repeate itself for max Retries
     *
     * @param retries Max Retries.
     */
    public RepeatableAsyncTask(int retries) {
        super();
        mMaxRetries = retries;
    }

    /**
     * Will be repeated for max retries while the result is null or an exception is thrown.
     *
     * @param inputs Same as AsyncTask's
     * @return Same as AsyncTask's
     */
    protected abstract C repeatInBackground(A... inputs);

    @Override
    protected final C doInBackground(A... inputs) {
        int tries = 0;
        C result = null;

        /* This is the main loop, repeatInBackground will be repeated until result will not be null */
        while (tries++ < mMaxRetries && result == null) {
            try {
                result = repeatInBackground(inputs);
            } catch (Exception exception) {
                /* You might want to log the exception everytime, do it here. */
                mException = exception;
            }
        }
        return result;
    }

    /**
     * Like onPostExecute but will return an eventual Exception
     *
     * @param c         Result same as AsyncTask
     * @param exception Exception thrown in the loop, even if the result is not null.
     */
    protected abstract void onPostExecute(C c, Exception exception);

    @Override
    protected final void onPostExecute(C c) {
        super.onPostExecute(c);
        onPostExecute(c, mException);
    }
}