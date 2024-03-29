package bn;

import bn.commandline.DynamicCommandLine;
import bn.commandline.StaticNetCommandLine;

/**
 * This class simply starts up the command line interfaces, either static or dynamic depending on
 * the arguments.
 * @author Nils F. Sandell
 */
public class CommandLine {
	
	public static void main(String[] args)
	{
		if(args.length!=1)
		{
			System.err.println("Expect only one command line argument - 'static' or 'dynamic'");
			return;
		}
		
		String dynamic = "dynamic";
		String statics = "static";
		
		if(dynamic.startsWith(args[0]))
			DynamicCommandLine.interactiveDynamicNetwork();
		else if(statics.startsWith(args[0]))
			StaticNetCommandLine.interactiveStaticNetwork();
		else
			System.err.println("Unrecognized mode, must either be 'static' or 'dynamic'");
	}
}
