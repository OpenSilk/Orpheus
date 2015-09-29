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

package org.opensilk.music.library.mediastore.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.opensilk.music.library.mediastore.util.FilesHelper.*;

/**
 * Created by drew on 11/11/14.
 */
@RunWith(RobolectricTestRunner.class)
@Config( manifest = Config.NONE)
public class FilesHelperTest {

    @Test
    public void testGetFileExtension() {
        File single = new File("/somepath/somefile.txt");
        assertThat(getFileExtension(single)).isEqualTo("txt");
        File douple = new File("/somepath/somefile.tar.gz");
        assertThat(getFileExtension(douple)).isEqualTo("tar.gz");
        File none = new File("/somepath/somefile");
        assertThat(getFileExtension(none)).isEqualTo("");
        File dots = new File("/somepath/somefile.with.dots.mp3");
        assertThat(getFileExtension(dots)).isEqualTo("mp3");
        File dotsdouble = new File("/somepath/samefile.with.dots.tar.gz");
        assertThat(getFileExtension(dotsdouble)).isEqualTo("tar.gz");
        File spaces = new File("/somepath/samefile with spaces.mp3");
        assertThat(getFileExtension(spaces)).isEqualTo("mp3");
    }

    @Test
    public void testtoRelativePath() {
        File base = new File("/data/media/0");
        File file = new File("/data/media/0/Music/somefile.mp3");
        assertThat(toRelativePath(base, file)).isEqualTo("Music/somefile.mp3");
        //make sure it reassembles to the same file
        assertThat(file).isEqualTo(new File(base, toRelativePath(base, file)));
    }
}
