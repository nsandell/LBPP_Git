package bn.distributions;

import java.io.PrintStream;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.Vector;

import bn.BNException;
import bn.interfaces.Printable;
import bn.messages.DiscreteMessage;

/**
 * Generic distribution interface.  Minimal interface for handling top level
 * methods irrespective of discrete, continuous, etc.  Mainly used as generic
 * argument type that will be cast down.
 * @author Nils F. Sandell
 */
public interface Distribution extends Printable, Serializable
{
	/**
	 * Get an empty sufficient statistic object that this distribution can 
	 * operate and optimize on.
	 * @return The sufficient statistic object.
	 */
	public SufficientStatistic getSufficientStatisticObj();
	
	/**
	 * Optimize this distribution based on a sufficient statistic object.
	 * @param obj The statistic to optimize on.
	 * @return The resulting change in optimizing.
	 * @throws BNException If the statistic is of the incorrect type.
	 */
	public double optimize(SufficientStatistic obj) throws BNException;
	
	/**
	 * Copy this distribution.
	 * @return An identical copy of this distribution
	 * @throws BNException
	 */
	public Distribution copy() throws BNException;
	
	public void printDistribution(PrintStream pr);
	
	/**
	 * Base interface for sufficient statistics.  They should be able to empty themselves
	 * and update themselves given a similar sufficient statistic.
	 * @author Nils F. Sandell
	 */
	public static interface SufficientStatistic
	{
		/**
		 * Reset this sufficient statistic to contain no information.
		 */
		public void reset();
		
		/**
		 * Merge a sufficient statistic object into "this" statistic, and return self.
		 * @param stat Other sufficient statistic object to merge into "this"
		 * @return "This"
		 * @throws BNException If "this" object and parameter object don't line up properly.
		 * 		(in type or dimension, for example).
		 */
		public SufficientStatistic update(SufficientStatistic stat) throws BNException;
	}
	
	/**
	 * A discrete sufficent statistic object, which should be able to update based on discrete
	 * messages about the node it models.
	 * @author Nils F. Sandell
	 */
	public static interface DiscreteSufficientStatistic extends SufficientStatistic
	{
		@Override // Same as inherited method, but narrows scope of return type.
		public DiscreteSufficientStatistic update(SufficientStatistic stat) throws BNException;
		
		/**
		 * Update this sufficient statistic given  discrete messages describing the state of this variable
		 * @param lambda Local lambda message a node with this CPD
		 * @param pi Local pi message for a node with this CPD (matching lambda)
		 * @param incomingPis Incoming pi messages for a node with this CPD (matching lambda)
		 * @return "This"
		 * @throws BNException If the messages are invalid.
		 */
		public DiscreteSufficientStatistic update(DiscreteMessage lambda, Vector<DiscreteMessage> incomingPis) throws BNException;
	}
	
	/**
	 * An index wrapper function such that we can pass discrete CPDs parent values either through
	 * an array of integers or a vector of objects that have an integer value (i.e. discrete nodes)
	 * @author Nils F. Sandell
	 */
	public static class ValueSet<ValueType>
	{
		/**
		 * Constructor if we want to use array of ints
		 * @param values Conditioning variable values
		 */
		public ValueSet(ValueType[] values)
		{
			this.values = values;
		}
		
		/**
		 * Constructor if we want to use an ArrayList of objects that have integer values
		 * @param list The arraylist
		 */
		public ValueSet(ArrayList<? extends ValueObject<ValueType>> list, int context)
		{
			this.valueObjects = list;
		}
		
		/**
		 * Objects that have an integer value can implement this to be used in a set
		 * @author Nils F Sandell
		 */
		public static interface ValueObject<ValueType>
		{
			/**
			 * Get the integer value associated with this object
			 * @return The integer value
			 * @throws BNException If the object doesn't currently have a value.
			 */
			public ValueType getValue(int context) throws BNException;
		}
		
		/**
		 * Get a value from this set.
		 * @param i Gets the 'ith' value
		 * @return The value desired.
		 * @throws BNException If that value doesn't exist
		 */
		public final ValueType getValue(int i) throws BNException
		{
			return values==null ? valueObjects.get(i).getValue(this.context) : values[i];
		}
		
		/**
		 * Get the number of values in this set.
		 * @return The number of values
		 */
		public final int length()
		{
			return values==null ? valueObjects.size() : values.length;
		}
		
		private ValueType[] values = null;
		private ArrayList<? extends ValueObject<ValueType>> valueObjects = null;
		int context;
	}
	
	public static class BetheTerms
	{
		double E = 0;
		double H = 0;
	}
}
