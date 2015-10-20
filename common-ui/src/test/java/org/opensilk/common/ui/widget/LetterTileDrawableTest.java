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

package org.opensilk.common.ui.widget;

import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.*;

/**
 * Created by drew on 10/2/15.
 */
@RunWith(RobolectricTestRunner.class)
@Config( manifest = Config.NONE)
public class LetterTileDrawableTest {

    @Test
    public void testFindFirstUsableLetter() {
        char c = LetterTileDrawable.findFirstUsableCharacter("@$$%#2138457");
        assertThat(c).isEqualTo('2');
        c = LetterTileDrawable.findFirstUsableCharacter("★");
        assertThat(c).isEqualTo('@');
        c = LetterTileDrawable.findFirstUsableCharacter("drew");
        assertThat(c).isEqualTo('d');
        c = LetterTileDrawable.findFirstUsableCharacter("01 File 1");
        assertThat(c).isEqualTo('1');
        c = LetterTileDrawable.findFirstUsableCharacter("001 File 1");
        assertThat(c).isEqualTo('1');
        c = LetterTileDrawable.findFirstUsableCharacter("000 Track 0");
        assertThat(c).isEqualTo('0');
        c = LetterTileDrawable.findFirstUsableCharacter("000");
        assertThat(c).isEqualTo('0');
        c = LetterTileDrawable.findFirstUsableCharacter("00");
        assertThat(c).isEqualTo('0');
        c = LetterTileDrawable.findFirstUsableCharacter("0");
        assertThat(c).isEqualTo('0');
        c = LetterTileDrawable.findFirstUsableCharacter("0-File1");
        assertThat(c).isEqualTo('0');
        c = LetterTileDrawable.findFirstUsableCharacter("00File1");
        assertThat(c).isEqualTo('0');
        c = LetterTileDrawable.findFirstUsableCharacter("0File1");
        assertThat(c).isEqualTo('0');
        c = LetterTileDrawable.findFirstUsableCharacter("000.txt");
        assertThat(c).isEqualTo('0');
    }

    @Test
    public void testFindSecondDigit() {
        assertThat(LetterTileDrawable.findSecondDigit("10", '1')).isEqualTo('0');
        assertThat(LetterTileDrawable.findSecondDigit("01", '1')).isEqualTo('@');
        assertThat(LetterTileDrawable.findSecondDigit("100", '1')).isEqualTo('0');
        assertThat(LetterTileDrawable.findSecondDigit("01 ", '1')).isEqualTo('@');
        assertThat(LetterTileDrawable.findSecondDigit("012310", '1')).isEqualTo('2');
    }
}
