package tests;
import bn.IStaticBayesNet;
import bn.commandline.StaticNetCommandLine;

public class FileSpeedTest {
	public static void main(String[] args) throws Exception
	{
		//IStaticBayesNet net = StaticNetCommandLine.loadNetwork(args[0]);
		IStaticBayesNet net = StaticNetCommandLine.loadNetwork("/Users/nsandell/Documents/MATLAB/speedtest11.lbp");
		
		long start = System.currentTimeMillis();
		net.run(10000, 0);
		long end = System.currentTimeMillis();
		double secs = ((double)(end-start))/1000.0;
		System.out.println("Elapsed "+secs);
	}
}
