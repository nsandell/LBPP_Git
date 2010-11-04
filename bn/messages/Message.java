package bn.messages;

import java.io.Serializable;

import bn.BNException;

public abstract class Message implements Serializable {

	abstract Message copy();
	
	public abstract Message getMarginal(Message other) throws BNException;

	public static class MessageInterface<MessageType extends Message> implements Serializable
	{
		public MessageInterface(MessageType lambda, MessageType pi, MessageType parent_local_pi)
		{
			this.parent_local_pi = parent_local_pi;
			this.lambda = lambda;
			this.pi = pi;
		}
		
		public void invalidate() throws BNException
		{
			this.lambda.invalidate();
			this.pi.invalidate();
		}
		
		private static final long serialVersionUID = 50L;
		public MessageType lambda, pi, parent_local_pi;
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
	
	public abstract void adopt(Message msg) throws BNException;
	
	boolean valid = true;
	
	private static final long serialVersionUID = 50L;
}
