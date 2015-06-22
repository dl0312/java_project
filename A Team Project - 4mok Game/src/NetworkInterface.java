import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.UnknownHostException;

class NetworkRoom implements Serializable {
	public static final int ONE_WINS = 1;	// 단판승부
	public static final int TWO_WINS = 2;	// 3판 2승제
	public static final int THREE_WINS = 3;	// 5판 3승제
	public static final int FOUR_WINS = 4;	// 7판 4승제
	
	private int roomId;
	private String roomName;
	private String[] guests;
	private boolean isOpened;
	private int gameMode;
	
	NetworkRoom(int roomId, String roomName, String[] guests, boolean isOpened, int gameMode) {
		this.roomId = roomId;
		this.roomName = roomName;
		this.guests = guests;
		this.isOpened = isOpened;
		this.gameMode = gameMode;
	}
	
	public int getRoomId() { return roomId; }
	public String getRoomName() { return roomName; }
	public String[] getGuests() { return guests; }
	public boolean getIsOpened() { return isOpened; }
	public int getGameMode() { return gameMode; }
}

class NetworkRoomList implements Serializable {
	private int roomNum;
	private NetworkRoom[] rooms;
	
	NetworkRoomList(int roomNum, NetworkRoom[] rooms) {
		this.roomNum = roomNum;
		this.rooms = rooms;
	}
	
	public int getRoomNum() { return roomNum; }
	public NetworkRoom[] getRooms() { return rooms; }
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
	public static final int READY_FAIL = 4321;
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
	
	public static final String ip = "163.239.200.78";
	public static final int port = 7779;
	
	private static boolean READY_GAME_RES_FLAG = true;
	private static boolean EXIT_ROOM_RES_FLAG = true;
	private static boolean WAIT_GAME_START_RES_FLAG = true;
	private static boolean DROP_BALL_RES_FLAG = true;
	private static boolean WAIT_DROP_RES_FLAG = true;
	private static Socket sock = null;
	private boolean isConnected = false;
	
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
			reqDataOutputStream.flush();
			reqData = reqDataStream.toByteArray();
			reqDataOutputStream.close();
			reqStream.writeInt(reqData.length);
			reqStream.write(reqData);
			reqStream.flush();
			System.out.println("[Debug] (enterLobby) reqData.length : " + reqData.length + " *** reqData : " + new String(reqData));
			
			DataInputStream resStream = new DataInputStream(sock.getInputStream());
			int dataLen = resStream.readInt();
			int readLen = 0;
			int readSz = -9999;
			byte[] resData = new byte[dataLen];
			while(readLen < dataLen && (readSz=resStream.read(resData,readLen,dataLen-readLen)) != -1) {
				readLen += readSz;
			}
			if(readLen < dataLen) {
				sock.close();
				return INVALID_RES;
			}
			ObjectInputStream resDataInputStream = new ObjectInputStream(new ByteArrayInputStream(resData));
			System.out.println("[Debug] (enterLobby) ** " + "dataLen : " + dataLen
					+ " readLen : " + readLen + " readSz : " + readSz);
			int flag = resDataInputStream.readInt();
			
			System.out.println("[Debug] (enterLobby) " + "flag : " + flag);

			if(flag != PacketFlag.ENTER_LOBBY_RES) {
				// error handling
				resDataInputStream.close();
				sock.close();
				return INVALID_RES;
			} else {
				int res = resDataInputStream.readInt();
				System.out.println("[Debug] (enterLobby) "+ "res : " + res + " *** ");
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
			e.printStackTrace();
			System.exit(1);
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
			reqDataOutputStream.flush();
			reqData = reqDataStream.toByteArray();
			reqDataOutputStream.close();
			reqStream.writeInt(reqData.length);
			reqStream.write(reqData);
			reqStream.flush();
			System.out.println("[Debug] (getUserNum) reqData.length : " + reqData.length + " *** reqData : " + new String(reqData));
			
			DataInputStream resStream = new DataInputStream(sock.getInputStream());
			int dataLen = resStream.readInt();
			int readLen = 0;
			int readSz = -9999;
			byte[] resData = new byte[dataLen];
			while(readLen < dataLen && (readSz=resStream.read(resData,readLen,dataLen-readLen)) != -1) {
				readLen += readSz;
			}
			if(readLen < dataLen) {
				sock.close();
				return INVALID_RES;
			}
			ObjectInputStream resDataInputStream = new ObjectInputStream(new ByteArrayInputStream(resData));
			System.out.println("[Debug] (getUserNum) ** " + "dataLen : " + dataLen
					+ " readLen : " + readLen + " readSz : " + readSz);
			int flag = resDataInputStream.readInt();
			
			System.out.println("[Debug] (getUserNum) " + "flag : " + flag);
			if(flag != PacketFlag.GET_USERNUM_RES) {
				resDataInputStream.close();
				return INVALID_RES;
			} else {
				int User_Num = resDataInputStream.readInt();
				resDataInputStream.close();
				System.out.println("[Debug] (getUserNum) " + "User_Num : " + User_Num);
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
			reqDataOutputStream.flush();
			reqData = reqDataStream.toByteArray();
			reqDataOutputStream.close();
			reqStream.writeInt(reqData.length);
			reqStream.write(reqData);
			reqStream.flush();
			System.out.println("[Debug] (getRoomList) reqData.length : " + reqData.length + " *** reqData : " + new String(reqData));
			
			DataInputStream resStream = new DataInputStream(sock.getInputStream());
			int dataLen = resStream.readInt();
			int readLen = 0;
			int readSz = -9999;
			byte[] resData = new byte[dataLen];
			while(readLen < dataLen && (readSz=resStream.read(resData,readLen,dataLen-readLen)) != -1) {
				readLen += readSz;
			}
			if(readLen < dataLen) {
				sock.close();
				return new Pair <Integer, NetworkRoomList>( new Integer(INVALID_RES), null);
			}
			ObjectInputStream resDataInputStream = new ObjectInputStream(new ByteArrayInputStream(resData));
			System.out.println("[Debug] (getRoomList) ** " + "dataLen : " + dataLen
					+ " readLen : " + readLen + " readSz : " + readSz);
			int flag = resDataInputStream.readInt();
			
			System.out.println("[Debug] (getRoomList) " + "flag : " + flag);
			if(flag != PacketFlag.GET_ROOMLIST_RES) {
				resDataInputStream.close();
				return new Pair <Integer, NetworkRoomList>( new Integer(INVALID_RES), null);
			} else {
				NetworkRoomList roomdata = (NetworkRoomList) resDataInputStream.readObject();
				resDataInputStream.close();
				return new Pair <Integer, NetworkRoomList>( new Integer(GET_ROOM_LIST_OK), roomdata);
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
			return new Pair <Integer, NetworkRoomList>( new Integer(NETWORK_ERROR), null);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return new Pair <Integer, NetworkRoomList>( new Integer(NETWORK_ERROR), null);
	}
	
	public int makeRoom(String roomName, int gameMode){
		try{
			DataOutputStream reqStream = new DataOutputStream(sock.getOutputStream());
			byte[] reqData;
			ByteArrayOutputStream reqDataStream = new ByteArrayOutputStream();
			ObjectOutputStream reqDataOutputStream = new ObjectOutputStream(reqDataStream);
			reqDataOutputStream.writeInt(PacketFlag.MAKE_ROOM_REQ);
			reqDataOutputStream.writeObject(roomName);
			reqDataOutputStream.writeInt(gameMode);
			reqDataOutputStream.flush();
			reqData = reqDataStream.toByteArray();
			reqDataOutputStream.close();
			reqStream.writeInt(reqData.length);
			reqStream.write(reqData);
			reqStream.flush();
			System.out.println("[Debug] (makeRoom) reqData.length : " + reqData.length + " *** reqData : " + new String(reqData));
			
			DataInputStream resStream = new DataInputStream(sock.getInputStream());
			int dataLen = resStream.readInt();
			int readLen = 0;
			int readSz = -9999;
			byte[] resData = new byte[dataLen];
			while(readLen < dataLen && (readSz=resStream.read(resData,readLen,dataLen-readLen)) != -1) {
				readLen += readSz;
			}
			if(readLen < dataLen) {
				sock.close();
				return INVALID_RES;
			}
			ObjectInputStream resDataInputStream = new ObjectInputStream(new ByteArrayInputStream(resData));
			System.out.println("[Debug] (makeRoom) ** " + "dataLen : " + dataLen
					+ " readLen : " + readLen + " readSz : " + readSz);
			int flag = resDataInputStream.readInt();
			
			System.out.println("[Debug] (makeRoom) " + "flag : " + flag);
			if(flag != PacketFlag.MAKE_ROOM_RES) {
				// error handling
				resDataInputStream.close();
				//sock.close();
				return INVALID_RES;
			} else {
				resDataInputStream.close();
				return MAKE_ROOM_OK;
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
			reqDataOutputStream.writeInt(roomId);
			reqDataOutputStream.flush();
			reqData = reqDataStream.toByteArray();
			reqDataOutputStream.close();
			reqStream.writeInt(reqData.length);
			reqStream.write(reqData);
			reqStream.flush();
			System.out.println("[Debug] (enterRoom) reqData.length : " + reqData.length + " *** reqData : " + new String(reqData));
			
			DataInputStream resStream = new DataInputStream(sock.getInputStream());
			int dataLen = resStream.readInt();
			int readLen = 0;
			int readSz = -9999;
			byte[] resData = new byte[dataLen];
			while(readLen < dataLen && (readSz=resStream.read(resData,readLen,dataLen-readLen)) != -1) {
				readLen += readSz;
			}
			if(readLen < dataLen) {
				sock.close();
				return INVALID_RES;
			}
			ObjectInputStream resDataInputStream = new ObjectInputStream(new ByteArrayInputStream(resData));
			System.out.println("[Debug] (enterRoom) ** " + "dataLen : " + dataLen
					+ " readLen : " + readLen + " readSz : " + readSz);
			int flag = resDataInputStream.readInt();
			
			System.out.println("[Debug] (enterRoom) " + "flag : " + flag);
			if(flag != PacketFlag.ENTER_ROOM_RES) {
				// error handling
				resDataInputStream.close();
				//sock.close();
				return INVALID_RES;
			} else {
				int res = resDataInputStream.readInt();
				resDataInputStream.close();
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
			reqDataOutputStream.flush();
			reqData = reqDataStream.toByteArray();
			reqDataOutputStream.close();
			reqStream.writeInt(reqData.length);
			reqStream.write(reqData);
			reqStream.flush();
			System.out.println("[Debug] (waitUser) reqData.length : " + reqData.length + " *** reqData : " + new String(reqData));
			
			DataInputStream resStream = new DataInputStream(sock.getInputStream());
			int dataLen = resStream.readInt();
			int readLen = 0;
			int readSz = -9999;
			byte[] resData = new byte[dataLen];
			while(readLen < dataLen && (readSz=resStream.read(resData,readLen,dataLen-readLen)) != -1) {
				readLen += readSz;
			}
			if(readLen < dataLen) {
				sock.close();
				return new Pair <Integer, String>( new Integer(INVALID_RES), null);			
			}
			ObjectInputStream resDataInputStream = new ObjectInputStream(new ByteArrayInputStream(resData));
			System.out.println("[Debug] (waitUser) ** " + "dataLen : " + dataLen
					+ " readLen : " + readLen + " readSz : " + readSz);
			int flag = resDataInputStream.readInt();
			
			System.out.println("[Debug] (waitUser) " + "flag : " + flag);
			
			if(flag == PacketFlag.WAIT_USER_RES) {
				String username = (String) resDataInputStream.readObject();
				resDataInputStream.close();
				return new Pair <Integer, String>( new Integer(USER_ENTER), username);
			} else if(flag == PacketFlag.WAIT_USER_TIMEOVER_RES){
				resDataInputStream.close();
				return new Pair <Integer, String>( new Integer(TIME_OVER), null);
			} else {
				resDataInputStream.close();
				return new Pair <Integer, String>( new Integer(INVALID_RES), null);
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
			return new Pair <Integer, String>( new Integer(NETWORK_ERROR), null);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return new Pair <Integer, String>( new Integer(NETWORK_ERROR), null);
	}
	
	public int readyGame(){
		try{
			if(READY_GAME_RES_FLAG){
				DataOutputStream reqStream = new DataOutputStream(sock.getOutputStream());
				byte[] reqData;
				ByteArrayOutputStream reqDataStream = new ByteArrayOutputStream();
				ObjectOutputStream reqDataOutputStream = new ObjectOutputStream(reqDataStream);
				reqDataOutputStream.writeInt(PacketFlag.GAME_READY_REQ);
				reqDataOutputStream.flush();
				reqData = reqDataStream.toByteArray();
				reqDataOutputStream.close();
				reqStream.writeInt(reqData.length);
				reqStream.write(reqData);
				reqStream.flush();
				System.out.println("[Debug] (readyGame) reqData.length : " + reqData.length + " *** reqData : " + new String(reqData));
				READY_GAME_RES_FLAG = false;
			}
			
			DataInputStream resStream = new DataInputStream(sock.getInputStream());
			int dataLen = resStream.readInt();
			int readLen = 0;
			int readSz = -9999;
			byte[] resData = new byte[dataLen];
			while(readLen < dataLen && (readSz=resStream.read(resData,readLen,dataLen-readLen)) != -1) {
				readLen += readSz;
			}
			if(readLen < dataLen) {
				sock.close();
				return INVALID_RES;
			}
			ObjectInputStream resDataInputStream = new ObjectInputStream(new ByteArrayInputStream(resData));
			System.out.println("[Debug] (readyGame) ** " + "dataLen : " + dataLen
					+ " readLen : " + readLen + " readSz : " + readSz);
			int flag = resDataInputStream.readInt();
			
			System.out.println("[Debug] (readyGame) " + "flag : " + flag);
			if( flag == PacketFlag.GAME_READY_RES ) {
				resDataInputStream.close();
				READY_GAME_RES_FLAG = true;
				return READY_OK;
			} else if(flag == PacketFlag.GAME_READY_FAIL_RES) {
				resDataInputStream.close();
				return READY_FAIL;
			} else if(flag == PacketFlag.ENEMY_EXIT) {
				resDataInputStream.close();
				return ENEMY_EXIT;
			} else {
				resDataInputStream.close();
				return INVALID_RES;
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
			System.out.println("[Debug] (exitRoom) ----------------1");
			if(EXIT_ROOM_RES_FLAG){
				System.out.println("[Debug] (exitRoom) ----------------2");
				DataOutputStream reqStream = new DataOutputStream(sock.getOutputStream());
				byte[] reqData;
				ByteArrayOutputStream reqDataStream = new ByteArrayOutputStream();
				ObjectOutputStream reqDataOutputStream = new ObjectOutputStream(reqDataStream);
				reqDataOutputStream.writeInt(PacketFlag.EXIT_ROOM_REQ);
				
				reqDataOutputStream.flush();
				reqData = reqDataStream.toByteArray();
				reqDataOutputStream.close();
				reqStream.writeInt(reqData.length);
				reqStream.write(reqData);
				reqStream.flush();
				EXIT_ROOM_RES_FLAG = false;
				System.out.println("[Debug] (exitRoom) reqData.length : " + reqData.length + " *** reqData : " + new String(reqData));
			}
			System.out.println("[Debug] (exitRoom) ----------------3");
			DataInputStream resStream = new DataInputStream(sock.getInputStream());
			int dataLen = resStream.readInt();
			System.out.println("[Debug] (exitRoom) ----------------3.5");
			int readLen = 0;
			int readSz = -9999;
			byte[] resData = new byte[dataLen];
			System.out.println("[Debug] (exitRoom) ----------------4 ::: " + dataLen);
			while(readLen < dataLen && (readSz=resStream.read(resData,readLen,dataLen-readLen)) != -1) {
				readLen += readSz;
			}
			System.out.println("[Debug] (exitRoom) ----------------5");
			if(readLen < dataLen) {
				sock.close();
				return INVALID_RES;
			}
			ObjectInputStream resDataInputStream = new ObjectInputStream(new ByteArrayInputStream(resData));
			System.out.println("[Debug] (exitRoom) ** " + "dataLen : " + dataLen
					+ " readLen : " + readLen + " readSz : " + readSz);
			int flag = resDataInputStream.readInt();
			
			System.out.println("[Debug] (exitRoom) " + "flag : " + flag);
			if(flag == PacketFlag.EXIT_ROOM_RES) {
				resDataInputStream.close();
				EXIT_ROOM_RES_FLAG = true;
				return EXIT_ROOM_OKAY;
			} else if(flag == PacketFlag.ENEMY_EXIT) {
				resDataInputStream.close();
				return ENEMY_EXIT;
			} else {
				resDataInputStream.close();
				return INVALID_RES;
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
				reqDataOutputStream.flush();
				reqData = reqDataStream.toByteArray();
				reqDataOutputStream.close();
				reqStream.writeInt(reqData.length);
				reqStream.write(reqData);
				reqStream.flush();
				WAIT_GAME_START_RES_FLAG = false;
				System.out.println("[Debug] (waitGameStart) reqData.length : " + reqData.length + " *** reqData : " + new String(reqData));
			}
			DataInputStream resStream = new DataInputStream(sock.getInputStream());
			int dataLen = resStream.readInt();
			int readLen = 0;
			int readSz = -9999;
			byte[] resData = new byte[dataLen];
			while(readLen < dataLen && (readSz=resStream.read(resData,readLen,dataLen-readLen)) != -1) {
				readLen += readSz;
			}
			if(readLen < dataLen) {
				sock.close();
				return INVALID_RES;
			}
			ObjectInputStream resDataInputStream = new ObjectInputStream(new ByteArrayInputStream(resData));
			System.out.println("[Debug] (waitGameStart) ** " + "dataLen : " + dataLen
					+ " readLen : " + readLen + " readSz : " + readSz);
			int flag = resDataInputStream.readInt();
			
			System.out.println("[Debug] (waitGameStart) " + "flag : " + flag);
			if(flag == PacketFlag.WAIT_GAMESTART_RES) {
				resDataInputStream.close();
				WAIT_GAME_START_RES_FLAG = true;
				return GAME_START;
			} else if(flag == PacketFlag.WAIT_GAMESTART_TIMEOVER_RES) {
				resDataInputStream.close();
				WAIT_GAME_START_RES_FLAG = true;
				return TIME_OVER;
			} else if(flag == PacketFlag.ENEMY_EXIT) {
				resDataInputStream.close();
				return ENEMY_EXIT;
			} else {
				resDataInputStream.close();
				return INVALID_RES;
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
				reqDataOutputStream.writeInt(pos);
				reqDataOutputStream.flush();
				reqData = reqDataStream.toByteArray();
				reqDataOutputStream.close();
				reqStream.writeInt(reqData.length);
				reqStream.write(reqData);
				reqStream.flush();
				DROP_BALL_RES_FLAG = false;
				System.out.println("[Debug] (dropBall) reqData.length : " + reqData.length + " *** reqData : " + new String(reqData));
			}
			DataInputStream resStream = new DataInputStream(sock.getInputStream());
			int dataLen = resStream.readInt();
			int readLen = 0;
			int readSz = -9999;
			byte[] resData = new byte[dataLen];
			while(readLen < dataLen && (readSz=resStream.read(resData,readLen,dataLen-readLen)) != -1) {
				readLen += readSz;
			}
			if(readLen < dataLen) {
				sock.close();
				return INVALID_RES;
			}
			ObjectInputStream resDataInputStream = new ObjectInputStream(new ByteArrayInputStream(resData));
			System.out.println("[Debug] (dropBall) ** " + "dataLen : " + dataLen
					+ " readLen : " + readLen + " readSz : " + readSz);
			int flag = resDataInputStream.readInt();
			
			System.out.println("[Debug] (dropBall) " + "flag : " + flag);
			if(flag == PacketFlag.DROP_BALL_RES) {
				resDataInputStream.close();
				DROP_BALL_RES_FLAG = true;
				return DROP_BALL_OK;
			} else if(flag == PacketFlag.ENEMY_EXIT) {
				resDataInputStream.close();
				return ENEMY_EXIT;
			} else {
				resDataInputStream.close();
				return INVALID_RES;
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
			System.out.println("[Debug] (waitDrop) ----------------1");
			if(WAIT_DROP_RES_FLAG){
				System.out.println("[Debug] (waitDrop) ----------------2");
				DataOutputStream reqStream = new DataOutputStream(sock.getOutputStream());
				byte[] reqData;
				ByteArrayOutputStream reqDataStream = new ByteArrayOutputStream();
				ObjectOutputStream reqDataOutputStream = new ObjectOutputStream(reqDataStream);
				reqDataOutputStream.writeInt(PacketFlag.ENEMY_DROP_BALL_REQ);
				reqDataOutputStream.flush();
				reqData = reqDataStream.toByteArray();
				reqDataOutputStream.close();
				reqStream.writeInt(reqData.length);
				reqStream.write(reqData);
				reqStream.flush();
				WAIT_DROP_RES_FLAG = false;
				System.out.println("[Debug] (waitDrop) reqData.length : " + reqData.length + " *** reqData : " + new String(reqData));
			}
			System.out.println("[Debug] (waitDrop) ----------------3");
			DataInputStream resStream = new DataInputStream(sock.getInputStream());
			int dataLen = resStream.readInt();
			int readLen = 0;
			int readSz = -9999;
			byte[] resData = new byte[dataLen];
			while(readLen < dataLen && (readSz=resStream.read(resData,readLen,dataLen-readLen)) != -1) {
				readLen += readSz;
			}
			if(readLen < dataLen) {
				sock.close();
				return new Pair <Integer, Integer>( new Integer(INVALID_RES), null);
			}
			ObjectInputStream resDataInputStream = new ObjectInputStream(new ByteArrayInputStream(resData));
			System.out.println("[Debug] (waitDrop) ** " + "dataLen : " + dataLen
					+ " readLen : " + readLen + " readSz : " + readSz);
			int flag = resDataInputStream.readInt();
			
			System.out.println("[Debug] (waitDrop) " + "flag : " + flag);
			if(flag == PacketFlag.ENEMY_DROP_BALL_RES) {
				int positiondrop = resDataInputStream.readInt();
				resDataInputStream.close();
				WAIT_DROP_RES_FLAG = true;
				return new Pair <Integer, Integer>( new Integer(ENEMY_DROP), positiondrop);
			} else if(flag == PacketFlag.ENEMY_DROP_BALL_TIMEOVER_RES) {
				resDataInputStream.close();
				WAIT_DROP_RES_FLAG = true;
				return new Pair <Integer, Integer>( new Integer(TIME_OVER), null);
			} else if(flag == PacketFlag.ENEMY_EXIT) {
				resDataInputStream.close();
				return new Pair <Integer, Integer>( new Integer(ENEMY_EXIT), null);
			} else {
				resDataInputStream.close();
				return new Pair <Integer, Integer>( new Integer(INVALID_RES), null);
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
			return new Pair <Integer, Integer>( new Integer(NETWORK_ERROR), null);
		}
	}
}