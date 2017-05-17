import java.lang.*;
import java.sql.*;
import java.net.*;
import java.util.*;
import java.text.*;
import net.arnx.jsonic.*;
import subscriptions.*;

public class VideoUploader{
    public static final String DB_NAME = "ASMRtist";

    public static void main(String arg[]) throws Exception{
	ArrayList<YouTubeChannel> youtubeChannelList = new ArrayList<YouTubeChannel>();
	try{
	    int activityLevel = Integer.parseInt(arg[0]);
	    if(activityLevel < 0 || activityLevel > 5){
		System.out.println("入力エラー");
		System.exit(-1);
	    }
	}catch(Exception e){
	    System.out.println("入力エラー");
	    System.exit(-2);
	}

	//JDBCを使用
	Class.forName("com.mysql.jdbc.Driver");

	try{
	    //DBに接続
	    Connection sqlConnection = DriverManager.getConnection("jdbc:mysql://localhost/" + DB_NAME + "?characterEncoding=UTF-8&connectionCollation=utf8mb4_general_ci&user=root&password=sonnawakenai4");
	    System.out.println("DBに接続");

	    //オートコミットモードOFF
	    sqlConnection.setAutoCommit(false);
	    System.out.println("オートコミットモードをOFF");
	    
	    //SQL文の準備
	    String getChannelCommand = "SELECT CHANNEL_ID,CHANNEL_TITLE,ACTIVITY_LEVEL,ASMR_ONLY,DESCRIPTION,LAST_UPDATE FROM YOUTUBE_CHANNEL_MST WHERE ACTIVITY_LEVEL = ?";
	    String updateChannelCommand = "UPDATE YOUTUBE_CHANNEL_MST SET LAST_UPDATE = NOW() WHERE CHANNEL_ID = ?";
	    String insertVideoCommand = "INSERT INTO YOUTUBE_VIDEO_MST (VIDEO_ID,TITLE,CHANNEL_ID,PUBLISHED_AT,DESCRIPTION,STATUS,TWEETED) VALUES(?,?,?,cast(? as datetime),?,'public','yet')";
	    String getLastUploadDateCommand = "select max(PUBLISHED_AT) from YOUTUBE_VIDEO_MST where CHANNEL_ID=?";
	    String updateActivityLevelCommand = "UPDATE YOUTUBE_CHANNEL_MST SET ACTIVITY_LEVEL = ? WHERE CHANNEL_ID = ?";
	    PreparedStatement getChannelStatement = sqlConnection.prepareStatement(getChannelCommand);
	    PreparedStatement updateChannelStatement = sqlConnection.prepareStatement(updateChannelCommand);
	    PreparedStatement insertVideoStatement = sqlConnection.prepareStatement(insertVideoCommand);
	    PreparedStatement getLastUploadDateStatement = sqlConnection.prepareStatement(getLastUploadDateCommand);
	    PreparedStatement updateActivityLevelStatement = sqlConnection.prepareStatement(updateActivityLevelCommand);
	    getChannelStatement.setString(1,arg[0]);
	    ResultSet rset = null;
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");


	    try{
		//ActivityLevelがarg[0]のチャンネル情報を取得
		rset = getChannelStatement.executeQuery();

		while(rset.next()){
		    //チャンネル情報をYouTubeChannelインスタンスに格納
		    YouTubeChannel youtubeChannel = new YouTubeChannel();
		    youtubeChannel.channelId = rset.getString(1);
		    youtubeChannel.channelTitle = rset.getString(2);
		    youtubeChannel.activityLevel = rset.getInt(3);
		    youtubeChannel.asmrOnly = rset.getBoolean(4);
		    youtubeChannel.description = rset.getString(5);

		    //新着動画を取得
		    ArrayList<YouTubeVideo> videoList = youtubeChannel.getVideoListAfter(rset.getTimestamp(6));

		    //最終更新時を更新（本来はYouTubeChannelクラスがメソッドとして持つべき）
		    updateChannelStatement.setString(1,youtubeChannel.channelId);
		    updateChannelStatement.executeUpdate();

		    //Acitivity Levelを更新（本来はYouTubeChannelクラスがメソッドとして持つべき）
		    getLastUploadDateStatement.setString(1,youtubeChannel.channelId);
		    ResultSet rset2 = getLastUploadDateStatement.executeQuery();
		    rset2.next();
		    updateActivityLevelStatement.setString(2,youtubeChannel.channelId);

		    if(videoList.size() != 0){
			updateActivityLevelStatement.setString(1,String.valueOf(calcActLv(youtubeChannel.activityLevel, rset2.getTimestamp(1),true)));
			for(YouTubeVideo video : videoList){
			    //System.out.println("videoId="+video.videoId + ", channelId=" + video.channelId);
			    insertVideoStatement.setString(1,video.videoId);
			    insertVideoStatement.setString(2,video.title);
			    insertVideoStatement.setString(3,video.channelId);
			    insertVideoStatement.setString(4,sdf.format(video.publishedAt));
			    insertVideoStatement.setString(5,video.description);
			    insertVideoStatement.executeUpdate();
			}
		    }else{
			updateActivityLevelStatement.setString(1,String.valueOf(calcActLv(youtubeChannel.activityLevel, rset2.getTimestamp(1),false)));
			System.out.println(calcActLv(youtubeChannel.activityLevel, rset2.getTimestamp(1),false));
		    }
		    updateActivityLevelStatement.executeUpdate();
		}
		sqlConnection.commit();
	    }catch(Exception e){
		System.out.println(e);

		//接続が閉じてない場合
		if(sqlConnection != null){
		    //ロールバック
		    sqlConnection.rollback();
		    System.out.println("トランザクションをロールバック");
		}
	    }finally{
		//ステートメントをクローズ
		getChannelStatement.close();
		updateChannelStatement.close();
		//コネクションをクローズ
		sqlConnection.close();
		System.out.println("接続をクローズ");
	    }

	}catch(Exception e){
	    System.out.println(e);
	}finally{
	}
    }

    //ActivityLevelを算出
    //新着動画ありの場合-1
    //新着動画なしの場合
    //現在lv5:5
    //現在lv4:最終投稿日から1年以上経過で+1
    //現在lv3:最終投稿日から6ヶ月以上経過で+1
    //現在lv2:最終投稿日から1ヶ月以上経過で+1
    //現在lv1:最終投稿日から1週間以上経過で+1
    //現在lv0:最終投稿日から1日以上経過で+1
    public static int calcActLv(int activityLevel,java.util.Date lastUploadDate,Boolean update){
	final long DAY_MILLISECONDS = 24 * 60 * 60 * 1000;
	java.util.Date now = new java.util.Date();
	if(update == true && activityLevel != 0){
	    activityLevel--;
	    System.out.println("test1");
	}else if(activityLevel == 0 && (now.getTime() - lastUploadDate.getTime() > DAY_MILLISECONDS)){
	    activityLevel++;
	    System.out.println("test2");
	}else if(activityLevel == 1 && (now.getTime() - lastUploadDate.getTime() > DAY_MILLISECONDS * 7)){
	    activityLevel++;
	    System.out.println("test3");
	}else if(activityLevel == 2 && (now.getTime() - lastUploadDate.getTime() > DAY_MILLISECONDS * 30)){
	    activityLevel++;
	    System.out.println("test4");
	}else if(activityLevel == 3 && (now.getTime() - lastUploadDate.getTime() > DAY_MILLISECONDS * 180)){
	    activityLevel++;
	    System.out.println("test5");
	}else if(activityLevel == 4 && (now.getTime() - lastUploadDate.getTime() > DAY_MILLISECONDS * 365)){
	    activityLevel++;
	    System.out.println("test6");
	}
	return activityLevel;
    }
}
