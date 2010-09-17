package util;

import java.util.Iterator;

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
