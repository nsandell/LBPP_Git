package tests;

import bn.BNException;
import bn.IBayesNet.RunResults;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.dynamic.IDynamicBayesNet;
import bn.dynamic.IFDiscDBNNode;
import bn.impl.dynbn.DynamicNetworkFactory;

public class PortTest {

	public static void main(String[] args) throws BNException
	{
		IDynamicBayesNet net = DynamicNetworkFactory.newDynamicBayesNet(500);
		IFDiscDBNNode x = net.addDiscreteNode("X", 2);
		net.addInterEdge("X", "X");
		x.setAdvanceDistribution(new DiscreteCPT(new double[][]{{.9,.1},{.1,.9}}, 2));
		x.setInitialDistribution(new DiscreteCPTUC(new double[]{.8,.2}));


		for(int i = 0; i < 30; i++)
		{
			IFDiscDBNNode y = net.addDiscreteNode("Y"+i, 2);
			y.setAdvanceDistribution(new DiscreteCPT(new double[][]{{.9,.1},{.1,.9}}, 2));
			net.addIntraEdge("X", "Y"+i);
		}
		
		IFDiscDBNNode y =(IFDiscDBNNode) net.getNode("Y1");
		y.setValue(100, 1);
		
		net.validate();
		while(true)
		{
			y.setValue(100, 1-y.getValue(100));
			RunResults res = net.run_parallel_queue(1,0);
			System.err.println(res.timeElapsed);
		}
	}

	public static void spawn(Object lock)
	{
		(new Threadtest(lock)).run();
	}
	
	public static class Threadtest extends Thread
	{
		public Threadtest(Object lock)
		{
			this.lock = lock;
		}
		Object lock;
		
		public void run()
		{
			try {
			Thread.sleep(50);
			} catch(InterruptedException e) {
				
			}
			synchronized (lock) {
				lock.notify();
			}
		}
	}
}
