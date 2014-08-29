	//-------------------------------------------------------------------------------
// Serial Helper implements SerialPortEventListener, necessary for communicating over the serial port
// and contains other methods for serial communication


/*
 * Copyright (c) 2008, Daniel Widyanto
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY Daniel Widyanto ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL Daniel Widyanto BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.TooManyListenersException;
 

public class SerialHelper implements SerialPortEventListener  {
 
	// SerialHelper variables:
	private App main;						// pointer to the main App
    private SerialPort serialPort;
    private OutputStream outStream;
    private InputStream inStream;
    private byte[] readBuffer = new byte[400];
    final int TIMEOUT_VALUE = 1000;
    final int baudRate = 9600;
    public String receivedString = null;
    public String dtmf = null;
 
    
	//-------------------------------------------------------------------------------
	// brief SerialHelper:
	//		link serial helper to the main App
    public SerialHelper(App mainProgram){
    	
    	main = mainProgram;
    }
    
    
	//-------------------------------------------------------------------------------
	// brief listSerialPorts:
	//		check for serial ports
    //		return the ports as a list of strings
    public String[] listSerialPorts() {
    	
        Enumeration<CommPortIdentifier> ports = CommPortIdentifier.getPortIdentifiers();
        ArrayList<String> portList = new ArrayList<String>();
        String portArray[] = null;
        while (ports.hasMoreElements()) {
            CommPortIdentifier port = (CommPortIdentifier) ports.nextElement();
            if (port.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                portList.add(port.getName());
            }
        }
        portArray = (String[]) portList.toArray(new String[0]);
        return portArray;
    }


	//-------------------------------------------------------------------------------
	// brief connect:
	//		open the connection to the serial port given in the argument
    public void connect(String portName) throws IOException {
    	
        try {
            CommPortIdentifier portId = CommPortIdentifier.getPortIdentifier(portName);
            serialPort = (SerialPort) portId.open(this.getClass().getName(), 2000);
            serialPort.setSerialPortParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
            outStream = serialPort.getOutputStream();
            inStream = serialPort.getInputStream();
            System.out.println("Serial connected");
            Thread.sleep(2000);
            
        } catch (NoSuchPortException e) {
        	System.out.println("error 1");
            throw new IOException(e.getMessage());
        } catch (PortInUseException e) {
        	System.out.println("error 2");
            throw new IOException(e.getMessage());
        } catch (IOException e) {
        	System.out.println("error 3");
            serialPort.close();
            throw e;
        } catch (UnsupportedCommOperationException ex) {
        	System.out.println("error 4");
        	throw new IOException("Unsupported serial port parameter");
        } catch (InterruptedException ex) {
        	System.out.println("catch in readInput");
        }
    }
    
    
	//-------------------------------------------------------------------------------
	// brief adddataAvailableListener:
	//		sets up the data available interrupt for the connected port
    public void addDataAvailableListener() throws TooManyListenersException {
    	
        serialPort.addEventListener(this);
        serialPort.notifyOnDataAvailable(true);
        System.out.println("Serial listener added");
    }
    
    
	//-------------------------------------------------------------------------------
	// brief writedata:
	//		writes the 'data' over the connected serial port
    public void writeData(String data) {
    	
    	 System.out.println("Sent: " + data);
    	 try {
    	 outStream.write(data.getBytes());
    	 } catch (Exception e) {
    	 System.out.println("could not write to port");
    	 }
    }
 
    
	//-------------------------------------------------------------------------------
	// brief disconnect:
	//		closes the connected serial port
    public void disconnect() {
    	
        if (serialPort != null) {
            try {
                outStream.close();
                inStream.close();
            } catch (IOException ex) {
            }
            serialPort.close();
            serialPort = null;
        }
        try{
        	Thread.sleep(1000);
        } catch (Exception e){
        	
        }
    }
    
    
	//-------------------------------------------------------------------------------
	// brief flush:
	//		clears the read buffer
    public void flush(){
    	
    	receivedString = null;
    }


	//-------------------------------------------------------------------------------
	// brief serialEvent:
	//		on interrupt:
    //		while there is data available read it
    //		process message if needed
    public void serialEvent(SerialPortEvent events) {
    	
        if (events.getEventType() == SerialPortEvent.DATA_AVAILABLE){
        	
            try {
                int availableBytes = inStream.available();
                if (availableBytes > 0) {
                    inStream.read(readBuffer, 0, availableBytes);
                    
                    if (receivedString == null){
                    	receivedString = new String(readBuffer, 0, availableBytes);
                    } else{
                    	receivedString = receivedString + new String(readBuffer, 0, availableBytes);
                    }
                    
                    System.out.println("Buggy : " + receivedString );
                    
                    if (receivedString.length() == 73){
                    	dtmf = receivedString.substring(0, 70);
                    	System.out.println("dtmf : " + dtmf );
                    	main.dtmf = dtmf;	
                    	
                    } else if ((receivedString.length() > 8) &&(receivedString.charAt(receivedString.length() - 8) == '!') && (receivedString.charAt(receivedString.length() - 5) == '!')){
                    	String data;
                    	if ((receivedString.charAt(receivedString.length() - 13)) == '*'){
                    	
                    		data = receivedString.substring(receivedString.length() - 12, receivedString.length() - 8);
                    	} else {
                    		data = receivedString.substring(receivedString.length() - 13, receivedString.length() - 8);
                    	}
                    	int num = Integer.parseInt(receivedString.substring(receivedString.length() - 7, receivedString.length() - 5));
                    	main.resultsString[num] = data;
                    	main.repaint();
                    	this.flush();
                    	
                    } else if ((receivedString.charAt(receivedString.length() - 4) == '!') && (receivedString.charAt(receivedString.length() - 3) == '!'))
                    {
                    	flush();
                    	
                    } else if (receivedString.charAt(receivedString.length() - 3) == '&'){
                    	switch(receivedString.charAt(receivedString.length() - 4)){
                    	
                    		case 'f':
                    			
                    			switch (main.buggy.dir){
                    				case 1:
                    					main.buggy.y++;
                    					break;
                    				case 2:
                    					main.buggy.x++;
                    					break;
                    				case 3:
                    					main.buggy.y--;
                    					break;
                    				case 4:
                    					main.buggy.x--;
                    					break;
                    			}
                    			break;
                    			
                    		case 'r':
                    			
                    			switch (main.buggy.dir){
                				case 1:
                					main.buggy.dir = 2;
                					break;
                				case 2:
                					main.buggy.dir = 3;
                					break;
                				case 3:
                					main.buggy.dir = 4;
                					break;
                				case 4:
                					main.buggy.dir = 1;
                					break;
                    			}
                    			break;
                    			
                    		case 'l':
                    			
                    			switch (main.buggy.dir){
                				case 1:
                					main.buggy.dir = 4;
                					break;
                				case 2:
                					main.buggy.dir = 1;
                					break;
                				case 3:
                					main.buggy.dir = 2;
                					break;
                				case 4:
                					main.buggy.dir = 3;
                					break;
                    			}
                    			break;
                    			
                    		case 'c':
                    			main.complete = true;
                    			break;
                    			
                    	}
                    	flush();
                    	main.repaint();
                    }
                }
            } catch (IOException e) {
            }
        }
    } 
}
