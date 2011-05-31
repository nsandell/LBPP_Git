package tests;

import bn.BNException;
import bn.distributions.DiscreteCPT;
import bn.distributions.PoissonCountdown;
import bn.dynamic.ICountdownNode;
import bn.dynamic.IDynamicBayesNet;
import bn.dynamic.IFDiscDBNNode;
import bn.impl.dynbn.DynamicNetworkFactory;

public class CountdownTest {
	public static void main(String[] args) throws BNException
	{
		IDynamicBayesNet net = DynamicNetworkFactory.newDynamicBayesNet(500);
		ICountdownNode nd = net.addCountdownNode("X", 50);
		nd.setAdvanceDistribution(new PoissonCountdown(50,8.0));
		nd.lockParameters();
		
		IFDiscDBNNode nd2 = net.addDiscreteNode("Y", 2);
		nd2.setAdvanceDistribution(new DiscreteCPT(new double[][]{{.9, .1},{.1, .9}}, 2));
		net.addIntraEdge(nd, nd2);
		
		int[] obs = new int[]{1,1,1,1,1,1,1,1,0,1,1,1,1,1,1,0,1,1,1,1,1,0,1,1,0,1,1,1,1,1,0,1,1,1,1,1,0,1,1,0,1,1,0,1,0,0,1,1,1,1,1,1,1,1,0,1,1,1,1,0,1,1,1,1,0,1,1,1,1,0,0,1,1,1,1,1,1,0,1,1,1,1,1,1,1,1,1,1,1,0,1,1,1,1,0,1,1,1,1,1,1,1,0,1,1,1,0,1,0,1,0,1,1,1,1,0,1,1,1,0,0,1,1,1,1,1,1,0,0,1,1,1,1,1,1,1,1,1,0,1,1,1,1,1,1,1,1,1,0,1,1,1,1,1,0,1,1,1,0,1,0,1,1,1,1,0,1,1,1,1,1,0,1,1,1,1,1,1,0,1,1,1,1,0,1,1,1,1,1,1,1,1,0,1,1,1,1,1,1,1,1,1,0,0,1,1,0,1,1,1,1,1,1,0,1,0,1,1,1,1,1,1,1,0,1,1,1,1,1,1,1,1,1,1,1,1,0,1,1,1,1,1,1,1,1,1,1,0,1,1,1,0,1,1,1,1,1,1,1,1,1,1,1,0,1,0,1,1,1,1,1,1,0,1,1,1,1,1,1,1,1,1,1,0,1,1,1,1,1,1,1,1,0,1,1,0,1,1,1,0,1,1,1,1,1,1,1,1,1,1,1,1,1,0,1,1,1,1,1,1,1,0,1,1,1,1,1,1,0,1,1,1,1,1,0,1,1,0,0,1,0,1,1,1,1,1,1,0,0,1,1,1,1,1,1,0,1,1,1,1,1,0,1,1,1,1,1,0,1,1,1,1,1,1,0,1,1,1,1,1,1,1,1,0,1,1,1,1,1,1,1,1,1,1,1,1,0,0,1,0,1,1,1,1,1,0,1,1,0,1,1,1,0,0,1,1,0,1,1,0,1,0,1,1,1,0,1,1,1,1,1,1,1,1,0,1,1,1,0,1,1,0,1,1,1,1,0,0,1,1,0,1,1,1,1,1,1,0,1,0,1,1,1,1,1,1,1,0,1,0,1,1,1,1,1,1,0,1,1,1,0,1,1,1,1,1,1,1,1,1,1,0,1,1,1,1,1,0,1,1};
		nd2.setValue(obs, 0);
		
		System.out.println("NumIts: " + net.run(100,0).numIts);
		net.run(1,0);
		net.optimize_parallel(1, 0, 100, 0);
	
		for(int t = 0; t < 500; t++)
			System.out.print(nd.getMarginal(t).getValue(0)  + " ");
		System.out.println();
		for(int t = 0; t < 500; t++)
		{
			double ez = 0;
			for(int i= 0; i < 50; i++)
				ez += nd.getMarginal(t).getValue(i)*((double)i);
			System.out.print(ez + " ");
		}
		System.out.println();
		
		nd2.getAdvanceDistribution().printDistribution(System.out);
		nd.getAdvanceDistribution().printDistribution(System.out);
	}
}
