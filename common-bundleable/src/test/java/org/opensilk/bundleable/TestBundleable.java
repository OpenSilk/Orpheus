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

package org.opensilk.bundleable;

import android.os.Bundle;

/**
 * Created by drew on 10/2/15.
 */
public class TestBundleable implements Bundleable {
    final String name;
    final int age;

    public TestBundleable(String name, int age) {
        this.name = name;
        this.age = age;
    }

    @Override
    public Bundle toBundle() {
        Bundle b = new Bundle(2);
        b.putString(CLZ, TestBundleable.class.getName());
        b.putString("name", name);
        b.putInt("age", age);
        return b;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestBundleable that = (TestBundleable) o;
        if (age != that.age) return false;
        return name.equals(that.name);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + age;
        return result;
    }

    public static final BundleCreator<TestBundleable> BUNDLE_CREATOR =
            new BundleCreator<TestBundleable>() {
                @Override
                public TestBundleable fromBundle(Bundle b) throws IllegalArgumentException {
                    if (!TestBundleable.class.getName().equals(b.getString(CLZ, TestBundleable.class.getName()))) {
                        throw new IllegalArgumentException("Invalid class");
                    }
                    final String name = b.getString("name");
                    final int age = b.getInt("age");
                    return new TestBundleable(name, age);
                }
            };
}
