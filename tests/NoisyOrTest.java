package tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import bn.BNException;
import bn.IDiscreteBayesNode;
import bn.IStaticBayesNet;
import bn.distributions.DiscreteCPTUC;
import bn.distributions.ScalarNoisyOr;
import bn.distributions.Distribution.SufficientStatistic;
import bn.impl.BayesNetworkFactory;

public class NoisyOrTest
{
	public static void main(String[] args) throws BNException
	{
		test1();
	}
	
	public static void test1() throws BNException
	{
		IStaticBayesNet snet = BayesNetworkFactory.getStaticNetwork();

		IDiscreteBayesNode x = snet.addDiscreteNode("X", 2);
		IDiscreteBayesNode y = snet.addDiscreteNode("Y", 2);
		snet.addEdge(x, y);
		snet.setDistribution("X",new DiscreteCPTUC(new double[]{.5,.5}));
		snet.setDistribution("Y",new ScalarNoisyOr(.85));
	
		ArrayList<String> names = new ArrayList<String>();
		names.add("Y");
		Random rand = new Random();
		int[] ydat = new int[1000];
		double onesFrac = 0;
		for(int trial = 0; trial < 1000; trial++)
		{
			if(rand.nextBoolean()) //x = 0
				ydat[trial] = 0;
			else if(rand.nextDouble() <= .8)
			{
				ydat[trial] = 1;
				onesFrac++;
			}
			else
				ydat[trial] = 0;
			
		}
		onesFrac /= 1000.0;
		for(int i = 0; i < 50; i++)
		{
			HashMap<String, SufficientStatistic> stats = new HashMap<String, SufficientStatistic>();
			for(int trial = 0; trial < 1000; trial++)
			{
				y.setValue(ydat[trial]);
				snet.run(100, 0);
				snet.collectSufficientStatistics(names, stats);
			}
			System.out.println(1-onesFrac/.5);
			y.optimizeParameters(stats.get("Y"));
		}
		int a = 3;
		a*=3;
	}
	
	public static void test2() throws BNException
	{
		IStaticBayesNet snet = BayesNetworkFactory.getStaticNetwork();

		IDiscreteBayesNode y = snet.addDiscreteNode("Y", 2);
		IDiscreteBayesNode x = snet.addDiscreteNode("X", 2);
		IDiscreteBayesNode x2 = snet.addDiscreteNode("X2", 2);
		snet.addEdge(x, y);
		snet.addEdge(x2,y);
		snet.setDistribution("X",new DiscreteCPTUC(new double[]{.5,.5}));
		snet.setDistribution("X2",new DiscreteCPTUC(new double[]{.3,.7}));
		snet.setDistribution("Y",new ScalarNoisyOr(.8));
	
		ArrayList<String> names = new ArrayList<String>();
		names.add("Y");
		int N = 50000;
		int[] ydat = new int[N];
		double onesFrac = 0;
		double twosFrac = 0;
		for(int trial = 0; trial < N; trial++)
		{
			snet.sample();
			ydat[trial] = y.getValue();
			if(x.getValue()==1 && x2.getValue()==1)
				twosFrac++;
			else if(x.getValue()==1 || x2.getValue()==1)
				onesFrac++;
		}
		snet.setDistribution("Y", new ScalarNoisyOr(.9));
		snet.clearAllEvidence();
		for(int i = 0; i < 50; i++)
		{
			HashMap<String, SufficientStatistic> stats = new HashMap<String, SufficientStatistic>();
			for(int trial = 0; trial < N; trial++)
			{
				y.setValue(ydat[trial]);
				snet.run(100, 0);
				snet.collectSufficientStatistics(names, stats);
			}
			System.out.println(1-onesFrac/.5);
			y.optimizeParameters(stats.get("Y"));
		}
		int a = 3;
		a*=3;
	}
}
