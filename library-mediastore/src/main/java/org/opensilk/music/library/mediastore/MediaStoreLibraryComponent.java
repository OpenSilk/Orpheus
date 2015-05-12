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

package org.opensilk.music.library.mediastore;

import org.opensilk.common.core.dagger2.AppContextComponent;
import org.opensilk.music.library.mediastore.provider.MediaStoreLibraryProvider;
import org.opensilk.music.library.mediastore.ui.StoragePickerActivity;

import dagger.Component;
import rx.functions.Func1;

/**
 * Created by drew on 5/4/15.
 */
@MediaStoreLibraryScope
@Component(
        dependencies = AppContextComponent.class,
        modules = {
                MediaStoreLibraryAuthorityModule.class,
        }
)
public interface MediaStoreLibraryComponent extends AppContextComponent {
    Func1<AppContextComponent, MediaStoreLibraryComponent> FACTORY =
            new Func1<AppContextComponent, MediaStoreLibraryComponent>() {
                @Override
                public MediaStoreLibraryComponent call(AppContextComponent appContextComponent) {
                    return DaggerMediaStoreLibraryComponent.builder()
                            .appContextComponent(appContextComponent)
                            .build();
                }
            };
    void inject(MediaStoreLibraryProvider provider);
    void inject(StoragePickerActivity activity);
}
