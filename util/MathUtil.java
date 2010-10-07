package util;

import java.util.Random;

/**
 * Generic math and random number generation utilities.
 * @author Nils F. Sandell
 *
 */
public class MathUtil
{
	public static class MathUtilException extends Exception
	{
		private static final long serialVersionUID = 1L;

		public MathUtilException(String err)
		{
			super(err);
		}
	}
	
	public static double logsum(double[] vals) throws MathUtilException
	{
		double max = Double.NEGATIVE_INFINITY;
		for(int i = 0; i < vals.length; i++)
			max = Math.max(vals[i], max);
		if(max >= 0)
			throw new MathUtilException("Logsum expects a nonpositive vector with at least one nonzero entry.");
		double innersum = 0;
		for(int i = 0; i < vals.length; i++)
			innersum += Math.exp(vals[i]-max);
		return max+Math.log(innersum);	
	}
	
	public static Random rand = new Random();
}