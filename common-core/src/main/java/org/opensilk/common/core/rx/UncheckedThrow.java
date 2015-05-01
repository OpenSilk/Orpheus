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

package org.opensilk.common.core.rx;

/**
 * Hacks, don't use... ever!
 * I am only using this to rethrow RemoteExceptions inside Observables. So i
 * can use map() instead of flatMap(). Because i think its more efficient.
 *
 * <pre>
 *     ...flatMap(...) {
 *         try {
 *             return Observable.just(fooservice.do());
 *         } catch (RemoteException e) {
 *             return Observable.error(e);
 *         }
 *     }
 *     ...map(...) {
 *         try {
 *             return fooservice.do();
 *         } catch (RemoteExeption e) {
 *             //Fuck you Mr. Checked Exception
 *             rethrow(e);
 *         }
 *     }
 * </pre>
 *
 * http://www.gamlor.info/wordpress/2010/02/throwing-checked-excpetions-like-unchecked-exceptions-in-java/
 */
public final class UncheckedThrow {
    private UncheckedThrow(){}

    public static RuntimeException rethrow(final Exception ex){
        // Now we use the 'generic' method. Normally the type T is inferred
        // from the parameters. However you can specify the type also explicit!
        // Now we du just that! We use the RuntimeException as type!
        // That means the throwsUnchecked throws an unchecked exception!
        // Since the types are erased, no type-information is there to prevent this!
        throw UncheckedThrow.<RuntimeException>throwsUnchecked(ex);
    }

    /**
     * Remember, Generics are erased in Java. So this basically throws an Exception. The real
     * Type of T is lost during the compilation
     */
    public static <T extends Exception> T throwsUnchecked(Exception toThrow) throws T {
        // Since the type is erased, this cast actually does nothing!!!
        // we can throw any exception
        throw (T) toThrow;
    }
}
