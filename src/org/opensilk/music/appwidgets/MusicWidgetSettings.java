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

package org.opensilk.music.appwidgets;

import android.app.ActionBar;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.ThemeHelper;

/**
 * Created by andrew on 4/3/14.
 */
public class MusicWidgetSettings extends Activity implements AdapterView.OnItemSelectedListener {

    public static final String PREFS_NAME = "WidgetPreferences";
    public static final String PREF_PREFIX_KEY = "pref_id_";
    private static final int DEFAULT_INDEX = 0;
    private static final int DEFAULT_PREVIEW_IMG = R.drawable.widget_preview_large_one;

    private Spinner mSpinner;
    private ImageView mPreviewImage;
    private Context mContext;
    private TypedArray mPreviewIds;

    private int mAppWidgetId = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.widget_settings);

        ActionBar actionBar = getActionBar();
        actionBar.setIcon(R.drawable.ic_action_cancel_white);
        actionBar.setHomeButtonEnabled(true);

        mContext = this;
        setResult(RESULT_CANCELED);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        mPreviewIds = getResources().obtainTypedArray(R.array.widget_large_style_previews);

        mSpinner = (Spinner) findViewById(R.id.widget_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.widget_large_style_labels, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);

        mSpinner.setOnItemSelectedListener(this);

        mPreviewImage = (ImageView) findViewById(R.id.widget_preview);
        /* Default widget is Simple */
        mSpinner.setSelection(DEFAULT_INDEX);
        mPreviewImage.setImageResource(DEFAULT_PREVIEW_IMG);

        Button okayButton = (Button) findViewById(R.id.button_okay);
        okayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SharedPreferences.Editor prefs = mContext.getSharedPreferences(PREFS_NAME,
                        Context.MODE_MULTI_PROCESS).edit();
                prefs.putInt(PREF_PREFIX_KEY + mAppWidgetId, mSpinner.getSelectedItemPosition());
                prefs.apply();

                Intent intent = new Intent(mContext, MusicWidgetService.class);
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                intent.putExtra(MusicWidgetService.WIDGET_TYPE, MusicWidget.LARGE.ordinal());
                mContext.startService(intent);

                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
            }
        });

        Button cancelButton = (Button) findViewById(R.id.button_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_CANCELED);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mPreviewImage.setImageResource(mPreviewIds.getResourceId(position, DEFAULT_PREVIEW_IMG));
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) { }
}
