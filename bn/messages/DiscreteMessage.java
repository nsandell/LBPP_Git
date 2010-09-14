package bn.messages;

import bn.BayesNet.BNException;

public class DiscreteMessage
{
	public DiscreteMessage(int cardinality)
	{
		this.message_values = new double[cardinality];
		for(int i = 0; i < cardinality; i++)
			this.message_values[i] = 0;
		this.cardinality = cardinality;
		this.isDelta = false;
	}
	
	public DiscreteMessage(int cardinality, int deltaIndex)
	{
		this.cardinality = cardinality;
		this.deltaIndex = deltaIndex;
		this.isDelta = true;
	}
	
	public void normalize()
	{
		if(this.isDelta)
			return;
		double sum = 0;
		for(int i = 0; i < message_values.length; i++)
			sum += message_values[i];
		if(sum==0) return;
		for(int i = 0; i < message_values.length; i++)
			message_values[i] /= sum;
	}
	
	public DiscreteMessage divide(DiscreteMessage other) throws BNException
	{
		if(other.cardinality!=this.cardinality)
			throw new BNException("Attempted to multiply messages of unequal length.");
		if(this.isDelta)
		{
			DiscreteMessage ret = new DiscreteMessage(this.cardinality);
			ret.setValue(this.deltaIndex, 1.0/other.getValue(this.deltaIndex));
			return ret;
		}
		else
		{
			DiscreteMessage ret = new DiscreteMessage(this.cardinality);
			for(int i = 0; i < this.cardinality; i++)
				ret.setValue(i, this.message_values[i]/other.message_values[i]);
			return ret;
		}
	}
	
	public DiscreteMessage multiply(DiscreteMessage other) throws BNException
	{
		if(other.cardinality!=this.cardinality)
			throw new BNException("Attempted to multiply messages of unequal length.");
		if(this.isDelta || other.isDelta)
		{
			double val = other.getValue(this.deltaIndex);
			if(val==1)
				return new DiscreteMessage(this.cardinality, this.deltaIndex);
			else
			{
				DiscreteMessage ret = new DiscreteMessage(this.cardinality);
				ret.setValue(this.deltaIndex, val);
				return ret;
			}
		}
		else
		{
			DiscreteMessage ret = new DiscreteMessage(this.cardinality);
			for(int i = 0; i < message_values.length; i++)
				ret.message_values[i] = this.message_values[i] * other.message_values[i];
			return ret;
		}
	}
	
	public static DiscreteMessage allOnesMessage(int card)
	{
		DiscreteMessage ret = new DiscreteMessage(card);
		for(int i = 0; i < card ; i++)
			ret.message_values[i] = 1;
		return ret;
	}
	
	public void setValue(int index, double value)
	{
		if(this.isDelta && (index!=deltaIndex || value!=1))
		{
			this.isDelta = false;
			this.message_values = new double[cardinality];
			this.message_values[deltaIndex] = 1;
		}
		this.message_values[index] = value;
	}
	
	public int getCardinality()
	{
		return this.cardinality;
	}
	
	public double getValue(int index)
	{
		if(!this.isDelta)
		{
			return this.message_values[index];
		}
		else
		{
			if(index==deltaIndex)
				return 1;
			else
				return 0;
		}
	}

	private int cardinality;
	private boolean isDelta;
	private int deltaIndex;
	private double[] message_values;
}
