/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.umass.lastfm.opensilk;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import de.umass.lastfm.CallException;
import de.umass.lastfm.Result;
import hugo.weaving.DebugLog;

/**
 * Base class for volley requests to lastfm
 *
 * Created by drew on 3/12/14.
 */
public abstract class MusicEntryRequest<T> extends Request<T> {

    private final Response.Listener<T> mListener;
    private Priority mPriority = Priority.NORMAL;

    public MusicEntryRequest(String url, MusicEntryResponseCallback<T> listener) {
        super(Method.GET, url, listener);
        mListener = listener;
    }

    /**
     * Implements Request abstract method
     * @param response Response from the network
     * @return success if we were able to parse the xml else error
     */
    @Override
    //@DebugLog
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        try {
            return Response.success(
                    buildEntry(createResultFromInputStream(new ByteArrayInputStream(response.data))),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (SAXException|IOException ignored) {
            // fall
        }
        return Response.error(new ParseError(new CallException()));
    }

    /**
     * Implemens Request abstract method
     * delivers the responce from parseNetworkResponse to the listener
     * @param response The parsed response returned by
     */
    @Override
    protected void deliverResponse(T response) {
        mListener.onResponse(response);
    }

    /**
     * Override default priority
     * @param newPriority
     */
    public void setPriority(Priority newPriority) {
        mPriority = newPriority;
    }

    @Override
    public Priority getPriority() {
        return mPriority;
    }

    /**
     * Creates object T from Result
     */
    protected abstract T buildEntry(Result result);

    /**
     * Builds LastFM result
     * @param inputStream
     * @return
     * @throws SAXException
     * @throws IOException
     */
    private Result createResultFromInputStream(InputStream inputStream) throws SAXException, IOException {
        Document document = newDocumentBuilder().parse(new InputSource(new InputStreamReader(inputStream, "UTF-8")));
        Element root = document.getDocumentElement(); // lfm element
        String statusString = root.getAttribute("status");
        Result.Status status = "ok".equals(statusString) ? Result.Status.OK : Result.Status.FAILED;
        if (status == Result.Status.FAILED) {
            throw new IOException("Status=FAILED");
//            Element errorElement = (Element) root.getElementsByTagName("error").item(0);
//            int errorCode = Integer.parseInt(errorElement.getAttribute("code"));
//            String message = errorElement.getTextContent();
//            return Result.createRestErrorResult(errorCode, message);
        } else {
            return Result.createOkResult(document);
        }
    }

    private DocumentBuilder newDocumentBuilder() {
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            return builderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            // better never happens
            throw new RuntimeException(e);
        }
    }

}
