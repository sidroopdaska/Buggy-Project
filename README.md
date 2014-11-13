Buggy-Project
=============

Third year group project

The aim of the Third Year Group Project was to design a mobile robot that could traverse around obstacles in a 
chequered arena and assess maximum number of targets at various locations in a timely and elegant manner. The locations 
for the targets were provided using DTMF tones that buggy used in conjunction with a route-finding algorithm to 
navigate itself in the arena. The targets contained electrical circuits that had to be electrically tested by the buggy. 
Data on the characteristics of each target was to be presented on a PC using GUI along with a real time update of the
location of buggy in the arena.

The Arduino firmware contains code for -
1)Motor control using automatic positive feedback from the sensors to control buggy movement 
2)Decoding the DTMF data on the coordinates of targets and obstacles into an ASCII string.
3)Using relays and obtaining inputs from numerous GPIO pins for determing electrical characteristcs of various targets on the chequered arena.

The Java code involves-
1)setting up a GUI to display the above computed information
2)communication with the arduino to receive decoded DTMF ASCII String and determining the optimal path using a route finding algorithm 
3)issuing commands to achieve the required movement to reach targets in minimalistic time whilst avoiding obstacles
