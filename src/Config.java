import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.*;

public class Config{
    public String driver = null;
    public String user = null;
    public String password = null;
    public String url = null;
    public String key = null;
    public String channelId = null;

    public void set(){
	try{
	    FileInputStream in = new FileInputStream("config.xml");
	    Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);

	    Node config = document.getFirstChild();
	    NodeList params = config.getChildNodes();

	    for(int i = 0; i < params.getLength(); i++){
		Node node = params.item(i);
		if(node.getNodeType() == Node.ELEMENT_NODE){
		    NodeList childNodes = node.getChildNodes();
		    for(int j = 0; j < childNodes.getLength(); j++){
			Node childNode = childNodes.item(j);
			if(childNode.getNodeType() == Node.ELEMENT_NODE){
			    String tag = childNode.getNodeName();
			    if(tag.equals("url")){
				url = childNode.getFirstChild().getNodeValue();
			    }else if(tag.equals("driver")){
				driver = childNode.getFirstChild().getNodeValue();
			    }else if(tag.equals("user")){
				user = childNode.getFirstChild().getNodeValue();
			    }else if(tag.equals("password")){
				password = childNode.getFirstChild().getNodeValue();
			    }else if(tag.equals("key")){
				key = childNode.getFirstChild().getNodeValue();
			    }else if(tag.equals("channel-id")){
				channelId = childNode.getFirstChild().getNodeValue();
			    }
			}
		    }

		}
	    }


	}catch(Exception e){
	}
    }


    public void Config(){
    }
    public void print(){
	System.out.println(url);
	System.out.println(user);
	System.out.println(driver);
	System.out.println(password);
	System.out.println(key);
	System.out.println(channelId);
    }

}
