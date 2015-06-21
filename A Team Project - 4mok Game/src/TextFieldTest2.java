
import java.awt.*;
import java.awt.event.*;

import javax.swing.BoxLayout;
 
public class TextFieldTest2 implements ActionListener, KeyListener
{
	public static int KEY;
	private CardLayout cardLayout;
	private CardLayout cardLayout1;
	private Panel cardPane; //카드들을 올려놓을 패널
	private Game game;
	private FourMok fm;
 
	String id;
	Label lid;
	Label lpwd;
	Label msg;
	TextField tfId;
	TextField tfPwd;
	TextField tfchat;
	TextArea tachat;
	Panel buttonPane;
	Panel pane03;
	Font f7;
 public TextFieldTest2() //카드들을 셋팅, 객체생성부터 색과, 다음 카드로 넘길 버튼을 지정
 {
  
  Frame frame = new Frame("4BINGGO");
  frame.setSize(800,800);
  cardPane=new Panel();
  Panel pane01 = new Panel();
  Panel pane02 = new Panel();
  pane03 = new Panel();
  Panel pane04 = new Panel();
  Panel pane05 = new Panel();
  Panel pane06 = new Panel();
  Panel pane07 = new Panel();
  Panel pane08 = new Panel();
  pane01.setBackground(Color.gray); 
  pane01.setSize(500, 300);
  pane02.setBackground(Color.BLACK);
  pane02.setSize(500,500);
  pane03.setBackground(Color.WHITE);
  pane03.setSize(400,500);
  pane04.setBackground(Color.DARK_GRAY);
  pane04.setSize(100,500);
  pane05.setBackground(Color.GRAY);
  pane05.setSize(100,300);
  pane06.setBackground(Color.GRAY);
  pane06.setSize(100,200);
  pane07.setBackground(Color.WHITE);
  pane07.setSize(400, 500);
  pane08.setBackground(Color.WHITE);
  pane08.setSize(400, 500);

//  pane06.setLayout(new BoxLayout(target, axis));
  pane04.setLayout(new BorderLayout());
  pane06.setLayout(new BorderLayout());
  
  
  lid = new Label("ID", Label.LEFT);
  lpwd = new Label ("password", Label.LEFT);
  msg = new Label();
  tfId = new TextField(10);
  tfPwd = new TextField(10);
  tfPwd.setEchoChar('*');
  tfchat = new TextField();
  tachat = new TextArea();
    
  
  Label jemok = new Label("4binggo");
  Font f1 = new Font("Serif",Font.PLAIN,100);
  jemok.setFont(f1);
//  jemok.setBounds(50, 200, 20, 300);
  pane01.add(jemok);
  
  cardLayout = new CardLayout();
  cardPane.setLayout(cardLayout);
  cardPane.add("1번 패널",pane01);
  cardPane.add("2번 패널",pane02);
  frame.add(cardPane);
  
  cardLayout1 = new CardLayout();
  pane03.setLayout(cardLayout1);
  pane03.add("3번 패널",pane07);
  pane03.add("4번 패널",pane08);
  
  
  pane02.setLayout(new BorderLayout());
  pane02.add(pane03,BorderLayout.CENTER);
 // pane02.add(pane07,BorderLayout.CENTER);
  pane02.add(pane04,BorderLayout.EAST);
//  Button bang = new Button("4binggo 입장");
//  Font f2 = new Font("Serif",Font.BOLD,150);
//  bang.setFont(f2);
//  pane03.add(bang);
  pane07.setLayout(new GridLayout(3,2));
  pane07.add(new Button("Room 1"));
  pane07.add(new Button("Room 2"));
  pane07.add(new Button("Room 3"));
  pane07.add(new Button("Room 4"));
  pane07.add(new Button("Room 5"));
  pane07.add(new Button("Room 6"));
  
  pane08.setLayout(new GridLayout(3,2));
  pane08.add(new Button("Room 7"));
  pane08.add(new Button("Room 8"));
  pane08.add(new Button("Room 9"));
  pane08.add(new Button("Room 10"));
  pane08.add(new Button("Room 11"));
  pane08.add(new Button("Room 12"));
  
//  pane04.setLayout(new FlowLayout(FlowLayout.LEFT));
  //up button
  Button up = new Button("UP");
  Font f3 = new Font("Serif",Font.PLAIN,50);
  up.setFont(f3);
  pane05.add(up);
  
  //down button
  Button down = new Button("DOWN");
  Font f4 = new Font("Serif",Font.PLAIN,50);
  down.setFont(f4);
  pane05.add(down);
  
  //방만들기 button
  Button start = new Button("방만들기");
  Font f5 = new Font("Serif",Font.PLAIN,50);
  start.setFont(f5);
  pane05.add(start);
  pane04.add(msg); 
  pane06.add(tfchat,"South");
  pane06.add(tachat,"North");
  
  
  //  Panel buttonPane = new Panel();
  buttonPane = new Panel();
  Button ok = new Button("OK");
  ok.addActionListener(this);
  up.addActionListener(this);
  down.addActionListener(this);
  start.addActionListener(this);
  
  //frame.setLayout(new BorderLayout());
 // frame.add(pane03,BorderLayout.CENTER);
 // frame.add(pane04,BorderLayout.EAST);
  
  buttonPane.add(lid);
  buttonPane.add(tfId);
  buttonPane.add(lpwd);
  buttonPane.add(tfPwd);
  buttonPane.add(ok);
  
  
//  buttonPane.add(next);
//  pane01.add(buttonPane, BorderLayout.SOUTH);
//   cardPane.add(buttonPane, BorderLayout.SOUTH);
  frame.add(buttonPane,BorderLayout.SOUTH);
 
  WindowDestroyer listener = new WindowDestroyer();  
  frame.addWindowListener(listener);
  
  frame.addKeyListener(this);
  tfchat.addActionListener(new ActionListener(){
   public void actionPerformed(ActionEvent e){
  
  tachat.append(id+" : "+tfchat.getText()+"\n");
  tfchat.setText("");
    tfchat.requestFocus();
   }
    });
  
  tachat.setEditable(false);
  tachat.setSize(100,50);
  tachat.setVisible(true);
  tfchat.setSize(100,10);
  tfchat.setVisible(true);
  tfchat.requestFocus();
  
//  pane04.add(pane05);
//  pane04.add(pane06);
 pane04.add(pane05,"North");
 pane04.add(pane06,"South");
  
  frame.setLocation(400,250);
  frame.setVisible(true);
 }
 
 public static void main(String args[])
 {
  new TextFieldTest2();
  }
 public void keyPressed(KeyEvent e) {
		KEY = e.getKeyCode();
		System.out.println("Sdf");
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
	}
 public void keyReleased(KeyEvent arg0) {}
	public void keyTyped(KeyEvent arg0) {}
 public void actionPerformed(ActionEvent ae)
 {
  id = tfId.getText(); 
  //f7 = new Font("Serif",Font.BOLD,20);
  //f7.setFont(f7);
  String password = tfPwd.getText();
  String cmd = ae.getActionCommand(); //새로 받은 액션 이벤트의 액션 커맨드를 받아온다.
  Font f6 = new Font("Serif",Font.ITALIC,70);
  msg.setFont(f6);
  msg.setText("로그인 : "+id); 
  //버튼의 액션커맨드는 버튼의 텍스트다.
  if(cmd.equals("방만들기")){
	  //cardLayout.next(cardPane);
	  fm = new FourMok("s");
	  fm.setVisible(true);
  }
  if(cmd.equals("OK"))
  {
      cardLayout.next(cardPane); //어디 pane까지 돌려야 하는지 지정, 이전 카드로 되돌리기
      buttonPane.setVisible(false);
     }
  if(cmd.equals("UP"))
  {
   cardLayout1.previous(pane03);
  }
  if(cmd.equals("DOWN"))
  {
   cardLayout1.next(pane03);
  }
  }
 }