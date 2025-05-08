Resources:
https://github.com/OrionStarGIT/RobotSample

https://github.com/OrionStarGIT/RobotSample/releases/download/V10.1/robotservice_10.1.jar

https://doc.orionstar.com/en/knowledge-base-category/apk-development/

About:

Overview
This Android-powered application serves as a telepresence system that allows admins to remotely control robots and communicate with users through video calls. Key functionalities include:
•	Robot Control via WebSockets: The app receives movement commands via WebSockets and executes them on the robot.
•	Real-time Position Updates: The robot continuously sends its current coordinates to the WebSocket server.
•	WebRTC Video Calls: The app integrates with the Agora SDK for live video and audio communication.
•	Robot Navigation: Users can store and navigate to predefined positions.
•	Robot SN Management: The app stores the robot’s serial number (SN) and a distance threshold in SharedPreferences. (Used to determine how far a point can be and still be considered “close” to the robot)
________________________________________
Features
•	WebSocket Communication: Enables two-way real-time data exchange for commands and updates.
•	Video Call Integration: Uses Agora WebRTC SDK for video conferencing.
•	Secure Settings: A PIN-protected settings dialog prevents unauthorized changes to the robot’s configuration.
________________________________________
