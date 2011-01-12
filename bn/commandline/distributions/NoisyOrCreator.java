package bn.commandline.distributions;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.regex.Pattern;

import util.Parser.ParserException;
import util.Parser.ParserFunction;

import bn.BNException;
import bn.commandline.distributions.CPDCreator.ICPDCreator;
import bn.distributions.*;

class NoisyOrCreator implements ICPDCreator
{
	static NoisyOrCreator getFactory()
	{
		return new NoisyOrCreator(null,null);
	}
	
	private NoisyOrCreator(HashMap<String, Distribution> distmap, String name)
	{
		this.distmap = distmap;
		this.name = name;
	}
	
	public void finish() throws ParserException{}
	public ParserFunction parseLine(String[] args, PrintStream str) throws ParserException
	{
		try {
			Double p = Double.parseDouble(args[0]);
			ScalarNoisyOr noisyOr = new ScalarNoisyOr(p);
			this.distmap.put(name, noisyOr);
			return null;
		} catch(NumberFormatException e) {
			throw new ParserException("Expected a single floating point number for the activation parameter.");
		} catch(BNException e) {
			throw new ParserException(e.getMessage());
		}
	}
	public ICPDCreator newCreator(String name, String argstr, HashMap<String, Distribution> distMap) throws ParserException {
		if(argstr!=null && !argstr.equals(""))
			throw new ParserException("Expect no arguments for noisy or creation...");
		return new NoisyOrCreator(distMap,name);
	}
	
	public String name(){return "NoisyOr";}
	public String description(){return "Create a \"noisy or\" node.  This variable has all binary parents, and is more like to " +
			"be found to be on (1) the more of it's parents are on.  Specifically p(var = on| parents) = 1 - c^(# Parents on).  " +
			"This distribution needs one parameter 0 <= c <= 1, c = 1-q.  The larger the value of c the less noisy this distribution" +
			" is.  This variable must have at least one parent.\nEx:\n\tA<NoisyOr()\n.9\n\nThis creates a noisy or distribution with" +
			" parameter c = .9 and names it A.";}
	
	private String name;
	public final int[] getGroups() {return groups;}
	public final Pattern getRegEx() {return patt;}
	public final String getPrompt() {return "Enter activation parameter: ";}			
	private HashMap<String,Distribution> distmap;
	private static int[] groups = new int[]{1};
	private static Pattern patt = Pattern.compile("([\\.e\\-0-9]*)");
}