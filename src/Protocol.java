import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.*;

/* continue on updating predecessor and successor*/
@SuppressWarnings("unused")
public class Protocol {
	
	private int state;
	private Object output;
	private int port;
	private InetAddress ip;
	private DatagramSocket socket;
	private Node myNode;
	private String hostIP;
	private int hostPort;
	public Protocol(DatagramSocket socket, String hostIP, int hostPort) {
		this.socket = socket;
		output = null;
		myNode = null;
		this.hostIP = hostIP;
		this.hostPort = hostPort;
	}
	
	public void protocolProcedure(Object input) {
		if(input instanceof Message) {
			Message m = (Message) input;
			Node n = null;
			
			
			if(m.getMsg().equals("start")) {
				 myNode= m.getNode();
				//System.out.println("asking for random node from host");
				m = new Message(myNode, "init");
				output = m;
				send();
			}else if(m.getMsg().equals("init")){
				Node temp = m.getNode();
				// send random object
				//System.out.println("[a peer asks for random node]");
				Host.addNewMember(temp);
				m = new Message(Host.getRandomNode(), "random node");
				output = m;
				send();
				//System.out.println("[sent a random node "+m.getNode().getID()+"]");
				
				
			}else if(m.getMsg().equals("random node")) {
				n = m.getNode();
				//System.out.println("received random node with "+n.getID());
				/*upon receiving a random node, ask for the real copy
				 * of the random node because the node passed by the host
				 * is not updated since only the basic info are needed by the host
				 * , updating would be just a wasteoftime :)*/
				
				sendSetup(n, myNode, "request for real node");
				//System.out.println("sent a request for real node of "+n.getID());
			}else if(m.getMsg().equals("request for real node")) {
				
				n = m.getNode();
				//System.out.println("received a request for my node from "+n.getID());
				sendSetup(n, myNode, "real node");
				//System.out.println("sent my real node");
			}else if(m.getMsg().equals("update your myNode info")) {
				//System.out.println("ABOUT TO UPDATE MYNODE INFO");
				
				BigInteger temp = myNode.getPredecessor();
				setReceiverIP(hostIP);
				setReceiverPort(hostPort);
				output = new Message(myNode, "asking for node of id_"+temp+"_updated predecessor");
				send();
				
				temp = myNode.getSuccessor();
				output = new Message(myNode, "asking for node of id_"+temp+"_updated successor");
				send();
			
			}else if(m.getMsg().equals("node of id_updated predecessor")){
				n = m.getNode();
				sendSetup(n,myNode,"request for updated predecessor");
			}else if(m.getMsg().equals("node of id_updated successor")){
				n = m.getNode();
				sendSetup(n,myNode,"request for updated successor");
			}else if(m.getMsg().equals("node of id_predecessor")) {
				n = m.getNode();
				sendSetup(n, myNode, "update predecessor");
			}else if(m.getMsg().equals("node of id_successor")) {
				n = m.getNode();
				sendSetup(n, myNode, "update successor");
			}
		}
		
	}
	
	private void sendSetup(Node n, Node nodeToSend, String msg) {
		setReceiverIP(n.getIP());
		setReceiverPort(n.getPort());
		output = new Message(nodeToSend, msg);
		send();
	}
	public void setReceiverPort(int port) {
		this.port = port;
	}
	
	public void setReceiverIP(String ip) {
		try {
			InetAddress var = InetAddress.getByName(ip);
			this.ip = var;
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	public void send() {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			ObjectOutputStream os = new ObjectOutputStream(outputStream);
			os.writeObject(output);
		} catch (Exception e) {
			e.printStackTrace();
		}
		byte[] data = outputStream.toByteArray();
		
		 try {
			 DatagramPacket sendPacket = 
					 new DatagramPacket(data, data.length, ip, port);
			 socket.send(sendPacket);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void setOutput(Object output) {
		this.output = output;
	}
	private void findSuccessor(Node rand) {
		Node successor = null;//rand.findSuccessor(myNode);
		notifyMyNode(successor);
		notifySuccessor(successor);
	}
	
	private void notifySuccessor(Node n) {
		port = n.getPort();
		
		try {
			ip = InetAddress.getByName(n.getIP());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		Message m = new Message(myNode, "update predecessor");
		output = m;
		send();
		
	}
	
	private void notifyMyNode(Node n) {
		port = myNode.getPort();
		
		try {
			ip = InetAddress.getByName(myNode.getIP());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		Message m = new Message(n, "update successor");
		output = m;
		send();
		
	}
	
	// intended for Host only
	public void setMyNode(Node myNode) {
		this.myNode  = myNode;
	}
	
	
}
