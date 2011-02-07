package bn.messages;

public abstract class DiscreteMessageBase extends Message {

	public DiscreteMessageBase(){}
	
	@Override
	Message copy() {
		return this.copyI();
	}
	
	public abstract double getValue(int index);
	
	protected abstract DiscreteMessageBase copyI();
	
	private static final long serialVersionUID = 1L;
}
