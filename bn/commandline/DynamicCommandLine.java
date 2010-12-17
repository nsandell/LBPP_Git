package bn.commandline;

import java.io.BufferedReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashMap;
import util.Parser;
import bn.BNException;
import bn.IDynBayesNet;
import bn.commandline.distributions.CPDCreator;
import bn.distributions.Distribution;
import bn.impl.BayesNetworkFactory;

public class DynamicCommandLine
{
	public static void main(String[] args)
	{
		interactiveDynamicNetwork();
	}
	
	private static Parser getParser(BufferedReader input, PrintStream output, PrintStream error, boolean breakOnExc, boolean printLineOnError, IDynBayesNet bn)
	{
		try
		{
			HashMap<String, Distribution> distMap = new HashMap<String, Distribution>();
			Parser parser = new Parser(input,output,error,breakOnExc,printLineOnError);
			parser.setCommentString("\\s*%\\s*");
			parser.addHandler(CPDCreator.getCreator(distMap));
			parser.addHandler(new UniversalCommandHandlers.BNValidate(bn));
			parser.addHandler(new UniversalCommandHandlers.BNRunner(bn));
			parser.addHandler(new UniversalCommandHandlers.CPDAssigner(bn, distMap));
			parser.addHandler(new DynamicCommandHandlers.InterEdgeHandler(bn));
			parser.addHandler(new DynamicCommandHandlers.IntraEdgeHandler(bn));
			parser.addHandler(new DynamicCommandHandlers.InterEdgeRemover(bn));
			parser.addHandler(new DynamicCommandHandlers.IntraEdgeRemover(bn));
			parser.addHandler(new DynamicCommandHandlers.DiscreteNodeAdder(bn));
			parser.addHandler(new DynamicCommandHandlers.InitialDistSetter(bn, distMap));
			parser.addHandler(new DynamicCommandHandlers.ParallelRunner(bn));
			parser.addHandler(new DynamicCommandHandlers.MarginalHandler(bn));
			parser.addHandler(new DynamicCommandHandlers.ObservationHandler(bn));
			parser.addHandler(new UniversalCommandHandlers.NetLLGetter(bn));
			parser.addHandler(new UniversalCommandHandlers.DefinitionPrinter(bn));
			//parser.addHandler(new UniversalCommandHandlers.LLGetter(bn));
			parser.addHandler(new UniversalCommandHandlers.Optimizer(bn));
			parser.addHandler(new DynamicCommandHandlers.ParallelOptimizer(bn));
			parser.addHandler(new UniversalCommandHandlers.NodeDistPrinter(bn));
			parser.addHandler(new UniversalCommandHandlers.BNSampler(bn));
			parser.addHandler(new UniversalCommandHandlers.NodeRemover(bn));
			parser.addHandler(new UniversalCommandHandlers.BNResetter(bn));
			parser.addHandler(new UniversalCommandHandlers.BNSaver(bn));
		
			return parser;
		}
		catch(Exception e) {
			System.err.println("Error initializing parser: " + e.getMessage());
			return null;
		}
	}

	public static IDynBayesNet loadNetwork(String file) throws BNException
	{
		try
		{
			BufferedReader input = new BufferedReader(new FileReader(file));
			int T = Integer.parseInt(input.readLine());
			IDynBayesNet bn = BayesNetworkFactory.getDynamicNetwork(T);
			Parser parser = getParser(input, null, System.err, true, true, bn);
			parser.go();
			return bn;
		} catch(FileNotFoundException e) {
			throw new BNException("Could not find file " + file);
		} catch(IOException e) {
			throw new BNException("Error reading first line for number of slices.");
		} catch(NumberFormatException e) {
			throw new BNException("First line of definition file must be the dynamic network time slice number.");
		}
	}

	public static void interactiveDynamicNetwork()
	{	
		int T = 0;
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		try
		{
			while(T < 2)
			{
				System.out.print("Enter Number of Time Slices : " );
				String firstLine = input.readLine();
				if(firstLine==null) return;
				try{T = Integer.parseInt(firstLine);}
				catch(NumberFormatException e){System.err.println("Invalid entry.");}
				if(T < 2)
					System.err.println("Error, number of slices must be at least 2.");
			}
			IDynBayesNet bn = BayesNetworkFactory.getDynamicNetwork(T);
			Parser parser = getParser(input,System.out,System.err, false, true, bn);
			parser.setPrompt("D>>");
			parser.go();
		} catch(IOException e) {}
	}
	
}
