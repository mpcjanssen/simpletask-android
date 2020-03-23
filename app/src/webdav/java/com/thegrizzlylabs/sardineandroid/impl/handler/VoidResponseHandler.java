package com.thegrizzlylabs.sardineandroid.impl.handler;

/**
 * Created by guillaume on 20/11/2017.
 */

import java.io.IOException;

import okhttp3.Response;

/**
 * {@link ResponseHandler} which just executes the request and checks the answer is
 * in the valid range of {@link ValidatingResponseHandler#validateResponse(okhttp3.Response)}.
 *
 * @author mirko
 */
public class VoidResponseHandler extends ValidatingResponseHandler<Void>
{
    @Override
    public Void handleResponse(Response response) throws IOException {
        validateResponse(response);
        return null;
    }
}
