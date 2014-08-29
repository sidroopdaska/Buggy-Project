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
1)motor control using automatic positive feedback from the sensors to control buggy movement 
2)to decode the DTMF data in the form of an ASCII string to be used to provide coordinates of the targets and 
obstacles for determining the optimal path.
3)code for using relays and digital data received from the numerous GPIO pins for determing electrical characteristcs
of various targets on the chequered arena.

The Java code involves setting up a GUI to display the above computed information, communication with the arduino to receive
decoded DTMF values, determining the optimal path using a route finding algorithm and issuing commands to achieve the 
required movement to reach targets in minimalistic time whilst avoiding obstacles
