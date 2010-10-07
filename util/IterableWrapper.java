package util;

import java.util.Iterator;

/**
 * Superclass iterable wrapper.
 * @author Nils F Sandell
 * @param <SuperType> The type we wish to iterate over, which is a superclass
 * 			of the iterator we have.
 */
public class IterableWrapper<SuperType> implements Iterable<SuperType>
{
	public IterableWrapper(Iterable<? extends SuperType> wrappedIterable)
	{
		this.wrappedIterable = wrappedIterable;
	}
	
	public Iterator<SuperType> iterator()
	{
		return new IteratorWrapper<SuperType>(wrappedIterable.iterator());
	}
	
	Iterable<? extends SuperType> wrappedIterable;
}
