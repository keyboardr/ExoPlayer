package com.keyboardr.dancedj.util;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

public abstract class CachedLoader<D> extends AsyncTaskLoader<D> {

    private D mResult;

    public CachedLoader(Context context) {
        super(context);
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        if (mResult != null) {
            deliverResult(mResult);
        }
        if (takeContentChanged() || mResult == null) {
            forceLoad();
        }

    }

    @Override
    public void deliverResult(D data) {
        mResult = data;
        if (isStarted()) {
            super.deliverResult(data);
        }
    }

    @Override
    protected void onStopLoading() {
        super.onStopLoading();
        cancelLoad();
    }

    @Override
    protected void onReset() {
        super.onReset();
        onStopLoading();
        mResult = null;
    }

}