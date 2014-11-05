/*
 * Copyright (c) 2014 OpenSilk Productions LLC
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

package org.opensilk.common.mortarflow;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by drew on 10/23/14.
 */
@Target(ElementType.TYPE) @Retention(RetentionPolicy.RUNTIME)
public @interface WithTransitions {

    final int SINGLE = 0;
    final int FORWARD = 1;
    final int BACKWARD = 2;
    final int REPLACE = 3;

    /**
     * one item array, our transition
     * Transition to use when we are the only screen changing
     * ie the screen switcher has no current child and we are
     *    being added.
     *    This will usually correspond to a resetTo() call
     *    which can can be either forward or backward but
     *    also applies to replaceTo() if no current screen
     *
     * Optional: If not set no animation will be used
     */
    int[] single() default {};

    /**
     * two item array, first is them (out) , second is us (in)
     * transitions to use when we are moving forward and replacing
     * another screen.
     * ie we are being added, they are being removed
     */
    int[] forward();

    /**
     * two item array, first is them (out) , second is us (in)
     * transitions to use when we are moving backward and being
     * replaced by another screen
     * ie stack is popped and we are being replaced by our
     *    predecessor
     */
    int[] backward();

    /**
     * two item array, first is them (out) , second is us (in)
     * transition to use when we are replacing another screen
     * corresponding to the replaceTo() method. which has
     * no direction and merits its own transition
     *
     * Optional if you don't intend to use replaceTo()
     */
    int[] replace() default {};
}
