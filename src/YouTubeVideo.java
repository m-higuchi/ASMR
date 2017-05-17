import java.util.*;
public class YouTubeVideo{
    public String videoId;
    public Date publishedAt;
    public String status;
    public String description;
    public String channelId;
    public String title;

    //コンストラクタ
    public YouTubeVideo(){
    }

    public String getUrl(){
	if(videoId != null){
	    return "http://youtu.be/"+ videoId;
	}else{
	    return null;
	}
    }
}
