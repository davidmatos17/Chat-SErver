# Chat-SErver

This is a project for a uni subject (Redes de Comunicacao), that implements a small chat bettween two terminals using localhost as a server.


The following commands are available to use the chat:

1. /nick user1
-> creates a user with the name "user1"

2. /join room1
-> if the room already exists, it joins you to the room, otherwise it creates the room with the name "room1" and joins the user 

3. /leave
-> used to leave the current room

4. /bye
-> disconnects from the server

5. /priv user2 message
-> sends a private message to user2



To test:

    1. Compile the ChatServer
    -> javac ChatServer.java

    2. Run the Server
    -> java ChatServer 8000

    3. Compile the ChatClient
    -> javac ChatClient.java

    4. Run the python file (it compiles and runs the ChatClient file creating two windows of the Chat we are supposed to use)
    -> python3 script.py



    1. and 2. on the same terminal
    open another terminal and then use 3. and 4.