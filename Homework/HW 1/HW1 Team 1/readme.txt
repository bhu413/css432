1. Compile programs

client: g++ -o client client.cpp
server: g++ -o server server.cpp -lpthread

2. Run programs

client: ./client [port] [repetitions] [nbufs] [bufsize] [server ip] [transfer type]

server: ./server [port] [repetitions]

[port] 		a port number between 1024 and 65535
[repetitions] 	number of times to perform write operations
[nbufs]		number of buffers
[bufsize]	buffer size in bytes
[server ip]	the ip address of the running server i.e. "192.168.10.100"
[transfer type]	type of transfer to perform (1 = multiple write, 2 = writev, 3 = single write)

3. NOTES

Run the server FIRST and then the client second.

The server will keep running forever. To exit, press ctrl + c.

Repetitions should be the same for both the client and the server.