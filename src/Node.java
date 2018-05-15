import java.io.Serializable;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@SuppressWarnings("unused")
public class Node implements Serializable {

	private static final long serialVersionUID = 1L;
	private String ip;
	private int port;
	private BigInteger id;
	private BigInteger successor, predecessor;
	private boolean largest = false;
	public Node (String ip, int port) {
		this.ip = ip;
		this.port = port;
		createNodeID();
	}
	
	public Node(BigInteger id) {
		this.id = id;
	}
	
	public void create() {
		successor = this.id;
		predecessor = this.id;
	}
	
	private void createNodeID(){
		String text = getIP()+""+port;
		MessageDigest digest;
		byte[] hash = null;
		try {
			digest = MessageDigest.getInstance("SHA-1");
			hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
		StringBuffer hex = new StringBuffer();
		
		for(int i = 0; i< hash.length; i++) {
			String s = Integer.toHexString(0xff & hash[i]);
			if(hex.length() == 1) hex.append('0');
			hex.append(s);
		}
		
		this.id = new BigInteger(hex.toString(),16);
		
	}
	
	public void setID(BigInteger id) {
		this.id = id;
	}
	
	public BigInteger getID() {
		return id;
	}
	
	public int getPort() {
		return port;
	}
	
	public String getIP() {
		return ip;
	}
	
	public BigInteger getSuccessor() {
		return successor;
	}
	
	public void setSuccessor(BigInteger successor) {
		this.successor = successor;
	} 
	
	public BigInteger getPredecessor() {
		return predecessor;
	}
	
	public void setPredecessor(BigInteger predecessor) {
		this.predecessor = predecessor;
	} 
	
}
