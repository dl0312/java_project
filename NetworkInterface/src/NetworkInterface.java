import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

class Pair<U, V> {

	/**
	 * The first element of this <code>Pair</code>
	 */
	private U first;

	/**
	 * The second element of this <code>Pair</code>
	 */
	private V second;

	/**
	 * Constructs a new <code>Pair</code> with the given values.
	 * 
	 * @param first
	 *            the first element
	 * @param second
	 *            the second element
	 */
	public Pair(U first, V second) {

		this.first = first;
		this.second = second;
	}
	
	public U first() {
        return first;
    }

    public V second() {
        return second;
    }
}

class NetworkRoom {
	public static final int ONE_WINS = 1;	// 단판승부
	public static final int TWO_WINS = 2;	// 3판 2승제
	public static final int THREE_WINS = 3;	// 5판 3승제
	public static final int FOUR_WINS = 4;	// 7판 4승제
	
	private int roomId;
	private String roomName;
	private String[] guests;
	private boolean isOpened;
	private int gameMode;
}

class NetworkRoomList {
	private int roomNum;
	private NetworkRoom[] rooms;
}

// 밑에 구체적인 상수 수치는 추후 변경할 것임!
interface NetworkInterface {
	
	//public void inputstream();// 추가
	
	// all functions can return NETWORK_ERROR
	public static final int NETWORK_ERROR = 1;
	// all functions can return INVALID_REQ or INVALID_RES
	public static final int INVALID_REQ = -2;
	public static final int INVALID_RES = -3;
	// wait로 표시된 함수들은 이 값을 반환할 수 있음.
	public static final int TIME_OVER = -1;
	
	public static final int NICKNAME_OK = 2;
	public static final int NICKNAME_DUP = 3;
	public static final int NICKNAME_INVALID = 4;
	int enterLobby(String nickname);
	
	// return server user number
	int getUserNum();
	
	// second is null if fail
	public static final int GET_ROOM_LIST_OK = 7;
	public Pair<Integer, NetworkRoomList> getRoomList();
	
	public static final int MAKE_ROOM_OK = 10;
	int makeRoom(String roomName, int gameMode);
	
	public static final int ENTER_ROOM_OK = 11;
	public static final int ROOM_FULL = 12;
	public static final int ROOM_DEL = 13;
	int enterRoom(int roomId);
	
	// String -> user name
	public static final int USER_ENTER = 14;
	Pair<Integer, String> waitUser();		// wait
	
	
	// 이 밑의 모든 함수들은 USER_EXIT을 반활 할 수 있음.
	public static final int ENEMY_EXIT = 111;
	
	// call one more if this function return USER_EXIT or INVALID_RES
	// internally using static flag, prevent duplicate sending packet.
	// 이 사실은 모든 함수들에 해당됨.
	public static final int READY_OK = 15;
	int readyGame();	// wait
	
	public static final int EXIT_ROOM_OKAY = 2222;
	int exitRoom();
	
	public static final int GAME_START = 16;
	int waitGameStart();
	
	public static final int DROP_BALL_OK = 17;
	int dropBall(int pos);
	
	public static final int ENEMY_DROP = 18;
	public Pair<Integer, Integer> waitDrop();	// wait
}


class NetworkMethod implements NetworkInterface {
	
	public static final String ip = "163.239.200.116";
	public static final int port = 7777;
	
	private static boolean READY_GAME_RES_FLAG = true;
	private static boolean EXIT_ROOM_RES_FLAG = true;
	private static boolean WAIT_GAME_START_RES_FLAG = true;
	private static boolean DROP_BALL_RES_FLAG = true;
	private static boolean WAIT_DROP_RES_FLAG = true;
	private static Socket sock = null;
	private boolean isConnected = false;
	private NetworkRoomList roomdata;
	private String username;
	private int positiondrop;
	
	
	public int enterLobby(String nickname) {
		try {
			sock = new Socket(ip, port);
			
			/*
			ObjectOutputStream outStream = new ObjectOutputStream(new BufferedOutputStream(sock.getOutputStream()));
			ObjectInputStream inStream = new ObjectInputStream(new BufferedInputStream(sock.getInputStream()));
			outStream.writeInt(0);
			outStream.writeInt(PacketFlag.ENTER_LOBBY_REQ);
			outStream.writeObject(nickname);
			username = nickname;
			outStream.close();
			inStream.readInt();
			int flag = inStream.readInt();
			*/
			
			DataOutputStream reqStream = new DataOutputStream(sock.getOutputStream());
			byte[] reqData;
			ByteArrayOutputStream reqDataStream = new ByteArrayOutputStream();
			ObjectOutputStream reqDataOutputStream = new ObjectOutputStream(reqDataStream);
			reqDataOutputStream.writeInt(PacketFlag.ENTER_LOBBY_REQ);
			reqDataOutputStream.writeObject(nickname);
			reqData = reqDataStream.toByteArray();
			reqStream.writeInt(reqData.length);
			reqStream.write(reqData);
			reqStream.flush();
			
			DataInputStream resStream = new DataInputStream(sock.getInputStream());
			int dataLen = resStream.readInt();
			int readLen = 0;
			int readSz;
			byte[] resData = new byte[dataLen];
			while(readLen < dataLen && (readSz=resStream.read(resData,readLen,dataLen-readLen)) != -1) {
				readLen += readSz;
			}
			if(readLen < dataLen) {
				sock.close();
				return INVALID_RES;
			}
			ObjectInputStream resDataInputStream = new ObjectInputStream(new ByteArrayInputStream(resData));
			
			int flag = resDataInputStream.readInt();
			
			if(flag != PacketFlag.ENTER_LOBBY_RES) {
				// error handling
				resDataInputStream.close();
				sock.close();
				return INVALID_RES;
			} else {
				int res = resDataInputStream.readInt();
				if(res == NICKNAME_OK) {
					resDataInputStream.close();
					isConnected = true;
					return res;
				}
				else if(res == NICKNAME_DUP || res == NICKNAME_INVALID) {
					resDataInputStream.close();
					sock.close();
					return res;
				} else {
					resDataInputStream.close();
					sock.close();
					return INVALID_RES;
				}
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			try {
				sock.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return NETWORK_ERROR;
		}
	}
	public int getUserNum(){
		try{
			
			DataOutputStream reqStream = new DataOutputStream(sock.getOutputStream());
			byte[] reqData;
			ByteArrayOutputStream reqDataStream = new ByteArrayOutputStream();
			ObjectOutputStream reqDataOutputStream = new ObjectOutputStream(reqDataStream);
			reqDataOutputStream.writeInt(PacketFlag.GET_USERNUM_REQ);
			reqData = reqDataStream.toByteArray();
			reqStream.writeInt(reqData.length);
			reqStream.write(reqData);
			reqStream.flush();
			
			DataInputStream resStream = new DataInputStream(sock.getInputStream());
			int dataLen = resStream.readInt();
			int readLen = 0;
			int readSz;
			byte[] resData = new byte[dataLen];
			while(readLen < dataLen && (readSz=resStream.read(resData,readLen,dataLen-readLen)) != -1) {
				readLen += readSz;
			}
			if(readLen < dataLen) {
				sock.close();
				return INVALID_RES;
			}
			ObjectInputStream resDataInputStream = new ObjectInputStream(new ByteArrayInputStream(resData));
			
			int flag = resDataInputStream.readInt();
			
			if(flag != PacketFlag.GET_USERNUM_RES) {
				// error handling
				resDataInputStream.close();
				sock.close();
				return INVALID_RES;
			} else {
				int User_Num = resDataInputStream.readInt();
				resDataInputStream.close();
				sock.close();
				return User_Num;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			try {
				sock.close();
				isConnected = false;
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return NETWORK_ERROR;
		}
	}
	
	public Pair<Integer, NetworkRoomList> getRoomList(){
		try{
			DataOutputStream reqStream = new DataOutputStream(sock.getOutputStream());
			byte[] reqData;
			ByteArrayOutputStream reqDataStream = new ByteArrayOutputStream();
			ObjectOutputStream reqDataOutputStream = new ObjectOutputStream(reqDataStream);
			reqDataOutputStream.writeInt(PacketFlag.GET_ROOMLIST_REQ);
			reqData = reqDataStream.toByteArray();
			reqStream.writeInt(reqData.length);
			reqStream.write(reqData);
			reqStream.flush();
			
			DataInputStream resStream = new DataInputStream(sock.getInputStream());
			int dataLen = resStream.readInt();
			int readLen = 0;
			int readSz;
			byte[] resData = new byte[dataLen];
			while(readLen < dataLen && (readSz=resStream.read(resData,readLen,dataLen-readLen)) != -1) {
				readLen += readSz;
			}
			if(readLen < dataLen) {
				sock.close();
				return new Pair <Integer, NetworkRoomList>( new Integer(INVALID_RES), roomdata);
			}
			ObjectInputStream resDataInputStream = new ObjectInputStream(new ByteArrayInputStream(resData));
			
			int flag = resDataInputStream.readInt();
			
			if(flag != PacketFlag.GET_ROOMLIST_RES) {
				// error handling
				resDataInputStream.close();
				sock.close();
				return new Pair <Integer, NetworkRoomList>( new Integer(INVALID_RES), roomdata);
			} else {
				int res = resDataInputStream.readInt();
				resDataInputStream.close();
				sock.close();
				if(res == GET_ROOM_LIST_OK){
					return new Pair <Integer, NetworkRoomList>( new Integer(res), roomdata);
				}
				else 
					return new Pair <Integer, NetworkRoomList>( new Integer(INVALID_RES), roomdata);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			try {
				sock.close();
				isConnected = false;
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return new Pair <Integer, NetworkRoomList>( new Integer(INVALID_RES), roomdata);
		}
	}
	
	public int makeRoom(String roomName, int gameMode){
		try{
			DataOutputStream reqStream = new DataOutputStream(sock.getOutputStream());
			byte[] reqData;
			ByteArrayOutputStream reqDataStream = new ByteArrayOutputStream();
			ObjectOutputStream reqDataOutputStream = new ObjectOutputStream(reqDataStream);
			reqDataOutputStream.writeInt(PacketFlag.MAKE_ROOM_REQ);
			reqDataOutputStream.writeObject(roomName);
			reqDataOutputStream.writeObject(gameMode);
			reqData = reqDataStream.toByteArray();
			reqStream.writeInt(reqData.length);
			reqStream.write(reqData);
			reqStream.flush();
			
			DataInputStream resStream = new DataInputStream(sock.getInputStream());
			int dataLen = resStream.readInt();
			int readLen = 0;
			int readSz;
			byte[] resData = new byte[dataLen];
			while(readLen < dataLen && (readSz=resStream.read(resData,readLen,dataLen-readLen)) != -1) {
				readLen += readSz;
			}
			if(readLen < dataLen) {
				sock.close();
				return INVALID_RES;
			}
			ObjectInputStream resDataInputStream = new ObjectInputStream(new ByteArrayInputStream(resData));
			
			int flag = resDataInputStream.readInt();
			
			if(flag != PacketFlag.MAKE_ROOM_RES) {
				// error handling

				resDataInputStream.close();
				sock.close();
				return INVALID_RES;
			} else {
				int res = resDataInputStream.readInt();
				resDataInputStream.close();
				sock.close();
				if(res == MAKE_ROOM_OK){
					return res;
				}
				else{
					return INVALID_RES;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			try {
				sock.close();
				isConnected = false;
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return NETWORK_ERROR;
		}
	}
	
	public int enterRoom(int roomId){
		try{
			DataOutputStream reqStream = new DataOutputStream(sock.getOutputStream());
			byte[] reqData;
			ByteArrayOutputStream reqDataStream = new ByteArrayOutputStream();
			ObjectOutputStream reqDataOutputStream = new ObjectOutputStream(reqDataStream);
			reqDataOutputStream.writeInt(PacketFlag.ENTER_ROOM_REQ);
			reqDataOutputStream.writeObject(roomId);
			reqData = reqDataStream.toByteArray();
			reqStream.writeInt(reqData.length);
			reqStream.write(reqData);
			reqStream.flush();
			
			DataInputStream resStream = new DataInputStream(sock.getInputStream());
			int dataLen = resStream.readInt();
			int readLen = 0;
			int readSz;
			byte[] resData = new byte[dataLen];
			while(readLen < dataLen && (readSz=resStream.read(resData,readLen,dataLen-readLen)) != -1) {
				readLen += readSz;
			}
			if(readLen < dataLen) {
				sock.close();
				return INVALID_RES;
			}
			ObjectInputStream resDataInputStream = new ObjectInputStream(new ByteArrayInputStream(resData));
			
			int flag = resDataInputStream.readInt();
			
			if(flag != PacketFlag.ENTER_ROOM_RES) {
				// error handling
				resDataInputStream.close();
				sock.close();
				return INVALID_RES;
			} else {
				int res = resDataInputStream.readInt();
				resDataInputStream.close();
				sock.close();
				if(res==ENTER_ROOM_OK){
					return res;
				}
				else if(res==ROOM_FULL || res == ROOM_DEL){
					return res;
				}
				else{
					return INVALID_RES;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			try {
				sock.close();
				isConnected = false;
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return NETWORK_ERROR;
		}
	}
	
	public Pair<Integer, String> waitUser(){
		try{
			DataOutputStream reqStream = new DataOutputStream(sock.getOutputStream());
			byte[] reqData;
			ByteArrayOutputStream reqDataStream = new ByteArrayOutputStream();
			ObjectOutputStream reqDataOutputStream = new ObjectOutputStream(reqDataStream);
			reqDataOutputStream.writeInt(PacketFlag.WAIT_USER_REQ);
			reqData = reqDataStream.toByteArray();
			reqStream.writeInt(reqData.length);
			reqStream.write(reqData);
			reqStream.flush();
			
			DataInputStream resStream = new DataInputStream(sock.getInputStream());
			int dataLen = resStream.readInt();
			int readLen = 0;
			int readSz;
			byte[] resData = new byte[dataLen];
			while(readLen < dataLen && (readSz=resStream.read(resData,readLen,dataLen-readLen)) != -1) {
				readLen += readSz;
			}
			if(readLen < dataLen) {
				sock.close();
				return new Pair <Integer, String>( new Integer(INVALID_RES), username);			
			}
			ObjectInputStream resDataInputStream = new ObjectInputStream(new ByteArrayInputStream(resData));
			
			int flag = resDataInputStream.readInt();

			
			if(flag != PacketFlag.WAIT_USER_RES) {
				// error handling
				resDataInputStream.close();
				sock.close();
				return new Pair <Integer, String>( new Integer(INVALID_RES), username);
			} else if(flag == TIME_OVER){
				return new Pair <Integer, String>( new Integer(TIME_OVER), username);
			} else {
				int res = resDataInputStream.readInt();
				resDataInputStream.close();
				sock.close();
				if(res==USER_ENTER){
					return new Pair <Integer, String>( new Integer(res), username);
				}
				
				else{
					return new Pair <Integer, String>( new Integer(INVALID_RES), username);
				}
			} 
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			try {
				sock.close();
				isConnected = false;
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return new Pair <Integer, String>( new Integer(NETWORK_ERROR), username);
		}
	}
	
	public int readyGame(){
		try{
			if(READY_GAME_RES_FLAG){
				READY_GAME_RES_FLAG = false;
			}
			
			DataOutputStream reqStream = new DataOutputStream(sock.getOutputStream());
			byte[] reqData;
			ByteArrayOutputStream reqDataStream = new ByteArrayOutputStream();
			ObjectOutputStream reqDataOutputStream = new ObjectOutputStream(reqDataStream);
			reqDataOutputStream.writeInt(PacketFlag.WAIT_USER_REQ);
			reqData = reqDataStream.toByteArray();
			reqStream.writeInt(reqData.length);
			reqStream.write(reqData);
			reqStream.flush();
			
			DataInputStream resStream = new DataInputStream(sock.getInputStream());
			int dataLen = resStream.readInt();
			int readLen = 0;
			int readSz;
			byte[] resData = new byte[dataLen];
			while(readLen < dataLen && (readSz=resStream.read(resData,readLen,dataLen-readLen)) != -1) {
				readLen += readSz;
			}
			if(readLen < dataLen) {
				sock.close();
				return INVALID_RES;
			}
			ObjectInputStream resDataInputStream = new ObjectInputStream(new ByteArrayInputStream(resData));
			
			int flag = resDataInputStream.readInt();
			
			if( flag!= PacketFlag.WAIT_USER_RES ){
				// error handling
				resDataInputStream.close();
				sock.close();
				return INVALID_RES;
			} else {
				int res = resDataInputStream.readInt();
				resDataInputStream.close();
				sock.close();
				if(res==READY_OK){
					return res;
				}
				else if(res==ENEMY_EXIT){
					return res;
				}
				else{
					return INVALID_RES;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			try {
				sock.close();
				isConnected = false;
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return NETWORK_ERROR;
		}
	}
	
	public int exitRoom(){
		try{
			if(EXIT_ROOM_RES_FLAG){
				DataOutputStream reqStream = new DataOutputStream(sock.getOutputStream());
				byte[] reqData;
				ByteArrayOutputStream reqDataStream = new ByteArrayOutputStream();
				ObjectOutputStream reqDataOutputStream = new ObjectOutputStream(reqDataStream);
				reqDataOutputStream.writeInt(PacketFlag.EXIT_ROOM_REQ);
				
				reqData = reqDataStream.toByteArray();
				reqStream.writeInt(reqData.length);
				reqStream.write(reqData);
				reqStream.flush();
				EXIT_ROOM_RES_FLAG = false;
			}
			
			DataInputStream resStream = new DataInputStream(sock.getInputStream());
			int dataLen = resStream.readInt();
			int readLen = 0;
			int readSz;
			byte[] resData = new byte[dataLen];
			while(readLen < dataLen && (readSz=resStream.read(resData,readLen,dataLen-readLen)) != -1) {
				readLen += readSz;
			}
			if(readLen < dataLen) {
				sock.close();
				return INVALID_RES;
			}
			ObjectInputStream resDataInputStream = new ObjectInputStream(new ByteArrayInputStream(resData));
			
			int flag = resDataInputStream.readInt();
			
			if(flag != PacketFlag.EXIT_ROOM_RES) {
				// error handling
				resDataInputStream.close();
				sock.close();
				return INVALID_RES;
			} else {
				int res = resDataInputStream.readInt();
				resDataInputStream.close();
				sock.close();
				if(res==EXIT_ROOM_OKAY){
					return res;
				}
				else if(res==ENEMY_EXIT){
					return res;
				}
				else{
					return INVALID_RES;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			try {
				sock.close();
				isConnected = false;
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return NETWORK_ERROR;
		}
	}
	
	public int waitGameStart(){
		try{
			if(WAIT_GAME_START_RES_FLAG){
				DataOutputStream reqStream = new DataOutputStream(sock.getOutputStream());
				byte[] reqData;
				ByteArrayOutputStream reqDataStream = new ByteArrayOutputStream();
				ObjectOutputStream reqDataOutputStream = new ObjectOutputStream(reqDataStream);
				reqDataOutputStream.writeInt(PacketFlag.WAIT_GAMESTART_REQ);
				reqData = reqDataStream.toByteArray();
				reqStream.writeInt(reqData.length);
				reqStream.write(reqData);
				reqStream.flush();
				EXIT_ROOM_RES_FLAG = false;
			}
			DataInputStream resStream = new DataInputStream(sock.getInputStream());
			int dataLen = resStream.readInt();
			int readLen = 0;
			int readSz;
			byte[] resData = new byte[dataLen];
			while(readLen < dataLen && (readSz=resStream.read(resData,readLen,dataLen-readLen)) != -1) {
				readLen += readSz;
			}
			if(readLen < dataLen) {
				sock.close();
				return INVALID_RES;
			}
			ObjectInputStream resDataInputStream = new ObjectInputStream(new ByteArrayInputStream(resData));
			
			int flag = resDataInputStream.readInt();
			
			if(flag != PacketFlag.WAIT_GAMESTART_RES) {
				// error handling
				resDataInputStream.close();
				sock.close();
				return INVALID_RES;
			} else {
				int res = resDataInputStream.readInt();
				resDataInputStream.close();
				sock.close();
				if(res==GAME_START){
					return res;
				}
				else if(res==ENEMY_EXIT){
					return res;
				}
				else{
					return INVALID_RES;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			try {
				sock.close();
				isConnected = false;
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return NETWORK_ERROR;
		}
	}
	
	public int dropBall(int pos){
		try{
			if(DROP_BALL_RES_FLAG){
				DataOutputStream reqStream = new DataOutputStream(sock.getOutputStream());
				byte[] reqData;
				ByteArrayOutputStream reqDataStream = new ByteArrayOutputStream();
				ObjectOutputStream reqDataOutputStream = new ObjectOutputStream(reqDataStream);
				reqDataOutputStream.writeInt(PacketFlag.DROP_BALL_REQ);
				reqDataOutputStream.writeObject(pos);
				reqData = reqDataStream.toByteArray();
				reqStream.writeInt(reqData.length);
				reqStream.write(reqData);
				reqStream.flush();
				EXIT_ROOM_RES_FLAG = false;
			}
			DataInputStream resStream = new DataInputStream(sock.getInputStream());
			int dataLen = resStream.readInt();
			int readLen = 0;
			int readSz;
			byte[] resData = new byte[dataLen];
			while(readLen < dataLen && (readSz=resStream.read(resData,readLen,dataLen-readLen)) != -1) {
				readLen += readSz;
			}
			if(readLen < dataLen) {
				sock.close();
				return INVALID_RES;
			}
			ObjectInputStream resDataInputStream = new ObjectInputStream(new ByteArrayInputStream(resData));
			
			int flag = resDataInputStream.readInt();
			
			if(flag != PacketFlag.DROP_BALL_RES) {
				// error handling
				resDataInputStream.close();
				sock.close();
				return INVALID_RES;
			} else {
				int res = resDataInputStream.readInt();
				resDataInputStream.close();
				sock.close();
				if(res==DROP_BALL_OK){
					return res;
				}
				else if(res==ENEMY_EXIT){
					return res;
				}
				else{
					return INVALID_RES;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			try {
				sock.close();
				isConnected = false;
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return NETWORK_ERROR;
		}
	}
	
	
	public Pair<Integer, Integer> waitDrop(){
		try{
			if(WAIT_DROP_RES_FLAG){
				DataOutputStream reqStream = new DataOutputStream(sock.getOutputStream());
				byte[] reqData;
				ByteArrayOutputStream reqDataStream = new ByteArrayOutputStream();
				ObjectOutputStream reqDataOutputStream = new ObjectOutputStream(reqDataStream);
				reqDataOutputStream.writeInt(PacketFlag.ENEMY_DROP_BALL_REQ);
				reqData = reqDataStream.toByteArray();
				reqStream.writeInt(reqData.length);
				reqStream.write(reqData);
				reqStream.flush();
				EXIT_ROOM_RES_FLAG = false;
			}
			
			
			DataInputStream resStream = new DataInputStream(sock.getInputStream());
			int dataLen = resStream.readInt();
			int readLen = 0;
			int readSz;
			byte[] resData = new byte[dataLen];
			while(readLen < dataLen && (readSz=resStream.read(resData,readLen,dataLen-readLen)) != -1) {
				readLen += readSz;
			}
			if(readLen < dataLen) {
				sock.close();
				return new Pair <Integer, Integer>( new Integer(INVALID_RES), positiondrop);
			}
			ObjectInputStream resDataInputStream = new ObjectInputStream(new ByteArrayInputStream(resData));
			
			int flag = resDataInputStream.readInt();
			
			if(flag != PacketFlag.ENEMY_DROP_BALL_RES) {
				// error handling
				resDataInputStream.close();
				sock.close();
				return new Pair <Integer, Integer>( new Integer(INVALID_RES), positiondrop);
			} else {
				int res = resDataInputStream.readInt();
				resDataInputStream.close();
				sock.close();
				if(res==ENEMY_DROP){
					return new Pair <Integer, Integer>( new Integer(res), positiondrop);
				}
				else if(res==ENEMY_EXIT){
					return new Pair <Integer, Integer>( new Integer(ENEMY_EXIT), positiondrop);
				}
				else{
					return new Pair <Integer, Integer>( new Integer(INVALID_RES), positiondrop);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			try {
				sock.close();
				isConnected = false;
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return new Pair <Integer, Integer>( new Integer(NETWORK_ERROR), positiondrop);
		}
	}
}
