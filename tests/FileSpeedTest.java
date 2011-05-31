package tests;
import bn.IBayesNet.RunResults;
import bn.commandline.StaticNetCommandLine;
import bn.statc.IStaticBayesNet;

public class FileSpeedTest {
	public static void main(String[] args) throws Exception
	{
		IStaticBayesNet net = StaticNetCommandLine.loadNetwork(args[0]);
		
		//long start = System.currentTimeMillis();
		RunResults res = net.run(10000, 0);
		//net.run_parallel_queue(10000, 0);
		//long end = System.currentTimeMillis();
		double secs = res.timeElapsed; //((double)(end-start))/1000.0;
		System.out.println("Elapsed "+secs);
	}
}
