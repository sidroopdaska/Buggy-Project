//-------------------------------------------------------------------------------
// App implements an applet, and contains the body of the program

import java.applet.Applet;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Event;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TooManyListenersException;

import javax.swing.JComboBox;
import javax.swing.JTextField;


public class App extends Applet {
	private static final long serialVersionUID = 100L;
	
	// App variables:
	//		move types
	public enum MoveType {forward, backward, right, left, targetF, targetNF, targetFR, targetNFR };
	
	//  	for GUI buttons and input boxes
	Button asciiButton, dtmfButton, codeEntryButton, example1Button, example2Button, connectButton, sendMoveButton, sendRouteButton;
	JComboBox portList, moveTypeList, moveNumList;
	JTextField codeEntry;
	
	// 		for the map objects
	Target[] targetArray = new Target[24];
	Node[][] nodes = new Node[14][10];
	Robot buggy = new Robot();
	int targetNum = 1, blockerNum = 0;

	//		for serial communication
	SerialHelper obj;
	SerialHelper listener;
	boolean connected = false, complete = false;
	String portArray[] = null;
	String[] resultsString = new String[30];
	String inputString = null;
	public String dtmf = null;
	
	
	//-------------------------------------------------------------------------------
	// brief init:
	//		fill the results array
	//		create instance of SerialHelper
	//		create buttons
	public void init() {
		
		// fill results array
		Arrays.fill(resultsString, "?");
		
		// setup serial communications
		obj = new SerialHelper(this);
		portArray = obj.listSerialPorts();

		// load buttons into window
		asciiButton = new Button("ASCII code");
		add(asciiButton);
		dtmfButton = new Button("DTMF code");
		add(dtmfButton);
		codeEntry = new JTextField(10);
		add(codeEntry, BorderLayout.SOUTH);
		codeEntryButton = new Button("Enter Code");
		add(codeEntryButton);
		example1Button = new Button("Example 1");
		add(example1Button);
		example2Button = new Button("Example 2");
		add(example2Button);
		portList = new JComboBox(portArray);
		add(portList);
		connectButton = new Button("Connect");
		add(connectButton);
		sendRouteButton = new Button("Send Route");
		add(sendRouteButton);
		moveTypeList = new JComboBox(MoveType.values());
		add(moveTypeList);
		String[] numbers = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "0" };
		moveNumList = new JComboBox(numbers);
		add(moveNumList);
		sendMoveButton = new Button("Send Move");
		add(sendMoveButton);

	}
	
	
	//-------------------------------------------------------------------------------
	// brief action:
	//		when the buttons are pressed in the window
	//		respond accordingly
	public boolean action(Event e, Object args) {
		
		if (e.target == asciiButton) {
			pathFinding(getASCII(codeEntry.getText() + ".txt"));

		} else if (e.target == dtmfButton) {
			System.out.println("dtmf : " + obj.dtmf );
			pathFinding(obj.dtmf);
			

		} else if (e.target == codeEntryButton) {
			pathFinding("*#*#*#*#" + codeEntry.getText() + "*#");

		} else if (e.target == example1Button) {
			pathFinding("*#*#*#*#C002B003B012D068B078B090C118*069*015*045*084*114*093*123*149*#");

		} else if (e.target == example2Button) {
			pathFinding("*#*#*#*#A057A064D026B001D132D100A137*003*033*045*117*060*114*075*149*#");

		} else if (e.target == connectButton) {
			connect((String) portList.getSelectedItem());

		} else if (e.target == sendMoveButton) {
			MoveType movetype = MoveType.valueOf(moveTypeList.getSelectedItem().toString());
			int num = Integer.parseInt(moveNumList.getSelectedItem().toString());
			Move move = new Move(movetype, num, 0);
			
			callAndResponse("rst", "ok");
			callAndResponse(move.moveString, "ok");
			callAndResponse("go", "go");

		} else if (e.target == sendRouteButton) {
			sendRoute();
			
		}
		return true;
	}


	//-------------------------------------------------------------------------------
	// brief reset:
	//		clears map data ready to start a new run
	public void reset() {
		Arrays.fill(resultsString, "?");
		
		for (int i = 0; i != 14; i++) {
			for (int j = 0; j != 10; j++) {
				nodes[i][j] = null;
				nodes[i][j] = new Node(i, j);
			}
		}
		
		for (int i = 0; i != blockerNum + targetNum; i++) {
			targetArray[i] = null;
		}
		
		buggy = null;
		buggy = new Robot();
		targetArray[0] = new Target(buggy.x, buggy.y, 6);
		targetNum = 1;
		blockerNum = 0;
		complete = false;
		
	}

	
	//-------------------------------------------------------------------------------
	// brief connect:
	//		connects to arduino on COM port
	//		if the correct response is received set connected to true
	public void connect(String portName) {
		
		obj.disconnect();
		try {
			obj.connect(portName);
			obj.addDataAvailableListener();
			obj.writeData("rst");	// rst string sent
			Thread.sleep(400);
		} catch (TooManyListenersException ex) {
			System.out.println("error 5");
			System.err.println(ex.getMessage());
		} catch (Exception e) {
			System.out.println("error 6");
			System.err.println(e.getMessage());
		}
		String reply = obj.receivedString.substring(0, obj.receivedString.length() - 2);
		if (reply.equals("ok")) {	// ok string received
			connected = true;
			repaint();
		}
		obj.flush();
	}
	
	
	//-------------------------------------------------------------------------------
	// brief callAndResponse:
	//		sends a string over the COM port, and waits for a predetermined response
	public void callAndResponse(String call, String response) {
		
		obj.flush();
		obj.writeData(call);
		String reply = "not";
		while (!reply.equals(response)){
			try {
				Thread.sleep(80);
			} catch (Exception e) {
			}
			if(obj.receivedString.length() > 2){
				reply = obj.receivedString.substring(0, obj.receivedString.length() - 2);
			}
		}
		obj.flush();
	}
	
	
	//-------------------------------------------------------------------------------
	// brief sendRoute:
	//		communicates the list of moves over the COM port to the arduino
	//		tells the arduino to start
	public void sendRoute() {
		
		complete = false;
		buggy.resetPosition();
		repaint();
		callAndResponse("rst", "ok");
		for (int i = 0; i != buggy.moveNum + 1; i++) {
			if (buggy.moves[i].num != 0){
				callAndResponse(buggy.moves[i].moveString, "ok");
			}
		}
		callAndResponse("go", "go");
	}
		
	
	//-------------------------------------------------------------------------------
	// brief getDTMF:
	//		request a path-finding code from the arduino over the COM port
	//		return this code
	public String getDTMF() {
		
		if (connected == true) {
			try {
				obj.writeData("dtmf");
				Thread.sleep(5000);
			} catch (Exception e) {
			}
			
			String reply = obj.receivedString.substring(0, obj.receivedString.length() - 2);
			obj.flush();
			
			if (reply.length() == 70) {
				return reply;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
		
	
	//-------------------------------------------------------------------------------
	// brief getASCII:
	//		read an ASCII string from file for use as a path-finding code
	//		filename is given by the text in the text input field
	//		return this code
	public String getASCII(String fileName){
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(fileName));
		    StringBuilder sb = new StringBuilder();
		    String line = br.readLine();
		    while (sb.length() != 70) {
		        sb.append(line);
		        line = br.readLine();
		    }
		    br.close();
		    System.out.println(sb.toString() + " received");
		    return sb.toString();
		} catch (IOException e){
			System.out.println("exception" + e);
			return null; 
		}
	}

	
	//-------------------------------------------------------------------------------
	// brief pathFinding:
	//		for a given code:
	//		create the arena using the other functions given below,
	//		save the moves corresponding to the optimal route
	//		around the arena
	public void pathFinding(String code) {
		
		reset();
		
		createTargets(code);
		
		for (int i = 0; i != 14; i++) {				// block nodes next to targets or blockers and reset 'G' values
			for (int j = 0; j != 10; j++) {
				nodes[i][j].blockFromTargets(targetArray, blockerNum + targetNum);
				nodes[i][j].resetGs();
			}
		}
		floodFill(0);								// calculate distances to the start node
		
		for (int i = 0; i != 14; i++) {				// block inaccessible nodes
			for (int j = 0; j != 10; j++) {
				nodes[i][j].blockFromGs();
			}
		}
		for (int i = 1; i != 8; i++){				// block/reverse the targets 
			if (targetArray[i].block(nodes) == true){
				blockerNum++;
				targetNum--;
			}
		}
		
		for (int i = 1; i != 8; i++) {				// calculate the distances between nodes
			floodFill(i);
		}
		
		findRoute();
	}
	
	
	//-------------------------------------------------------------------------------
	// brief createTargets:
	//		create the map from the code given
	//		including all targets and blockers
	public boolean createTargets(String code){
		
		if ((code.length() == 70) && (code.charAt(0) == '*')
				&& (code.charAt(1) == '#') && (code.charAt(2) == '*')
				&& (code.charAt(3) == '#') && (code.charAt(4) == '*')
				&& (code.charAt(5) == '#') && (code.charAt(6) == '*')
				&& (code.charAt(7) == '#') && (code.charAt(68) == '*')
				&& (code.charAt(69) == '#')) {

			int x, y, direction;

			for (int i = 0; i != 15; i++) {
				x = Integer.parseInt(code.substring(9 + 4 * i, 11 + 4 * i));
				y = Integer.parseInt(code.substring(11 + 4 * i, 12 + 4 * i));
				if ((x != 14) || (y != 9)) {
					switch (code.charAt(8 + 4 * i)) {
					case 'A':
						direction = 1;
						targetNum++;
						break;
					case 'B':
						direction = 2;
						targetNum++;
						break;
					case 'C':
						direction = 3;
						targetNum++;
						break;
					case 'D':
						direction = 4;
						targetNum++;
						break;
					case '*':
						direction = 5;
						blockerNum++;
						break;
					default:
						direction = 0;
						break;
					}
					// create targets and blockers
					targetArray[i + 1] = new Target(x, y, direction);
				}
			}
		}
		else return false;

		// search for blocker pairs, if pair is found create 2 new blockers between them
		int save = blockerNum + targetNum;
		for (int j = 8; j != save; j++) {
			if (targetArray[j].paired == false) {
				for (int i = 8; i != save; i++) {
					if ((targetArray[i].paired == false)
							&& (targetArray[i].y == targetArray[j].y)
							&& (targetArray[i].x == targetArray[j].x + 3)) {
						targetArray[i].paired = true;
						targetArray[j].paired = true;
						targetArray[blockerNum + targetNum] = new Target(targetArray[i].x - 1, targetArray[i].y, 5);
						blockerNum++;
						targetArray[blockerNum + targetNum] = new Target(targetArray[i].x - 2, targetArray[i].y, 5);
						blockerNum++;
						
					} else if ((targetArray[i].paired == false)
							&& (targetArray[i].x == targetArray[j].x)
							&& (targetArray[i].y == targetArray[j].y + 3)) {
						targetArray[i].paired = true;
						targetArray[j].paired = true;
						targetArray[blockerNum + targetNum] = new Target(targetArray[i].x, targetArray[i].y - 1, 5);
						blockerNum++;
						targetArray[blockerNum + targetNum] = new Target(targetArray[i].x, targetArray[i].y - 2, 5);
						blockerNum++;
					}
				}
			}
		}
		return true;
	}

	
	//-------------------------------------------------------------------------------
	// brief floodFill:
	//		calculate g values (distances from target) for each node
	//		save value is array position corresponding to run
	public void floodFill(int run){
		

		ArrayList<Node> openList = new ArrayList<Node>(); // lists for path
															// finding
		ArrayList<Node> closedList = new ArrayList<Node>();

		openList.add(nodes[targetArray[run].xApp][targetArray[run].yApp]);
		nodes[targetArray[run].xApp][targetArray[run].yApp].setG(run, 0, targetArray[run].dApp);

		while (openList.isEmpty() == false) {
				
			if ((openList.get(0).y < 9)
				&& (nodes[openList.get(0).x][openList.get(0).y + 1].blocked == false)
				&& (closedList.contains(nodes[openList.get(0).x][openList.get(0).y + 1]) == false)) {
					
				nodes[openList.get(0).x][openList.get(0).y + 1].setG(run, openList.get(0).g[run] + 1, 3);
				openList.add(nodes[openList.get(0).x][openList.get(0).y + 1]);
			}
			if ((openList.get(0).x < 13)
				&& (nodes[openList.get(0).x + 1][openList.get(0).y].blocked == false)
				&& (closedList.contains(nodes[openList.get(0).x + 1][openList.get(0).y]) == false)) {
				
				nodes[openList.get(0).x + 1][openList.get(0).y].setG(run, openList.get(0).g[run] + 1, 4);
				openList.add(nodes[openList.get(0).x + 1][openList.get(0).y]);
			}
			if ((openList.get(0).y > 0)
				&& (nodes[openList.get(0).x][openList.get(0).y - 1].blocked == false)
				&& (closedList.contains(nodes[openList.get(0).x][openList.get(0).y - 1]) == false)) {
				
				nodes[openList.get(0).x][openList.get(0).y - 1].setG(run, openList.get(0).g[run] + 1, 1);
				openList.add(nodes[openList.get(0).x][openList.get(0).y - 1]);
			}
			if ((openList.get(0).x > 0)
				&& (nodes[openList.get(0).x - 1][openList.get(0).y].blocked == false)
				&& (closedList.contains(nodes[openList.get(0).x - 1][openList.get(0).y]) == false)) {
				
				nodes[openList.get(0).x - 1][openList.get(0).y].setG(run, openList.get(0).g[run] + 1, 2);
				openList.add(nodes[openList.get(0).x - 1][openList.get(0).y]);
			}
			
			closedList.add(openList.get(0));
			openList.remove(0);	
		}
	}
	
	
	//-------------------------------------------------------------------------------
	// brief findRoute:
	//		find r[0] to r[7] such that no two equal the same number, or zero.
	//		compare lengths of different paths to find the shortest
	//		convert route into moves
	public void findRoute(){
		
		int shortestDist = 120, currentDist = 120;
		int[] shortestRoute = new int[8], r = new int[8];

		for (r[0] = 1; r[0] != 8; r[0]++) {

			if (targetArray[r[0]].blocked == false) {
				currentDist = nodes[targetArray[r[0]].xApp][targetArray[r[0]].yApp].g[0];
				for (r[1] = 1; r[1] != 8; r[1]++) {
					if ((r[1] != r[0]) && (targetArray[r[1]].blocked == false)) {
						currentDist = currentDist
								+ nodes[targetArray[r[1]].xApp][targetArray[r[1]].yApp].g[r[0]];
						for (r[2] = 1; r[2] != 8; r[2]++) {
							if ((r[2] != r[0]) && (r[2] != r[1])
									&& (targetArray[r[2]].blocked == false)) {
								currentDist = currentDist
										+ nodes[targetArray[r[2]].xApp][targetArray[r[2]].yApp].g[r[1]];
								if ((targetNum == 4)
										&& (shortestDist > currentDist)) {
									shortestDist = currentDist;
									for (int i = 0; i != targetNum - 1; i++) {
										shortestRoute[i] = r[i];
									}
								}
								for (r[3] = 1; r[3] != 8; r[3]++) {
									if ((targetNum > 4)
											&& (r[3] != r[0])
											&& (r[3] != r[1])
											&& (r[3] != r[2])
											&& (targetArray[r[3]].blocked == false)) {
										currentDist = currentDist
												+ nodes[targetArray[r[3]].xApp][targetArray[r[3]].yApp].g[r[2]];
										if ((targetNum == 5)
												&& (shortestDist > currentDist)) {
											shortestDist = currentDist;
											for (int i = 0; i != targetNum - 1; i++) {
												shortestRoute[i] = r[i];
											}
										}
										for (r[4] = 1; r[4] != 8; r[4]++) {
											if ((targetNum > 5)
													&& (r[4] != r[0])
													&& (r[4] != r[1])
													&& (r[4] != r[2])
													&& (r[4] != r[3])
													&& (targetArray[r[4]].blocked == false)) {
												currentDist = currentDist
														+ nodes[targetArray[r[4]].xApp][targetArray[r[4]].yApp].g[r[3]];
												if ((targetNum == 6)
														&& (shortestDist > currentDist)) {
													shortestDist = currentDist;
													for (int i = 0; i != targetNum - 1; i++) {
														shortestRoute[i] = r[i];
													}
												}
												for (r[5] = 1; r[5] != 8; r[5]++) {
													if ((targetNum > 6)
															&& (r[5] != r[0])
															&& (r[5] != r[1])
															&& (r[5] != r[2])
															&& (r[5] != r[3])
															&& (r[5] != r[4])
															&& (targetArray[r[5]].blocked == false)) {
														currentDist = currentDist
																+ nodes[targetArray[r[5]].xApp][targetArray[r[5]].yApp].g[r[4]];
														if ((targetNum == 7)
																&& (shortestDist > currentDist)) {
															shortestDist = currentDist;
															for (int i = 0; i != targetNum - 1; i++) {
																shortestRoute[i] = r[i];
															}
														}
														for (r[6] = 1; r[6] != 8; r[6]++) {
															if ((targetNum > 7)
																	&& (r[6] != r[0])
																	&& (r[6] != r[1])
																	&& (r[6] != r[2])
																	&& (r[6] != r[3])
																	&& (r[6] != r[4])
																	&& (r[6] != r[5])
																	&& (targetArray[r[6]].blocked == false)) {
																currentDist = currentDist
																		+ nodes[targetArray[r[6]].xApp][targetArray[r[6]].yApp].g[r[5]];
																if (shortestDist > currentDist) {
																	shortestDist = currentDist;
																	for (int i = 0; i != targetNum - 1; i++) {
																		shortestRoute[i] = r[i];
																	}
																}
																currentDist = currentDist
																		- nodes[targetArray[r[6]].xApp][targetArray[r[6]].yApp].g[r[5]];
															}
														}
														currentDist = currentDist
																- nodes[targetArray[r[5]].xApp][targetArray[r[5]].yApp].g[r[4]];
													}
												}
												currentDist = currentDist
														- nodes[targetArray[r[4]].xApp][targetArray[r[4]].yApp].g[r[3]];
											}
										}
										currentDist = currentDist
												- nodes[targetArray[r[3]].xApp][targetArray[r[3]].yApp].g[r[2]];
									}
								}
								currentDist = currentDist
										- nodes[targetArray[r[2]].xApp][targetArray[r[2]].yApp].g[r[1]];
							}
						}
						currentDist = currentDist
								- nodes[targetArray[r[1]].xApp][targetArray[r[1]].yApp].g[r[0]];
					}
				}
				currentDist = 0;
			}
		}
		
		// turn route into moves
		int currentD, currentX = 0, currentY = 8;

		for (int move = 0; move != targetNum - 1; move++) {
			while (nodes[currentX][currentY].g[shortestRoute[move]] != 0) {
				currentD = nodes[currentX][currentY].dir[shortestRoute[move]];

				// if carrying on in the current direction is the same distance
				// from the target as the planned route, continue in current
				// direction, to minimise turns
				int p = buggy.moves[buggy.moveNum].dir;
				if (currentD != p) {
					
					switch (buggy.moves[buggy.moveNum].dir) {
					case 1:
						if ((currentY < 9)
								&& (nodes[currentX][currentY + 1].g[shortestRoute[move]] == nodes[currentX][currentY].g[shortestRoute[move]] - 1)) {
							currentD = 1;
						}
						break;
					case 2:
						if ((currentX < 13)
								&& (nodes[currentX + 1][currentY].g[shortestRoute[move]] == nodes[currentX][currentY].g[shortestRoute[move]] - 1)) {
							currentD = 2;
						}
						break;
					case 3:
						if ((currentY > 0)
								&& (nodes[currentX][currentY - 1].g[shortestRoute[move]] == nodes[currentX][currentY].g[shortestRoute[move]] - 1)) {
							currentD = 3;
						}
						break;
					case 4:
						if ((currentX > 0)
								&& (nodes[currentX - 1][currentY].g[shortestRoute[move]] == nodes[currentX][currentY].g[shortestRoute[move]] - 1)) {
							currentD = 4;
						}
						break;
					}
				}
				
				buggy.move(currentD);

				switch (currentD) {
				case 1:
					currentY = currentY + 1;
					break;
				case 2:
					currentX = currentX + 1;
					break;
				case 3:
					currentY = currentY - 1;
					break;
				case 4:
					currentX = currentX - 1;
					break;
				}
			}
			
			buggy.interrogateTarget(targetArray[shortestRoute[move]].dApp,
					shortestRoute[move], targetArray[shortestRoute[move]].reversed);
		}
		
		buggy.victoryRoll();
		repaint();
		
	}
	
	
	//-------------------------------------------------------------------------------
	// brief paint:
	//		create the GUI
	public void paint(Graphics g) {

		Graphics2D g2 = (Graphics2D) g;
		g2.setStroke(new BasicStroke(8));
		g2.setColor(Color.black);

		// draw tiles
		int borderX = 35;
		int borderY = 30;
		for (int i = 0; i != 15; i++) {
			for (int j = 0; j != 11; j++) {
				if (((i + j) % 2) != 0) {
					g.fillRect(42 * i + borderX, 42 * j + borderY, 42, 42);
				}
			}
		}

		// draw targets
		for (int i = 1; i != blockerNum + targetNum; i++) {
			if (i < 8) {
				g2.setColor(Color.green.darker());
			} else {
				g2.setColor(Color.gray);
			}

			g.fillRect(42 * targetArray[i].x + borderX + 21, 42
					* (10 - targetArray[i].y) + borderY - 21, 42, 42);
			g2.setColor(Color.yellow);

			if (targetArray[i].blocked == false) {
				
				switch (targetArray[i].dir) {
				case 1:
					g.drawLine(42 * (targetArray[i].x + 1) + borderX, 42
							* (10 - targetArray[i].y) + borderY, 42
							* (targetArray[i].x + 1) + borderX, 42
							* (8 - targetArray[i].y) + borderY);
					break;
				case 2:
					g.drawLine(42 * (targetArray[i].x + 1) + borderX, 42
							* (10 - targetArray[i].y) + borderY, 42
							* (targetArray[i].x + 3) + borderX, 42
							* (10 - targetArray[i].y) + borderY);
					break;
				case 3:
					g.drawLine(42 * (targetArray[i].x + 1) + borderX, 42
							* (10 - targetArray[i].y) + borderY, 42
							* (targetArray[i].x + 1) + borderX, 42
							* (12 - targetArray[i].y) + borderY);
					break;
				case 4:
					g.drawLine(42 * (targetArray[i].x + 1) + borderX, 42
							* (10 - targetArray[i].y) + borderY, 42
							* (targetArray[i].x - 1) + borderX, 42
							* (10 - targetArray[i].y) + borderY);
					break;
				}
				
			} else {
				
				switch (targetArray[i].dir) {
				case 1:
					g.drawLine(42 * (targetArray[i].x + 1) + borderX, 42
							* (10 - targetArray[i].y) + borderY, 42
							* (targetArray[i].x + 1) + borderX, 42
							* (10 - targetArray[i].y) - 21 + borderY);
					break;
				case 2:
					g.drawLine(42 * (targetArray[i].x + 1) + borderX, 42
							* (10 - targetArray[i].y) + borderY, 42
							* (targetArray[i].x + 1) + 21 + borderX, 42
							* (10 - targetArray[i].y) + borderY);
					break;
				case 3:
					g.drawLine(42 * (targetArray[i].x + 1) + borderX, 42
							* (10 - targetArray[i].y) + borderY, 42
							* (targetArray[i].x + 1) + borderX, 42
							* (10 - targetArray[i].y) + 21 + borderY);
					break;
				case 4:
					g.drawLine(42 * (targetArray[i].x + 1) + borderX, 42
							* (10 - targetArray[i].y) + borderY, 42
							* (targetArray[i].x + 1) - 21 + borderX, 42
							* (10 - targetArray[i].y) + borderY);
					break;
				}
			}
		}

		
		// draw moves
		g2.setColor(Color.green);
		for (int move = 1; move != buggy.moveCounter; move++) {
			g.drawLine(42 * (buggy.newX[move - 1] + 1) + borderX, 42
					* (10 - buggy.newY[move - 1]) + borderY, 42
					* (buggy.newX[move] + 1) + borderX, 42
					* (10 - buggy.newY[move]) + borderY);
		}

		
		// draw buggy
		g2.setColor(Color.blue.darker());
		g.fillRect(42 * buggy.x + borderX + 21, 42 * (10 - buggy.y)
				+ borderY - 21, 42, 42);
		g2.setColor(Color.yellow);
		switch (buggy.dir) {
		case 1:
			g.drawLine(42 * (buggy.x + 1) + borderX, 42 * (10 - buggy.y)
					+ borderY, 42 * (buggy.x + 1) + borderX, 42
					* (10 - buggy.y) - 21 + borderY);
			break;
		case 2:
			g.drawLine(42 * (buggy.x + 1) + borderX, 42 * (10 - buggy.y)
					+ borderY, 42 * (buggy.x + 1) + 21 + borderX, 42
					* (10 - buggy.y) + borderY);
			break;
		case 3:
			g.drawLine(42 * (buggy.x + 1) + borderX, 42 * (10 - buggy.y)
					+ borderY, 42 * (buggy.x + 1) + borderX, 42
					* (10 - buggy.y) + 21 + borderY);
			break;
		case 4:
			g.drawLine(42 * (buggy.x + 1) + borderX, 42 * (10 - buggy.y)
					+ borderY, 42 * (buggy.x + 1) - 21 + borderX, 42
					* (10 - buggy.y) + borderY);
			break;
		}

		
		// write target information
		g2.setColor(Color.black);
		int x = 10; // starting x position
		int y = 515; // starting y position
		int z = 15; // line height
		int w = 120; // column width
		
		for (int i = 1; i != 8; i++) {
			
			y = 515;
			g.drawString("      " + Integer.toString(i), x, y);
			y = y + z;

			switch (i) {
			case 1:
				g.drawString("Voltage Regulator", x, y);
				break;
			case 2:
				g.drawString("Voltage Divider", x, y);
				break;
			case 3:
				g.drawString("Delta Resistors", x, y);
				break;
			case 4:
				g.drawString("Star Resistors", x, y);
				break;
			case 5:
				g.drawString("High Pass Filter", x, y);
				break;
			case 6:
				g.drawString("Low Pass Filter", x, y);
				break;
			case 7:
				g.drawString("Band Pass Filter", x, y);
				break;
			}
			y = y + z;

			if (targetArray[i] != null) {

				g.drawString("(" + Integer.toString(targetArray[i].x) + ","
						+ Integer.toString(targetArray[i].y) + ")", x, y);
				y = y + z;

				if (targetArray[i].blocked == true) {
					g2.setColor(Color.red);
					g.drawString("BLOCKED!!", x, y);
					g2.setColor(Color.black);
				} else {
					switch (i) {
					case 1:
						g.drawString(" V = " + resultsString[0] + " V", x, y);
						break;
					case 2:
						g.drawString("R1 = " + resultsString[5] + " kOhm", x, y);
						g.drawString("R2 = " + resultsString[7] + " kOhm", x, y + z);
						break;
					case 3:
						g.drawString("R2 = " + resultsString[9]  + " kOhm", x, y);
						g.drawString("R3 = " + resultsString[11]  + " kOhm", x, y + z);
						break;
					case 4:
						g.drawString("R1 = " + resultsString[13] + " kOhm", x, y);
						g.drawString("R3 = " + resultsString[15] + " kOhm", x, y + z);
						break;
					case 5:
						g.drawString("R1 = " + resultsString[16] + " KOhm", x, y);
						g.drawString("C1 = " + resultsString[17] + " nF", x, y + z);
						g.drawString("   F = " + resultsString[18] + " kHz", x, y + z + z);
						break;
					case 6:
						g.drawString("R1 = " + resultsString[20] + " kOhm", x, y);
						g.drawString("C1 = " + resultsString[21] + " nF", x, y + z);
						g.drawString("   F = " + resultsString[22] + " kHz", x, y + z + z);
						break;
					case 7:
						g.drawString("R1 = " + resultsString[24] + " Ohms", x, y);
						g.drawString("C1 = " + resultsString[25] + " nF", x, y + z);
						g.drawString("   F = " + resultsString[26] + " kHz", x, y + z + z);
						break;
					}
				}
			}
			x = x + w;
		}

		if (connected == true) {
			g2.setColor(Color.green);
			g.drawString("Connected", 700, 50);
		} else {
			g2.setColor(Color.red);
			g.drawString("Not Connected", 700, 50);
		}
		
		if (complete == true) {
			g2.setColor(Color.red);
			g.drawString("MAP COMPLETE!!", 700, 100);
		}
	}
	 
	
	//-------------------------------------------------------------------------------
	// brief stop:
	//		on exit:
	//		disconnect from serial
	public void stop() {
		
		obj.disconnect();
	}
}
