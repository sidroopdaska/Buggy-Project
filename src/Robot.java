//-------------------------------------------------------------------------------
// Robot represents the the buggy; creates the moves to be carried out

public class Robot {
	
	// Robot variables:
	//		position, orientation and approach nodes co-ordinates; state of the target
	public int dir = 3, x = 0, y = 8, newX[], newY[], newD[], moveCounter = 1,
			xPix, yPix, moveNum = 0;
	public Move[] moves;

	
	//-------------------------------------------------------------------------------
	// brief Robot:
	//		set variables
	//		create an initial, zero distance, move
	public Robot() {
		
		moves = new Move[100];
		newX = new int[60];
		newY = new int[60];
		newX[0] = x;
		newY[0] = y;
		moves[0] = new Move(App.MoveType.forward, 0, dir);
	}

	
	//-------------------------------------------------------------------------------
	// brief move:
	//		from the move direction and the current orientation:
	//		create a turn if necessary
	//		create a forward move, or update the previous one 
	public void move(int direction) {
		
		switch (direction) {
		case 1:
			
			switch (moves[moveNum].dir) {
			case 1:
				moves[moveNum].forward();
				break;
			case 2:
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.left, 1, direction);
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.forward, 1, direction);
				break;
			case 3:
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.right, 2, direction);
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.forward, 1, direction);
				break;
			case 4:
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.right, 1, direction);
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.forward, 1, direction);
				break;
			}
			break;
			
		case 2:
			
			switch (moves[moveNum].dir) {
			case 1:
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.right, 1, direction);
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.forward, 1, direction);
				break;
			case 2:
				moves[moveNum].forward();
				break;
			case 3:
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.left, 1, direction);
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.forward, 1, direction);
				break;
			case 4:
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.right, 2, direction);
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.forward, 1, direction);
				break;
			}
			break;
			
		case 3:
			
			switch (moves[moveNum].dir) {
			case 1:
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.right, 2, direction);
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.forward, 1, direction);
				break;
			case 2:
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.right, 1, direction);
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.forward, 1, direction);
				break;
			case 3:
				moves[moveNum].forward();
				break;
			case 4:
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.left, 1, direction);
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.forward, 1, direction);
				break;
			}
			break;
			
		case 4:
			
			switch (moves[moveNum].dir) {
			case 1:
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.left, 1, direction);
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.forward, 1, direction);
				break;
			case 2:
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.right, 2, direction);
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.forward, 1, direction);
				break;
			case 3:
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.right, 1, direction);
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.forward, 1, direction);
				break;
			case 4:
				moves[moveNum].forward();
				break;
			}
			break;
		}

		switch (direction) {
		case 1:
			newX[moveCounter] = newX[moveCounter - 1];
			newY[moveCounter] = newY[moveCounter - 1] + 1;
			break;
		case 2:
			newX[moveCounter] = newX[moveCounter - 1] + 1;
			newY[moveCounter] = newY[moveCounter - 1];
			break;
		case 3:
			newX[moveCounter] = newX[moveCounter - 1];
			newY[moveCounter] = newY[moveCounter - 1] - 1;
			break;
		case 4:
			newX[moveCounter] = newX[moveCounter - 1] - 1;
			newY[moveCounter] = newY[moveCounter - 1];
			break;
		}
		moveCounter++;

	}
	
	
	//-------------------------------------------------------------------------------
	// brief victoryRoll:
	//		from the current orientation and the last forward move:
	//		create moves to carry out the victory roll
	public void victoryRoll(){
		
		if (moves[moveNum - 1].type == App.MoveType.left){
			moveNum++;
			moves[moveNum] = new Move(App.MoveType.right, 1, 0);
		} else if (moves[moveNum - 1].type == App.MoveType.right){
			moveNum++;
			moves[moveNum] = new Move(App.MoveType.left, 1, 0);
		}
		
		moveNum++;
		moves[moveNum] = new Move(App.MoveType.left, 6, 0);
		moveNum++;
		moves[moveNum] = new Move(App.MoveType.forward, 1, 0);
		moveNum++;
		moves[moveNum] = new Move(App.MoveType.right, 2, 0);
		moveNum++;
		moves[moveNum] = new Move(App.MoveType.forward, 1, 0);
	}

	
	//-------------------------------------------------------------------------------
	// brief interrogateTarget:
	//		from the current orientation and orientation of the buggy
	//		create a turn move, if needed, and target move
	public void interrogateTarget(int direction, int targetNum, boolean reverse) {

		switch (direction) {
		
		case 1:
			
			switch (moves[moveNum].dir) {
			case 2:
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.left, 1, direction);
				break;
			case 3:
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.right, 2, direction);
				break;
			case 4:
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.right, 1, direction);
				break;
			}
			break;
			
		case 2:
			
			switch (moves[moveNum].dir) {
			case 1:
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.right, 1, direction);
				break;
			case 3:
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.left, 1, direction);
				break;
			case 4:
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.right, 2, direction);
				break;
			}
			break;
			
		case 3:
			
			switch (moves[moveNum].dir) {
			case 1:
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.right, 2, direction);
				break;
			case 2:
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.right, 1, direction);
				break;
			case 4:
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.left, 1, direction);
				break;
			}
			break;
			
		case 4:
			
			switch (moves[moveNum].dir) {
			case 1:
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.left, 1, direction);
				break;
			case 2:
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.right, 2, direction);
				break;
			case 3:
				moveNum++;
				moves[moveNum] = new Move(App.MoveType.right, 1, direction);
				break;
			}
			break;
		}
		
		moveNum++;
		if (reverse == true){
			moves[moveNum] = new Move(App.MoveType.targetFR, targetNum, direction);
		} else {
			moves[moveNum] = new Move(App.MoveType.targetF, targetNum, direction);
		}
	}


	//-------------------------------------------------------------------------------
	// brief resetPosition:
	//		reset the position and orientation of the buggy
	public void resetPosition(){
		
	dir = 3;
	x = 0;
	y = 8;
	}

}
