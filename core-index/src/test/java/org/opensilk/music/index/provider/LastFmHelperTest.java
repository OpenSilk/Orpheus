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

package org.opensilk.music.index.provider;

import android.os.Build;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.index.BuildConfig;
import org.opensilk.music.index.IndexComponent;
import org.opensilk.music.index.IndexTestApplication;
import org.opensilk.music.index.database.IndexDatabase;
import org.opensilk.music.index.scanner.ScannerService;
import org.opensilk.music.library.sort.AlbumSortOrder;
import org.opensilk.music.library.sort.ArtistSortOrder;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.Artist;
import org.opensilk.music.model.Metadata;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ServiceController;

import java.util.List;

/**
 * Created by drew on 9/21/15.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(
        constants = BuildConfig.class,
        sdk = Build.VERSION_CODES.LOLLIPOP,
        application = IndexTestApplication.class
)
public class LastFmHelperTest {

    LastFMHelper mHelper;


    @Before
    public void setup() {
        IndexComponent cmp = DaggerService.getDaggerComponent(RuntimeEnvironment.application);
        mHelper = cmp.lastFMHelper();
    }

    @Test
    public void testArtistLookup() {
        Metadata meta = mHelper.lookupArtistInfo("foxes");
        Assertions.assertThat(meta).isNotNull();
    }

    @Test
    public void testAlbumLookup() {
        Metadata meta = mHelper.lookupAlbumInfo("foxes", "glorious");
        Assertions.assertThat(meta).isNotNull();
    }

//    @Test
//    public void testlookup() {
//        ServiceController<ScannerService> controller = Robolectric.buildService(ScannerService.class);
//        controller.create();
//        ScannerService service = controller.get();
//
//        long id = service.lookupArtistInfo("foxes");
//        Assertions.assertThat(id).isNotEqualTo(-1);
//        long id2 = service.lookupAlbumInfo("foxes", "glorious", id);
//        Assertions.assertThat(id2).isNotEqualTo(-1);
//        long id3 =service.mIndexDatabase.hasArtist("foxes");
//        Assertions.assertThat(id3).isNotEqualTo(-1);
//        long id4 = service.mIndexDatabase.hasAlbum("foxes", "glorious");
//        Assertions.assertThat(id4).isNotEqualTo(-1);
//
//        IndexComponent acc = DaggerService.getDaggerComponent(RuntimeEnvironment.application);
//        IndexDatabase db = acc.indexDatabase();
//        //make sure they show in the views
//        List<Album> albums = db.getAlbums(AlbumSortOrder.A_Z);
//        Assertions.assertThat(albums.size()).isEqualTo(1);
//        List<Artist> artists = db.getArtists(ArtistSortOrder.A_Z);
//        Assertions.assertThat(artists.size()).isEqualTo(1);
//    }

}
