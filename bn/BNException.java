package bn;

/**
 * Basic exception for this package.  Turn printOnThrow to true to get stack traces
 * when any are thrown for debugging.
 * 
 * @author Nils F. Sandell
 *
 */
public class BNException extends Exception {
	
	static boolean printOnThrow = true;

	public BNException(String message) {
		super(message);
		if(printOnThrow)
		{
			System.err.println("Exception of type " + this.getClass().toString() + 
				" thrown : " + message + "\nStacktrace:");
			this.printStackTrace(System.err);
		}
	}

	public BNException(BNException inner) {
		super(inner);
		if(printOnThrow)
		{
			System.err.println("Exception of type " + this.getClass().toString() + 
				" thrown wrapping one of " + inner.getClass().toString() + " : "
				+ inner.getMessage());
			this.printStackTrace(System.err);
		}
	}

	public BNException(String message, BNException inner) {
		super(message, inner);
		if(printOnThrow)
		{
			System.err.println("Exception of type " + this.getClass().toString() + 
				" thrown wrapping one of " + inner.getClass().toString() + " : "
				+ message + "  (" + inner.getMessage() + ")");
			this.printStackTrace(System.err);
		}
	}
	private static final long serialVersionUID = 1L;
}
