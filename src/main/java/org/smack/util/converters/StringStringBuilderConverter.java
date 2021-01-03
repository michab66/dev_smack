/* $Id$
 *
 * Utilities
 *
 * Released under Gnu Public License
 * Copyright © 2017 Michael G. Binz
 */
package org.smack.util.converters;

import org.smack.util.resource.ResourceConverter;
import org.smack.util.resource.ResourceMap;

/**
 *
 * @version $Rev$
 * @author Michael Binz
 */
public class StringStringBuilderConverter extends ResourceConverter {

    public StringStringBuilderConverter() {
        super(StringBuilder.class);
    }

    @Override
    public Object parseString( String s, ResourceMap notUsed )
    {
        return new StringBuilder( s );
    }
}
