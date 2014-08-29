//-------------------------------------------------------------------------------
// Node represents a points on the arena

public class Node {
	
	// Target variables:
	public int x, y;					// position of the node
	boolean blocked = false;			// true if the node is inaccessible
	public int blockedNum = 0;			// number of targets blocking the node
	public int[]  g, dir;				// the 'G' (distance) and direction arrays
										// from the start position and each of the targets
	
	//-------------------------------------------------------------------------------
	// brief Node:
	//		set variables
	public Node(int X, int Y){
	
		x = X;
		y = Y;
		g = new int[8];
		resetGs();
		dir = new int[8];
	}
	
	
	//-------------------------------------------------------------------------------
	// brief blockFromtargets:
	//		if the target and blocker locations make it inaccessible
	//	block the node
	public void blockFromTargets(Target[] targets, int targetNumber){
		
		for(int i = 1; i != targetNumber; i++){
			if (Math.abs(targets[i].x - x) + Math.abs(targets[i].y - y) < 2){
				blocked = true;
				blockedNum++;
			}
		}
	}
	
	
	//-------------------------------------------------------------------------------
	// brief blockFromGs:
	//		if the current G[0]  value is 100
	//	block the node
	public void blockFromGs(){
		if (g[0] == 100){
			blocked = true;
		}
	}

	
	//-------------------------------------------------------------------------------
	// brief setG:
	//		set the 'G' and direction value
	//		for the path-finding run given by runNumber
	public void setG(int runNumber, int G, int newDir){
		
		if(g[runNumber] > G){
			g[runNumber] = G;
			dir[runNumber] = newDir;
		}	
	}
	

	//-------------------------------------------------------------------------------
	// brief resetGs:
	//		set all the 'G' values to 100
	public void resetGs(){
		
		for (int i = 0; i != 8; i ++){
			g[i] = 100;
		}
	}
}



