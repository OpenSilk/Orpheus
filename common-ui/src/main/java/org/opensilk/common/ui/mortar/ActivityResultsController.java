/*
 * Copyright (C) 2015 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.common.ui.mortar;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import mortar.MortarScope;

/**
 * Created by drew on 3/10/15.
 */
public interface ActivityResultsController {
    void register(MortarScope scope, ActivityResultsListener listener);
    void startActivityForResult(Intent intent, int requestCode, @Nullable Bundle options);
    void setResultAndFinish(int resultCode, @Nullable Intent data);
}
