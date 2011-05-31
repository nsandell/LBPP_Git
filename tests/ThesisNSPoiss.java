package tests;

import java.io.File;
import java.io.PrintStream;
import java.util.Scanner;

import util.MathUtil;

import bn.BNException;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.distributions.SwitchingPoisson;
import bn.dynamic.IDynamicBayesNet;
import bn.dynamic.IFDiscDBNNode;
import bn.dynamic.IInfDiscEvDBNNode;
import bn.impl.dynbn.DynamicNetworkFactory;

public class ThesisNSPoiss {

	public static void main(String[] args) throws Exception
	{
		int[] obs = loadData(args[0], 249);
		
		for(int ns = 2; ns < 9; ns++)
		{
			IDynamicBayesNet net = DynamicNetworkFactory.newDynamicBayesNet(249);
			double[] pi = new double[ns];
			
			pi[0] = 1;
			
			IFDiscDBNNode x = net.addDiscreteNode("X", ns);
			x.setInitialDistribution(new DiscreteCPTUC(pi));
			x.setAdvanceDistribution(new DiscreteCPT(trans(ns), ns));
			
			IInfDiscEvDBNNode y = net.addDiscreteEvidenceNode("Y", obs);
			y.setAdvanceDistribution(new SwitchingPoisson(means(ns)));
			
			net.addInterEdge(x, x);
			net.addIntraEdge(x, y);
			
			net.validate();
			net.optimize_parallel(20, 1e-6, 20, 1e-6);
			
			PrintStream ps = new PrintStream(args[1]+ns);
			net.print(ps);
	
			System.out.println(ns);
			System.out.println(net.getLogLikelihood());
			System.out.print(y.conditionalLL(0));
			for(int i = 1; i < 249; i++)
				System.out.print(" " + y.conditionalLL(i));
			System.out.println();
		}
	}
	
	public static double[] means(int ns)
	{
		double[] means = new double[ns];
		for(int i =0; i < ns; i++)
			means[i] = MathUtil.rand.nextDouble()*10;
		return means;
	}
	
	public static double[][] trans(int ns)
	{
		double[][] a = new double[ns][ns];
		for(int i = 0; i < ns; i++)
		{
			for(int j = 0; j < ns; j++)
			{
				if(i==j)
					a[i][j] = .9;
				else
					a[i][j] = .1/(ns-1);
			}
		}
		return a;
	}
	
	public static int[] loadData(String file, int nd) throws Exception
	{
		Scanner scan = new Scanner(new File(file));
		int[] dat = new int[nd];
		for(int i = 0; i < nd; i++)
		{
			if(!scan.hasNext())
				throw new BNException("Failed to read data, less elements than specified!");
			dat[i] = scan.nextInt();
		}
		return dat;
	}
}
