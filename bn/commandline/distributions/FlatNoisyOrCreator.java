package bn.commandline.distributions;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.regex.Pattern;

import util.Parser.ParserException;
import util.Parser.ParserFunction;

import bn.BNException;
import bn.commandline.distributions.CPDCreator.ICPDCreator;
import bn.distributions.*;

class FlatNoisyOrCreator implements ICPDCreator
{
	static FlatNoisyOrCreator getFactory()
	{
		return new FlatNoisyOrCreator(null,null);
	}
	
	private FlatNoisyOrCreator(HashMap<String, Distribution> distmap, String name)
	{
		this.distmap = distmap;
		this.name = name;
	}
	
	public void finish() throws ParserException{}
	public ParserFunction parseLine(String[] args, PrintStream str) throws ParserException
	{
		try {
			Double p = Double.parseDouble(args[0]);
			FlatNoisyOr noisyOr = new FlatNoisyOr(p);
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
			throw new ParserException("Expect no arguments for flat noisy or creation...");
		return new FlatNoisyOrCreator(distMap,name);
	}
	
	private String name;
	public final int[] getGroups() {return groups;}
	public final Pattern getRegEx() {return patt;}
	public final String getPrompt() {return "Enter activation parameter: ";}			
	private HashMap<String,Distribution> distmap;
	private static int[] groups = new int[]{1};
	private static Pattern patt = Pattern.compile("([\\.e\\-0-9]*)");
}