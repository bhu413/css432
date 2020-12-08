/*
TJBB - Temesgen Habte, James Kim, Brandon Hu, Bryant Nguyen

HW3:
This assignment showcases differnt algorithms for sending data across a network.

Unreliable: The client just sends all data without checking for acks

Stop and wait: The client sends each packet and then waits for the ack before sending the next one.
This can also be seen as a sliding window algorithm with a window size of 1.

Sliding window: The client sends packets without receiving ack until the window fills up.
Then as the acks are received from the server, the window "slides" over and more packets are sent.
If a time-out occurs. We go back to the last received ack and start over from there.

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
                    cerr << message[0] << endl;
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

void serverEarlyRetrans( UdpSocket &sock, const int max, int message[], int windowSize ) {
    int sequence = 0;
    while (sequence < max) {
        bool keeprunning = true;
        while (keeprunning) {
            if (sock.pollRecvFrom() > 0) {
                sock.recvFrom((char *) message, MSGSIZE);
                cerr << message[0] << endl;
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