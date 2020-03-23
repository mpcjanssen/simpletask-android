package com.thegrizzlylabs.sardineandroid.impl.handler;

import com.thegrizzlylabs.sardineandroid.impl.SardineException;
import com.thegrizzlylabs.sardineandroid.model.Prop;
import com.thegrizzlylabs.sardineandroid.util.SardineUtil;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by guillaume on 20/11/2017.
 */

public class LockResponseHandler extends ValidatingResponseHandler<String>
{
    @Override
    public String handleResponse(Response response) throws IOException {
        validateResponse(response);
        ResponseBody body = response.body();
        if (body == null) {
            throw new SardineException("No entity found in response", response.code(), response.message());
        }

        return getToken(body.byteStream());
    }

    /**
     * Helper method for getting the Multistatus response processor.
     *
     * @param stream The input to read the status
     * @return Multistatus element parsed from the stream
     * @throws java.io.IOException When there is a JAXB error
     */
    protected String getToken(InputStream stream) throws IOException {
        Prop prop = SardineUtil.unmarshal(Prop.class, stream);
        return prop.getLockdiscovery().getActivelock().iterator().next().getLocktoken().getHref();
    }
}
