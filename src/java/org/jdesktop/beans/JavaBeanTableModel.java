/* $Id$
 *
 * Michael's Application Construction Kit (MACK)
 *
 * Released under Gnu Public License
 * Copyright © 2007-2014 Michael G. Binz
 */
package org.jdesktop.beans;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.table.AbstractTableModel;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.util.ReflectionUtil;

/**
 * <p>A table model that allows to display a list of Java Beans.
 * </p><p>
 * Note that the table model supports localization.  This is done by looking up
 * the resource map of the passed class.
 *
 * The key "PROPERTY_DISPLAY_ORDER" may list the JavaBean properties in the
 * order used for initial column ordering (example:
 * "PROPERTY_DISPLAY_ORDER = PropA PropC PropB").
 * </p><p>
 * The name in the table header can be defined by adding a key
 * propertyName=displayName to the resource map (example: PropA=Artist).
 * </p>
 *
 * @param <B> The Java Bean class.
 *
 * @version $Rev$y
 * @author Michael Binz
 */
@SuppressWarnings("serial")
public class JavaBeanTableModel<B> extends AbstractTableModel
{
    /**
     * The logger for this class.
     */
    private static final Logger _log = Logger
            .getLogger( JavaBeanTableModel.class.getName() );

    /**
     * The list of property descriptors corresponding to the model's columns.
     */
    private PropertyDescriptor[] _columns;

    /**
     * The model's rows.
     */
    private final B[] _rows;

    private final boolean _modified[];

    /**
     * The global read only flag.
     */
    private final boolean _readOnly;



    /**
     * Creates an instance.
     *
     * @param rows An array of JavaBeans, one for each row.
     * @param readOnly If true, the resulting table model is read-only.
     */
    public JavaBeanTableModel( B[] rows, boolean readOnly )
    {
        Class<?> rowType = rows.getClass().getComponentType();

        BeanInfo columns;
        try
        {
            columns = Introspector.getBeanInfo( rowType, Object.class );
        }
        catch ( IntrospectionException e )
        {
            throw new IllegalArgumentException( e );
        }

        _columns = columns.getPropertyDescriptors();
        _rows = rows;
        _modified = new boolean[_rows.length];
        _readOnly = readOnly;

        // Check whether we have a localized description.
        ResourceMap rm = Application.getResourceManager().getResourceMap( rowType );

        String[] columnList = rm.get( "PROPERTY_DISPLAY_ORDER", String[].class );

        if ( columnList != null )
            _columns = orderColums( _columns, columnList );

        for ( PropertyDescriptor pd : _columns )
        {
            String displayName = rm.getString( pd.getName() );

            if ( displayName != null )
                pd.setDisplayName( displayName );
        }
    }

    /**
     * Handles unexpected exceptions by writing them to the
     * log file and creating an InternalError.
     *
     * @param e The unexpected exception to handle.
     * @return An InternalError that has the passed exception set as cause.
     */
    private InternalError unexpectedException( Exception e )
    {
        _log.log( Level.SEVERE, e.getLocalizedMessage(), e );
        InternalError ie = new InternalError();
        ie.initCause( e );
        return ie;
    }



    /**
     * Handles unexpected InvocationTarget exceptions.
     *
     * @param e
     * @return
     */
    private Object unexpectedItException( Throwable e )
    {
        _log.log( Level.SEVERE, e.getLocalizedMessage(), e );
        return "n/a";
    }



    /*
     * Inherit Javadoc.
     */
    @Override
    public int getRowCount()
    {
        return _rows.length;
    }



    /*
     * Inherit Javadoc.
     */
    @Override
    public int getColumnCount()
    {
        return _columns.length;
    }



    /**
     * Returns the localised display name of the property that corresponds
     * to this column.
     *
     * @param columnIndex The index of the column.
     * @return The localised display name for this column.
     */
    @Override
    public String getColumnName( int columnIndex )
    {
        return _columns[columnIndex].getDisplayName();
    }



    /**
     * Get the class of the property displayed in a certain column.
     *
     * @param columnIndex The index of the column.
     * @return The property class that is displayed in this column.
     */
    @Override
    public Class<?> getColumnClass( int columnIndex )
    {
        return ReflectionUtil.normalizePrimitives(
                _columns[columnIndex].getPropertyType() );
    }



    /**
     * Check if a cell can be modified.
     *
     * @param rowIndex The cell's row index.
     * @param columnIndex The cell's column index.
     * @return True if the cell is editable.
     */
    @Override
    public boolean isCellEditable( int rowIndex, int columnIndex )
    {
        return (!_readOnly) && _columns[columnIndex].getWriteMethod() != null;
    }



    /**
     * Get a cell's value by calling the property's read method.
     *
     * @param rowIndex The cell's row index.
     * @param columnIndex The cell's column index.
     * @return The cell's value.
     */
    @Override
    public Object getValueAt( int rowIndex, int columnIndex )
    {
        Method getter = _columns[columnIndex].getReadMethod();

        try
        {
            return getter.invoke( _rows[rowIndex], (Object[]) null );
        }
        catch ( IllegalAccessException e )
        {
            throw unexpectedException( e );
        }
        catch ( InvocationTargetException e )
        {
            return unexpectedItException( e.getCause() );
        }
    }



    /**
     * Checks whether the passed value is already set in the table model.
     *
     * @param aValue The value to set.
     * @param rowIndex The row index to set.
     * @param columnIndex The column index to set.
     * @return {@code True} if the cell's value already
     * corresponds to the new value.
     */
    private boolean valueAlreadySet( Object aValue, int rowIndex, int columnIndex  )
    {
        Object currentValue = getValueAt( rowIndex, columnIndex );

        if ( currentValue == null )
            return aValue == null;

        return currentValue.equals( aValue );
    }



    /**
     * Set a cell's value by calling the property's write method.
     *
     * @param aValue The value to set.
     * @param rowIndex The row index.
     * @param columnIndex The column index.
     */
    @Override
    public void setValueAt( Object aValue, int rowIndex, int columnIndex )
    {
        if ( _readOnly )
        {
            _log.warning( "setValueAt() called in readOnly state." );
            return;
        }

        // Check whether the cell already has the passed value.  If this is the
        // case the set operation is not actually called.
        if ( valueAlreadySet( aValue, rowIndex, columnIndex ) )
            return;

        Method setter = _columns[columnIndex].getWriteMethod();

        try
        {
            setter.invoke(
                _rows[rowIndex],
                new Object[]{ aValue } );
            _modified[rowIndex] = true;
        }
        catch ( IllegalAccessException e )
        {
            throw unexpectedException( e );
        }
        catch ( InvocationTargetException e )
        {
            unexpectedItException( e );
        }
    }



    /**
     * Order the model's columns.
     *
     * @param properties The full list of properties displayed in this table.
     * @param nameList The names of the columns in order.
     * @return A reordered list of property descriptors.
     */
    private static PropertyDescriptor[] orderColums(
        PropertyDescriptor[] properties,
        String[] nameList )
    {
        if ( nameList.length > properties.length )
        {
            String msg = "nameList.length > properties.length";
            _log.severe( msg );
            throw new IllegalArgumentException( msg );
        }

        Hashtable<String, Integer> map = new Hashtable<String, Integer>();

        for ( int i = 0 ; i < properties.length ; i++ )
        {
            PropertyDescriptor pd = properties[i];
            map.put( pd.getName(), i );
        }

        PropertyDescriptor[] result = new PropertyDescriptor[properties.length];

        for ( int i = 0 ; i < nameList.length ; i++ )
        {
            Integer srcIdx = map.get( nameList[i] );

            if ( srcIdx == null )
            {
                String msg = "List contains unknown entry: " + nameList[i];
                _log.severe( msg );
                throw new InternalError( msg );
            }
            else
            {
                result[i] = properties[srcIdx];
                properties[srcIdx] = null;
            }
        }

        // Put all non-null elements from the array to the vector.
        Vector<PropertyDescriptor> vector = new Vector<PropertyDescriptor>();
        for ( int i = 0 ; i < properties.length ; i++ )
        {
            PropertyDescriptor current = properties[i];
            if ( current != null )
                vector.add( current );
        }

        // Append all vector entries to the result array.
        for ( int i = 0 ; i < vector.size() ; i++ )
        {
            result[nameList.length + i] = vector.elementAt( i );
        }

        return result;
    }
}
