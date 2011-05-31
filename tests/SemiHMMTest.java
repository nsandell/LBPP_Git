package tests;

import bn.distributions.DiscreteCPT;
import bn.distributions.PoissonCountdown;
import bn.dynamic.ICountdownNode;
import bn.dynamic.IDynamicBayesNet;
import bn.dynamic.IFDiscDBNNode;
import bn.impl.dynbn.DynamicNetworkFactory;

public class SemiHMMTest {
	public static void main(String[] args) throws Exception
	{
		IDynamicBayesNet net = DynamicNetworkFactory.newDynamicBayesNet(500);
		
		ICountdownNode z = net.addCountdownNode("Z", 50);
		IFDiscDBNNode x = net.addDiscreteNode("X", 2);
		IFDiscDBNNode y = net.addDiscreteNode("Y", 2);
		
		net.addInterEdge(x, x);
		net.addIntraEdge(z, x);
		net.addIntraEdge(x, y);
		
		z.setAdvanceDistribution(new PoissonCountdown(50,25.0));
		//x.setAdvanceDistribution(new DiscreteCPT(new int[]{2,2}, 2, new double[][]{{0,1},{1,0},{.9,.1},{.1,.9}})); //TRUTH
		//x.setAdvanceDistribution(new DiscreteCPT(new int[]{2,2}, 2, new double[][]{{1,0},{0,1},{.99,.01},{.01,.99}})); 
		//x.setAdvanceDistribution(new DiscreteCPT(new int[]{2,2}, 2, new double[][]{{0,1},{1,0},{1,0},{0,1}})); 
		x.setAdvanceDistribution(new DiscreteCPT(new int[]{2,2}, 2, new double[][]{{0,1},{1,0},{1,0},{0,1}})); 
		x.setInitialDistribution(new DiscreteCPT(new double[][]{{1,0},{1,0}},2));
		//y.setAdvanceDistribution(new DiscreteCPT(new double[][]{{.9,.1},{.1,.9}}, 2));
		y.setAdvanceDistribution(new DiscreteCPT(new double[][]{{.98,.02},{.2,.8}}, 2));
		
		net.validate();
		
		int[] obs = new int[]{0,0,0,0,0,0,1,1,1,0,1,1,1,1,1,1,1,0,1,1,1,1,0,0,0,1,0,1,0,0,0,0,1,1,1,1,1,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,1,0,1,1,1,1,1,1,1,1,1,1,1,0,1,0,0,0,0,0,1,0,0,0,0,1,1,1,1,1,1,1,1,0,0,1,1,0,0,0,0,0,0,0,0,1,0,0,0,0,0,1,1,1,0,0,0,0,0,0,0,0,0,0,0,0,1,1,1,1,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,0,0,0,0,0,0,0,1,0,1,1,1,1,1,1,1,0,1,1,0,1,1,1,1,1,0,0,0,0,0,0,0,0,0,0,1,1,0,1,1,0,1,1,1,1,1,1,1,1,1,0,0,0,0,0,0,0,1,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,0,0,0,0,0,0,0,1,0,0,1,0,1,1,1,1,1,0,0,0,0,0,0,0,0,0,0,1,1,1,1,1,1,1,1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,1,1,1,1,1,0,0,1,0,0,1,1,0,1,0,0,0,1,1,1,1,1,0,1,1,1,0,0,0,0,0,0,0,0,0,0,0,1,1,1,1,1,1,1,1,0,0,0,0,0,0,0,0,0,0,0,1,1,1,1,1,1,1,1,1,1,1,1,1,0,0,0,0,0,0,0,1,0,1,1,1,1,1,1,1,1,1,0,1,0,0,0,0,1,0,0,0,0,0,1,0,1,0,1,1,1,1,0,1,1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,1,1,1,1,0,1,1,1,1,1,0,0,0,0,0,0,0,1,0,0,0,1,1,1,1,0,1,0,1,0,1,1,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,1,1,1,1,1,0,0,0,0,1,0,1,0,0,0,0,0,1,1,1,1,1,1,1,1,1,1,1,0,1,0,0,0,0,0,0,0,0,1,1,1,1,1,1,1,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,1,1,0};
		y.setValue(obs,0);
		
		net.run(100,0);
		//net.optimize(1, 0, 50, 1e-6);
		//net.run(50,0);
		net.optimize_parallel_queue(20, 0, 30, 1e-6);
		//System.out.println("Converged in " + rr.numIts + " Iterations with error " + rr.error);
		
		for(int i = 0; i < 2; i++)
		{
			for(int t = 0; t < 500; t++)
				System.out.print(x.getMarginal(t).getValue(i) + " ");
			System.out.println();
		}
		for(int t= 0; t < 500; t++)
		{
			//double ml = 0; int mli = 0;
			double ez = 0;
			for(int i = 0; i < 30; i++)
			{
				//if(z.getMarginal(t).getValue(i) > ml)
				//{
				ez += z.getMarginal(t).getValue(i)*((double)i);
					//ml = z.getMarginal(t).getValue(i);
					//mli = i;
				//}
			}
			System.out.print(ez + " ");
		}
		System.out.println();
		
		z.getAdvanceDistribution().printDistribution(System.out);
		x.getAdvanceDistribution().printDistribution(System.out);
		y.getAdvanceDistribution().printDistribution(System.out);
	}
}
