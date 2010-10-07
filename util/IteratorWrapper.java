package util;

import java.util.Iterator;
/**
 * Superclass iterator wrapper
 * @author Nils F. Sandell
 *
 * @param <SuperType> Type we wish to get out of the iterator, which
 * 		is a superclass of the type for which we have an iterator.
 */
public class IteratorWrapper<SuperType> implements Iterator<SuperType>
{
	public IteratorWrapper(Iterator<? extends SuperType> wrappedIt) 
	{
		this.wrappedIt = wrappedIt;
	}
	
	public boolean hasNext(){return wrappedIt.hasNext();}
	public SuperType next(){return wrappedIt.next();}
	public void remove(){}
	
	Iterator<? extends SuperType> wrappedIt;
}
