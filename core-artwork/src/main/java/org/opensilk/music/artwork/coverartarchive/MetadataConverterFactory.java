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

package org.opensilk.music.artwork.coverartarchive;

import android.util.JsonReader;

import com.squareup.okhttp.ResponseBody;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts CAA json response to Metadata. Doing this manually so i can hopefully
 * remove Gson as a dependency
 *
 * Created by drew on 10/4/15.
 */
public class MetadataConverterFactory extends retrofit.Converter.Factory {

    private final Converter converter = new Converter();

    @Override
    public retrofit.Converter<ResponseBody, ?> fromResponseBody(Type type, Annotation[] annotations) {
        if (type == Metadata.class) {
            return converter;
        } else {
            return super.fromResponseBody(type, annotations);
        }
    }

    static class Converter implements retrofit.Converter<ResponseBody, Metadata> {
        public Metadata convert(ResponseBody value) throws IOException {
            try {
                final List<Metadata.Image> images = new ArrayList<>();
                String release = null;
                JsonReader reader = new JsonReader(value.charStream());
                reader.beginObject();
                while (reader.hasNext()) {
                    final String nextName = reader.nextName();
                    switch (nextName) {
                        case "images":
                            images.addAll(parseImages(reader));
                            break;
                        case "release":
                            release = reader.nextString();
                            break;
                        default:
                            reader.skipValue();
                            break;
                    }
                }
                return new Metadata(release, images);
            } finally {
                value.close();
            }
        }
    }


    private static List<Metadata.Image> parseImages(JsonReader reader) throws IOException {
        List<Metadata.Image> images = new ArrayList<>();
        reader.beginArray();
        while (reader.hasNext()) {
            images.add(parseImage(reader));
        }
        reader.endArray();
        return images;
    }

    private static Metadata.Image parseImage(JsonReader reader) throws IOException {
        String image = null;
        boolean front = false;
        boolean approved = false;
        reader.beginObject();
        while (reader.hasNext()) {
            String nextName = reader.nextName();
            switch (nextName) {
                case "image":
                    image = reader.nextString();
                    break;
                case "front":
                    front = reader.nextBoolean();
                    break;
                case "approved":
                    approved = reader.nextBoolean();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        return new Metadata.Image(image, front, approved);
    }
}
