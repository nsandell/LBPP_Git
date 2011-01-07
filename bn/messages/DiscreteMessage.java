package bn.messages;

import bn.BNException;

/**
 * Class that implements message functionality between discrete nodes
 * and nodes that can be connected to discrete nodes.
 * @author Nils F. Sandell
 */
public class DiscreteMessage extends Message
{
	/**
	 * Creates a message with desired cardinality.
	 * @param cardinality Cardinality.
	 */
	public DiscreteMessage(int cardinality)
	{
		this.message_values = new double[cardinality];
		for(int i = 0; i < cardinality; i++)
			this.message_values[i] = 0;
	}
	
	/**
	 * Normalize this message to sum to 1. If this
	 * message is all 0s it stays that way and returns 0.
	 * @return The sum that was used to normalize.
	 */
	public double normalize()
	{
		double sum = 0;
		for(int i = 0; i < message_values.length; i++)
			sum += message_values[i];
		if(sum==0) return 0;
		for(int i = 0; i < message_values.length; i++)
			message_values[i] /= sum;
		return sum;
	}
	
	public void setUniform()
	{
		for(int i = 0; i < this.message_values.length; i++)
			message_values[i] = 1.0/((double)message_values.length);
	}
	
	public DiscreteMessage getMarginal(DiscreteMessage other) throws BNException
	{
		try
		{
			DiscreteMessage marginal = this.multiply(other);
			marginal.normalize();
			return marginal;
		} catch(BNException e) {
			throw new BNException("Error attempting to get a marginal from two discrete messages : " + e.getMessage());
		}
	}
	
	public DiscreteMessage getMarginal(Message other) throws BNException
	{
		if(!(other instanceof DiscreteMessage))
			throw new BNException("Attempted to get a marginal with a discrete message and non-discrete message.");
		return this.getMarginal((DiscreteMessage)other);
	}
	
	public DiscreteMessage copy()
	{
		DiscreteMessage newMsg = new DiscreteMessage(this.message_values.length);
		newMsg.message_values = this.message_values.clone();
		return newMsg;
	}
	
	public void empty()
	{
		for(int i = 0; i < this.message_values.length; i++)
			this.message_values[i] = 0;
	}
	
	/**
	 * Perform entry-wise division between this message and an argument message,
	 * storing the result in a new message.
	 * @param other The other message (the denominator).
	 * @return Resultant message
	 * @throws BNException If the two operands are of unequal length.
	 */
	public DiscreteMessage divide(DiscreteMessage other) throws BNException
	{
		if(other.message_values.length!=this.message_values.length)
			throw new BNException("Attempted to multiply messages of unequal length.");
		DiscreteMessage ret = new DiscreteMessage(this.message_values.length);
		for(int i = 0; i < this.message_values.length; i++)
			ret.setValue(i, this.message_values[i]/other.message_values[i]);
		return ret;
	}
	
	public void setInitial()
	{
		this.setUniform();
	}

	/**
	 * Perform entry-wise multiplication between this message and an argument message,
	 * storing the result in a new message.
	 * @param other The other message (the denominator).
	 * @return Resultant message
	 * @throws BNException If the two operands are of unequal length.
	 */
	public DiscreteMessage multiply(DiscreteMessage other) throws BNException
	{
		if(other.message_values.length!=this.message_values.length)
			throw new BNException("Attempted to multiply messages of unequal length.");
		DiscreteMessage ret = new DiscreteMessage(this.message_values.length);
		for(int i = 0; i < message_values.length; i++)
			ret.message_values[i] = this.message_values[i] * other.message_values[i];
		return ret;
	}
	
	/**
	 * Get a message consisting of all ones.
	 * @param card The cardinality of the message.
	 * @return All-ones message.
	 */
	public static DiscreteMessage allOnesMessage(int card)
	{
		DiscreteMessage ret = new DiscreteMessage(card);
		for(int i = 0; i < card ; i++)
			ret.message_values[i] = 1;
		return ret;
	}
	
	public static DiscreteMessage normalMessage(int card)
	{
		DiscreteMessage ret = new DiscreteMessage(card);
		for(int i = 0; i < card; i++)
			ret.message_values[i] = 1.0/((double)card);
		return ret;
	}
	
	/**
	 * Set the value of this message.
	 * @param index Index at which to set.
	 * @param value Value to set.
	 */
	public void setValue(int index, double value)
	{
		this.message_values[index] = value;
	}
	
	/**
	 * Get the cardinality of this message.	
	 * @return Integer cardinality
	 */
	public int getCardinality()
	{
		return this.message_values.length;
	}
	
	/**
	 * Get the value of this message
	 * @param index At this index
	 * @return The value
	 */
	public double getValue(int index)
	{
		return this.message_values[index];
	}
	
	public void adjustCardinality(int newCard)
	{
		this.message_values = new double[newCard];
	}
	
	public void adopt(Message msg) throws BNException
	{
		if(!(msg instanceof DiscreteMessage))
			throw new BNException("Attempted to adopt message values of non-discrete message.");
		DiscreteMessage dmsg = (DiscreteMessage)msg;
		if(dmsg.message_values.length!=this.message_values.length)
			throw new BNException("Attempted to adopt message of differing cardinality.");
		for(int i = 0;i < this.message_values.length; i++)
			this.message_values[i] = dmsg.message_values[i];
	}

	private double[] message_values;
	private static final long serialVersionUID = 50L;
}
