/*
TJBB - Temesgen Habte, James Kim, Brandon Hu, Bryant Nguyen

This file is pretty much the same as udphw3.cpp except for the ServerEarlyRetrans method.

There is a random number generator to simulate percentages of dropped ack packets as required for case 4.

*/


#include "UdpSocket.h"
#include "Timer.h"

#define TIMEOUT 1500 //timeout threshold in usec

int clientStopWait( UdpSocket &sock, const int max, int message[] ) {
    Timer timer;
    int numRetransmitted = 0;

    int sequence = 0;
    while (sequence < max) {
        message[0] = sequence;

        //MSGSIZE from UdpSocket.h = 1460
        sock.sendTo((char *) message, MSGSIZE);
        cerr << message[0] << endl; 

        //start timer for ack after sending
        timer.start();

        //wait for recieve
        while (sock.pollRecvFrom() < 1) {
            //see if time has run out
            if (timer.lap() > TIMEOUT) {
                //go back to resend
                numRetransmitted++;
                sequence--;
                break;
            }
        }

        //if socket has data waiting
        if (sock.pollRecvFrom() > 0) {
            //recieve data
            sock.recvFrom((char *) message, MSGSIZE);
            
            //if not the correct ack retransmit
            if (message[0] != sequence) {
                numRetransmitted++;
                sequence--;
            }
        }
        
        sequence++;

    }
    return numRetransmitted;
}

void serverReliable( UdpSocket &sock, const int max, int message[] ) {
    for (int sequence = 0; sequence < max; sequence++) {
        while (sock.pollRecvFrom() < 0) {} //continuously check for data. Will exit out of loop when data is present
        
        //there has to be data at this point
        sock.recvFrom((char *)message, MSGSIZE);
        cerr << message[0] << endl;
        
        //see if sequence matches
        if (message[0] == sequence) {
            //if so, send ack
            sock.ackTo((char *) &sequence, sizeof(sequence));
        } else {
            //go back if wrong sequence
            sequence--;
        }
    }
}

int clientSlidingWindow( UdpSocket &sock, const int max, int message[], int windowSize ) {
    
    int numRetransmitted = 0;
    int ack = 0;
    int numUnack = 0;
    int sequence = 0;

    while (sequence < max) {
        if (numUnack < windowSize) {
            message[0] = sequence;
            sock.sendTo((char *) message, MSGSIZE);
            numUnack++;
        }

        //window has been filled
        if (numUnack == windowSize) {
            Timer timer;
            timer.start();
           
            while (true) {
                //try to see if there is data
                if (sock.pollRecvFrom() > 0) {
                    sock.recvFrom((char *) message, MSGSIZE);
                    if (message[0] == ack) {
                        ack++;
                        numUnack--;
                        break;
                    }
                }
                //if timeout, go back 
                else if (timer.lap() > TIMEOUT) {
                    //add number of retransmitted
                    numRetransmitted += sequence + windowSize - ack;
                    sequence = ack;
                    numUnack = 0;
                }
            }
        }
        sequence++;
    }
    return numRetransmitted;
}

void serverEarlyRetrans( UdpSocket &sock, const int max, int message[], int windowSize, int drops ) {
    
    int sequence = 0;
    while (sequence < max) {
        bool keeprunning = true;
        while (keeprunning) {
            if (sock.pollRecvFrom() > 0) {
                //set seed for randomness
                srand (time(NULL));
                //get random number between 0 and 100
                int percent = rand() % 100;
                if (percent <= drops) {
                    continue;
                }
                sock.recvFrom((char *) message, MSGSIZE);
                sock.ackTo((char *) &sequence, sizeof(sequence));
                //if it is the right packet, increment window
                if (message[0] == sequence) {
                    sequence++;
                    //since we can move on to the next sequence
                    keeprunning = false;
                }
            }
        }
    }
}