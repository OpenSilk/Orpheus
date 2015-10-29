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

package org.opensilk.music.ui3.renderer;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.music.ui3.MusicActivityComponent;

import dagger.Component;
import rx.functions.Func2;

/**
 * Created by drew on 10/27/15.
 */
@ScreenScope
@Component(
        dependencies = MusicActivityComponent.class,
        modules = RendererScreenModule.class
)
public interface RendererScreenComponent {
    Func2<MusicActivityComponent, RendererScreen, RendererScreenComponent> FACTORY =
            new Func2<MusicActivityComponent, RendererScreen, RendererScreenComponent>() {
                @Override
                public RendererScreenComponent call(MusicActivityComponent musicActivityComponent, RendererScreen rendererScreen) {
                    return DaggerRendererScreenComponent.builder()
                            .musicActivityComponent(musicActivityComponent)
                            .rendererScreenModule(new RendererScreenModule(rendererScreen))
                            .build();
                }
            };
    void inject(RendererScreenView view);
}
