/* $Id$
 *
 * Released under Gnu Public License
 * Copyright (c) 2017 Michael G. Binz
 */
package org.jdesktop.application;

import java.util.Set;

/**
 * Allows to discover command line interface programs.
 *
 * @see CliApplication
 * @version $Rev$
 * @author michab
 */
public abstract class CliContributor
{
    /**
     * @return The list of command line programs contributed by this module.
     */
    public abstract Set<Class<? extends CliApplication>> getClis();
}
