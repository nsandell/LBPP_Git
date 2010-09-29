package bn.commandline;

import java.io.BufferedReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashMap;
import util.Parser;
import bn.BNException;
import bn.IStaticBayesNet;
import bn.commandline.distributions.CPDCreator;
import bn.distributions.Distribution;
import bn.impl.BayesNetworkFactory;

public class StaticNetCommandLine
{
	
	
	public static void main(String[] args)
	{
		interactiveStaticNetwork();
	}

	private static Parser getParser(BufferedReader input, PrintStream output, PrintStream error, boolean breakOnExc, boolean printLineOnError, IStaticBayesNet bn)
	{
		try
		{
			Parser parser = new Parser(input,output,error,breakOnExc,printLineOnError);
			HashMap<String, Distribution> distMap = new HashMap<String, Distribution>();
			parser.setCommentString("\\s*%\\s*");
			parser.addHandler(CPDCreator.getCreator(distMap));
			parser.addHandler(new StaticCommandHandlers.DiscreteNodeAdder(bn));
			parser.addHandler(new StaticCommandHandlers.StaticEdgeHandler(bn));
			parser.addHandler(new UniversalCommandHandlers.CPDAssigner(bn, distMap));
			parser.addHandler(new StaticCommandHandlers.MarginalHandler(bn));
			parser.addHandler(new UniversalCommandHandlers.BNRunner(bn));
			parser.addHandler(new UniversalCommandHandlers.BNValidate(bn));
			parser.addHandler(new StaticCommandHandlers.ObservationHandler(bn));
			parser.addHandler(new UniversalCommandHandlers.LLGetter(bn));
			return parser;
		} catch(Exception e) {
			System.err.println("Error loading parser : " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	public static IStaticBayesNet loadNetwork(String file) throws BNException
	{
		try
		{
			IStaticBayesNet bn = BayesNetworkFactory.getStaticNetwork();
			Parser parser = getParser(new BufferedReader(new FileReader(file)), null, null, true, true, bn);
			if(parser==null) return null;
			parser.go();
			return bn;
		} catch(FileNotFoundException e) {
			throw new BNException("Could not find file " + file);
		}
	}

	public static void interactiveStaticNetwork()
	{
		IStaticBayesNet bn = BayesNetworkFactory.getStaticNetwork();
		Parser parser = getParser(	new BufferedReader(new InputStreamReader(System.in)),
									System.out,System.err,false,true,bn);
		if(parser==null)
			return;
		parser.setPrompt("S>>");
		parser.go();
	}
}