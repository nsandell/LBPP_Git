package complex.featural.proposal_generators;

import complex.featural.ModelController;
import complex.featural.ModelController.LatentPair;
import complex.featural.ProposalGenerator;
import complex.featural.ProposalAction.MergeAction;

public class RandomMergeGenerator implements ProposalGenerator
{

	public RandomMergeGenerator(double pmerge, double psplit)
	{
		this.pmerge = pmerge;
		this.psplit = psplit;
	}
	
	public Proposal generate(ModelController cont)
	{
		int N = cont.getLatentNodes().size();
		LatentPair pair = cont.randomLatentPair();
		if(pair==null)
			return null;
		
		cont.log("Proposing the merge of nodes " + pair.l1.getName() + " and " + pair.l2.getName());
		
		double fp = this.pmerge/((double)(N*(N-1)));
		double bp = this.psplit/((double)(N-1));		//Really rough but eh
		
		return new Proposal(fp, bp, new MergeAction(pair.l1,pair.l2));
	}

	
	double pmerge, psplit;
}
