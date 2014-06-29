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

package org.opensilk.music.util;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.widget.Toast;

/**
 * Created by drew on 6/28/14.
 */
public class CommandRunner extends AsyncTask<Void, Void, CharSequence> {

    private Context context;
    private Command command;

    public CommandRunner(Context context, Command command) {
        this.context = context;
        this.command = command;
    }

    @Override
    protected CharSequence doInBackground(Void... params) {
        return command.execute();
    }

    @Override
    protected void onPostExecute(CharSequence charSequence) {
        if (!TextUtils.isEmpty(charSequence)) {
            Toast.makeText(context, charSequence, Toast.LENGTH_SHORT);
        }
    }
}
