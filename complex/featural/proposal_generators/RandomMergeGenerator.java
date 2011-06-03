package complex.featural.proposal_generators;

import java.util.HashSet;

import complex.featural.FeaturalModelController;
import complex.featural.IFeaturalChild;
import complex.featural.FeaturalModelController.LatentPair;
import complex.featural.MHProposalGenerator;
import complex.featural.ProposalAction.MergeAction;

public class RandomMergeGenerator implements MHProposalGenerator
{
	public RandomMergeGenerator(double pmerge, double psplit)
	{
		this.pmerge = pmerge;
		this.psplit = psplit;
	}
	
	public String name()
	{
		return "Random merges";
	}
	
	public MHProposal generate(FeaturalModelController cont)
	{
		int N = cont.getLatentNodes().size();
		if(N < 2)
			return null;
		LatentPair pair = cont.randomLatentPair();
		if(pair==null)
			return null;
		
		cont.log("Proposing the merge of nodes " + pair.l1.getName() + " and " + pair.l2.getName());
		
		HashSet<IFeaturalChild> totalChildren = new HashSet<IFeaturalChild>();
		totalChildren.addAll(cont.getChildren(pair.l1));
		totalChildren.addAll(cont.getChildren(pair.l2));
		
		double fp = this.pmerge/((double)(N*(N-1)));
		double bp = 2*Math.pow(.5,totalChildren.size())*this.psplit/((double)(N-1));		//Really rough but eh
		
		return new MHProposal(fp, bp, new MergeAction(pair.l1,pair.l2,true));
	}
	
	double pmerge, psplit;
}
