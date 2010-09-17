package bn;

public class BNException extends Exception {

	public BNException(String message) {
		super(message);
		System.err.println("Exception of type " + this.getClass().toString() + 
				" thrown : " + message);
	}

	public BNException(BNException inner) {
		super(inner);
		System.err.println("Exception of type " + this.getClass().toString() + 
				" thrown wrapping one of " + inner.getClass().toString() + " : "
				+ inner.getMessage());
	}

	public BNException(String message, BNException inner) {
		super(message, inner);
		System.err.println("Exception of type " + this.getClass().toString() + 
				" thrown wrapping one of " + inner.getClass().toString() + " : "
				+ message + "  (" + inner.getMessage() + ")");

	}
	private static final long serialVersionUID = 1L;
}
