package complex.featural;

import bn.BNException;

public class FMMException extends BNException
{
	public FMMException(String cause)
	{
		super(cause);
	}
	
	public FMMException(String cause, boolean ignoreable)
	{
		super(cause);
		this.ignoreable = ignoreable;
	}
	
	public boolean isIgnorable(){return this.ignoreable;}
	
	private boolean ignoreable = false;
	private static final long serialVersionUID = 1L;
}