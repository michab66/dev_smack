/* $Id$
 *
 * Utilities
 *
 * Released under Gnu Public License
 * Copyright (c) 2002 Michael G. Binz
 */
package org.jdesktop.smack.util;

import java.util.Enumeration;



/**
 * <p>Implements an <code>Enumeration</code> that is based on an array.
 * Supports enumerating the whole array, a subset and a copy of the array.
 * Enumerating on a copy prevents concurrent array access and modification.</p>
 *
 * @version $Rev$
 * @author Michael Binz
 */
public class ArrayEnumerator<T>
  implements
    Enumeration<T>
{
  /**
   * Create an ArrayEnumerator on a given array.
   *
   * @param theArray The array to be enumerated.
   */
  public ArrayEnumerator( T[] theArray )
  {
      _theArray = theArray;
      _endIdx = theArray.length;
  }



  /**
   * Create an ArrayEnumerator on a copy of a given array.  This prevents the
   * possibility of concurrent array modifications while enumerating.
   *
   * @param theArray The array to be enumerated.
   * @param copy If true the enumerator will create an internal copy of the
   *        array.
   * @deprecated Use {@link ReflectionUtils#cloneArray(Object[])}
   */
  public ArrayEnumerator( T[] theArray, boolean copy )
  {
    initialise( theArray, 0, theArray.length, copy );
  }



  /**
   * Create an ArrayEnumerator on a subrange of a given array.
   *
   * @param theArray The array to be enumerated.
   * @param startIdx The start index for the enumeration.
   * @param endIdx The index of the first element <i>not</i> returned by the
   *               enumerator.  Has to be greater than <code>startIdx</code>.
   * @throws ArrayIndexOutOfBoundsException If the indices are wrong.
   * @deprecated Use {@link ReflectionUtils#cloneArray(Object[])}
   */
  public ArrayEnumerator( T[] theArray, int startIdx, int endIdx )
  {
    initialise( theArray, startIdx, endIdx, false );
  }



  /**
   * Create an ArrayEnumerator on a subrange of a given array.  The passed
   * array can be copied.
   *
   * @param theArray The array to enumerate.
   * @param startIdx The start index for the enumeration.
   * @param endIdx The index of the first element <i>not</i> returned by the
   *               enumerator.  Has to be greater than <code>startIdx</code>.
   * @param copy If true the enumerator will create an internal copy of the
   *        array.
   * @throws ArrayIndexOutOfBoundsException If the indices are wrong.
   * @deprecated Use {@link ReflectionUtils#cloneArray(Object[])}
   */
  public ArrayEnumerator( T[] theArray,
                          int startIdx,
                          int endIdx,
                          boolean copy )
  {
    initialise( theArray, startIdx, endIdx, copy );
  }



  /**
   * Returns <code>true</code> if there are more elements to enumerate.
   *
   * @return <code>true</code> if there are more elements to enumerate.
   */
  public boolean hasMoreElements()
  {
    return _currentIdx < _endIdx;
  }



  /**
   * Returns the next element.
   *
   * @return The next element.
   * @throws ArrayIndexOutOfBounds If there are no more elements to enumerate.
   *         Note that this is a programming error, since
   *         <code>hasMoreElements</code> had returned <code>false</code> in
   *         this case.
   */
  public T nextElement()
  {
    return _theArray[ _currentIdx++ ];
  }



  /**
   * The common instance initialiser for <code>ArrayEnumerator</code>s.
   *
   * @param array The array to be enumerated.
   * @param start The start index for the enumeration.
   * @param end The index of the first element not returned by the
   *            enumerator.  Has to be greater than <code>start</code>.
   * @param copy If true the enumerator will create an internal copy of the
   *        array.
   * @throws ArrayIndexOutOfBoundsException If the indices are wrong.
   * @deprecated Used only by deprecated operations.
   */
  private void initialise( T[] array, int start, int end, boolean copy )
  {
    // Check the arguments...
    if ( start < 0 ||
         start > array.length ||
         end < 0 ||
         end > array.length ||
         end < start )
      // ...and be loud if some index is out of bounds.
      throw new ArrayIndexOutOfBoundsException( "ArrayEnumerator" );

    // Assign the indices.
    _currentIdx = start;
    _endIdx = end;

    // In case a copy is requested...
    if ( copy )
    {
      // ...copy only the necessary subset.
      _theArray = ReflectionUtils.cloneArray( array, start, end-start );
    }
    else
      // If no copy is requested keep the reference.
      _theArray = array;
  }



  /**
   * The position of the next element the enumerator will return.
   */
  private int _currentIdx = 0;



  /**
   * The first index position that is not returned by the enumerator.
   */
  private int _endIdx;



  /**
   * A reference to the array we are enumerating.
   */
  private T[] _theArray;
}
