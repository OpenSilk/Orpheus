/*
 * Copyright (c) 2014 OpenSilk Productions LLC
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

package org.opensilk.upnp.contentdirectory;

import org.fourthline.cling.model.types.Datatype;
import org.fourthline.cling.support.model.DescMeta;
import org.seamless.xml.SAXParser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.StringReader;
import java.net.URI;

/**
 * Parse a FeatureList response
 *
 * example input (and only one i tested):
 * <pre>
 *      <Features xmlns="urn:schemas-upnp-org:av:avs" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *              xsi:schemaLocation="urn:schemas-upnp-org:av:avs http://www.upnp.org/schemas/av/avs.xsd">
 *          <Feature name="samsung.com_BASICVIEW" version="1">
 *              <container id="1" type="object.item.audioItem"/>
 *              <container id="2" type="object.item.videoItem"/>
 *              <container id="3" type="object.item.imageItem"/>
 *          </Feature>
 *      </Features>
 * </pre>
 *
 * Created by drew on 6/18/14.
 */
public class FeaturesParser extends SAXParser {

    public Features parse(String xml) throws Exception {
        if (xml == null || xml.length() == 0) {
            throw new RuntimeException("Null or empty XML");
        }
        Features features = new Features();
        createRootHandler(features, this);
        parse(new InputSource(new StringReader(xml)));
        return features;
    }

    protected RootHandler createRootHandler(Features instance, SAXParser parser) {
        return new RootHandler(instance, parser);
    }

    protected FeatureHandler createFeatureHandler(Feature instance, Handler parent) {
        return new FeatureHandler(instance, parent);
    }

    protected Feature createFeature(Attributes attributes) {
        Feature feature = new Feature();

        feature.setName(attributes.getValue("name"));

        Integer version = (Integer) Datatype.Builtin.INT.getDatatype().valueOf(attributes.getValue("version"));
        if (version != null) {
            feature.setVersion(version);
        }

        return feature;
    }

    protected DescMeta createDescMeta(Attributes attributes) {
        DescMeta desc = new DescMeta();

        desc.setId(attributes.getValue("id"));

        if ((attributes.getValue("type") != null))
            desc.setType(attributes.getValue("type"));

        if ((attributes.getValue("nameSpace") != null))
            desc.setNameSpace(URI.create(attributes.getValue("nameSpace")));

        return desc;
    }

    public class RootHandler extends Handler<Features> {

        RootHandler(Features instance, SAXParser parser) {
            super(instance, parser);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            super.startElement(uri, localName, qName, attributes);

            if (!Features.NAMESPACE.equals(uri)) return;

            if (localName.equals("Feature")) {
                Feature feature = createFeature(attributes);
                getInstance().addFeature(feature);
                createFeatureHandler(feature, this);
            }

        }

        @Override
        protected boolean isLastElement(String uri, String localName, String qName) {
            return Features.NAMESPACE.equals(uri) && "Features".equals(localName);
        }
    }

    public class FeatureHandler extends Handler<Feature> {

        FeatureHandler(Feature instance, Handler parent) {
            super(instance, parent);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            super.startElement(uri, localName, qName, attributes);

            if (!Features.NAMESPACE.equals(uri)) return;

            if (localName.equals("container")) {
                DescMeta descMeta = createDescMeta(attributes);
                getInstance().addMeta(descMeta);
            }

        }

        @Override
        protected boolean isLastElement(String uri, String localName, String qName) {
            return Features.NAMESPACE.equals(uri) && "Feature".equals(localName);
        }
    }

}
