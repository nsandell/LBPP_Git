package tests;
import bn.Options.InferenceOptions;
import bn.IStaticBayesNet;
import bn.commandline.StaticNetCommandLine;

public class FileSpeedTest {
	public static void main(String[] args) throws Exception
	{
		IStaticBayesNet net = StaticNetCommandLine.loadNetwork(args[0]);
		
		long start = System.currentTimeMillis();
		net.run(new InferenceOptions(10000, 0));
		long end = System.currentTimeMillis();
		double secs = ((double)(end-start))/1000.0;
		System.out.println("Elapsed "+secs);
	}
}
