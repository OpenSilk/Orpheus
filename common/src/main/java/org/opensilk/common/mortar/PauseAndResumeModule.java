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

package org.opensilk.common.mortar;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by drew on 10/15/14.
 */
@Module(
        library = true
)
public class PauseAndResumeModule {
    @Provides @Singleton PauseAndResumePresenter providePauseAndResumePresenter() { return new PauseAndResumePresenter(); }
    @Provides @Singleton PauseAndResumeRegistrar providePauseAndResumeRegistar(PauseAndResumePresenter presenter) { return presenter; }
}
