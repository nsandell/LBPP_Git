package bn.commandline.distributions;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.regex.Pattern;

import util.Parser.ParserException;
import util.Parser.ParserFunction;

import bn.commandline.distributions.CPDCreator.ICPDCreator;
import bn.distributions.*;

class TrueOrCreator implements ICPDCreator
{
	static TrueOrCreator getFactory(){return new TrueOrCreator(null,null);}
	
	private TrueOrCreator(HashMap<String, Distribution> distmap, String name){}
	
	public void finish() throws ParserException{}
	
	public ParserFunction parseLine(String[] args, PrintStream str) throws ParserException
	{throw new ParserException("This should never be called...");}
	
	public ICPDCreator newCreator(String name, String argstr, HashMap<String, Distribution> distMap) throws ParserException {
		if(argstr!=null && !argstr.equals(""))
			throw new ParserException("Expect no arguments for true noisy or creation...");
		distMap.put(name, TrueOr.getInstance());
		return null;
	}
	
	public final int[] getGroups() {return null;}
	public final Pattern getRegEx() {return null;}
	public final String getPrompt() {return null;}
}