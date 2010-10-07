package bn.messages;

import bn.BNException;

/**
 * Class that implements message functionality between discrete nodes
 * and nodes that can be connected to discrete nodes.
 * @author Nils F. Sandell
 */
public class DiscreteMessage
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
		this.cardinality = cardinality;
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
	
	/**
	 * Perform entry-wise division between this message and an argument message,
	 * storing the result in a new message.
	 * @param other The other message (the denominator).
	 * @return Resultant message
	 * @throws BNException If the two operands are of unequal length.
	 */
	public DiscreteMessage divide(DiscreteMessage other) throws BNException
	{
		if(other.cardinality!=this.cardinality)
			throw new BNException("Attempted to multiply messages of unequal length.");
		DiscreteMessage ret = new DiscreteMessage(this.cardinality);
		for(int i = 0; i < this.cardinality; i++)
			ret.setValue(i, this.message_values[i]/other.message_values[i]);
		return ret;
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
		if(other.cardinality!=this.cardinality)
			throw new BNException("Attempted to multiply messages of unequal length.");
		DiscreteMessage ret = new DiscreteMessage(this.cardinality);
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
		return this.cardinality;
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

	private int cardinality;
	private double[] message_values;
}
