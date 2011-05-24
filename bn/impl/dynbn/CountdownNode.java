package bn.impl.dynbn;

import java.io.PrintStream;

import java.util.ArrayList;

import util.MathUtil;

import bn.BNException;
import bn.distributions.CountdownDistribution;
import bn.distributions.CountdownDistribution.CountdownStatistic;
import bn.distributions.Distribution;
import bn.distributions.Distribution.SufficientStatistic;
import bn.dynamic.ICountdownNode;
import bn.impl.dynbn.DynamicContextManager.DynamicMessageIndex;
import bn.impl.dynbn.DynamicContextManager;
import bn.impl.dynbn.DynamicContextManager.DynamicMessageSet;
import bn.messages.FiniteDiscreteMessage;
import bn.messages.FiniteDiscreteMessage.FDiscMessageInterfaceSet;
import bn.messages.Message.MessageInterfaceSet;

public class CountdownNode extends DBNNode implements ICountdownNode {
	
	//TODO right now this can be used to sample or infer, but not both!
	
	public CountdownNode(DynamicBayesianNetwork net, String name, int maxlen)
	{
		super(net,name);
		this.lambdas = new ArrayList<FiniteDiscreteMessage>();
		this.interlambdas = new ArrayList<FiniteDiscreteMessage>();
		this.pis = new ArrayList<FiniteDiscreteMessage>();
		this.interpis = new ArrayList<FiniteDiscreteMessage>();
		this.marginal = new ArrayList<FiniteDiscreteMessage>();
		this.truncation = maxlen;
		this.values = new Integer[net.getT()];
		
		this.childrenMessages = new DynamicContextManager.DynamicChildManager<FiniteDiscreteMessage>(net.getT());
		
		for(int i = 0; i <net.getT(); i++)
		{
			this.lambdas.add(FiniteDiscreteMessage.normalMessage(maxlen));
			this.interlambdas.add(FiniteDiscreteMessage.normalMessage(maxlen));
			this.interpis.add(FiniteDiscreteMessage.normalMessage(maxlen));
			this.pis.add(FiniteDiscreteMessage.normalMessage(maxlen));
			this.marginal.add(FiniteDiscreteMessage.normalMessage(maxlen));
		}
	}

	@Override
	public double betheFreeEnergy() throws BNException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public SufficientStatistic getSufficientStatistic() throws BNException {
		CountdownStatistic stat = this.dist.getSufficientStatisticObj();
		
		FiniteDiscreteMessage inc_pi0 = new FiniteDiscreteMessage(this.truncation);
		inc_pi0.setValue(0, 1.0);
		stat.update(inc_pi0,this.lambdas.get(0));
		
		for(int t = 1; t < this.getNetwork().getT(); t++)
		{
			DynamicMessageSet<FiniteDiscreteMessage> incLambs = this.childrenMessages.getIncomingLambdas(t-1);
			double p0 = 1;
			double p1 = 1;
			for(FiniteDiscreteMessage msg : incLambs)
			{
				p0 *= msg.getValue(0);
				p1 *= msg.getValue(1);
			}
			p0 = p0/(p0+p1); p1 = 1-p0;
			FiniteDiscreteMessage pi_prev = this.pis.get(t-1);
			FiniteDiscreteMessage inc_pi = new FiniteDiscreteMessage(this.truncation);
			inc_pi.setValue(0,p0*pi_prev.getValue(0));
			for(int i = 1; i < pi_prev.getCardinality(); i++)
				inc_pi.setValue(i, p1*pi_prev.getValue(i));
			inc_pi.normalize();
			stat.update(inc_pi, this.lambdas.get(t));
		}
		return stat;
	}

	@Override
	public String getNodeDefinition() {
		String def = this.getName() + ":CountDownNode()\n";
		def += this.getName() + " < " + this.dist.getDefinition();
		return def;
	}

	@Override
	public void sample() {
		if(!this.sample)
			return;
		//Sample initial value
		double[] dist = this.dist.getDist();
		this.values[0] = MathUtil.discreteSample(dist);
		int t = 0;
		while(t < this.getNetwork().getT()-1)
		{
			if(values[t]==0)
			{
				t++;
				values[t] = MathUtil.discreteSample(dist);
			}
			while(t < this.getNetwork().getT()-1 && values[t] > 0)
			{
				t++;
				values[t] = values[t-1]-1;
			}
		}
	}

	@Override
	public void setSample(boolean sample) {
		this.sample = sample;
	}
	boolean sample = true;
	

	@Override
	public void clearEvidence()
	{
		for(int i = 0; i < this.getNetwork().getT(); i++)
			this.values[i] = null;
	}

	@Override
	public double conditionalLL(int t) {
		return this.values[t]==null ? 0 : this.marginal.get(t).getValue(this.values[t]);
	}

	@Override
	public void setInitialDistribution(Distribution dist) throws BNException {
		throw new BNException("Countdown nodes do not accept initial distributions");
	}

	@Override
	public void setAdvanceDistribution(Distribution dist) throws BNException {
		if(!(dist instanceof CountdownDistribution))
			throw new BNException("Attempted to se countdown node distribution to something other than a countdown distribution.");
		this.dist = (CountdownDistribution)dist;
	}

	@Override
	public CountdownDistribution getAdvanceDistribution() {
		return this.dist;
	}

	@Override
	public Distribution getInitialDistribution() {
		return null;
	}

	@Override
	protected MessageInterfaceSet<?> newChildInterface(int T)
			throws BNException {
		FDiscMessageInterfaceSet ret = new FDiscMessageInterfaceSet(2);
		for(int i = 0; i < T; i++)
		{
			ret.lambda_v.add(FiniteDiscreteMessage.normalMessage(2));
			FiniteDiscreteMessage pi = new FiniteDiscreteMessage(2);
			pi.setValue(0, this.marginal.get(i).getValue(0));
			pi.setValue(1, 1-pi.getValue(0));
			ret.pi_v.add(pi);
		}
		return ret;
	}

	@Override
	protected DynamicMessageIndex addInterParentInterface(
			MessageInterfaceSet<?> mia) throws BNException {
		throw new BNException("The only parent of a countdown node is itself.");
	}

	@Override
	protected DynamicMessageIndex addIntraParentInterface(
			MessageInterfaceSet<?> mia) throws BNException {
		throw new BNException("The only parent of a countdown node is itself.");
	}

	@Override
	protected DynamicMessageIndex addInterChildInterface(
			MessageInterfaceSet<?> mia) throws BNException {
		if(!(mia instanceof FDiscMessageInterfaceSet))
			throw new BNException("Attempted to add a child interface that is not a finite discrete message set.");
		FDiscMessageInterfaceSet set = (FDiscMessageInterfaceSet)mia;
		return this.childrenMessages.newInterChild(set.pi_v,set.lambda_v);
	}

	@Override
	protected DynamicMessageIndex addIntraChildInterface(
			MessageInterfaceSet<?> mia) throws BNException {
		if(!(mia instanceof FDiscMessageInterfaceSet))
			throw new BNException("Attempted to add a child interface that is not a finite discrete message set.");
		FDiscMessageInterfaceSet set = (FDiscMessageInterfaceSet)mia;
		return this.childrenMessages.newIntraChild(set.pi_v,set.lambda_v);
	}

	@Override
	protected void removeInterParentInterface(DynamicMessageIndex index)
			throws BNException {
		throw new BNException("The only parent of a countdown node is itself.");
	}

	@Override
	protected void removeIntraParentInterface(DynamicMessageIndex index)
			throws BNException {
		throw new BNException("The only parent of a countdown node is itself.");
	}

	@Override
	protected void removeInterChildInterface(DynamicMessageIndex index)
			throws BNException {
		this.childrenMessages.removeInterChild(index);
	}

	@Override
	protected void removeIntraChildInterface(DynamicMessageIndex index)
			throws BNException {
		this.childrenMessages.removeIntraChild(index);
	}

	@Override
	public void validate() throws BNException {
		//TODO Anything that can be wrong?
	}

	@Override
	protected double updateMessages(int t) throws BNException {
		
		// Update local lambda
		if(this.values[t]==null)
		{
			FiniteDiscreteMessage lambda_now = this.lambdas.get(t);

			DynamicMessageSet<FiniteDiscreteMessage> incLambs = this.childrenMessages.getIncomingLambdas(t);
			double p0 = 1;
			double p1 = 1;
			for(FiniteDiscreteMessage msg : incLambs)
			{
				p0 *= msg.getValue(0);
				p1 *= msg.getValue(1);
			}
			p0 = p0/(p0+p1); p1 = 1-p0;
			
			if(t==this.bayesNet.T-1)
			{
				lambda_now.setValue(0, p0);
				for(int i = 1; i < this.truncation; i++)
					lambda_now.setValue(i, p1);
			}
			else
			{
				FiniteDiscreteMessage inc_lambda = this.interlambdas.get(t);
	
				lambda_now.setValue(0, p0*inc_lambda.getValue(0));
				for(int i = 1; i < this.truncation; i++)
					lambda_now.setValue(i, p1*inc_lambda.getValue(i));
			}
			lambda_now.normalize();
		}
		else
		{
			FiniteDiscreteMessage lambda_now = this.lambdas.get(t);
			lambda_now.empty();
			lambda_now.setDelta(this.values[t], 1.0);
		}
		
		// Update local pi
		if(t==0)
		{
			FiniteDiscreteMessage pi_now = this.pis.get(t);
			double[] dist = this.dist.getDist();
			for(int i = 0; i < dist.length; i++)
				pi_now.setValue(i, dist[i]);
		}
		else 
		{
			FiniteDiscreteMessage pi_now = this.pis.get(t);
			FiniteDiscreteMessage incomingPi = this.interpis.get(t-1);
			
			double[] dist = this.dist.getDist();
			for(int i = 0; i < this.truncation - 1; i++)
				pi_now.setValue(i,incomingPi.getValue(i+1));
			double p0 = incomingPi.getValue(0);
			pi_now.setValue(this.truncation-1, 0);
			for(int i = 0; i < this.truncation; i++)
				pi_now.setValue(i,pi_now.getValue(i) + p0*dist[i]);
			pi_now.normalize(); //Probably only necessary to take care of accumulating rounding errors..
		}
		
		//Update outgoing lambda
		if(t > 0)
		{
			FiniteDiscreteMessage outLambMe = this.interlambdas.get(t-1);
//			FiniteDiscreteMessage localLambda = this.lambdas.get(t-1);
			FiniteDiscreteMessage localLambda = this.lambdas.get(t);
		
			for(int i = 1; i < this.truncation; i++)
				outLambMe.setValue(i, localLambda.getValue(i-1));
			double sum = 0;
			double[] dist = this.dist.getDist();
			for(int i = 0; i < this.truncation; i++)
				sum += localLambda.getValue(i)*dist[i];
			outLambMe.setValue(0, sum);
		}
		
		//Update outgoing pis
		FiniteDiscreteMessage localPi = this.pis.get(t);
		FiniteDiscreteMessage incLambMe = null;
		if(t!=this.bayesNet.T-1)
			incLambMe = this.interlambdas.get(t);
		
		DynamicMessageSet<FiniteDiscreteMessage> incLambs = this.childrenMessages.getIncomingLambdas(t);
		DynamicMessageSet<FiniteDiscreteMessage> outPis = this.childrenMessages.getOutgoingPis(t);
		//TODO Ignoring cases where we have a value since we have more pressing concerns.,also this is inefficient
		
		FiniteDiscreteMessage prod2 = new FiniteDiscreteMessage(2);
		if(incLambMe!=null)
		{
			FiniteDiscreteMessage tmp = localPi.multiply(incLambMe);tmp.normalize();
			prod2.setValue(0, tmp.getValue(0));
			prod2.setValue(1, 1-tmp.getValue(0));
		}
		else
		{
			prod2.setValue(0, localPi.getValue(0));
			prod2.setValue(1, localPi.getValue(1));
		}
		
		for(int i = 0; i < outPis.size(); i++)
		{
			FiniteDiscreteMessage outpi = outPis.get(i);
			outpi.adopt(prod2);
			for(int j = 0; j < outPis.size(); j++)
			{
				if(i==j) continue;
				outpi = outpi.multiply(incLambs.get(j));
			}
			outpi.normalize();
		}
		
		if(t<this.bayesNet.T-1)
		{
			double p0 = 1, p1 = 1;
			for(int i = 0;  i < incLambs.size(); i++)
			{
				p0 *= incLambs.get(i).getValue(0);
				p1 *= incLambs.get(i).getValue(1);
			}
			FiniteDiscreteMessage outPiMe = this.interpis.get(t);
			outPiMe.setValue(0, p0*localPi.getValue(0));
			for(int i = 1; i < this.truncation; i++)
				outPiMe.setValue(i, p1*localPi.getValue(i));
			outPiMe.normalize();
		}
		

		FiniteDiscreteMessage newmarg = this.pis.get(t).multiply(this.lambdas.get(t));
		newmarg.normalize();
		FiniteDiscreteMessage marg = this.marginal.get(t);
		double maxerr = 0;
		for(int i = 0; i < this.truncation; i++)
			maxerr = Math.max(maxerr, Math.abs(newmarg.getValue(i)-marg.getValue(i)));
		marg.adopt(newmarg);
		return maxerr;
	}
	
	@Override 
	public Integer getValue(int t)
	{
		return this.values[t];
	}
	
	public FiniteDiscreteMessage getMarginal(int t)
	{
		return this.marginal.get(t);
	}

	@Override
	public void resetMessages() {
		for(int i = 0; i < this.getNetwork().T; i++)
		{
			this.lambdas.get(i).setInitial();
			this.pis.get(i).setInitial();
			this.childrenMessages.resetMessages();
		}
	}

	@Override
	public void printDistributionInfo(PrintStream ps) {
		this.dist.printDistribution(ps);
	}

	@Override
	protected double optimizeParametersI() throws BNException {
		return this.dist.optimize(this.getSufficientStatistic());
	}

	@Override
	protected double optimizeParametersI(SufficientStatistic stat)
			throws BNException {
		return this.dist.optimize(stat);
	}

	private Integer[] values;
	private int truncation;
	private CountdownDistribution dist;
	private ArrayList<FiniteDiscreteMessage> lambdas, pis, marginal;
	private ArrayList<FiniteDiscreteMessage> interlambdas, interpis;
	private DynamicContextManager.DynamicChildManager<FiniteDiscreteMessage> childrenMessages;

}
