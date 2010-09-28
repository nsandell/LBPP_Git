package bn.commandline.distributions;

import java.util.HashMap;
import java.util.regex.Pattern;

import util.Parser.ParserException;
import util.Parser.ParserFunction;
import bn.BNException;
import bn.distributions.Distribution;
import bn.distributions.NoisyOr;
import bn.commandline.distributions.CPDCreator.ICPDCreator;

class NoisyOrCreator implements ICPDCreator
{
	static NoisyOrCreator getFactory()
	{
		return new NoisyOrCreator(null,null,0);
	}
	
	private NoisyOrCreator(HashMap<String, Distribution> distmap, String name, int numParents)
	{
		this.distmap = distmap;
		this.name = name;
		this.numParents = numParents;
	}
	public void finish() throws ParserException{}
	public ParserFunction parseLine(String[] args) throws ParserException
	{
		try {
			Double p = Double.parseDouble(args[0]);
			NoisyOr noisyOr = new NoisyOr(this.numParents, p);
			this.distmap.put(name, noisyOr);
			return null;
		} catch(NumberFormatException e) {
			throw new ParserException("Expected a single floating point number for the activation parameter.");
		} catch(BNException e) {
			throw new ParserException(e.getMessage());
		}
	}
	public ICPDCreator newCreator(String name, String argstr, HashMap<String, Distribution> distMap) throws ParserException {
		try{
			return new NoisyOrCreator(distMap,name,Integer.parseInt(argstr));
		} catch(NumberFormatException e) {
			throw new ParserException("Expected argument to Noisy-Or to be simply the number of parents.");
		}
	}
	
	private String name;
	private int numParents;
	public final int[] getGroups() {return groups;}
	public final Pattern getRegEx() {return patt;}
	public final String getPrompt() {return "Enter activation parameter: ";}			
	private HashMap<String,Distribution> distmap;
	private static int[] groups = new int[]{1};
	private static Pattern patt = Pattern.compile("([\\.e\\-0-9]*)");
}