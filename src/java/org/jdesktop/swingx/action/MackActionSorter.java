/* $Id$
 *
 * Common.
 *
 * Released under Gnu Public License
 * Copyright © 2012 Michael G. Binz
 */

package org.jdesktop.swingx.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jdesktop.smack.util.StringUtils;


/**
 * Utility class for sorting actions.
 *
 * @version $Rev$
 * @author Michael Binz
 */
class MackActionSorter
implements Comparator<MackAction>
{
    /**
     * Sort the actions in the passed collection. The actions are expected to
     * be in the same category.
     *
     * @param actionCategory The actions to sort.
     * @return A list holding the sorted actions.
     */
    List<MackAction> sortCategory( Collection<MackAction> actionCategory )
    {
        if ( actionCategory == null )
            throw new NullPointerException();
        if ( actionCategory.size() == 0 )
            return Collections.emptyList();

        String categoryName = null;

        for ( MackAction c : actionCategory )
        {
            String cGroupName =
                StringUtils.toString( c.getCategory() );

            if ( categoryName == null )
                categoryName = cGroupName;
            else if ( ! categoryName.equals( cGroupName ) )
                throw new IllegalArgumentException( "More than a single group." );
        }

        ArrayList<MackAction> result =
            new ArrayList<MackAction>( actionCategory );

        Collections.sort( result, this );

        return result;
    }



    /* (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare( MackAction o1, MackAction o2 )
    {
        String g1 =
                o1.getCategorySortId();
        String g2 =
                o2.getCategorySortId();

        return g1.compareTo( g2 );
    }
}
