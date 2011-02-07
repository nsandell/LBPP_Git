package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

/**
 * Generic math and random number generation utilities.
 * @author Nils F. Sandell
 *
 */
public class MathUtil
{
	public static void main(String[] args)
	{
		boolean[][] input = {{false,true,false,true},{true,true,false,false},{false,true,true,true},{false,true,true,false},{true,false,false,false}};
		input = lofSort(input);
		for(int j = 0; j < input[0].length; j++)
		{
			for(int i = 0; i < input.length; i++)
			{
				if(input[i][j])
					System.out.print(" 1");
				else 
					System.out.print(" 0");
			}
			System.out.println();
		}
	}
	
	public static boolean[][] lofSort(boolean[][] input)
	{
		ArrayList<boolean[]> entries = new ArrayList<boolean[]>();
		for(int i = 0; i < input.length; i++)
			entries.add(input[i]);
		Collections.sort(entries,LOFCOLComparer.singleton);
		for(int i = 0; i < input.length; i++)
			input[i] = entries.get(i);
		return input;
	}
	
	private static class LOFCOLComparer implements Comparator<boolean[]>
	{
		public static LOFCOLComparer singleton = new LOFCOLComparer();
		
		@Override
		public int compare(boolean[] o1, boolean[] o2) throws ClassCastException
		{
			if(o1.length!=o2.length) throw new ClassCastException();
			for(int i = o1.length-1; i >= 0; i--)
			{
				if(o1[i] && !o2[i])
					return -1;
				if(o2[i] && !o1[i])
					return 1;
			}
			return 0;
		}
		
	}
	
	public static LOFResults lofSort2(boolean[][] input)
	{
		ArrayList<LOFRow> entries = new ArrayList<LOFRow>();
		for(int i = 0; i < input.length; i++)
			entries.add(new LOFRow(input[i],i));
		Collections.sort(entries,LOFCOLComparer2.singleton);
		int[] indices = new int[entries.size()];
		for(int i = 0; i < input.length; i++)
		{
			input[i] = entries.get(i).rowdata;
			indices[i] = entries.get(i).rowindex;
		}
		LOFResults res = new LOFResults(); res.matrix = input; res.reordering = indices;
		return res;
	}
	
	private static class LOFRow
	{
		public LOFRow(boolean[] row, int index){this.rowdata = row;this.rowindex = index;}
		boolean[] rowdata;
		int rowindex;
	}
	
	public static class LOFResults
	{
		public boolean[][] matrix;
		public int[] reordering;
	}
	
	private static class LOFCOLComparer2 implements Comparator<LOFRow>
	{
		public static LOFCOLComparer2 singleton = new LOFCOLComparer2();
		
		@Override
		public int compare(LOFRow r1, LOFRow r2) throws ClassCastException
		{
			boolean[] o1 = r1.rowdata; boolean[] o2 = r2.rowdata;
			if(o1.length!=o2.length) throw new ClassCastException();
			for(int i = o1.length-1; i >= 0; i--)
			{
				if(o1[i] && !o2[i])
					return -1;
				if(o2[i] && !o1[i])
					return 1;
			}
			return 0;
		}
		
	}
	
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
	
	public static double logsum(double[] vals, int maxi)
	{
		double maxv = vals[maxi];
		double innersum = 0;
		for(int i = 0; i < vals.length; i++)
			innersum += Math.exp(vals[i]-maxv);
		return maxv+Math.log(innersum);
	}
	
	public static double logPoissPDF(double lambda, int k)
	{
		double v = k*Math.log(lambda)-lambda;
		for(int i = 2; i <= k;  i++)
			v -= Math.log(i);
		return v;
	}
	
	public static int discreteSample(double[] p)
	{
		double choice = rand.nextDouble();

		int i = 0;
		while(choice > p[i])
		{
			choice -= p[i];
			i++;
		}
		
		return i;
	}
	
	/*
	public static Matrix normalVector(int len)
	{
		Matrix ret = new Matrix(len, 1);
		for(int i =0; i < len; i++)
			ret.set(i, 0, rand.nextGaussian());
		return ret;
	}*/
	
	public static Random rand = new Random();
}