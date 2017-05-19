import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.TwitterException;
import twitter4j.auth.RequestToken;
import twitter4j.auth.AccessToken;
import twitter4j.Status;
import java.util.*;
import java.io.*;
import java.sql.*;

public class TwitterLoginOAuth{
    public static void main(String[] args){
	final String m_ConsumerKey = "E6jgC2PHHPxqIuGLiDSOqj1Sm";
	final String m_ConsumerSecret = "pCSTnBxUFluSgFABL8vdsddPg0ljoOhdOkZVvB90meCEwNliKC";
	Twitter twitter;

	//アクセストークンの読み込み
	AccessToken accessToken = loadAccessToken();

	//アクセストークンが既に保存されていればそれを利用してTwitter認証を行う
	//保存されていなければアクセストークを取りに行く
	try{
	    if(accessToken != null){
		//自分のディレクトリに保存していたAcdess Tokenを利用する
		twitter = new TwitterFactory().getInstance();
		twitter.setOAuthConsumer(m_ConsumerKey,m_ConsumerSecret);
		twitter.setOAuthAccessToken(accessToken);
            } else {
                twitter = new TwitterFactory().getInstance();
                twitter.setOAuthConsumer(m_ConsumerKey, m_ConsumerSecret);
                //アクセストークンの取得
                accessToken = getOAuthAccessToken(twitter);

                //自分のディレクトリに Access Token を保存しておく
                storeAccessToken(accessToken);
            }
            
            //自分のステータスの更新（＝ツイートの投稿）
            //Status status = twitter.updateStatus(args[0]);
            //System.out.println("Successfully updated the status to [" + status.getText() + "].");
	    for(String message : getMessages()){
		Status status = twitter.updateStatus(message);
	    }

        } catch(Exception e){
            System.err.println(e);
            System.exit(1);
        }
    }

    //ツイート内容取得
    public static ArrayList<String> getMessages()throws SQLException,ClassNotFoundException{
	ArrayList<String> messageArray = new ArrayList<String>();
	//JDBCを使用
	Class.forName("com.mysql.jdbc.Driver");
	//接続文字列
	String strConnection = "jdbc:mysql://localhost/ASMRtist?characterEncoding=UTF-8&connectionCollation=utf8mb4_general_ci&user=root&password=sonnawakenai4";
	Connection sqlConnection = null;
	try{
	    //接続
	    sqlConnection = DriverManager.getConnection(strConnection);
	    String sqlCommand = "SELECT YOUTUBE_CHANNEL_MST.CHANNEL_TITLE,YOUTUBE_VIDEO_MST.VIDEO_ID,YOUTUBE_VIDEO_MST.TITLE FROM YOUTUBE_VIDEO_MST INNER JOIN YOUTUBE_CHANNEL_MST ON YOUTUBE_CHANNEL_MST.CHANNEL_ID=YOUTUBE_VIDEO_MST.CHANNEL_ID WHERE YOUTUBE_VIDEO_MST.TWEETED='yet'";
	    String sqlCommand2 = "UPDATE YOUTUBE_VIDEO_MST SET TWEETED='tweeted' WHERE VIDEO_ID=?";
	    Statement stmt = sqlConnection.createStatement();
	    PreparedStatement pstmt = sqlConnection.prepareStatement(sqlCommand2);
	    ResultSet rset = stmt.executeQuery(sqlCommand);
	    while(rset.next()){
		String channelTitle = rset.getString(1);
		String videoId = rset.getString(2);
		String title = rset.getString(3);
		String message = "◆" +channelTitle + "◆"+ "\n" + title + "\n" + "http://youtu.be/" + videoId;
		messageArray.add(message);

		pstmt.setString(1,videoId);
		pstmt.executeUpdate();
	    }
	}catch(Exception e){
	    System.out.println(e);
	}finally{
	    sqlConnection.close();
	}	
	return messageArray;
    }

    //アクセストークンの取得
    static AccessToken getOAuthAccessToken(Twitter twitter){
        RequestToken requestToken = null;
        AccessToken accessToken = null;
        
        try {
            //リクエストトークンの作成
            //(メモ) アクセストークンを取得後，保存して再利用するならば
            // リクエストトークンの作成は１度きりでよい．
            requestToken = twitter.getOAuthRequestToken();
            
            //ブラウザで Twitter 認証画面を表示するよう促し，
            // PIN コードを入力させ，アクセストークンを作成（取得）する
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            while (null == accessToken) {
                System.out.println("Open the following URL and grant access to your account:");
                System.out.println(requestToken.getAuthorizationURL());
                System.out.print("Enter the PIN(if aviailable) or just hit enter.[PIN]:");
                String pin = br.readLine();
                try{
                    if(pin.length() > 0){
                        accessToken = twitter.getOAuthAccessToken(requestToken, pin);
                    }else{
                        accessToken = twitter.getOAuthAccessToken();
                    }
                } catch (TwitterException te) {
                    if(401 == te.getStatusCode()){
                        System.out.println("Unable to get the access token.");
                    }else{
                        te.printStackTrace();
                    }
                }
            } //while()
        } catch(Exception e){
            e.printStackTrace();
            System.exit(1);
        }
	System.out.println(accessToken.getScreenName());
	System.out.println(accessToken.getUserId());
	System.out.println(accessToken.hashCode());
	System.out.println(accessToken.toString());
        return accessToken;
    }
    
    //アクセストークンの読み込み
    private static AccessToken loadAccessToken(){
        File f = createAccessTokenFileName();

        ObjectInputStream is = null;
        try {
            is = new ObjectInputStream(new FileInputStream(f));
            AccessToken accessToken = (AccessToken) is.readObject();
            return accessToken;
        } catch (IOException e) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if(is != null){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //アクセストークンの保存
    private static void storeAccessToken(AccessToken accessToken){
        //ファイル名の生成
        File f = createAccessTokenFileName();

        //親ディレクトリが存在しない場合，親ディレクトリを作る．
        File d = f.getParentFile();
        if (!d.exists()) { d.mkdirs(); }

        //ファイルへの書き込み
        ObjectOutputStream os = null;
        try {
            os = new ObjectOutputStream(new FileOutputStream(f));
            os.writeObject(accessToken);
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            if(os != null){
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // アクセストークンを保存するファイル名を生成する
    static File createAccessTokenFileName() {
        // (メモ) System.getProperty("user.home") の返し値は
        // ホームディレクトリの絶対パス
        String s = System.getProperty("user.home") + "/.twitter/client/sample/accessToken.dat";
        return new File(s);
    }
}
