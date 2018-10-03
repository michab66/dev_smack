/* $Id$
 *
 * Michael's Application Construction Kit (MACK)
 *
 * Released under Gnu Public License
 * Copyright (c) 2008 Michael G. Binz
 */
package org.jdesktop.smack.rendits;

import java.awt.Component;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.EventObject;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTree;



/**
 *
 * @version $Rev$
 * @author Michael Binz
 */
public class EnumEditor extends DefaultCellEditor
{
    /*
     * The logger for this class.
     */
    private static final Logger _log =
        Logger.getLogger( EnumEditor.class.getName() );



    /**
     * Create an instance.
     */
    public EnumEditor()
    {
        super( new JComboBox() );

        getComponent().setName( EnumEditor.class.getName() );
    }



    /* (non-Javadoc)
     * @see javax.swing.DefaultCellEditor#cancelCellEditing()
     */
    @Override
    public void cancelCellEditing()
    {
        _log.info( "cancelCellEditing" );
        super.cancelCellEditing();
    }




    /* (non-Javadoc)
     * @see javax.swing.DefaultCellEditor#getCellEditorValue()
     */
    @Override
    public Object getCellEditorValue()
    {
        _log.info( "getCellEditorValue" );
        return super.getCellEditorValue();
    }


    /* (non-Javadoc)
     * @see javax.swing.DefaultCellEditor#getComponent()
     */
    @Override
    public Component getComponent()
    {
        _log.info( "getComponent" );
        return super.getComponent();
    }

    /* (non-Javadoc)
     * @see javax.swing.DefaultCellEditor#getTableCellEditorComponent(javax.swing.JTable, java.lang.Object, boolean, int, int)
     */
    @Override
    public Component getTableCellEditorComponent(
            JTable table,
            Object value,
            boolean isSelected,
            int row,
            int column )
    {
        _log.info( "getTableCellEditorComponent" );
        Class<?> clazz = value.getClass();

        JComboBox result = null;

        // Check whether the passed value really is an Enum.
        if ( Enum.class.isAssignableFrom( clazz ) )
        {
            // Get the enum elements and fill a combo box model.
            DefaultComboBoxModel dcbm =
                new DefaultComboBoxModel(
                        getEnumerationValues( (Enum<?>)value ) );

            // Get the combo box.
            result = (JComboBox)super.getComponent();

            // Set the model on the combo box.
            result.setModel( dcbm );
            // Make sure we use the same font as the table does.
            result.setFont( table.getFont() );
        }
        else if ( value != null )
        {
            _log.warning( "EnumEditor called for value of type:" + value.getClass().getName() );
        }
        else
            _log.warning( "EnumEditor called for null-value." );

        return result;
    }

    /* (non-Javadoc)
     * @see javax.swing.DefaultCellEditor#getTreeCellEditorComponent(javax.swing.JTree, java.lang.Object, boolean, boolean, boolean, int)
     */
    @Override
    public Component getTreeCellEditorComponent( JTree tree, Object value,
            boolean isSelected, boolean expanded, boolean leaf, int row )
    {
        _log.info( "getTreeCellEditorComponent" );
        return super.getTreeCellEditorComponent(tree, value, isSelected, expanded,
                leaf, row);
    }

    /* (non-Javadoc)
     * @see javax.swing.DefaultCellEditor#isCellEditable(java.util.EventObject)
     */
    @Override
    public boolean isCellEditable( EventObject anEvent )
    {
        _log.info( "isCellEditable" );
        return super.isCellEditable(anEvent);
    }


    /* (non-Javadoc)
     * @see javax.swing.DefaultCellEditor#shouldSelectCell(java.util.EventObject)
     */
    @Override
    public boolean shouldSelectCell( EventObject anEvent )
    {
        _log.info( "shouldSelectCell" );
        return super.shouldSelectCell(anEvent);
    }

    /* (non-Javadoc)
     * @see javax.swing.DefaultCellEditor#stopCellEditing()
     */
    @Override
    public boolean stopCellEditing()
    {
        _log.info( "stopCellEditing" );
        return super.stopCellEditing();
    }



    /**
     * Create an object array containing the enumeration elements
     * from the passed enumeration.
     *
     * @param en The enumeration.
     * @return The object array with the enumeration elements.
     */
    private Object[] getEnumerationValues( Enum<?> en )
    {
        Method values = null;


        try
        {
            // An enumeration class needs to have a 'values' operation.
            values = en.getClass().getMethod(
                    "values", new Class[0] );
        }
        catch ( Exception e )
        {
            String msg = "Enumeration class has no values() operation.";

            _log.log( Level.SEVERE, msg, e );
            throw new InternalError( msg );
        }

        Object intermediate = null;
        try
        {
            // Get the actual elements.
            intermediate = values.invoke( null, new Object[0] );
        }
        catch ( Exception e )
        {
            String msg = "Values() operation failed.";

            _log.log( Level.SEVERE, msg, e );
            throw new InternalError( msg );
        }

        // Convert the array of values to an object array.
        return convertToObjectArray( intermediate );
    }



    /**
     * Converts an array with any component type to an equivalent
     * Object[].
     *
     * @param o The array to convert.
     * @return The equivalent Object[].
     * @throws IllegalArgumentException If the passed object is not an array.
     */
    private Object[] convertToObjectArray( Object o )
    {
        int len = Array.getLength( o );

        Object[] result = new Object[ len ];

        for ( int i = 0 ; i < len ; i++ )
        {
            result[ i ] =
                Array.get( o, i );
        }

        return result;
    }
}
