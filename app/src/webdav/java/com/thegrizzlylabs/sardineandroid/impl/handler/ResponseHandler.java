package com.thegrizzlylabs.sardineandroid.impl.handler;

import java.io.IOException;

import okhttp3.Response;

/**
 * Created by guillaume on 20/11/2017.
 */

public interface ResponseHandler<T> {

    T handleResponse(Response response) throws IOException;
}
