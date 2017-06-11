import subscriptions.*;
import search.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.text.*;
import java.util.Map;
import net.arnx.jsonic.*;

public class YouTubeChannel{

    public String channelTitle;
    public String channelId;
    public int activityLevel;
    public Boolean asmrOnly;
    public String description;
    public String country;
    public Config conf = new Config();

    //コンストラクタ
    public YouTubeChannel(){
	//設定を読み込む
	conf.set("/home/ec2-user/ASMR/bin/config_JP.xml");
    }
    /*
    public YouTubeChannel(subscriptions.Item item){
	//設定を読み込む
	//conf.set("config2.xml");
	//country = conf.country;

	channelTitle = item.snippet.title;
	description = item.snippet.description;
	channelId = item.snippet.resourceId.channelId;
	activityLevel = 3; //初期値
	asmrOnly = true;
	}*/
    public YouTubeChannel(subscriptions.Item item,Config config){
	//設定を読み込む
	conf = config;
	country = conf.country;
	channelTitle = item.snippet.title;
	description = item.snippet.description;
	channelId = item.snippet.resourceId.channelId;
	activityLevel = 3; //初期値
	asmrOnly = true;
    }

    //date以降のVideoオブジェクトのArrayListを返す
    public ArrayList<YouTubeVideo> getVideoListAfter(java.util.Date date){
	Config confForKey = new Config();
	confForKey.set("/home/ec2-user/ASMR/bin/config_" + country + ".xml");
	final String MAX_RESULTS = "5";

	ArrayList<YouTubeVideo> videoList = new ArrayList<YouTubeVideo>();

	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH'%3A'mm'%3A'ss.SSS'Z'");
	String dateString = sdf.format(date);

	//HTTPリクエスト
	System.out.println(channelTitle + "の新着動画情報を取得...");
	String pageToken = "";
	String urlString = "https://www.googleapis.com/youtube/v3/search?part=snippet&id&channelId=" + channelId + "&maxResults=" + MAX_RESULTS + "&order=date&type=video&publishedAfter=" + dateString + pageToken + "&fields=items(id(channelId,videoId),snippet(channelId,description,publishedAt,title)),nextPageToken&key=" + confForKey.key;
	try{
	    System.out.println("config_" + confForKey.country + ".xmlを使用してHTTPリクエスト : " + urlString);
	    URI uri = new URI(urlString);
	    URLConnection httpConnection = uri.toURL().openConnection();
	    BufferedReader reader = new BufferedReader(new InputStreamReader(httpConnection.getInputStream(),"UTF-8"));
	    String buffer = "";
	    String strJson = "";
	    while(buffer != null){
		strJson = strJson + buffer;
		buffer = reader.readLine();
	    }
	    //json文字列をjsonオブジェクトに変換
	    Search search = JSON.decode(strJson, Search.class);
	    pageToken = "&pageToken=" + search.nextPageToken;

	    sdf.applyPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	    for(int i = 0; i < search.items.length; i++){
		YouTubeVideo video = new YouTubeVideo();
		video.videoId = search.items[i].id.videoId;
		video.publishedAt = sdf.parse(search.items[i].snippet.publishedAt);
		video.status = "public";
		video.description = search.items[i].snippet.description;
		video.channelId = search.items[i].snippet.channelId;
		video.title = search.items[i].snippet.title;

		videoList.add(video);
	    }
	    /*
	    //JDBCドライバを使用
	    Class.forName("com.mysql.jdbc.Driver");

	    try{
		//DBに接続
		Connection sqlConnection = DriverManager.getConnection("jdbc:mysql://localhost/" + DB_NAME + "?characterEncoding=UTF-8&connectionCollation=utf8mb4_general_ci&user=root&password=sonnawakenai4w");
		try{
		    //オートコミットモードOFF
		    sqlConnection.setAutoCommit(false);

		    String command = "UPDATE YOUTUBE_CHANNEL_MST SET LAST_UPDATE = ?";
		    PreparedStatement pstmt = sqlConnection.prepareStatement(command);
		    pstmt.setString(1,dateString);
		    pstmt.executeUpdate();

		    for(int i = 0; i < search.items.length; i++){
			//YouTubeVideoオブジェクトを生成
			YouTubeVideo video = new YouTubeVideo();
			video.videoId = search.items[i].id.videoId;
			video.uploadedAt = sdf.parse(search.items[i].snippet.publishedAt);
			video.status = "public";
			video.description = search.items[i].snippet.description;
			video.channelId = search.items[i].snippet.channelId;
			video.title = search.items[i].snippet.title;

			videoList.add(video);
		    }
		}catch(Exception e){
		    //ロールバック
		    sqlConnection.rollback();
		    System.out.println("トランザクションをロールバック");
		    sqlConnection.close();
		    System.out.println("接続をクローズ");
		}
	    }catch(Exception e){
		System.out.println("aa");
		System.out.println(e);
	    }
	    */
	}catch(Exception e){
	    System.out.println(e);
	    return null;
	}
	return  videoList;
    }

}
