package com.thegrizzlylabs.sardineandroid.impl.handler;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.Response;

/**
 * Created by guillaume on 20/11/2017.
 */

public class InputStreamResponseHandler extends ValidatingResponseHandler<InputStream> {

    @Override
    public InputStream handleResponse(Response response) throws IOException {
        validateResponse(response);
        return response.body().byteStream();
    }
}
