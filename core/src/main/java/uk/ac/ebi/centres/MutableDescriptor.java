/*
 * Copyright (c) 2012. John May
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA
 */

package uk.ac.ebi.centres;

import uk.ac.ebi.centres.descriptor.General;

/**
 * Holds a mutable descriptor value that can be passed around and set. This
 * allows centralisation and sharing of the descriptors so that when one centre
 * is perceived and it's descriptors. Access and mutation of the descriptor are
 * synchronized.
 *
 * @author John May
 */
public class MutableDescriptor {

    private Descriptor descriptor = General.UNKNOWN;


    public synchronized Descriptor get() {
        return descriptor;
    }


    public synchronized void set(Descriptor descriptor) {
        if (descriptor == null)
            throw new IllegalArgumentException("Provided descriptor was null");
        this.descriptor = descriptor;
    }


}
