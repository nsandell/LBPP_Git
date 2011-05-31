package bn.commandline.distributions;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.regex.Pattern;

import util.Parser.ParserException;
import util.Parser.ParserFunction;
import bn.commandline.distributions.CPDCreator.ICPDCreator;
import bn.distributions.CountdownDistribution;
import bn.distributions.CountdownDistribution.SwitchingCountdownDistribution;
import bn.distributions.Distribution;

public class SwitchingCountdownCreator implements ICPDCreator {
	
	static SwitchingCountdownCreator getFactory()
	{
		return new SwitchingCountdownCreator(null, null,0);
	}
	
	private SwitchingCountdownCreator(HashMap<String, Distribution> distmap, String name, int nums)
	{
		this.distmap = distmap;
		this.name = name;
		this.cdds = new CountdownDistribution[nums];
	}
	CountdownDistribution[] cdds;
	int currentdist = 0;

	@Override
	public Pattern getRegEx() {
		return patt;
	}

	@Override
	public int[] getGroups() {
		return groups;
	}

	@Override
	public String getPrompt() {
		return "Enter Switching Distribution: ";
	}

	@Override
	public void finish() throws ParserException {
		SwitchingCountdownDistribution dist =new SwitchingCountdownDistribution();
		dist.distributions = this.cdds;
		this.distmap.put(this.name, dist);
	}

	@Override
	public ParserFunction parseLine(String[] args, PrintStream output)
			throws ParserException {
		CountdownCreator creator = getCountdownCreators().get(args[0]);
		if(creator==null)
			throw new ParserException("Unknown countdown distribution type!");
		cdds[currentdist] = creator.getDistribution(args[1]);
		currentdist++;
		if(currentdist==this.cdds.length)
		{
			this.finish();
			return null;
		}
		return this;
	}

	@Override
	public String name() {
		return "SwitchingCountdownDistribution";
	}

	@Override
	public String description() {
		return "Creates a switching countdown distribution."; //TODO Elaborate
	}

	@Override
	public ICPDCreator newCreator(String name, String argstr,
			HashMap<String, Distribution> distMap) throws ParserException {
		try {
			return new SwitchingCountdownCreator(distMap, name, Integer.parseInt(argstr));
		} catch(NumberFormatException e) {
			throw new ParserException("Expected the number of countdonwn distributions as an argument..");
		}
	}
	
	private static HashMap<String, CountdownCreator> getCountdownCreators()
	{
		if(countdownCreators==null)
		{
			countdownCreators = new HashMap<String, CountdownCreator>();
			countdownCreators.put("PoissonCountdown", new PoissonCountdownCreator(null, null));
		}
		return countdownCreators;
	}
	private static HashMap<String, CountdownCreator> countdownCreators = null;
	
	static interface CountdownCreator
	{
		CountdownDistribution getDistribution(String args) throws ParserException;
	}

	private String name;
	private HashMap<String, Distribution> distmap;
	private static int[] groups = new int[]{1,2};
	private static Pattern patt = Pattern.compile("^\\s*(\\w+)\\((.*)\\)\\s*$");
}
