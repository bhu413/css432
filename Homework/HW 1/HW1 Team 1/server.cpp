#include <string.h>
#include <unistd.h>
#include <stdio.h>
#include <netdb.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netinet/tcp.h> 
#include <fcntl.h>
#include <signal.h>
#include <sys/time.h> //gettimeofday
#include <iostream>
#include <fstream>
#include <stdlib.h>
#include <pthread.h>
using namespace std;

const int MAX_CONNECTIONS = 10;
const int BUFSIZE = 1500;
int repetition;
int serverSd;
int newSd;


/**
*   getStats
*This method gets statistics for the RTT and data-sending time
*
*@param *data
*@retun void
*/
void *getStats(void *data) {
    char databuf[BUFSIZE];

    struct timeval start;
    struct timeval end;

    gettimeofday(&start, nullptr); //start timer

    int count = 0; 
    for(int i = 0; i < repetition; i++){
        //Repeat reading data from client
        for(int nRead = 0;(nRead += read(newSd, databuf, BUFSIZE - nRead)) < BUFSIZE; ++count);
    }
    
    gettimeofday(&end, nullptr); //end timer
    write(newSd, &count, sizeof(count));

    long roundTrip = ((end.tv_sec - start.tv_sec) * 1000000) + (end.tv_usec - start.tv_usec);
    cout << "datareceiving time = " << roundTrip << " usec" << endl;
    
    close(newSd);
    return 0;
}

int main(int argc, char *argv[]) {
    if (argc != 3) {
        cout << "Invalid number of arguments" << endl;
        return -1;
    }

    int port = stoi(argv[1]);  // the last 4 digits of your student id
    repetition = stoi(argv[2]);

    if (port < 1024 || port > 65535) { //Makes sure that the port is within range
        cout << "port must be between 1024 and 65535" << endl;
        return -1;
    }

    if (repetition < 0) { //Ensure that repition input is valid
        cout << "repetition must be a positive number" << endl;
        return -1;
    }

    sockaddr_in acceptSockAddr;
    bzero( (char*)&acceptSockAddr, sizeof( acceptSockAddr ) );
    acceptSockAddr.sin_family      = AF_INET; // Address Family Internet
    acceptSockAddr.sin_addr.s_addr = htonl( INADDR_ANY );
    acceptSockAddr.sin_port        = htons( port );

    serverSd = socket( AF_INET, SOCK_STREAM, 0 ); //open socket with Address Family Internet
    
    const int on = 1;
    setsockopt( serverSd, SOL_SOCKET, SO_REUSEADDR, (char *)&on, 
                sizeof( int ) );

    //bind socket to its local address
    int bound = bind( serverSd, ( sockaddr* )&acceptSockAddr, sizeof( acceptSockAddr ) );
    if (bound < 0) {
        cout << "counld not bind socket to ip address" << endl;
        return -1;
    }

    listen( serverSd, MAX_CONNECTIONS ); //listen up to 10 client requests at a time

    //looping forever to create multiple threads for multiple connections
    while(true){
        sockaddr_in newSockAddr;
        socklen_t newSockAddrSize = sizeof( newSockAddr );
        newSd = accept( serverSd, ( sockaddr *)&newSockAddr, &newSockAddrSize );
        
        pthread_t thread;
        pthread_create(&thread, NULL, getStats, (void*) &newSd);
    }
    return 0;
}

