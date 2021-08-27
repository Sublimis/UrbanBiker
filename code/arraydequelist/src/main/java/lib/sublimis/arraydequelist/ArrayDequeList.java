/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package lib.sublimis.arraydequelist;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

/**
 * ArrayDeque implementing the List interface, to support fast random access of elements (unlike pure ArrayDeque).
 * DOES NOT permit <b>null</b> elements (like ArrayDeque, unlike LinkedList or ArrayList).
 *
 * ArrayDequeEx class is just the ArrayDeque class copied from the Android source code, with essential deque
 * structures marked as "protected" so we can access them from this subclass. Simply subclassing the ArrayDeque
 * does not allow us such access.
 *
 * @author Sublimis
 * @version 1.0 (2016-12-30)
 * @version 1.1 (2018-07-13)
 */
public class ArrayDequeList<E> extends ArrayDequeEx<E> implements Deque<E>, List<E>, RandomAccess, IListDequeRndAccess<E>
{
	public ArrayDequeList()
	{
		super();
	}

	public ArrayDequeList(Collection<? extends E> c)
	{
		super(c);
	}

	public ArrayDequeList(int numElements)
	{
		super(numElements);
	}

	@Override
	public void add(int index, E element)
	{
		if (index > this.size())
			throw new IndexOutOfBoundsException(outOfBoundsMsg(index));

		if (element == null)
		{
			throw new NullPointerException();
		}
		else
		{
			if (index == 0)
			{
				this.addFirst(element);
			}
			else if (index == this.size())
			{
				this.addLast(element);
			}
			else
			{
				/* Save last element of the list (not array) which will be overwritten on purpose */
				final E lastElement = this.getLast();

				final int arrayIndex = getArrayIndex(index);

				if (arrayIndex < this.tail)
				{
					/* Easier case */

					System.arraycopy(elements, arrayIndex, elements, arrayIndex + 1, this.tail - arrayIndex - 1);
				}
				else
				{
					/* Tougher case (wrapped deque) */

					/* Save last element of the array (not list) which will be overwritten now.
					 *  This element may or my not be equal to the previously saved lastElement of the list. */
					final E lastArrayElement = this.elements[this.elements.length - 1];

					System.arraycopy(elements, arrayIndex, elements, arrayIndex + 1, this.elements.length - arrayIndex - 1);

					if (this.tail > 0)
					{
						System.arraycopy(elements, 0, elements, 1, this.tail - 1);

						this.elements[0] = lastArrayElement;
					}
				}

				/* Insert the new element */
				this.elements[arrayIndex] = element;

				/* Insert saved last element and give deque a chance to increase its capacity if necessary */
				this.addLast(lastElement);
			}
		}
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c)
	{
		if (index > this.size())
			throw new IndexOutOfBoundsException(outOfBoundsMsg(index));

		if (c == null)
		{
			throw new NullPointerException();
		}
		else if (c.size() > 0)
		{
			for (final E element : c)
			{
				if (element == null)
				{
					throw new NullPointerException();
				}
				else
				{
					this.add(index++, element);
				}
			}

			return true;
		}
		else
		{
			return false;
		}
	}

	@Override
	public E get(int index)
	{
		rangeCheck(index);

		return getListElement(index);
	}

	@Override
	public int indexOf(Object o)
	{
		if (o == null)
		{
			throw new NullPointerException();
		}
		else
		{
			for (int i = 0; i < this.size(); i++)
				if (o.equals(getListElement(i)))
					return i;
		}

		return -1;
	}

	@Override
	public int lastIndexOf(Object o)
	{
		if (o == null)
		{
			throw new NullPointerException();
		}
		else
		{
			for (int i = this.size() - 1; i >= 0; i--)
				if (o.equals(getListElement(i)))
					return i;
		}

		return -1;
	}

	@Override
	public ListIterator<E> listIterator()
	{
		return new ListIter(0);
	}

	@Override
	public ListIterator<E> listIterator(int index)
	{
		return new ListIter(index);
	}

	@Override
	public E remove(int index)
	{
		rangeCheck(index);

		if (index == 0)
		{
			return this.removeFirst();
		}
		else if (index == this.size() - 1)
		{
			return this.removeLast();
		}
		else
		{
			final int arrayIndex = getArrayIndex(index);

			final E previous = this.elements[arrayIndex];

			if (arrayIndex < this.tail)
			{
				/* Easier case */

				System.arraycopy(elements, arrayIndex + 1, elements, arrayIndex, this.tail - arrayIndex - 1);
			}
			else
			{
				/* Tougher case (wrapped deque) */

				System.arraycopy(elements, arrayIndex + 1, elements, arrayIndex, this.elements.length - arrayIndex - 1);

				if (this.tail > 0)
				{
					this.elements[this.elements.length - 1] = this.elements[0];

					System.arraycopy(elements, 1, elements, 0, this.tail - 1);
				}
			}

			this.tail = (this.tail - 1) & (this.elements.length - 1);
			this.elements[this.tail] = null;

			return previous;
		}
	}

	@Override
	public E set(int index, E element)
	{
		rangeCheck(index);

		if (element == null)
		{
			throw new NullPointerException();
		}
		else
		{
			final E previous = this.getListElement(index);

			this.setListElement(index, element);

			return previous;
		}
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex)
	{
		return super.subList(fromIndex, toIndex);
	}

	/**
	 * Checks if the given index is in range. If not, throws an appropriate
	 * runtime exception. This method does *not* check if the index is negative:
	 * It is always used immediately prior to an array access, which throws an
	 * ArrayIndexOutOfBoundsException if index is negative.
	 */
	private void rangeCheck(int index)
	{
		if (index >= this.size())
			throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
	}

	/**
	 * Constructs an IndexOutOfBoundsException detail message. Of the many
	 * possible refactorings of the error handling code, this "outlining"
	 * performs best with both server and client VMs.
	 */
	private String outOfBoundsMsg(int index)
	{
		return "Index: " + index + ", Size: " + this.size();
	}

	private int getArrayIndex(int index)
	{
		final int arrayIndex = (head + index) & (elements.length - 1);

		return arrayIndex;
	}

	private int getListIndex(int arrayIndex)
	{
		final int listIndex = (arrayIndex - head) & (elements.length - 1);

		return listIndex;
	}

	private E getListElement(int index)
	{
		return elements[getArrayIndex(index)];
	}

	private void setListElement(int index, E element)
	{
		elements[getArrayIndex(index)] = element;
	}

	@Override
	public ArrayDequeList<E> clone()
	{
		return (ArrayDequeList<E>) super.clone();
	}

	private static final long serialVersionUID = 2340985798034038923L;

	private class ListIter extends DeqIterator implements ListIterator<E>
	{
		ListIter(int index)
		{
			super();
			cursor = getArrayIndex(index);
		}

		public boolean hasPrevious()
		{
			return cursor != getArrayIndex(0);
		}

		public int nextIndex()
		{
			return getListIndex(cursor);
		}

		public int previousIndex()
		{
			return getListIndex(cursor) - 1;
		}

		public E previous()
		{
			checkForComodification();

			final int listIndex = previousIndex();

			if (listIndex < 0)
				throw new NoSuchElementException();

			if (listIndex >= ArrayDequeList.this.size())
				throw new ConcurrentModificationException();

			cursor = getArrayIndex(listIndex);
			lastRet = cursor;

			return ArrayDequeList.this.getListElement(listIndex);
		}

		public void set(E e)
		{
			if (lastRet < 0)
				throw new IllegalStateException();

			checkForComodification();

			try
			{
				ArrayDequeList.this.set(getListIndex(lastRet), e);
			}
			catch (IndexOutOfBoundsException ex)
			{
				throw new ConcurrentModificationException();
			}
		}

		public void add(E e)
		{
			checkForComodification();

			try
			{
				final int listIndex = getListIndex(cursor);

				ArrayDequeList.this.add(listIndex, e);

				cursor = getArrayIndex(listIndex + 1);
				lastRet = -1;
			}
			catch (IndexOutOfBoundsException ex)
			{
				throw new ConcurrentModificationException();
			}
		}

		private void checkForComodification()
		{
		}
	}
}
