package bn.messages;

public class DiscreteEvidenceMessage extends DiscreteMessageBase {

	public DiscreteEvidenceMessage(int val)
	{
		this.val = val;
	}
	
	public int getValue()
	{
		return this.val;
	}
	
	@Override
	public void setInitial() {}
	
	public double getValue(int idx)
	{
		return idx==val ? 1.0 : 0.0;
	}
	
	protected DiscreteEvidenceMessage copyI()
	{
		return new DiscreteEvidenceMessage(this.val);
	}
	
	protected int val;
	private static final long serialVersionUID = 1L;
}
