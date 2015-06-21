import java.awt.*;
import java.awt.event.*;
 
class WindowDestroyer extends WindowAdapter
{
    public void windowClosing(WindowEvent e) 
    {
        System.exit(0);
    }
}
public class FourMok extends Frame implements KeyListener, ActionListener{
	public static int KEY;
	
	public static NetworkInterface nt;
	
	private int userNum;
	public static Room[] rooms;
	public static RoomList roomlists;
	private int roomCount ;
	
	public static CardLayout cld;		// cardlayout
	private Panel ENTER;		// 첫 화면
	private Panel E_top;		// 첫 화면 윗 부분
	private Panel E_bot;		// 첫 화면 아랫 부분
	private Label E_subject;	// 제목
	private TextField E_nic;	// nickname
	private Button E_b;			// 입장 
	private String login;		// 
	
	private Panel ROOM;			// room list 화면
	private CardLayout Roomcard;  // roomlist card
	private Panel List;			// room list
	private Button[] bt;
	private Panel listone;		// first page
	private Panel listtwo;		// second page
	private Panel info;			// up,down, make, information
	private Panel button;		// up,down,make
	private Button reset;
	private Button makeroom;	
	private Label nameLabel;
	private Panel roominfo;		// 방정보 몇명
	private Label infoLabel1;
	private Label infoLabel2;
	
	
	public static Panel GAME;			// room( game 진행 화면)
	
	private Game game;			// game 함수
	
	public FourMok(String str){
		super(str);
		roomCount = 0;
		nt = new NetworkMethod();
		cld = new CardLayout();
		bt = new Button[6];
		setLayout(cld);
		// 1 첫 화면
		ENTER = new Panel();
		ENTER.setBackground(Color.GRAY);
		ENTER.setLayout(new BorderLayout());
		//// 1-1 제목 
		E_top = new Panel();
		E_top.setBackground(Color.GRAY);
		E_subject = new Label("4Binggo!");
		E_subject.setFont(new Font("Serif",Font.ITALIC,100));
		E_top.add(E_subject);
		//// 1-2 닉네임
		E_bot = new Panel();
		E_nic = new TextField(10);
		E_b = new Button("입장");
		E_bot.add(E_nic );
		E_bot.add(E_b);
		////
		ENTER.add("Center", E_top);
		ENTER.add("South",E_bot);
		//1 end
		
		//2 Room
		ROOM = new Panel();
		ROOM.setLayout(new BorderLayout());
		////2-1 roomlist
		Roomcard = new CardLayout();
		List = new Panel();
		List.setLayout(Roomcard);
		listone = new Panel();
		listone.setLayout(new GridLayout(3,2));
		for(int i = 0 ; i < 6; i++){	//	6개
			listone.add(bt[i] = new Button("empty"));
		}
		List.add(listone);
		
		/*listtwo = new Panel();
		listtwo.setLayout(new GridLayout(3,2));
		listtwo.add(new Button());
		listtwo.add(new Button());
		listtwo.add(new Button());
		listtwo.add(new Button());
		listtwo.add(new Button());
		listtwo.add(new Button());
		List.add(listtwo);
		*/
		
		////2-2 up ,down,방만들기(info)
		info = new Panel();
		info.setLayout(new BorderLayout());
		info.setBackground(Color.DARK_GRAY);
		button = new Panel();
		button.setBackground(Color.LIGHT_GRAY);
		Font ft = new Font("Serif",Font.BOLD,50);
		reset = new Button("RESET");
		makeroom = new Button("MAKE");
		reset.setFont(ft);
		makeroom.setFont(ft);
		button.add(reset); button.add(makeroom);

		nameLabel = new Label();

		roominfo = new Panel();
		roominfo.setLayout(new BorderLayout());
		roominfo.setBackground(Color.LIGHT_GRAY);
		infoLabel1 = new Label("User number: ");
		infoLabel2 = new Label("Room number: ");
		infoLabel1.setFont(new Font("Serif", Font.BOLD, 30));
		infoLabel2.setFont(new Font("Serif", Font.BOLD, 30));
		roominfo.add("North",infoLabel1);
		roominfo.add("Center",infoLabel2);
		
		info.add("North",button);
		info.add(nameLabel);
		info.add("South",roominfo);
		////
		ROOM.add("Center", List);
		ROOM.add("East", info);
		
		//2 end
		
		//3 Game
		GAME = new Panel();
		GAME.setLayout(new GridLayout(1,1,0,0));
		//3 end
		
		// card 추가
		add(ENTER, "enter");
		add(ROOM , "room");
		add(GAME, "game");
		
		for(int i = 0 ; i < 6; i++){
			bt[i].addActionListener(this);
		}
		E_b.addActionListener(this);
		reset.addActionListener(this);
		makeroom.addActionListener(this);
		
		cld.show(this,"ENTER");
	}
	public void keyPressed(KeyEvent e) {
		KEY = e.getKeyCode();
		System.out.println(KEY);
		switch(KEY){
			case KeyEvent.VK_LEFT: //37
				game.repaint();
				break;
			case KeyEvent.VK_RIGHT: //39
				game.repaint();
				break;
			case KeyEvent.VK_SPACE: //32
				game.repaint();
				break;
		}
		/*if(game.state == game.ST_EXIT){
			GAME.remove(game);
			System.out.println("dddd");
		}*/
	}
	public void keyReleased(KeyEvent arg0) {}
	public void keyTyped(KeyEvent arg0) {}
	public void actionPerformed(ActionEvent e) {
		String get = e.getActionCommand();
		System.out.println(get);
		if( get.equals("입장")){
			int entercheck;	// 입장 
			entercheck = nt.enterLobby(E_nic.getText());
			System.out.println(entercheck);
			if( entercheck == nt.NICKNAME_OK){
				int getroomchk ;
				cld.next(this);
				login = E_nic.getText();
				nameLabel.setFont(new Font("Serif", Font.BOLD+Font.ITALIC,100));
				nameLabel.setText((login));
				button.requestFocus();
				userNum = nt.getUserNum();
				infoLabel1.setText("User number: "+Integer.toString(userNum));
				getroomchk = nt.getRoomList().first();
				if( getroomchk == nt.GET_ROOM_LIST_OK){	// roomlist
					roomlists = new RoomList(nt.getRoomList().second().getRoomNum(), nt.getRoomList().second().getRooms());
					newlist();
				}
				else if( getroomchk == nt.NETWORK_ERROR ){
					System.out.println("Net error");
				}
			}
			else if( entercheck == nt.NETWORK_ERROR)
				System.out.println("Net error");
			else if( entercheck == nt.NICKNAME_DUP){
				System.out.println("du");
			}
			else if( entercheck == nt.NICKNAME_INVALID ){
				System.out.println("invalid");
			}
		}
		else if( get.equals("RESET")){
			newlist();
		}
		else if( get.equals("MAKE")){
			int makechk;
			makechk = nt.makeRoom(Integer.toString(roomCount+1), NetworkRoom.ONE_WINS);
			if( makechk == nt.MAKE_ROOM_OK ){
				roomCount++;
				game = new Game(this);
				GAME.add(game);
				cld.next(this);
				GAME.setFocusable(true);
				GAME.requestFocus();
				GAME.addKeyListener(this);
			}
			else if( makechk == nt.NETWORK_ERROR ){
				System.out.println("Net error");
			}	
		}
		else{
			int enterRoomchk;
			enterRoomchk = nt.enterRoom(Integer.parseInt(get));
			if(enterRoomchk == nt.USER_ENTER){
				game = new Game(this);
				GAME.add(game);
				cld.next(this);
				GAME.setFocusable(true);
				GAME.requestFocus();
				GAME.addKeyListener(this);
			}
		}
	}
	public void newlist(){
		Font fff = new Font("Serif",Font.BOLD,30);
		for(int i = 0 ; i < roomlists.roomNum ; i++){
			bt[i].setFont(fff);
			bt[i].setLabel("roomId: " + Integer.toString(rooms[i].roomId));
		}
	}
	
	public static void main(String[] args) {
		FourMok f = new FourMok("4BINGGO");
		f.setSize(800, 800);
		WindowDestroyer listener = new WindowDestroyer();  
		f.addWindowListener(listener);
		f.setVisible(true);
	}
}

class Room{
	int roomId;
	String[] guests;
	boolean isOpened;
	Room(int roomId, String[] guests,boolean isOpened){
		this.roomId = roomId;
		this.guests = guests;
		this.isOpened = isOpened;
	}
}

class RoomList{
	int roomNum;	// room 갯수
	RoomList(int roomNum, /*FourMok.nt.*/NetworkRoom[] rooms){
		this.roomNum = roomNum;
		FourMok.rooms = new Room[rooms.length];
		for(int i = 0 ; i < rooms.length; i++){
			FourMok.rooms[i] = new Room(rooms[i].getRoomId(), rooms[i].getGuests(),rooms[i].getIsOpened());
			/*FourMok.rooms[i].roomId = rooms[i].getRoomId();
			FourMok.rooms[i].guests = rooms[i].getGuests();
			FourMok.rooms[i].isOpened = rooms[i].getIsOpened();*/
		}
	}
}