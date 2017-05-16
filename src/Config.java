import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.*;

public class Config{
    public String driver = null;
    public String user = null;
    public String password = null;
    public String url = null;

    public void set(){
	try{
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    FileInputStream in = new FileInputStream("config.xml");
	    Document root = builder.parse(in);
	    Node connection = root.getFirstChild();
	    NodeList params = connection.getChildNodes();

	    for(int i = 0; i < params.getLength(); i++){
		Node node = params.item(i);
		if(node.getNodeType() == Node.ELEMENT_NODE){
		    String tag = node.getNodeName();
		    if(tag.equals("driver")){
			driver = node.getFirstChild().getNodeValue();
		    }
		    if(tag.equals("user")){
			user = node.getFirstChild().getNodeValue();
		    }
		    if(tag.equals("url")){
			url = node.getFirstChild().getNodeValue();
		    }
		    if(tag.equals("password")){
			password = node.getFirstChild().getNodeValue();
		    }
		}
	    }
	}catch(Exception e){
	}
    }


    public void Config(){
    }

}
