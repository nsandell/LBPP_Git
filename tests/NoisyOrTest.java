package tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import bn.BNException;
import bn.IDiscreteBayesNode;
import bn.IStaticBayesNet;
import bn.IBayesNet.RunResults;
import bn.distributions.DiscreteCPTUC;
import bn.distributions.FlatNoisyOr;
import bn.distributions.ScalarNoisyOr;
import bn.distributions.Distribution.SufficientStatistic;
import bn.impl.BayesNetworkFactory;

public class NoisyOrTest
{
	public static void main(String[] args) throws BNException
	{
		test4();
	}
	
	public static void test1() throws BNException
	{
		IStaticBayesNet snet = BayesNetworkFactory.getStaticNetwork();

		snet.addDiscreteNode("X", 2);
		snet.addDiscreteNode("Y", 2);
		snet.addEdge("X", "Y");
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
				snet.addEvidence("Y", ydat[trial]);
				snet.run(100, 0);
				snet.collectSufficientStatistics(names, stats);
			}
			System.out.println(1-onesFrac/.5);
			snet.optimize(names, stats);
		}
		int a = 3;
		a*=3;
	}
	
	public static void test2() throws BNException
	{
		IStaticBayesNet snet = BayesNetworkFactory.getStaticNetwork();

		snet.addDiscreteNode("Y", 2);
		snet.addDiscreteNode("X", 2);
		snet.addDiscreteNode("X2", 2);
		snet.addEdge("X", "Y");
		snet.addEdge("X2","Y");
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
			//snet.sample(); //TODO I think this test is broken because of no sampling
			ydat[trial] = (Integer)snet.getEvidence("Y");//y.getValue();
			int x1val = (Integer)snet.getEvidence("X");
			int x2val = (Integer)snet.getEvidence("X2");
			if(x1val==1 && x2val==1)
				twosFrac++;
			else if(x1val==1 || x2val==1)
				onesFrac++;
		}
		snet.setDistribution("Y", new ScalarNoisyOr(.9));
		snet.clearAllEvidence();
		for(int i = 0; i < 50; i++)
		{
			HashMap<String, SufficientStatistic> stats = new HashMap<String, SufficientStatistic>();
			for(int trial = 0; trial < N; trial++)
			{
				snet.addEvidence("Y", ydat[trial]);
				//y.setValue(ydat[trial]);
				snet.run(100, 0);
				snet.collectSufficientStatistics(names, stats);
			}
			System.out.println(1-onesFrac/.5);
			snet.optimize(names, stats);
			//y.optimizeParameters(stats.get("Y"));
		}
		int a = 3;
		a*=3;
	}
	
	public static void test3() throws BNException
	{
		IStaticBayesNet snet = BayesNetworkFactory.getStaticNetwork();

		IDiscreteBayesNode y = snet.addDiscreteNode("Y", 2);
		IDiscreteBayesNode y2 = snet.addDiscreteNode("Y2", 2);
		IDiscreteBayesNode x = snet.addDiscreteNode("X", 2);
		snet.addEdge("X", "Y");
		snet.addEdge("X", "Y2");
		
		snet.setDistribution("X",new DiscreteCPTUC(new double[]{.7,.3}));
		snet.setDistribution("Y",new FlatNoisyOr(.8));
		snet.setDistribution("Y2",new FlatNoisyOr(.9));
		
		y.setValue(0);
		y2.setValue(1);
		
		RunResults rr = snet.run(20, 0);
		System.out.println("No X2");
		System.out.println(rr.numIts + " iterations.");
		System.out.println("P(X1==1)=" + x.getMarginal().getValue(1));

		IDiscreteBayesNode x2 =snet.addDiscreteNode("X2", 2);
		snet.addEdge("X2","Y2");
		snet.setDistribution("X2",new DiscreteCPTUC(new double[]{.6,.4}));


		rr = snet.run(20, 0);
		System.out.println("X2");
		System.out.println(rr.numIts + " iterations.");	
		System.out.println("P(X1==1)=" + x.getMarginal().getValue(1));
		System.out.println("P(X2==1)=" + x2.getMarginal().getValue(1));
		
		snet.removeNode(x2);
		rr = snet.run(20,0);
		System.out.println("No X2");
		System.out.println(rr.numIts + " iterations.");
		System.out.println("P(X1==1)=" + x.getMarginal().getValue(1));
		
		x2 =snet.addDiscreteNode("X2", 2);
		snet.addEdge("X2","Y2");
		snet.setDistribution("X2",new DiscreteCPTUC(new double[]{.6,.4}));

		rr = snet.run(20, 0);
		System.out.println("X2");
		System.out.println(rr.numIts + " iterations.");	
		System.out.println("P(X1==1)=" + x.getMarginal().getValue(1));
		System.out.println("P(X2==1)=" + x2.getMarginal().getValue(1));
	}
	
	public static void test4() throws BNException
	{
		IStaticBayesNet snet = BayesNetworkFactory.getStaticNetwork();
		IDiscreteBayesNode x1 = snet.addDiscreteNode("X1", 2);
		IDiscreteBayesNode x2 = snet.addDiscreteNode("X2", 2);
		snet.addDiscreteNode("X3", 2);
		snet.addDiscreteNode("Y", 2);
		snet.addEdge("X1", "Y");
		snet.addEdge("X2", "Y");
		snet.addEdge("X3", "Y");
		snet.setDistribution("X1", new DiscreteCPTUC(new double[]{.1,.9}));
		snet.setDistribution("X2", new DiscreteCPTUC(new double[]{.5,.5}));
		snet.setDistribution("X3", new DiscreteCPTUC(new double[]{.5,.5}));
		snet.setDistribution("Y", new FlatNoisyOr(.9));
		snet.validate();
		snet.addEvidence("Y", 1);
		snet.addEvidence("X1", 1);
		snet.addEvidence("X3", 1);
		snet.run(100, 0);
		System.out.println("P(X1==1)="+x1.getMarginal().getValue(1));
		System.out.println("P(X2==1)="+x2.getMarginal().getValue(1));
		System.out.println("BE:"+snet.getLogLikelihood());
	}
}
