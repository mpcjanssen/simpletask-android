package com.thegrizzlylabs.sardineandroid.impl.handler;

import android.util.Log;

import com.thegrizzlylabs.sardineandroid.DavResource;
import com.thegrizzlylabs.sardineandroid.model.Multistatus;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Response;

/**
 * Created by guillaume on 20/11/2017.
 */

public class ResourcesResponseHandler implements ResponseHandler<List<DavResource>> {

    private static final String TAG = ResourcesResponseHandler.class.getSimpleName();

    @Override
    public List<DavResource> handleResponse(Response response) throws IOException {
        Multistatus multistatus = new MultiStatusResponseHandler().handleResponse(response);
        List<com.thegrizzlylabs.sardineandroid.model.Response> davResponses = multistatus.getResponse();
        List<DavResource> resources = new ArrayList<>(davResponses.size());
        for (com.thegrizzlylabs.sardineandroid.model.Response davResponse : davResponses) {
            try {
                resources.add(new DavResource(davResponse));
            } catch (URISyntaxException e) {
                Log.w(TAG, String.format("Ignore resource with invalid URI %s", davResponse.getHref()/*.get(0)*/));
            }
        }
        return resources;
    }
}
