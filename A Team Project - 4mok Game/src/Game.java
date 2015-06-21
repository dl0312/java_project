import java.awt.*;
import java.awt.event.*;

public class Game extends Canvas{
	public static int ST_WAIT = -1;
	public static int ST_MAIN = 0;
	public static int ST_GAME = 1;
	public static int ST_ENDING = 2;
	public static int ST_EXIT = 3;

	
	public int state;			//game state

	private Frame f;
	
	private Image Space ;	// space image
	private Image Rball ;		// red ball image
	private Image Bball ;		// blue ball image
	private Image Victory;
	private Image Image_B;		// BEFORE image
	private Image Image_A;		// AFTER image
	private Image Image_M;		// AFTER image
	private Image Image_E;		// AFTER image

	private Graphics BEFORE;
	private Graphics AFTER;
	private Graphics MAIN;
	private Graphics ENDING;
	
	private static int ArrowX[];
	private static int ArrowY[];
	private static int Adx;
	
	private static int col;
	private static int row;
	private static int x[];
	private static int y[];
	private static char status[][];	// red or blue or null('C')
	private char turn;		// turn of(red or blue)
	public char	winner; 			// winner
	
	public Game(Frame fm,char t){
		f = fm;
		turn = t;
		Space = Toolkit.getDefaultToolkit().getImage("space.png");
		Rball = Toolkit.getDefaultToolkit().getImage("red.png");
		Bball = Toolkit.getDefaultToolkit().getImage("blue.png");
		Victory = Toolkit.getDefaultToolkit().getImage("victory.png");;
		state = ST_MAIN;
		Init();
	}
	public void Init(){
		x = new int[7]; y = new int[9];	status = new char[7][9];
		ArrowX = new int[3]; ArrowY = new int[3];
		col = 0; winner ='c';
		
		ArrowX[0] = 220; ArrowX[1] = 200; ArrowX[2] = 240;
		ArrowY[0] = 560; ArrowY[1] = 580; ArrowY[2] = 580;
		Adx = 0;
		
		for(int i = 0 ; i < 7; i++){
			x[i] = 100+ i*40;
			x[i] = 100+i*40;
			for(int j = 0 ; j < 9; j++){
				y[j] = 180 + j*40;
				status[i][j] = 'C';
			}
		}
	}
	public void paint(Graphics g){
		if(BEFORE == null || AFTER == null || MAIN == null || ENDING == null){
			Image_B = createImage(800, 800);
			Image_A = createImage(800, 800);
			Image_M = createImage(800, 800);
			Image_E = createImage(800, 800);
			if(Image_B == null || Image_A == null || Image_M == null || Image_E == null)
				System.out.println("error");
			else{
				BEFORE = Image_B.getGraphics();
				AFTER = Image_A.getGraphics();
				MAIN = Image_M.getGraphics();
				ENDING = Image_E.getGraphics();
			}
		}
		update(g);
	}
	public void update(Graphics g){
		keyReader();
		g.drawImage(Image_A,0,0,this);
	}
	public void Background(){
		BEFORE.drawImage(Space,0,0,this);
		((Graphics2D) BEFORE).setStroke(new BasicStroke(10));
		BEFORE.setColor(new Color(0,51,102));
		BEFORE.fillRect(95,95,290,450);
		BEFORE.setColor(Color.BLACK);
		BEFORE.fillRect(100, 100, 280, 440);
		BEFORE.setColor(new Color(0,51,102));
		BEFORE.drawLine(105, 160, 375, 160); // game Space
		
		BEFORE.setColor(new Color(0,255,255));
		BEFORE.setFont(new Font("돋움",Font.BOLD,50));
		BEFORE.drawString("READY", 130, 650);
		BEFORE.drawString("EXIT", 500, 650);
		
		ENDING.drawImage(Victory,0,0,this);
	}
	public void DrawArrow(){
		((Graphics2D) AFTER).setStroke(new BasicStroke(10));
		AFTER.setColor(new Color(0,255,255));
		AFTER.drawLine(ArrowX[0]+Adx, ArrowY[0], 220+Adx, 600);
		AFTER.drawLine(ArrowX[1]+Adx, ArrowY[1], 220+Adx, 600);
		AFTER.drawLine(ArrowX[2]+Adx, ArrowY[2], 220+Adx, 600);
	}
	private void keyReader()
	{	
		
		if( state == ST_MAIN){
			Background();
			AFTER.drawImage(Image_B, 0, 0, this);
			DrawArrow();
		}
		
		if(state == ST_GAME){
			if(turn == 'r'){
				AFTER.drawImage(Image_B,0,0,this);
				AFTER.drawImage(Rball, x[col], 100, 40,40,this);
				shadow(AFTER);
			}
			else{
				AFTER.drawImage(Image_B,0,0,this);
				AFTER.drawImage(Bball, x[col], 100, 40,40,this);
				shadow(AFTER);
			}
		}
		
		switch(FourMok.KEY)
		{
			case 37: // left
				FourMok.KEY = 0;
				if(state == ST_MAIN){
					Adx = 0;
					repaint();
				}
				if( state == ST_GAME){
					if(col > 0){					
						AFTER.drawImage(Image_B,0,0,this);
						if(turn == 'r')
							AFTER.drawImage(Rball, x[--col], 100, 40,40,this);
						else
							AFTER.drawImage(Bball, x[--col], 100, 40,40,this);
						shadow(AFTER);
					}
				}
				break;
			case 39: // right
				FourMok.KEY = 0;
				if(state == ST_MAIN){
					Adx = 330;
					repaint();
				}
				if( state == ST_GAME){
					if(col < 6){
						AFTER.drawImage(Image_B,0,0,this);
						if(turn == 'r')
							AFTER.drawImage(Rball, x[++col], 100, 40,40,this);
						else
							AFTER.drawImage(Bball, x[++col], 100, 40,40,this);
						shadow(AFTER);
					}
				}
				break;
			case 32: // space bar (drop Ball)
				FourMok.KEY = 0;
				if( state == ST_MAIN)
				{
					if( Adx == 0 ){
						int readychk = FourMok.nt.readyGame();
						int startchk;
						if( readychk == FourMok.nt.READY_OK){
							while(true){
								startchk = FourMok.nt.waitGameStart();
								if(startchk == FourMok.nt.GAME_START){
									state = ST_GAME;
									repaint();
									break;
								}
								else if(startchk == FourMok.nt.TIME_OVER){
									System.out.println("TIME OVER");
									break;
								}
								else if(startchk == FourMok.nt.NETWORK_ERROR){
									System.out.println("Net Error");
									FourMok.cld.previous(f);
									FourMok.cld.previous(f);
									break;
								}
								else if( startchk==FourMok.nt.INVALID_REQ || startchk==FourMok.nt.INVALID_RES){
									System.out.println("INVALID");
								}
							}
						}
						else if(readychk == FourMok.nt.NETWORK_ERROR){
							System.out.println("Net Error");
							FourMok.cld.previous(f);
							FourMok.cld.previous(f);
						}
						else if( readychk==FourMok.nt.INVALID_REQ || readychk==FourMok.nt.INVALID_RES){
							System.out.println("INVALID");
							FourMok.cld.previous(f);
							FourMok.cld.previous(f);
						}
					}
					else{
						int exitchk = FourMok.nt.exitRoom();
						if(exitchk == FourMok.nt.EXIT_ROOM_OKAY){
							FourMok.newlist();
							FourMok.cld.previous(f);
							FourMok.GAME.remove(this);
						}
						else if(exitchk == FourMok.nt.NETWORK_ERROR){
							System.out.println("Net Error");
							FourMok.cld.previous(f);
							FourMok.cld.previous(f);
						}
						else if( exitchk==FourMok.nt.INVALID_REQ || exitchk==FourMok.nt.INVALID_RES){
							System.out.println("INVALID");
							FourMok.cld.previous(f);
							FourMok.cld.previous(f);
						}
					}
				}
				else if( state == ST_ENDING){
					state = ST_MAIN;
					repaint();
				}
				else if( state == ST_GAME){
					if(status[col][0] != 'C') // full line
						return;
					int dropchk = FourMok.nt.dropBall(col);
					if( dropchk == FourMok.nt.DROP_BALL_OK){
						shadow(BEFORE);		// draw droped ball
						status[col][row] = turn;
						if(Victory() == true)
							return;
						col = 0;
						AFTER.drawImage(Image_B,0,0,this);
						if(turn == 'r'){			// red
							//turn = 'b';
							AFTER.drawImage(Bball,x[0], 100, 40,40,this);				
							shadow(AFTER);
						}
						else{				// blue
							//turn = 'r';				
							AFTER.drawImage(Rball, x[0], 100, 40,40,this);
							shadow(AFTER);
						}
						//////ENEMY_DROP
						Pair<Integer,Integer> d = FourMok.nt.waitDrop() ;
						int enemychk; //int enemydrop;
						while(true){
							enemychk = d.first();
							if(enemychk == FourMok.nt.ENEMY_DROP){
								col = d.second();
								break;
							}
						}
						shadow(BEFORE);
						status[col][row] = turn;
						if(Victory() == true)
							return;
						col = 0;
						AFTER.drawImage(Image_B,0,0,this);
						if(turn == 'r'){			// red
							//turn = 'b';
							AFTER.drawImage(Rball,x[0], 100, 40,40,this);				
							shadow(AFTER);
						}
						else{				// blue
							//turn = 'r';				
							AFTER.drawImage(Bball, x[0], 100, 40,40,this);
							shadow(AFTER);
						}
						
					}
					else if(dropchk == FourMok.nt.NETWORK_ERROR){
						System.out.println("net error");
					}
					
				}
				break;
		}
} 
	public boolean Victory(){
		int count = 0;
		int n_row; 
		int n_col;
		int check = 0;
		
		for(int i = 0 ; i < 7; i++){	// row check
			if(status[i][row] == turn)
				count++;
			else
				count = 0;
			
			if(count == 4 )
				check = 1;
		}
		
		count = 0 ;
		for(int i = 0 ; i < 9 ; i++){	// column check
			if(status[col][i] == turn)
				count++;
			else
				count = 0;
			if(count == 4 )
				check = 1;
		}
		
		count = 0; n_row = row; n_col = col;
		while( (n_row<8) && (n_col>0) ){	// 대각선 '/'모양 check  
			n_row++; n_col--;
		}
		while( (n_row >= 0) && (n_col<=6)){
			if( status[n_col++][n_row--] == turn)
				count++;
			else
				count = 0;
			if(count == 4)
				check = 1;
		}
		
		count = 0; n_row = row; n_col = col;
		while( (n_row<8) && (n_col<6) ){	// 대각선 '\' 모양 check
			n_row++; n_col++;
		}
		while( (n_row>=0) && (n_col>=0)){
			if( status[n_col--][n_row--] == turn)
				count++;
			else
				count = 0;
			if(count == 4)
				check = 1;
		}
		
		if( check == 1 ){
			ENDING.setFont(new Font("돋움",Font.BOLD,100));
			if(turn == 'r'){
				ENDING.setColor(Color.RED);
				ENDING.drawString("RED", 300, 530);
			}
			else{
				ENDING.setColor(Color.BLUE);
				ENDING.drawString("BLUE", 270, 530);
			}
			AFTER.drawImage(Image_E, 0, 0, this);
			state = ST_ENDING;
			Init();
			return true;
		}
		return false;
	}
	public void shadow(Graphics g){
		if(status[col][0] != 'C')
			return;
		for(int j = 0; j < 9 ; j++){
			if(status[col][j] == 'C')
				row = j;
		}
		if(turn == 'r')
			g.drawImage(Rball, x[col], y[row], 40,40,this);
		else
			g.drawImage(Bball, x[col], y[row], 40,40,this);
	}
}