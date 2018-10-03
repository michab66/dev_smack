/* $Id$
 *
 * Michael's Application Construction Kit (MACK)
 *
 * Released under Gnu Public License
 * Copyright (c) 2006 Michael G. Binz
 */
package org.jdesktop.smack;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;



/**
 * Merges several table models into a single common one.
 *
 * @author Michael Binz
 */
public class CombinedTableModel
    implements
      TableModel
{
  private final TableModel[] _delegates;

  private final Column[] _columnMap;

  /**
   *
   * @param delegates
   */
  public CombinedTableModel( TableModel[] delegates )
  {
    assert( delegates != null );
    assert( delegates.length >= 1 );

    int lengthFirst = delegates[0].getRowCount();
    for ( int i = 1 ; i < delegates.length ; i++ )
      assert( lengthFirst == delegates[i].getRowCount() );

    _delegates = delegates;

    int numberOfColumns = 0;
    for ( int i = 0 ; i < _delegates.length ; i++ )
      numberOfColumns += _delegates[i].getColumnCount();

    _columnMap = new Column[numberOfColumns];
    for ( int i = 0 ; i < _columnMap.length ; i++ )
      _columnMap[i] = new Column();

    int overallColumn = 0;
    for ( int i = 0 ; i < _delegates.length ; i++ )
    {
      for ( int j = 0 ; j < _delegates[i].getColumnCount() ; j++)
      {
        Column currentColumn = _columnMap[overallColumn++];
        currentColumn.idx = j;
        currentColumn.tableModel = _delegates[i];
      }
    }
  }
  /*
   * Inherit Javadoc.
   */
  public int getRowCount()
  {
    return _delegates[ 0 ].getRowCount();
  }



  /*
   * Inherit Javadoc.
   */
  public int getColumnCount()
  {
    return _columnMap.length;
  }



  /*
   * Inherit Javadoc.
   */
  public String getColumnName( int columnIndex )
  {
    Column c = _columnMap[columnIndex];
    return c.tableModel.getColumnName( c.idx );
  }



  /*
   * Inherit Javadoc.
   */
  public Class getColumnClass( int columnIndex )
  {
    Column c = _columnMap[columnIndex];
    return c.tableModel.getColumnClass( c.idx );
  }



  /*
   * Inherit Javadoc.
   */
  public boolean isCellEditable( int rowIndex, int columnIndex )
  {
    Column c = _columnMap[columnIndex];
    return c.tableModel.isCellEditable( rowIndex, c.idx );
  }



  /*
   * Inherit Javadoc.
   */
  public Object getValueAt( int rowIndex, int columnIndex )
  {
    Column c = _columnMap[columnIndex];
    return c.tableModel.getValueAt( rowIndex, c.idx );
  }



  /*
   * Inherit Javadoc.
   */
  public void setValueAt( Object aValue, int rowIndex, int columnIndex )
  {
    Column c = _columnMap[columnIndex];
    c.tableModel.setValueAt( aValue, rowIndex, c.idx );
  }



  /*
   * Inherit Javadoc.
   */
  public void addTableModelListener( TableModelListener l )
  {
    for (int i = 0; i < _delegates.length; i++)
    {
      _delegates[i].addTableModelListener( l );
    }
  }



  /*
   * Inherit Javadoc.
   */
  public void removeTableModelListener( TableModelListener l )
  {
    for (int i = 0; i < _delegates.length; i++)
    {
      _delegates[i].removeTableModelListener( l );
    }
  }

  static class Column
  {
    TableModel tableModel;
    int idx;
  }
}
