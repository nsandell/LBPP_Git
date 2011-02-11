package complex.featural.proposal_generators;

import java.util.HashSet;

import util.MathUtil;
import complex.featural.IChildProcess;
import complex.featural.IParentProcess;
import complex.featural.ModelController;
import complex.featural.ProposalGenerator;
import complex.featural.ProposalAction.SplitAction;

public class RandomSplitGenerator implements ProposalGenerator
{

	public RandomSplitGenerator(double pmerge, double psplit)
	{
		this.pmerge = pmerge;
		this.psplit = psplit;
	}
	
	public String name()
	{
		return "Random splits";
	}
	
	public Proposal generate(ModelController cont)
	{
		int N = cont.getLatentNodes().size();
		if(N < 1)
			return null;
		IParentProcess lat = cont.randomLatent();
		HashSet<IChildProcess> children = cont.getChildren(lat);
		HashSet<IChildProcess> splits = new HashSet<IChildProcess>();
		for(IChildProcess child : children)
			if(MathUtil.rand.nextDouble() < .5)
				splits.add(child);
		
		if(splits.size()==0 || splits.size()==children.size())
			return null;
		
		double fp = this.psplit*Math.pow(.5, splits.size())/((double)N);
		double bp = this.pmerge/((double)(N*(N-1)));
		
		cont.log("Proposing split of node " + lat.getName());
		
		return new Proposal(fp, bp, new SplitAction(lat,splits));
	}

	
	double pmerge, psplit;
}
