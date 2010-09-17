package util;

import java.util.Iterator;

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
