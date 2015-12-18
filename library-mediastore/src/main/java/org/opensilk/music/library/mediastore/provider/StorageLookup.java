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

package org.opensilk.music.library.mediastore.provider;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.text.TextUtils;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.music.library.mediastore.provider.FoldersLibraryScope;
import org.opensilk.music.library.mediastore.R;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

/**
 * Created by drew on 4/28/15.
 */
@FoldersLibraryScope
public class StorageLookup {

    public static final int DEFAULT_PRIMARY_STORAGE_ID = 0;

    private static final boolean DUMPSTACKS = true;

    final Context appContext;

    private String[] storagePaths;
    private List<StorageVolume> storageVolumes;

    @Inject
    public StorageLookup(
            @ForApplication Context appContext
    ) {
        this.appContext = appContext;
    }

    public static class StorageVolume {
        public final String path;
        public final String description;
        public final int id;
        public StorageVolume(String path, String description, int id) {
            this.path = path;
            this.description = description;
            this.id = id;
        }

        @Override
        public String toString() {
            return "Volume{id="+id+", desc="+description+", path="+path+"}";
        }
    }

    List<StorageVolume> lookupStorageVolumes() {
        try {
            StorageManager sm = (StorageManager) appContext.getSystemService(Context.STORAGE_SERVICE);
            Method getVolumeList = StorageManager.class.getDeclaredMethod("getVolumeList");
            Class<?> volume = Class.forName("android.os.storage.StorageVolume");
            Method getPath =  volume.getDeclaredMethod("getPath");
            Method getDescription;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                getDescription = volume.getDeclaredMethod("getDescription", Context.class);
            }  else {
                getDescription = volume.getDeclaredMethod("getDescription");
            }
            Method getStorageId = volume.getDeclaredMethod("getStorageId");
            Object[] volumes = (Object[]) getVolumeList.invoke(sm);
            List<StorageVolume> storages = new ArrayList<>(volumes.length);
            for (Object v : volumes) {
                StorageVolume storageVolume;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    storageVolume = new StorageVolume((String) getPath.invoke(v),
                            (String) getDescription.invoke(v, appContext),
                            (Integer) getStorageId.invoke(v));
                } else {
                    storageVolume = new StorageVolume((String) getPath.invoke(v),
                            (String) getDescription.invoke(v),
                            (Integer) getStorageId.invoke(v));
                }
                Timber.d("Found volume %s", storageVolume);
                storages.add(storageVolume);
            }
            return storages;
        } catch (Exception e) {
            if (DUMPSTACKS) Timber.e(e, "lookupStorageVolumes");
        }
        Timber.w("Failed to get storage paths via reflection");
        return Collections.emptyList();
    }

    List<StorageVolume> getStorageVolumesInternal() {
        List<StorageVolume> volumes = lookupStorageVolumes();
        if (!volumes.isEmpty()) {
            for (StorageVolume v : volumes) {
                if (StringUtils.equals(v.path, getPrimaryStoragePath())){
                    //has primary all good.
                    return volumes;
                }
            }
            Timber.w("Primary storage not found in volumes...");
        }
        return Collections.singletonList(
                new StorageVolume(getPrimaryStoragePath(),
                        appContext.getString(R.string.folders_storage_primary),
                        DEFAULT_PRIMARY_STORAGE_ID
                )
        );
    }

    public List<StorageVolume> getStorageVolumes() {
        if (storageVolumes == null) {
            synchronized (this) {
                if (storageVolumes == null) {
                    storageVolumes = getStorageVolumesInternal();
                }
            }
        }
        return storageVolumes;
    }

    public StorageVolume getStorageVolume(String id) {
        int sid = Integer.valueOf(id);
        List<StorageVolume> storages = getStorageVolumes();
        for (StorageVolume v : storages) {
            if (v.id == sid) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown id "+sid);
    }

    public File getStorageFile(String id) {
        return new File(getStorageVolume(id).path);
    }

    @Deprecated
    public String[] getStoragePaths() {
        if (storagePaths == null) {
            synchronized (this) {
                if (storagePaths == null) {
                    storagePaths = lookupStoragePaths();
                }
            }
        }
        return storagePaths;
    }

    String[] lookupStoragePaths() {
        final String[] paths;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            paths = getStoragePathsKK();
        } else {
            paths = getStoragePathsCompat();
        }
        final String primary = getPrimaryStoragePath();
        if (paths == null || paths.length <= 1) {
            //Even if we found a path if theres only one, just use primary storage
            //to ensure we use the right one
            return new String[]{primary};
        }
        if (paths.length == 2) {
            if (TextUtils.equals(paths[0], primary)) {
                //Primary is first.. all good
                return paths;
            } else if (TextUtils.equals(paths[1], primary)) {
                //Reverse them to put primary first
                return new String[]{paths[1], paths[0]};
            } else {
                //Unexpected situation
                Timber.w("%s not in %s... Using %s", primary, Arrays.toString(paths), primary);
            }
        } else {
            //Unexpected situation;
            Timber.w("Device has more that two storage volumes. This isn't supported... Using %s", primary);
        }
        return new String[]{primary};
    }

    String[] getStoragePathsKK() {
        try {
            Field scu = Environment.class.getDeclaredField("sCurrentUser");
            scu.setAccessible(true);
            Object cu = scu.get(null);
            Class<?> ue = Class.forName("android.os.Environment$UserEnvironment");
            Method getExternalDirsForApp = ue.getDeclaredMethod("getExternalDirsForApp");
            Object[] dirs = (Object[]) getExternalDirsForApp.invoke(cu);
            String[] paths = new String[dirs.length];
            for (int ii=0; ii<dirs.length; ii++) {
                paths[ii] = ((File)dirs[ii]).getPath();
            }
            return paths;
        } catch (Exception e) {
            if (DUMPSTACKS) Timber.e(e, "getStoragePathsKK", e);
        }
        return getStoragePathsCompat();
    }

    String[] getStoragePathsCompat() {
        try {
            StorageManager sm = (StorageManager) appContext.getSystemService(Context.STORAGE_SERVICE);
            Method getVolumeList = StorageManager.class.getDeclaredMethod("getVolumeList");
            Class<?> volume = Class.forName("android.os.storage.StorageVolume");
            Method getPath =  volume.getDeclaredMethod("getPath");
            Object[] volumes = (Object[]) getVolumeList.invoke(sm);
            String[] paths = new String[volumes.length];
            for (int ii=0; ii<volumes.length; ii++) {
                paths[ii] = (String) getPath.invoke(volumes[ii]);
            }
            return paths;
        } catch (Exception e) {
            if (DUMPSTACKS) Timber.e(e, "getStoragePathsCompat");
        }
        Timber.w("Failed to get storage paths via reflection");
        return new String[0];
    }

    String getPrimaryStoragePath() {
        return Environment.getExternalStorageDirectory().getPath();
    }
}
