package complex;

public class CMException extends Exception {
	public CMException(String cause)
	{
		super(cause);
	}
	
	public CMException(String cause, boolean ignoreable)
	{
		super(cause);
		this.ignoreable = ignoreable;
	}
	
	public boolean isIgnorable(){return this.ignoreable;}
	
	private boolean ignoreable = false;
	private static final long serialVersionUID = 1L;
}
