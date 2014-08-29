//-------------------------------------------------------------------------------
// StartClass contains the main method to be called on program start

import java.awt.Color;

public class StartClass {
	
	
	//-------------------------------------------------------------------------------
	// brief main:
	//		create App instance
	//		open the window and set some of its properties
	public static void main(String[] args) {

	    App theApplet = new App();
	    theApplet.init();
	    theApplet.start();
	    
	    javax.swing.JFrame window = new javax.swing.JFrame("Group C");
	    window.setContentPane(theApplet);
	    window.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
	    window.pack();
	    window.setVisible(true);
	    window.setSize(1000, 700);
	    window.setBackground(Color.white);
	}
}
