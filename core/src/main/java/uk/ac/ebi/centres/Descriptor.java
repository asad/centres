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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package uk.ac.ebi.centres;

/**
 * Defines a descriptor which can be assigned to an atom to indicate the type of
 * chirality (if there is any). Each descriptor defines it's general @{link
 * Type} which can be useful when comparing centres of different geometry.
 *
 * @author John May
 * @see uk.ac.ebi.centres.descriptor.Tetrahedral
 * @see uk.ac.ebi.centres.descriptor.Trigonal
 * @see uk.ac.ebi.centres.descriptor.Planar
 */
public interface Descriptor {

    /**
     * Defines the type of the descriptor.
     */
    public enum Type {

        /**
         * An asymmetric stereo descriptor
         */
        ASYMMETRIC,

        /**
         * A pseudo-asymmetric descriptor
         */
        PSEUDO_ASYMMETRIC,

        /**
         * A non-stereogenic descriptor
         */
        NON_STEREOGENIC

    }

    /**
     * Access the {@link Type} of the descriptor. The type can be useful when
     * ranking descriptors where similar type descriptors can be considered
     * equal.
     *
     * @return the type
     *
     * @see Type
     * @see uk.ac.ebi.centres.descriptor.Tetrahedral#getType()
     * @see uk.ac.ebi.centres.descriptor.Planar#getType()
     * @see uk.ac.ebi.centres.descriptor.Trigonal#getType()
     */
    public Type getType();

}