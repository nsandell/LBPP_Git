package bn.commandline.distributions;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.regex.Pattern;

import util.Parser.ParserException;
import util.Parser.ParserFunction;
import bn.commandline.distributions.CPDCreator.ICPDCreator;
import bn.commandline.distributions.SwitchingCountdownCreator.CountdownCreator;
import bn.distributions.CountdownDistribution;
import bn.distributions.Distribution;
import bn.distributions.PoissonCountdown;

public class PoissonCountdownCreator implements ICPDCreator, CountdownCreator {


	PoissonCountdownCreator(HashMap<String, Distribution> distmap, String name)
	{
		this.dismap = distmap;
		this.name = name;
	}
	HashMap<String,Distribution> dismap;
	String name;

	@Override
	public Pattern getRegEx() {return null;}

	@Override
	public int[] getGroups() {return null;}

	@Override
	public String getPrompt() {return null;}

	@Override
	public void finish() throws ParserException {}

	@Override
	public ParserFunction parseLine(String[] args, PrintStream output)
			throws ParserException {
		return null;
	}

	@Override
	public String name() {
		return "PoissonCountdown";
	}

	@Override
	public String description() {
		return "Creates a poisson countdown distribution."; //TODO More detail
	}

	@Override
	public ICPDCreator newCreator(String name, String argstr,
			HashMap<String, Distribution> distMap) throws ParserException {
		distMap.put(name,this.getDist(argstr));
		return null;
	}
	
	private PoissonCountdown getDist(String argstr) throws ParserException
	{
		String[] substrs = argstr.split(",");
		if(substrs.length!=2)
			throw new ParserException("Expected both a poisson parameter and a truncation length as arguments.");
		
		return new PoissonCountdown(Integer.parseInt(substrs[0]), Double.parseDouble(substrs[1]));
	}

	@Override
	public CountdownDistribution getDistribution(String args) throws ParserException {
		return this.getDist(args);
	}
}
