package bn.dynamic;

import bn.messages.FiniteDiscreteMessage;

public interface ICountdownNode extends IDBNNode {

	FiniteDiscreteMessage getMarginal(int t);
	Integer getValue(int t);
}
