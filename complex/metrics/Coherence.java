package complex.metrics;

import util.MathUtil;

public class Coherence
{
	public static interface DisagreementMeasure
	{
		public double[] getDisagreement();
	}
	
	//TODO A more apropos name than coherence?
	public static double coherence(DisagreementMeasure[] measures, int T)
	{
		double coherence = 0;
		double[] maxes = new double[measures.length];
		
		for(int t = 0; t < T; t++)
			for(int idx = 0; idx < measures.length; idx++)
				maxes[idx] = Math.max(maxes[idx], measures[idx].getDisagreement()[t]);
		
		for(int t = 0; t < T; t++)
		{
			double mean = 0;
			for(int idx = 0; idx < measures.length; idx++)
				mean += measures[idx].getDisagreement()[t]/maxes[idx];
			mean /= measures.length;
			for(int idx = 0; idx < measures.length; idx++)
				coherence += Math.pow(measures[idx].getDisagreement()[t]/maxes[idx]-mean,2);
		}
		
		return coherence;
	}
	
	public static boolean[] partition(DisagreementMeasure[] measures, int T)
	{
		double[][] normalized = new double[measures.length][T];
		double[] centroid1 = new double[T];
		double[] centroid2 = new double[T];
		for(int i = 0; i < measures.length; i++)
		{
			double max = 0;
			for(int t = 0; t < T; t++)
				max = Math.max(max, measures[i].getDisagreement()[t]);
			for(int t = 0; t < T; t++)
				normalized[i][t] = measures[i].getDisagreement()[t]/max;
		}
		
		boolean[] membership = randSeed(measures.length);
		getCentroids(membership,normalized,centroid1,centroid2);
		boolean changed = true;
		while(changed)
		{
			changed = false;
			for(int i = 0; i < membership.length; i++)
			{
				boolean assignment = assign(normalized[i],centroid1,centroid2);
				if(assignment!=membership[i])
					changed = true;
				membership[i] = assignment;
			}
		}
		return membership;
	}
	
	private static void getCentroids(boolean[] membership, double[][] data, double[] centroid1, double[] centroid2)
	{
		double num1 = 0, num2 = 0;
		
		for(int t = 0; t < centroid1.length; t++)
		{
			centroid1[t] = 0; centroid2[t] = 0;
		}
		
		for(int i = 0; i < membership.length; i++)
		{
			if(membership[i])
			{
				num2++;
				for(int t = 0; t < centroid1.length; t++)
					centroid2[t] += data[i][t];
			}
			else
			{
				num1++;
				for(int t = 0; t < centroid1.length; t++)
					centroid1[t] += data[i][t];
			}
		}
		for(int t = 0; t < centroid1.length; t++)
		{
			centroid1[t] /= num1;
			centroid2[t] /= num2;
		}
	}
	
	private static boolean assign(double[] data, double[] centroid1, double[] centroid2)
	{
		double distance1 = 0;
		double distance2 = 0;
		for(int t = 0; t < data.length; t++)
		{
			distance1 += Math.pow(data[t]-centroid1[t], 2);
			distance2 += Math.pow(data[t]-centroid2[t], 2);
		}
		return distance1 > distance2;
	}
	
	
	private static boolean[] randSeed(int L)
	{
		boolean[] ret = new boolean[L];
		
		boolean allTrue = true;
		
		for(int i = 0; i < L; i++)
		{
			if(MathUtil.rand.nextDouble() > .5)
			{
				ret[i] = false;
				allTrue = false;
			}
			else ret[i] = true;
		}
		if(allTrue)
		{
			int switcher = MathUtil.rand.nextInt(L);
			ret[switcher] = false;
		}
		return ret;
	}
	
}
