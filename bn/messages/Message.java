package bn.messages;

import java.io.Serializable;

import bn.BNException;

public abstract class Message implements Serializable {

	abstract Message copy();
	
	public static class MessageInterface implements Serializable
	{
		public MessageInterface(Message lambda, Message pi)
		{
			this.lambda = lambda;
			this.pi = pi;
		}
		
		public void invalidate() throws BNException
		{
			this.lambda.invalidate();
			this.pi.invalidate();
		}
		
		private static final long serialVersionUID = 50L;
		public Message lambda, pi;
	}
	
	public void invalidate()
	{
		this.valid = false;
	}
	
	public boolean isValid()
	{
		return this.valid;
	}
	
	public abstract void setInitial();
	
	//public abstract void adopt(Message msg) throws BNException;
	
	boolean valid = true;
	
	private static final long serialVersionUID = 50L;
}
