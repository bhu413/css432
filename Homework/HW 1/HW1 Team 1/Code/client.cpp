#include <string.h>
#include <unistd.h>
#include <stdio.h>
#include <netdb.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <arpa/inet.h>
#include <sys/uio.h> // writev
#include <sys/time.h> //gettimeofday
#include <iostream>
#include <fstream>
#include <strings.h>
#include <stdlib.h>
#include <string>
#include <pthread.h>
using namespace std;

int main(int argc, char *argv[]) {
    if (argc != 7) {
        cout << "Invalid number of arguments" << endl;
        return -1;
    }
    
    int port = atoi(argv[1]); // the last 4 digits of your student id
    int repetition = atoi(argv[2]);
    int nbufs = atoi(argv[3]);
    int bufsize = atoi(argv[4]);
    const char *serverIP = argv[5];
    int type = atoi(argv[6]);

    if (nbufs * bufsize != 1500) { //check valid nbuf*bufsize
        cout << "nbufs * bufsize must equal 1500" << endl;
        return -1;
    }

    if (port < 1024 || port > 65535) { //check valid port number input
        cout << "port must be between 1024 and 65535" << endl;
        return -1;
    }

    if (type < 1 || type > 3) { //check validity of type
        cout << "type must be 1, 2, or 3" << endl;
        return -1;
    }

    if (repetition < 0) { //check valid repition input
        cout << "repetition must be a positive number" << endl;
        return -1;
    }

    struct hostent* host = gethostbyname(serverIP); //takes in serverIP address as argument
    if (host == nullptr) {
        cout << "could not find host" << endl;
        return -1;
    }

    //creating socket
    sockaddr_in sendSockAddr;
    bzero( (char*)&sendSockAddr, sizeof( sendSockAddr ) );
    sendSockAddr.sin_family      = AF_INET; // Address Family Internet
    sendSockAddr.sin_addr.s_addr =
        inet_addr( inet_ntoa( *(struct in_addr*)*host->h_addr_list ) );
    sendSockAddr.sin_port        = htons( port );

    //try to open socket
    int clientSd = socket( AF_INET, SOCK_STREAM, 0 );
    if (clientSd < 0) {
        cout << "could not open socket" << endl;
        return -1;
    }

    //try to connect to server
    int connected = connect( clientSd, ( sockaddr* )&sendSockAddr, sizeof( sendSockAddr ) );
    if (connected < 0) { //if failed
        cout << "connection failed" << endl;
        close(clientSd); //close socket
        return -1;
    }

    char databuf[nbufs][bufsize]; // where nbufs * bufsize = 1500

    //set up time tracking
    struct timeval start;
    struct timeval end;
    struct timeval lap;

    gettimeofday(&start, nullptr); //start timer

    //writing data with repetition
    for (int i = 0; i < repetition; i++) {
        if (type == 1) { //multiple writes
            for ( int j = 0; j < nbufs; j++ )
            write( clientSd, databuf[j], bufsize );

        } else if (type == 2) { // vritev
            struct iovec vector[nbufs];
            for ( int j = 0; j < nbufs; j++ ) {
                vector[j].iov_base = databuf[j];
                vector[j].iov_len = bufsize;
            }
            writev( clientSd, vector, nbufs );

        } else if (type == 3) { // single write
            write( clientSd, databuf, nbufs * bufsize ); // sd: socket descriptor
        }
    }

    gettimeofday(&lap, nullptr); //lap timer

    int reads;
    read(clientSd, &reads, sizeof(reads));

    gettimeofday(&end, nullptr); //stop timer

    long sendTime = ((lap.tv_sec - start.tv_sec) * 1000000) + (lap.tv_usec - start.tv_usec); //Calculate data-sending time
    long roundTrip = ((end.tv_sec - start.tv_sec) * 1000000) + (end.tv_usec - start.tv_usec); //Calculate RTT
    
    //Print stats
    cout << "Test " << type << ": ";
    cout << "data-sending time = " << sendTime << " usec, ";
    cout << "round-trip time = " << roundTrip << " usec, ";
    cout << "#reads = " << reads << endl;

    printf("%s\n",databuf ); 
    close(clientSd); //Close socket
    return 0;
}
