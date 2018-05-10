import java.io.Serializable;

public class Message implements Serializable {

	private static final long serialVersionUID = 1L;
	private Node node;
	private String msg;
	public Message(Node node, String msg) {
		this.node = node;
		this.msg = msg;
	}
	
	public Node getNode() {
		return node;
	}
	
	public String getMsg() {
		return msg;
	}
}
