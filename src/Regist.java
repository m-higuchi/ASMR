import java.io.*;
import java.net.*;
import java.util.*;
import java.sql.*;
import java.util.Map;
import net.arnx.jsonic.*;
import subscriptions.*;

public class Regist{
    //定数の定義
    public static final String MAX_RESULTS = "50";
    public static final String DB_NAME = "asmrtist";

    public static void main(String arg[]) throws Exception {
	Config conf = new Config();
	conf.set("/home/ec2-user/ASMR/bin/" + arg[0]);

	String pageToken = "";
	do{
	    String urlString = "https://www.googleapis.com/youtube/v3/subscriptions?part=snippet&channelId=" + conf.channelId + "&maxResults=" + MAX_RESULTS + "&key=" + conf.key + pageToken + "&fields=items(snippet(resourceId(channelId),title,description)),pageInfo,nextPageToken";

	    //HTTPリクエスト
	    URI uri = new URI(urlString);
	    URLConnection httpConnection = uri.toURL().openConnection();

	    /*ヘッダ取得
	      Map headers = httpConnection.getHeaderFields();
	      for(Object key : headers.keySet()){
	      System.out.println(key + ": " + headers.get(key));
	      }
	    */
	    
	    //レスポンスを取得
	    BufferedReader reader = new BufferedReader(new InputStreamReader(httpConnection.getInputStream(), "UTF-8"));
	    String buffer = "";
	    String strJson = "";
	    while(buffer != null){
		strJson = strJson + buffer;
		buffer = reader.readLine();
	    }

	    //json文字列をオブジェクトに変換
	    subscriptions.Subscriptions subscriptions = JSON.decode(strJson, Subscriptions.class);
	    if(subscriptions.nextPageToken != null){
		pageToken = "&pageToken=" + subscriptions.nextPageToken;
	    }else{
		pageToken = "";
	    }

	    //JDBCを使用
	    String connectionURL = null;
	    String user = null;
	    String password = null;


	    Class.forName(conf.driver);

	    try{
		//DBに接続
		Connection sqlConnection = DriverManager.getConnection(conf.url, conf.user,conf.password);
		System.out.println("DBに接続");

		//オートコミットモードOFF
		sqlConnection.setAutoCommit(false);
		System.out.println("オートコミットモードをOFF");

		Statement stmt = sqlConnection.createStatement();
		String command = "SELECT * FROM YOUTUBE_CHANNEL_MST WHERE CHANNEL_ID = ?  LIMIT 1";
		PreparedStatement pstmt1 = sqlConnection.prepareStatement(command);
		command = "INSERT INTO YOUTUBE_CHANNEL_MST(CHANNEL_ID, CHANNEL_TITLE, ACTIVITY_LEVEL, ASMR_ONLY, DESCRIPTION, ASMRTIST_ID, LAST_UPDATE,LANG) VALUES(?,?,?,?,?,?,now(),?)";
		PreparedStatement pstmt2 = sqlConnection.prepareStatement(command);
		command = "UPDATE YOUTUBE_CHANNEL_MST SET CHANNEL_TITLE=?, DESCRIPTION=? WHERE CHANNEL_ID=?";
		PreparedStatement pstmt3 = sqlConnection.prepareStatement(command);
		ResultSet rset;
		try{
		    for(int i = 0; i < subscriptions.items.length; i++){
			//YouTubeChannelインスタンスを生成
			YouTubeChannel ytc = new YouTubeChannel(subscriptions.items[i],conf);

			pstmt1.setString(1,ytc.channelId);
			rset = pstmt1.executeQuery();
			if(rset.next()){
			    System.out.println(ytc.channelTitle + " already exists in the YouTube Channel table.");
			    //Channel TitleまたはDescriptionに変更があればDBを修正
			    if(!ytc.channelTitle.equals(rset.getString(2)) || !ytc.description.equals(rset.getString(6))){
				System.out.println("チャンネルタイトルまたは概要に変更あり");
				pstmt3.setString(1,ytc.channelTitle);
				pstmt3.setString(2,ytc.description);
				pstmt3.setString(3,ytc.channelId);
				pstmt3.executeUpdate();
				System.out.println("DBを更新");
			    }
			}else{ //レコードが存在しなければ追加
			    command = "INSERT INTO ASMRTIST_MST(name) VALUES (null)";
			    stmt.executeUpdate(command);
			    System.out.println("Inserted into the ASMRtist table.");

			    command = "SELECT LASTVAL()";
			    rset = stmt.executeQuery(command);
			    if(rset.next()){
				System.out.println(ytc.description);
				pstmt2.setString(1,ytc.channelId);
				pstmt2.setString(2,ytc.channelTitle);
				pstmt2.setInt(3,ytc.activityLevel);
				pstmt2.setBoolean(4,ytc.asmrOnly);
				pstmt2.setString(5,ytc.description);
				pstmt2.setInt(6,rset.getInt(1));
				pstmt2.setString(7,ytc.country);
				pstmt2.executeUpdate();
			    }
			    System.out.println("Inserted into the YouTubeChannel table.");
			}
		    }
		    sqlConnection.commit();
		}catch(Exception e){
		    System.out.println(e);
		    //ステートメントが閉じてない場合
		    if(stmt != null){
			stmt.close();
		    }
		    if(pstmt1 != null){
			pstmt1.close();
		    }
		    if(pstmt2 != null){
			pstmt2.close();
		    }
		    //接続が閉じてない場合
		    if(sqlConnection != null){
			//ロールバック
			sqlConnection.rollback();
			System.out.println("トランザクションをロールバック");
			sqlConnection.close();
			System.out.println("接続をクローズ");
		    }
		}finally{
		}
	    }catch(Exception e){
		System.out.println(e);
	    }
	}while(!pageToken.equals(""));
    }
}
