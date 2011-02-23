package complex.featural.proposal_generators;

import java.util.HashSet;
import java.util.Vector;

import util.MathUtil;

import complex.featural.IChildProcess;
import complex.featural.IParentProcess;
import complex.featural.ModelController;
import complex.featural.ProposalAction;
import complex.featural.ProposalGenerator;
import complex.metrics.Coherence;

public class CoherenceSplitter implements ProposalGenerator {
	
	public CoherenceSplitter(double pcspl, double prspl, double prmerge, double pdmerge)
	{
		this.pcspl = pcspl; this.prspl = prspl; this.prmerge = prmerge; this.pdmerge = pdmerge;
	}
	double pcspl; double prspl; double prmerge; double pdmerge;

	@Override
	public Proposal generate(ModelController cont) {
		Vector<IParentProcess> latents = cont.getLatentNodes();
		if(latents.size()==0)
			return null;
		double[] coherence = new double[latents.size()];
		double sum = 0;
		for(int i = 0; i < latents.size(); i++)
		{
			Vector<IChildProcess> children = new Vector<IChildProcess>(cont.getChildren(latents.get(i)));
			coherence[i] = smoothness + Coherence.coherence(children,cont.getT());
			sum += coherence[i];
		}
		for(int i = 0; i < coherence.length; i++)
			coherence[i] /= sum;
		
	
		int selection = MathUtil.discreteSample(coherence);
		if(cont.getChildren(latents.get(selection)).size()<2)
			return null;
		System.out.println("Attempting to split node " + latents.get(selection).getName() + " based on incoherence.");
		double fp = this.pcspl*coherence[selection] + this.prspl/coherence.length;
		double bp = this.prmerge/coherence.length/(coherence.length+1); //TODO determine if we want to exactly determine reverse move probability
		                                 
		Vector<IChildProcess> children = new Vector<IChildProcess>(cont.getChildren(latents.get(selection)));
		boolean[] membership = Coherence.partition(children, cont.getT());
		
		HashSet<IChildProcess> movers = new HashSet<IChildProcess>();
		for(int i = 0; i < membership.length; i++)
			if(membership[i])
				movers.add(children.get(i));
		
		return new Proposal(fp,bp,new ProposalAction.SplitAction(latents.get(selection),movers));
	}

	@Override
	public String name() {
		return "Coherence-based splitter";
	}

	public double smoothness = 0;
}
