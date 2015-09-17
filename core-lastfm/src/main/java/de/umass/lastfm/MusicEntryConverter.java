/*
 * Copyright (c) 2015 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.umass.lastfm;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import retrofit.Converter;

/**
 * Created by drew on 9/16/15.
 */
public abstract class MusicEntryConverter<T> implements Converter<T> {

    public static Result createResultFromInputStream(InputStream inputStream) throws SAXException, IOException {
        Document document = newDocumentBuilder().parse(new InputSource(new InputStreamReader(inputStream, "UTF-8")));
        Element root = document.getDocumentElement(); // lfm element
        String statusString = root.getAttribute("status");
        Result.Status status = "ok".equals(statusString) ? Result.Status.OK : Result.Status.FAILED;
        if (status == Result.Status.FAILED) {
            Element errorElement = (Element) root.getElementsByTagName("error").item(0);
            int errorCode = Integer.parseInt(errorElement.getAttribute("code"));
            String message = errorElement.getTextContent();
            return Result.createRestErrorResult(errorCode, message);
        } else {
            return Result.createOkResult(document);
        }
    }

    public static DocumentBuilder newDocumentBuilder() {
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            return builderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            // better never happens
            throw new RuntimeException(e);
        }
    }

}
