package tests;
import bn.commandline.StaticNetCommandLine;
import bn.statc.IStaticBayesNet;

public class FileSpeedTest {
	public static void main(String[] args) throws Exception
	{
		IStaticBayesNet net = StaticNetCommandLine.loadNetwork(args[0]);
		
		long start = System.currentTimeMillis();
		net.run(10000, 0);
		long end = System.currentTimeMillis();
		double secs = ((double)(end-start))/1000.0;
		System.out.println("Elapsed "+secs);
	}
}
