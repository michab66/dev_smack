/* $Id$
 *
 * Mack. Michael's Application Construction Kit.
 *
 * Released under Gnu Public License
 * Copyright © 2014 Michael G. Binz
 */
package org.jdesktop.application;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jdesktop.smack.util.StringUtils;


/**
 * Marks the field as a resource to be injected.
 * In order to inject the resources for this class, the resources must be
 * defined in a class resource file. You should name field resources in the
 * resource file using the class name followed by a period (.) and the key:
 * <pre>
 *  &lt;classname&gt;.&lt;fieldname&gt;
 * </pre>
 *
 * @author Michael Binz
 * @author Hans Muller (Hans.Muller@Sun.COM)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Resource
{
    /**
     * Key for resource injection. If not specified the name of the field will
     * be used.
     */
    String key() default StringUtils.EMPTY_STRING;

    /**
     * A value taken if no value is defined in the class resource file.
     * This value is then used in the normal type conversion process.
     */
    String defaultValue() default StringUtils.EMPTY_STRING;
}
