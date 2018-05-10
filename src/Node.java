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
	private Node successor, predecessor;
	private boolean largest = false;
	public Node (String ip, int port) {
		this.ip = ip;
		this.port = port;
		createNodeID();
	}
	
	public void create() {
		successor = this;
		predecessor = this;
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
	
	public Node findSuccessor(Node n) {
		
		BigInteger idN = n.getID();
		System.out.println("idN = "+idN);
		BigInteger idSuccessor = successor.getID();
		
		System.out.println("idSuccessor = "+idSuccessor);
		System.out.println("id = "+id);
		if(isIDNinRange(idN, idSuccessor)) {
			if(-1 == idSuccessor.compareTo(idN)) {
				System.out.println("idSuccessor < idN");
				
				if(largest) {
					System.out.println("will return "+successor.getID());
					return successor;
				}else {
					System.out.println("will return "+successor.getSuccessor().getID());
					return successor.getSuccessor();
				}
			}else {
				System.out.println("will return "+successor.getID());
				System.out.println("idSuccessor >= idN");
				return successor;
			}
		}else {
			Node tmp = closestPreceedingNode();
			return tmp.findSuccessor(n);
		}
	}
	
	private boolean isIDNinRange (BigInteger idN, BigInteger idSuccessor){
		
		if(-1 == id.compareTo(idSuccessor)) {
			System.out.println("id < idSuccessor");
			if(1== idN.compareTo(id) && (-1 == idN.compareTo(idSuccessor) ||
					0 == idN.compareTo(idSuccessor))) {
				return true;
			}
		}else if(1 == id.compareTo(idSuccessor)) {
			System.out.println("id > idSuccessor");
			BigInteger temp = id.mod(idN);
			System.out.println("temp = "+temp);
			if(-1 == temp.compareTo(idSuccessor)) {
				System.out.println("temp < idSuccessor");
				largest = false;
				return true;
			}else {
				System.out.println("set largest");
				largest = true;
				return true;
			}
			
		}else if(0 == id.compareTo(idSuccessor)) {
			/*if(1 == idN.compareTo(id)) {
				
			}else if(-1 == idN.compareTo(id)) {
				return true;
			}*/
			return true;
		}
		return false;
	}
	
	private Node closestPreceedingNode() {
		return successor;
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
	
	public Node getSuccessor() {
		return successor;
	}
	
	public void setSuccessor(Node successor) {
		this.successor = successor;
	} 
	
	public Node getPredecessor() {
		return predecessor;
	}
	
	public void setPredecessor(Node predecessor) {
		this.predecessor = predecessor;
	} 
	
}
