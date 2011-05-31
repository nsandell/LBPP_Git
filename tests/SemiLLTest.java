package tests;

import bn.IBayesNet.RunResults;
import bn.distributions.CountdownDistribution;
import bn.distributions.DiscreteCPT;
import bn.distributions.PoissonCountdown;
import bn.distributions.CountdownDistribution.SwitchingCountdownDistribution;
import bn.dynamic.ICountdownNode;
import bn.dynamic.IDynamicBayesNet;
import bn.dynamic.IFDiscDBNNode;
import bn.impl.dynbn.DynamicNetworkFactory;

public class SemiLLTest {

	public static void main(String[] args) throws Exception
	{
		test1();
//		test2();
	}
	
	public static void test1() throws Exception
	{

		IDynamicBayesNet net = DynamicNetworkFactory.newDynamicBayesNet(100);
		ICountdownNode z = net.addSwitchingCountdownNode("Z", 50);
		IFDiscDBNNode x = net.addDiscreteNode("X", 2);
		SwitchingCountdownDistribution scd = new SwitchingCountdownDistribution();
		IFDiscDBNNode y = net.addDiscreteNode("Y", 2);
		scd.distributions = new CountdownDistribution[2];
		scd.distributions[0] = new PoissonCountdown(50,5.0);
		scd.distributions[1] = new PoissonCountdown(50,25.0);
		z.setAdvanceDistribution(scd);
		y.setAdvanceDistribution(new DiscreteCPT(new double[][]{{.9,.1},{.1,.9}}, 2));
		x.setInitialDistribution(new DiscreteCPT(new double[][]{{1.0,0.0},{1.0,0.0}},2));
		x.setAdvanceDistribution(new DiscreteCPT(new int[]{2,2}, 2, new double[][]{{0,1},{1,0},{1,0},{0,1}}));
		
		net.addIntraEdge(z, x);
		net.addIntraEdge(x, y);
		net.addInterEdge(x, x);
		net.addInterEdge(x, z);
		
		net.validate();
		
		y.setValue(new int[]{0,0,0,0,0,1,1,0,1,1,1,1,0,1,0,1,1,1,1,1,0,1,1,1,1,0,1,1,1,0,0,1,1,1,1,0,0,0,0,0,0,1,1,1,1,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,1,0,0,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1,1,1,0,1,1,0},0);
		//y.setValue(new int[]{1,0,0,0,0,0,0,0,1,1,0,1,1,0,1,1,1,1,1,1,0,1,1,1,1,1,1,1,1,1,1,0,0,0,0,0,0,0,1,0,1,1,1,1,1,0,1,1,1,0,1,1,1,1,1,1,1,1,1,0,1,1,0,0,0,0,0,1,1,1,1,1,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,0,0,0,0,0,1,1,1,1,1},0);
	
		RunResults res = net.run(50,0);

		System.out.println("Z");
		for(int i = 0; i < 5; i++)
		{
			for(int t = 0; t < 50; t++)
			{
				System.out.print(z.getMarginal(t).getValue(i) + " ");
			}
			System.out.println();
		}
		System.out.println();
		
		System.out.println("X");
		for(int i = 0; i < 2; i++)
		{
			for(int t = 0; t < 50; t++)
			{
				System.out.print(x.getMarginal(t).getValue(i) + " ");
			}
			System.out.println();
		}
		System.out.println();
		
		System.out.println(res.numIts + " - " + res.error);
		System.out.println(net.getLogLikelihood());
	}
	
	public static void test2() throws Exception
	{

		double[][] cutdist = new double[10][2];
		cutdist[0][0] = 1;
		for(int i = 1; i < 10; i++)
			cutdist[i][1] = 1;
		IDynamicBayesNet net = DynamicNetworkFactory.newDynamicBayesNet(500);
		ICountdownNode z = net.addSwitchingCountdownNode("Z", 10);
		IFDiscDBNNode zc = net.addDiscreteNode("ZC", 2);
		zc.setAdvanceDistribution(new DiscreteCPT(cutdist,2));
		IFDiscDBNNode x = net.addDiscreteNode("X", 2);
		SwitchingCountdownDistribution scd = new SwitchingCountdownDistribution();
		IFDiscDBNNode y = net.addDiscreteNode("Y", 2);
		scd.distributions = new CountdownDistribution[2];
		scd.distributions[0] = new PoissonCountdown(10,1.0);
		scd.distributions[1] = new PoissonCountdown(10,2.0);
		z.setAdvanceDistribution(scd);
		y.setAdvanceDistribution(new DiscreteCPT(new double[][]{{.9,.1},{.1,.9}}, 2));
		x.setInitialDistribution(new DiscreteCPT(new double[][]{{1.0,0.0},{1.0,0.0}},2));
		x.setAdvanceDistribution(new DiscreteCPT(new int[]{2,2}, 2, new double[][]{{0,1},{1,0},{.9,.1},{.9,.1}}));
		
		net.addIntraEdge(z, zc);
		net.addIntraEdge(zc, x);
		net.addIntraEdge(x, y);
		net.addInterEdge(x, x);
		net.addInterEdge(x, z);
		
		net.validate();
		
		y.setValue(new int[]{0,1,1,1,0,0,1,1,1,0,0,1,1,1,0,0,1,1,1,0,0,1,1,1,0,0,1,1,1,0,0,1,1,1,0,0,1,1,1,0,0,1,1,1,0,0,1,1,1,0,}, 0);
	
		RunResults res = net.run(50,0);

		System.out.println("Z");
		for(int i = 0; i < 5; i++)
		{
			for(int t = 0; t < 5; t++)
			{
				System.out.print(z.getMarginal(t).getValue(i) + " ");
			}
			System.out.println();
		}
		System.out.println();
		
		System.out.println("X");
		for(int i = 0; i < 2; i++)
		{
			for(int t = 0; t < 5; t++)
			{
				System.out.print(x.getMarginal(t).getValue(i) + " ");
			}
			System.out.println();
		}
		System.out.println();
		
		System.out.println(res.numIts + " - " + res.error);
		System.out.println(net.getLogLikelihood());
	}
}
