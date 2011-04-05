package complex.featural.proposal_generators;

import java.util.HashSet;
import java.util.Vector;

import util.MathUtil;

import complex.IParentProcess;
import complex.featural.FeaturalModelController;
import complex.featural.IFeaturalChild;
import complex.featural.ProposalAction;
import complex.featural.ProposalGenerator;
import complex.metrics.Coherence;

public class CoherenceAdder<ChildProcess extends IFeaturalChild, ParentProcess extends IParentProcess> implements ProposalGenerator<ChildProcess,ParentProcess> {
	
	public CoherenceAdder(double pcspl, double prspl, double prmerge, double pdmerge)
	{
		this.pcspl = pcspl; this.prspl = prspl; this.prmerge = prmerge; this.pdmerge = pdmerge;
	}
	double pcspl; double prspl; double prmerge; double pdmerge;

	@Override
	public Proposal<ChildProcess,ParentProcess> generate(FeaturalModelController<ChildProcess,ParentProcess> cont) {
		Vector<ParentProcess> latents = cont.getLatentNodes();
		if(latents.size()==0)
			return null;
		double[] coherence = new double[latents.size()];
		double sum = 0;
		for(int i = 0; i < latents.size(); i++)
		{
			Vector<ChildProcess> children = new Vector<ChildProcess>(cont.getChildren(latents.get(i)));
			coherence[i] = smoothness + Coherence.coherence(children,cont.getT());
			sum += coherence[i];
		}
		for(int i = 0; i < coherence.length; i++)
			coherence[i] /= sum;
		
	
		int selection = MathUtil.discreteSample(coherence);
		if(cont.getChildren(latents.get(selection)).size()<2)
			return null;
		System.out.println("Attempting to add a new coparent for node " + latents.get(selection).getName() + " based on incoherence.");
		//double fp = 1;//this.pcspl*coherence[selection] + this.prspl/coherence.length;
		double fp = 1;//this.pcspl/latents.size();
		double bp = 1;//this.prmerge/latents.size()/latents.size();//1;//this.prmerge/coherence.length/(coherence.length+1); //TODO determine if we want to exactly determine reverse move probability
		                                 
		Vector<ChildProcess> children = new Vector<ChildProcess>(cont.getChildren(latents.get(selection)));
		boolean[] membership = Coherence.partition(children, cont.getT());
		
		HashSet<ChildProcess> movers = new HashSet<ChildProcess>();
		for(int i = 0; i < membership.length; i++)
			if(membership[i])
				movers.add(children.get(i));
		
		return new Proposal<ChildProcess,ParentProcess>(fp,bp,new ProposalAction.MultiUniqueParentAddAction<ChildProcess,ParentProcess>(movers,true));
	}

	@Override
	public String name() {
		return "Coherence-based co-parent adder";
	}

	public double smoothness = 0;
}
