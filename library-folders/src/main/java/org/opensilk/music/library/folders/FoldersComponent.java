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

package org.opensilk.music.library.folders;

import org.opensilk.common.core.dagger2.AppContextComponent;
import org.opensilk.music.library.folders.provider.FoldersLibraryProvider;
import org.opensilk.music.library.folders.ui.StoragePickerActivity;

import dagger.Component;
import rx.functions.Func1;

/**
 * Created by drew on 4/28/15.
 */
@FoldersScope
@Component(
       dependencies = AppContextComponent.class
)
public interface FoldersComponent {
    Func1<AppContextComponent, FoldersComponent> FACTORY =
            new Func1<AppContextComponent, FoldersComponent>() {
                @Override
                public FoldersComponent call(AppContextComponent appContextComponent) {
                    return DaggerFoldersComponent.builder()
                            .appContextComponent(appContextComponent)
                            .build();
                }
            };
    void inject(FoldersLibraryProvider provider);
    void inject(StoragePickerActivity activity);
}
