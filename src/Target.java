//-------------------------------------------------------------------------------
// Target represents a target or blocker

public class Target{
	
	// Target variables:
	//		position, orientation and approach nodes co-ordinates; state of the target
	public int 	x, y, dir, dApp, xApp, yApp, xDock, yDock;
	public boolean paired = false, blocked = false, reversed = false;
	
	
	//-------------------------------------------------------------------------------
	// brief Target:
	//		set variables
	//		reverse the target if pointing outside the arena
	public Target(int X, int Y, int direction){
		
		x = X;
		y = Y;
		dir = direction;
		
		switch (direction){
		case 1: yApp = y + 2; xApp = x; dApp = 3; yDock = y + 1; xDock = x; break;
		case 2: xApp = x + 2; yApp = y; dApp = 4; xDock = x + 1; yDock = y; break;
		case 3: yApp = y - 2; xApp = x; dApp = 1; yDock = y - 1; xDock = x; break;
		case 4: xApp = x - 2; yApp = y; dApp = 2; xDock = x - 1; yDock = y; break;
		case 6: xApp = x; yApp = y; dApp = dir; break;
		}
		
		if(xApp < 0 || xApp > 13|| yApp < 0||yApp > 9 ){
			switchTarget();
		}
	}
	
	
	//-------------------------------------------------------------------------------
	// brief switchTarget:
	//		reverse the direction of the target by changing its variables
	//		only if this does not point it outside the arena
	public void switchTarget(){
		
		switch (dir){
		case 3:	
			if(y < 8){ yApp = y + 2; xApp = x; dApp = 3; yDock = y + 1; xDock = x; dir = 1; reversed = !reversed;
			} break;		
		case 4:	
			if(x < 12){xApp = x + 2; yApp = y; dApp = 4; xDock = x + 1; yDock = y; dir = 2; reversed = !reversed;
			} break;	
		case 1:		
			if(y > 1){yApp = y - 2; xApp = x; dApp = 1; yDock = y - 1; xDock = x; dir = 3; reversed = !reversed;
			} break;	
		case 2:		
			if(x > 1){xApp = x - 2; yApp = y; dApp = 2; xDock = x - 1; yDock = y; dir = 4; reversed = !reversed;
			}  break;
		}
	}
	
	
	//-------------------------------------------------------------------------------
	// brief block:
	//		from the array of nodes:
	//		block/reverse the target as necessary
	//		if blocked return true
	public boolean block(Node[][] nodes){
		
		// if approach node or dock node is blocked try reversing it
		if ((reversed == false)&&((nodes[xApp][yApp].blocked == true) || (nodes[xDock][yDock].blockedNum > 1))){
			switchTarget();
		}
		
		// if this has no effect turn it around the other way and mark it as blocked
		if ((nodes[xApp][yApp].blocked == true) || (nodes[xDock][yDock].blockedNum > 1)){
			switchTarget();
			blocked = true;
			return true;
		} else {
			return false;
		}
	}
}
