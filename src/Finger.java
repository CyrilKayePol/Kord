import java.math.BigInteger;

public class Finger {
	private BigInteger key, successor;
	
	public Finger(String key, String successor) {
		
		this.key = new BigInteger(key);
		this.successor = new BigInteger(successor);
	}
	
	public BigInteger getKey() {
		return key;
	}
	
	public BigInteger getKeySuccessor() {
		return successor;
	}
	
	public void print() {
		System.out.println("-----------------------------");
		System.out.println("key = "+key);
		System.out.println("key successor = "+successor);
		System.out.println("-----------------------------");
	}
}
