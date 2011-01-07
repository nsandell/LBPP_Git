package bn.messages;

import bn.BNException;

public abstract class ValueMessage<ValueType> extends Message {

	public ValueMessage(ValueType val)
	{
		this.val = val;
	}
	
	public ValueType getValue()
	{
		return this.val;
	}
	
	@Override
	public Message getMarginal(Message other) throws BNException {
		throw new BNException("Tried to get marginal distribution from a value message!");
	}
	
	@Override
	public void adopt(Message msg) throws BNException {
		if(!(msg instanceof ValueMessage))
			throw new BNException("Attempted to have a value message adopt the message of a non-value message.");
		this.val = this.getValue(((ValueMessage<?>)msg));
	}
	
	@Override //TODO Verify this is what we want here
	public void setInitial() {this.val = null;}
	
	protected abstract ValueType getValue(ValueMessage<?> msg) throws BNException;
	
	public static class IntMessage extends ValueMessage<Integer> {

		public IntMessage(int value){super(value);}
		
		@Override
		Message copy() {return new IntMessage(this.val);}
		protected Integer getValue(ValueMessage<?> msg) throws BNException
		{
			if(msg.val instanceof Integer)
				return (Integer)msg.val;
			else
				throw new BNException("Attempted to set value of integer value message to a " + msg.val.getClass().toString());
		}
		private static final long serialVersionUID = 1L;
	}
	
	public static class DoubleMessage extends ValueMessage<Double> {

		public DoubleMessage(double value){super(value);}
		
		@Override
		Message copy() {return new DoubleMessage(this.val);}
		protected Double getValue(ValueMessage<?> msg) throws BNException
		{
			if(msg.val instanceof Double)
				return (Double)msg.val;
			else
				throw new BNException("Attempted to set value of double value message to a " + msg.val.getClass().toString());
		}
		private static final long serialVersionUID = 1L;
	}
	
	protected ValueType val;
	private static final long serialVersionUID = 1L;
}
