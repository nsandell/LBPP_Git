package bn;

public class BNException extends Exception {
	
	static boolean printOnThrow = false;

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
