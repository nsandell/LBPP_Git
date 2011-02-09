package bn.messages;

import java.io.Serializable;
import java.util.Vector;

import bn.BNException;

public abstract class Message implements Serializable {

	public abstract Message copy();
	
	public static class MessageInterface<MessageType extends Message> implements Serializable
	{
		public MessageInterface(MessageType lambda, MessageType pi)
		{
			this.lambda = lambda;
			this.pi = pi;
		}
		private static final long serialVersionUID = 50L;
		public MessageType lambda, pi;
	}
	
	public static class MessageInterfaceSet<MessageType extends Message> implements Serializable
	{
		public MessageInterfaceSet(int T)
		{
			this.pi_v = new Vector<MessageType>(T);
			this.lambda_v = new Vector<MessageType>(T);
		}
		private static final long serialVersionUID = 50L;
		public Vector<MessageType> pi_v, lambda_v;
	}

	public abstract void setInitial();
	
	public abstract void adopt(Message msg) throws BNException;
	
	private static final long serialVersionUID = 50L;
}
