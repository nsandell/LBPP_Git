package bn.messages;

import bn.BNException;

/**
 * Class that implements message functionality between discrete nodes
 * and nodes that can be connected to discrete nodes.
 * @author Nils F. Sandell
 */
public class FiniteDiscreteMessage extends Message
{

	public static class FDiscMessageInterface extends MessageInterface<FiniteDiscreteMessage>
	{
		public FDiscMessageInterface(FiniteDiscreteMessage lambda, FiniteDiscreteMessage pi)
		{
			super(lambda,pi);
		}
		
		private static final long serialVersionUID = 50L;
	}
	
	public static class FDiscMessageInterfaceSet extends MessageInterfaceSet<FiniteDiscreteMessage>
	{
		public FDiscMessageInterfaceSet(int T)
		{
			super(T);
		}
		private static final long serialVersionUID = 50L;
	}
	
	/**
	 * Creates a message with desired cardinality.
	 * @param cardinality Cardinality.
	 */
	public FiniteDiscreteMessage(int cardinality)
	{
		this(0,0.0,cardinality);
	}
	
	public FiniteDiscreteMessage(int index, double value, int cardinality)
	{
		this.message_values = null;
		this.vp = new ValuePair(index,value,cardinality);
	}
	
	/**
	 * Normalize this message to sum to 1. If this
	 * message is all 0s it stays that way and returns 0.
	 * @return The sum that was used to normalize.
	 */
	public double normalize()
	{
		if(this.vp!=null)
		{
			double tmp = this.vp.value;
			this.vp.value = 1.0;
			return tmp;
		}
		else
		{
			double sum = 0;
			for(int i = 0; i < message_values.length; i++)
				sum += message_values[i];
			if(sum==0) return 0;
			for(int i = 0; i < message_values.length; i++)
				message_values[i] /= sum;
			return sum;
		}
	}
	
	public boolean isDelta()
	{
		return this.vp!=null;
	}
	
	public FiniteDiscreteMessage getMarginal(FiniteDiscreteMessage other) throws BNException
	{		
		if(this.vp!=null && other.vp!=null)
		{
			if(this.vp.value!=this.vp.value)
				throw new BNException("Attempted to compute marginals from conflicting evidence.");
		}
		try
		{
			FiniteDiscreteMessage marginal = this.multiply(other);
			marginal.normalize();
			return marginal;
		} catch(BNException e) {
			throw new BNException("Error attempting to get a marginal from two discrete messages : " + e.getMessage());
		}
	}
	
	public FiniteDiscreteMessage getMarginal(Message other) throws BNException
	{
		if(!(other instanceof FiniteDiscreteMessage))
			throw new BNException("Attempted to get a marginal with a discrete message and non-discrete message.");
		return this.getMarginal((FiniteDiscreteMessage)other);
	}
	
	public FiniteDiscreteMessage copy()
	{
		if(this.vp!=null)
			return new FiniteDiscreteMessage(this.vp.index,this.vp.value,this.vp.cardinality);
		FiniteDiscreteMessage newMsg = new FiniteDiscreteMessage(this.message_values.length);
		newMsg.message_values = this.message_values.clone();
		return newMsg;
	}


	public void empty()
	{
		if(this.message_values!=null)
		{
			for(int i = 0; i < this.message_values.length; i++)
				this.message_values[i] = 0;
		}
	}
	
	public void setInitial()
	{
		if(this.vp==null)
		{
			for(int i = 0; i < this.message_values.length; i++)
				message_values[i] = 1.0/((double)message_values.length);
		}
	}
	
	/**
	 * Perform entry-wise multiplication between this message and an argument message,
	 * storing the result in a new message.
	 * @param other The other message (the denominator).
	 * @return Resultant message
	 * @throws BNException If the two operands are of unequal length.
	 */
	public FiniteDiscreteMessage multiply(FiniteDiscreteMessage other) throws BNException
	{
		FiniteDiscreteMessage ret;
		if(this.vp!=null && other.vp!=null)
		{
			if(this.vp.index==other.vp.index)
			{
				ret = this.copy();
				ret.vp.value *= other.vp.value;
			}
			else
				return new FiniteDiscreteMessage(this.getCardinality());
		}
		else if(this.vp!=null)
		{
			ret = this.copy();
			ret.vp.value *= other.message_values[ret.vp.index];
		}
		else if(other.vp!=null)
		{
			ret = other.copy();
			ret.vp.value *= this.message_values[ret.vp.index];
		}
		else
		{
			if(other.message_values.length!=this.message_values.length)
				throw new BNException("Attempted to multiply messages of unequal length.");
			ret = new FiniteDiscreteMessage(this.message_values.length);
			for(int i = 0; i < message_values.length; i++)
				ret.setValue(i,this.message_values[i] * other.message_values[i]);
		}
		return ret;
	}
	
	/**
	 * Get a message consisting of all ones.
	 * @param card The cardinality of the message.
	 * @return All-ones message.
	 */
	public static FiniteDiscreteMessage allOnesMessage(int card)
	{
		FiniteDiscreteMessage ret = new FiniteDiscreteMessage(card);
		for(int i = 0; i < card ; i++)
			ret.setValue(i, 1.0);
		return ret;
	}
	
	public static FiniteDiscreteMessage normalMessage(int card)
	{
		FiniteDiscreteMessage ret = new FiniteDiscreteMessage(card);
		for(int i = 0; i < card; i++)
			ret.setValue(i,1.0/((double)card));
		return ret;
	}
	
	/**
	 * Set the value of this message.
	 * @param index Index at which to set.
	 * @param value Value to set.
	 */
	public void setValue(int index, double value)
	{
		if(this.message_values!=null)
			this.message_values[index] = value;
		else if(this.vp.index==index && this.vp.value==0.0)
			this.vp.value = value;
		else
		{
			this.message_values = new double[this.vp.cardinality];
			this.message_values[index] = value;
			this.message_values[this.vp.index] = this.vp.value;
			this.vp = null;
		}
	}
	
	public void setDelta(int index, double value)
	{
		if(this.vp==null)
		{
			this.vp = new ValuePair(index, value, this.message_values.length);
			this.message_values = null;
		}
		else
		{
			this.vp.index = index;
			this.vp.value = value;
		}
	}
	
	/**
	 * Get the cardinality of this message.	
	 * @return Integer cardinality
	 */
	public int getCardinality()
	{
		return this.message_values==null? this.vp.cardinality : this.message_values.length;
	}
	
	/**
	 * Get the value of this message
	 * @param index At this index
	 * @return The value
	 */
	public double getValue(int index)
	{
		return this.message_values != null ? this.message_values[index] : (index==this.vp.index ? this.vp.value : 0.0);
	}
	
	public void adjustCardinality(int newCard)
	{
		if(this.message_values!=null)
			this.message_values = new double[newCard];
		else
		{
			this.vp.cardinality = newCard;
			this.vp.value = 0;
			this.vp.index = 0;
		}			
	}
	
	public void adopt(Message msg) throws BNException
	{
		if(!(msg instanceof FiniteDiscreteMessage))
			throw new BNException("Attempted to adopt message values of non-discrete message.");
		FiniteDiscreteMessage dmsg = (FiniteDiscreteMessage)msg;
		if(dmsg.message_values!=null && this.message_values!=null)
		{
			if(dmsg.message_values.length!=this.message_values.length)
				throw new BNException("Attempted to adopt message of differing cardinality.");
			for(int i = 0;i < this.message_values.length; i++)
				this.message_values[i] = dmsg.message_values[i];
		}
		else if (dmsg.message_values!=null && this.message_values==null)
		{
			if(dmsg.message_values.length!=this.vp.cardinality)
				throw new BNException("Attempted to adopt message of differing cardinality.");
			this.message_values = new double[this.vp.cardinality];
			this.vp = null;
			for(int i = 0;i < this.message_values.length; i++)
				this.message_values[i] = dmsg.message_values[i];
		}
		else if (dmsg.message_values==null)
		{
			if(this.vp==null)
			{
				if(dmsg.vp.cardinality!=this.message_values.length)
					throw new BNException("Attempted to adopt message of differing cardinality.");
				this.vp = new ValuePair(dmsg.vp.index, dmsg.vp.value, dmsg.vp.cardinality);
				this.message_values = null;
			}
			else
			{
				if(dmsg.vp.cardinality!=this.vp.cardinality)
					throw new BNException("Attempted to adopt message of differing cardinality.");
				this.vp.index = dmsg.vp.index;
				this.vp.value = dmsg.vp.value;
			}
		}
	}

	// If message_values is null, this is a value node, and 'value' is the index of the value that is certain.
	private double[] message_values;
	private ValuePair vp;
	private static final long serialVersionUID = 50L;
	
	private static class ValuePair
	{
		public ValuePair(int index, double value, int cardinality)
		{
			this.index = index; 
			this.cardinality = cardinality; 
			this.value = value;
		}
		
		public int cardinality, index;
		public double value;
	}
}
