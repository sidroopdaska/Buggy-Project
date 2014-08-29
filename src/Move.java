//-------------------------------------------------------------------------------
// Move represents a move to be sent to the arduino

public class Move {
	
	// Move variables:
	public App.MoveType type;			// type of move
	private char typeChar;				// char representing the move type
	public int num, dir;				// move 'number' and direction of move
	public String moveString = null;	// string to be sent representing the move
	
	
	//-------------------------------------------------------------------------------
	// brief Move:
	//		set move variables
	public Move(App.MoveType moveType, int moveNum, int direction){
		
		dir = direction;
		type = moveType;
		switch(moveType){
		case forward :
			typeChar = 'a';
			break;
		case backward :
			typeChar = 'b';
			break;
		case right :
			typeChar = 'c';
			break;
		case left :
			typeChar = 'd';
			break;
		case targetF :
			typeChar = 'e';
			break;
		case targetNF :
			typeChar = 'f';
			break;
		case targetFR:
			typeChar = 'g';
			break;
		case targetNFR:
			typeChar = 'h';
			break;
		}
		num = moveNum;
		updateString();
	}
	
	
	//-------------------------------------------------------------------------------
	// brief forward:
	//		if the move type is 'forward'
	//		increase the move number by one and update the string
	public void forward(){
		
		if (typeChar == 'a'){
			num++;
			updateString();
		}
	}

	
	//-------------------------------------------------------------------------------
	// brief updateString:
	//		updates the string representing the move with the current variables
	public void updateString(){
		
		moveString = (String.valueOf(typeChar) + String.valueOf(num) + "**");
	}
}
