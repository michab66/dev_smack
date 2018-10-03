/* $Id$
 *
 * Michael's Application Construction Kit (MACK)
 *
 * Released under Gnu Public License
 * Copyright (c) 2008 Michael G. Binz
 */
package org.jdesktop.smack;



/**
 *
 * @param <T>
 * @version $Rev$
 * @author Michael Binz
 */
public interface ArrayLoader<T>
{
    void load( T[] item );
}
