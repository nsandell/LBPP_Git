package bn.impl;

class BNEdge
{
	BNEdge(BNNode from, BNNode to, int fromIndex, int toIndex)
	{
		this.from = from;
		this.to = to;
		this.fromIndex = fromIndex;
		this.toIndex = toIndex;
	}
	
	public final BNNode from;
	public final BNNode to;
	public int fromIndex;
	public int toIndex;
}
