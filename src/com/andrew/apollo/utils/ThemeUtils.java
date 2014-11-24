/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

import org.opensilk.music.BuildConfig;
import org.opensilk.music.R;

/**
 * In order to implement the theme chooser for Apollo, this class returns a
 * {@link Resources} object that can be used like normal. In other words, when
 * {@code getDrawable()} or {@code getColor()} is called, the object returned is
 * from the current theme package name and because all of the theme resource
 * identifiers are the same as all of Apollo's resources a little less code is
 * used to implement the theme chooser.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ThemeUtils {

    /**
     * Used to searc the "Apps" section of the Play Store for "Apollo Themes".
     */
    private static final String SEARCH_URI = "https://market.android.com/search?q=%s&c=apps&featured=APP_STORE_SEARCH";

    /**
     * Used to search the Play Store for a specific theme.
     */
    private static final String APP_URI = "market://details?id=";

    /**
     * Default package name.
     */
    public static final String APOLLO_PACKAGE = BuildConfig.PACKAGE_NAME;

    /**
     * Current theme package name.
     */
    public static final String PACKAGE_NAME = "theme_package_name";

    /**
     * Used to get and set the theme package name.
     */
    private final SharedPreferences mPreferences;

    /**
     * The theme package name.
     */
    private final String mThemePackage;

    /**
     * The keyword to use when search for different themes.
     */
    private static String sApolloSearch;

    /**
     * This is the current theme color as set by the color picker.
     */
    private final int mCurrentThemeColor;

    /**
     * Package manager
     */
    private final PackageManager mPackageManager;

    /**
     * The theme resources.
     */
    private Resources mResources;

    /**
     * Constructor for <code>ThemeUtils</code>
     * 
     * @param context The {@link Context} to use.
     */
    public ThemeUtils(final Context context) {
        // Get the preferences
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        // Get the theme package name
        mThemePackage = getThemePackageName();
        // Initialze the package manager
        mPackageManager = context.getPackageManager();
        try {
            // Find the theme resources
            mResources = mPackageManager.getResourcesForApplication(mThemePackage);
        } catch (final Exception e) {
            // If the user isn't using a theme, then the resources should be
            // Apollo's.
            setThemePackageName(APOLLO_PACKAGE);
        }
        // Get the current theme color
        mCurrentThemeColor = android.R.color.holo_blue_dark;
    }

    /**
     * Set the new theme package name.
     *
     * @param packageName The package name of the theme to be set.
     */
    public void setThemePackageName(final String packageName) {
        mPreferences.edit().putString(PACKAGE_NAME, packageName).apply();
    }

    /**
     * Return the current theme package name.
     *
     * @return The default theme package name.
     */
    public final String getThemePackageName() {
        return mPreferences.getString(PACKAGE_NAME, APOLLO_PACKAGE);
    }

    /**
     * Used to return a color from the theme resources.
     * 
     * @param resourceName The name of the color to return. i.e.
     *            "action_bar_color".
     * @return A new color from the theme resources.
     */
    public int getColor(final String resourceName) {
        final int resourceId = mResources.getIdentifier(resourceName, "color", mThemePackage);
        try {
            return mResources.getColor(resourceId);
        } catch (final Resources.NotFoundException e) {
            // If the theme designer wants to allow the user to theme a
            // particular object via the color picker, they just remove the
            // resource item from the themeconfig.xml file.
        }
        return mCurrentThemeColor;
    }

    /**
     * Used to return a drawable from the theme resources.
     * 
     * @param resourceName The name of the drawable to return. i.e.
     *            "pager_background".
     * @return A new color from the theme resources.
     */
    public Drawable getDrawable(final String resourceName) {
        final int resourceId = mResources.getIdentifier(resourceName, "drawable", mThemePackage);
        try {
            return mResources.getDrawable(resourceId);
        } catch (final Resources.NotFoundException e) {
            //$FALL-THROUGH$
        }
        return null;
    }

    /**
     * This is used to set the color of a {@link MenuItem}. For instance, when
     * the current song is a favorite, the favorite icon will use the current
     * theme color.
     * 
     * @param menuItem The {@link MenuItem} to set.
     * @param resourceColorName The color theme resource key.
     * @param resourceDrawableName The drawable theme resource key.
     */
    public void setMenuItemColor(final MenuItem menuItem, final String resourceColorName,
            final String resourceDrawableName) {

        final Drawable maskDrawable = getDrawable(resourceDrawableName);
        if (!(maskDrawable instanceof BitmapDrawable)) {
            return;
        }

        final Bitmap maskBitmap = ((BitmapDrawable)maskDrawable).getBitmap();
        final int width = maskBitmap.getWidth();
        final int height = maskBitmap.getHeight();

        final Bitmap outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(outBitmap);
        canvas.drawBitmap(maskBitmap, 0, 0, null);

        final Paint maskedPaint = new Paint();
        maskedPaint.setColor(getColor(resourceColorName));
        maskedPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));

        canvas.drawRect(0, 0, width, height, maskedPaint);

        final BitmapDrawable outDrawable = new BitmapDrawable(mResources, outBitmap);
        menuItem.setIcon(outDrawable);
    }

    /**
     * Used to search the Play Store for a specific app.
     *
     * @param context The {@link Context} to use.
     * @param themeName The theme name to search for.
     */
    public static void openAppPage(final Context context, final String themeName) {
        final Intent shopIntent = new Intent(Intent.ACTION_VIEW);
        shopIntent.setData(Uri.parse(APP_URI + themeName));
        shopIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shopIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(shopIntent);
    }
}
