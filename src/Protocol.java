import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.*;

@SuppressWarnings("unused")
public class Protocol {
	
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
				m = new Message(myNode, "init");
				output = m;
				send();
			}else if(m.getMsg().equals("init")){
				Node temp = m.getNode();
				Host.addNewMember(temp);
				m = new Message(Host.getRandomNode(), "random node");
				output = m;
				send();	
				
			}else if(m.getMsg().equals("random node")) {
				n = m.getNode();
				
				sendSetup(n, myNode, "request for real node");
			}else if(m.getMsg().equals("request for real node")) {
				
				n = m.getNode();
				sendSetup(n, myNode, "real node");
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
	
	// intended for Host only
	public void setMyNode(Node myNode) {
		this.myNode  = myNode;
	}
	
	
}
