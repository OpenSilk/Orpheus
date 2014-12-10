/*
 * Copyright 2014 Square Inc.
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

package org.opensilk.common.flow;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;
import android.view.View;

import org.opensilk.common.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class Screen implements Parcelable {
  private SparseArray<Parcelable> viewState;

  @Override public boolean equals(Object o) {
    return o != null && o instanceof Screen && this.getName().equals(((Screen) o).getName());
  }

  @Override public int hashCode() {
    return getName().hashCode();
  }

  public String getName() {
    return ObjectUtils.getClass(this).getName();
  }

  protected SparseArray<Parcelable> getViewState() {
    return viewState;
  }

  public void setViewState(SparseArray<Parcelable> viewState) {
    this.viewState = viewState;
  }

  public void restoreHierarchyState(View view) {
    if (getViewState() != null) {
      view.restoreHierarchyState(getViewState());
    }
  }

  protected void buildPath(List<Screen> path) {
  }

  public final List<Screen> getPath() {
    List<Screen> path = new ArrayList<>();
    buildPath(path);
    // For convenience, we don't require leaf classes to override buildPath().
    if (path.isEmpty() || isPathLeaf(path)) {
      path.add(this);
    }
    return path;
  }

  private boolean isPathLeaf(List<Screen> path) {
    return !equals(path.get(path.size() - 1));
  }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (viewState == null) {
            dest.writeInt(-1);
        } else {
            int N = viewState.size();
            dest.writeInt(N);
            for (int ii = 0; ii < N; ii++) {
                dest.writeInt(viewState.keyAt(ii));
                dest.writeParcelable(viewState.valueAt(ii), flags);
            }
        }
    }

    protected final void restoreFromParcel(Parcel in) {
        int N = in.readInt();
        if (N < 0) {
            viewState = null;
        } else {
            viewState = new SparseArray<>(N);
            while (N > 0) {
                int key = in.readInt();
                Parcelable value = in.readParcelable(null);
                viewState.append(key, value);
                N--;
            }
        }
    }
}
