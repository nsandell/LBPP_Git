package bn.commandline.distributions;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.regex.Pattern;

import util.Parser.ParserException;
import util.Parser.ParserFunction;
import bn.distributions.Distribution;

public class CPDCreator implements ParserFunction
{
	
	public static CPDCreator getCreator(HashMap<String,Distribution> distmap)
	{
		CPDCreator cr = new CPDCreator(distmap);
		//TODO Update this for ALL distributions
		cr.addCPDCreator("SwitchingPoisson", SwitchingPoissonCreator.getFactory());
		cr.addCPDCreator("PV", DiscreteCPTUCCreator.getFactory());
		cr.addCPDCreator("CPT", CPTCreator.getFactory());
		cr.addCPDCreator("SparseCPT", SparseCPTCreator.getFactory());
		cr.addCPDCreator("NoisyOr", NoisyOrCreator.getFactory());
		cr.addCPDCreator("FlatNoisyOr", FlatNoisyOrCreator.getFactory());
		cr.addCPDCreator("Or", TrueOrCreator.getFactory());
		cr.addCPDCreator("AdditivePoisson", SumOfPoissonCreator.getFactory());
		cr.addCPDCreator("AdditivePoissonInhib", SumOfPoissonInhibCreator.getFactory());
		cr.addCPDCreator("AdditivePoissonInhib", SumOfPoissonInhibCreator.getFactory());
		cr.addCPDCreator("PoissonCountdown", new PoissonCountdownCreator(null, null));
		cr.addCPDCreator("SwitchingCountdownDistribution", SwitchingCountdownCreator.getFactory());
		return cr;
	}
	
	private CPDCreator(HashMap<String,Distribution> distmap)
	{
		this.distmap = distmap;
	}

	public int[] getGroups() {return groups;}
	public Pattern getRegEx() {return patt;}
	public String getPrompt() {return null;}
	public void finish() throws ParserException {}

	public ParserFunction parseLine(String[] args, PrintStream out) throws ParserException
	{
		String name = args[0];
		String type = args[1];
		String argsstr = args[2];
		if(this.creators.get(type)==null)
			throw new ParserException("Unknown CPD Type : " + type);
		return this.creators.get(type).newCreator(name, argsstr,distmap);
	}
	
	public void addCPDCreator(String typeName, ICPDCreator creator){this.creators.put(typeName, creator);}
	
	static interface ICPDCreator extends ParserFunction
	{
		ICPDCreator newCreator(String name, String argstr, HashMap<String,Distribution> distMap) throws ParserException;
	}
	
	public String name(){return "<";}
	public String description(){return "Distribution assignment operator.  Assigns a distribution to a variable name.  " +
			"Further input will be requested as necessary.\n\nEx:\n\tA<PV(3) creates a probability vector of length 3.";}
	
	private HashMap<String,ICPDCreator> creators = new HashMap<String, ICPDCreator>();
	private HashMap<String,Distribution> distmap;
	private static int[] groups = new int[]{1,2,3};
	private static Pattern patt = Pattern.compile("^\\s*(\\w+)\\s*<\\s*(\\w+)\\((.*)\\)\\s*$");
}