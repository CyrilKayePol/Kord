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
	}
	
	public Node(BigInteger id, String ip, int port) {
		this.id = id;
		this.ip = ip;
		this.port = port;
	}
	
	public void create() {
		successor = this.id;
		predecessor = this.id;
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
