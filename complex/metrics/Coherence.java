package complex.metrics;

import java.util.Vector;

import util.MathUtil;

public class Coherence
{
	public static interface DisagreementMeasure
	{
		public double getDisagreement(int t);
	}
	
	//TODO A more apropos name than coherence?
	public static double coherence(Vector<? extends DisagreementMeasure> measures, int T)
	{
		double coherence = 0;
		double[] maxes = new double[measures.size()];
		
		for(int t = 0; t < T; t++)
			for(int idx = 0; idx < measures.size(); idx++)
				maxes[idx] = Math.max(maxes[idx], measures.get(idx).getDisagreement(t));
		
		for(int t = 0; t < T; t++)
		{
			double mean = 0;
			for(int idx = 0; idx < measures.size(); idx++)
				mean += measures.get(idx).getDisagreement(t)/maxes[idx];
			mean /= measures.size();
			for(int idx = 0; idx < measures.size(); idx++)
				coherence += Math.pow(measures.get(idx).getDisagreement(t)/maxes[idx]-mean,2);
		}
		
		return coherence;
	}
	
	public static boolean[] partition(Vector<? extends DisagreementMeasure> measures, int T)
	{
		double[][] normalized = new double[measures.size()][T];
		double[] centroid1 = new double[T];
		double[] centroid2 = new double[T];
		for(int i = 0; i < measures.size(); i++)
		{
			double max = 0;
			for(int t = 0; t < T; t++)
				max = Math.max(max, measures.get(i).getDisagreement(t));
			for(int t = 0; t < T; t++)
				normalized[i][t] = measures.get(i).getDisagreement(t)/max;
		}
		
		int seed1 = MathUtil.rand.nextInt(measures.size());
		int seed2 = MathUtil.rand.nextInt(measures.size()-1);
		if(seed2 > seed1) seed2++;
		boolean same = true;
		
		for(int t = 0; t < T; t++)
		{
			centroid1[t] = normalized[seed1][t];
			centroid2[t] = normalized[seed2][t];
			if(centroid1[t]!=centroid2[t])
				same = false;
		}
		if(same)
		{
			for(int t = 0; t < T; t++)
				centroid1[t] = centroid1[t] + MathUtil.rand.nextDouble()/10;
		}
		
		boolean[] membership = randSeed(measures.size());
		//getCentroids(membership,normalized,centroid1,centroid2);
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
			getCentroids(membership,normalized,centroid1,centroid2);
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
