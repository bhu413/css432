#include <string.h>
#include <unistd.h>
#include <stdio.h>
#include <netdb.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <arpa/inet.h>
#include <iostream>
#include <fstream>
#include <strings.h>
#include <stdlib.h>
#include <string>
#include <pthread.h>
using namespace std;

int makeGetRequest(int socket, string host, string file) {
    //GET request is ended with \r\n\r\n to signal end of request
    string request = "GET /" + file + " HTTP/1.1\nHOST: " + host + "\r\n\r\n";
    int sent = send(socket, request.c_str(), request.length(), 0);
    if (sent <= 0) {
        cout << "could not send GET request" << endl;
        return -1;
    }
    return 0;
}

string getHeader(int socket) {
    
    //header is separated from body by 2 crlf (carriage return line feed aka \r\n\r\n)
    string header = "";
    char current;

    //get first char and add to string
    recv(socket, &current, 1, 0);
    header += current;

    char next;
    
    while (header.find("\r\n\r\n") == string::npos) {
        recv(socket, &next, 1, 0);
        header += next;
        current = next;
    }

    return header;
}

//gets the content length from the header so we know how big to make buffer
int getContentLength(string header) {
    size_t index = header.find("Content-Length: ", 0);
    size_t newline = header.find("\r\n", index);
    return atoi(header.substr(index + 16, header.length() - newline).c_str());
}

//get status code from header to see if its ok or forbidden or not found
int getStatusCode(string header) {
    size_t spaceBefore = header.find("HTTP/1.1 ", 0);
    return atoi(header.substr(spaceBefore + 9, 3).c_str());
}

int main(int argc, char *argv[]) {
    if (argc < 2) {
        cout << "Invalid number of arguments" << endl;
        return -1;
    }

    //parsing argument
    string arg1 = string(argv[1]);
    int addressBegin = arg1.find(":", 0) + 3;
    int addressEnd = arg1.find("/", addressBegin);
    //extracts file name
    string file = arg1.substr(addressEnd + 1, arg1.length() - addressEnd);
    //extracts server address
    const char *serverIP = arg1.substr(addressBegin, addressEnd - addressBegin).c_str();
    
    int port = 80; // 80 for http protocol

    //option of adding a port. Not tested yet
    if (argc == 3) {
        port = atoi(argv[2]);
    }

    if (port != 80) { //check valid port number input
        if (port < 1024 || port > 65535) {
            cout << "port must be between 1024 and 65535" << endl;
        return -1;
        }
    }

    //finds the host either by domain name or ip
    struct hostent* host = gethostbyname(serverIP);
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

    //make the GET request
    makeGetRequest(clientSd, serverIP, file);

    //get the header from response
    string header = getHeader(clientSd);

    //get content length using the header
    int contentLength = getContentLength(header);

    //get status code from header
    int statusCode = getStatusCode(header);
    
    //make a buffer with content length
    char buffer[contentLength];
    //put everytning in the buffer
    recv(clientSd, &buffer, contentLength, 0);

    //output to file if everythinh ok
    //else display error code on screen and dont save file
    ofstream fileOutput;

    if (statusCode != 200) {
        cout << "Server returned error code " + to_string(statusCode) << endl << endl;
        for (int i = 0; i < contentLength; i++) {
            cout << buffer[i];
        }
    } else {
        fileOutput.open("output.html");
        for (int i = 0; i < contentLength; i++) {
            cout << buffer[i];
            fileOutput << buffer[i];
        }
    }
    
    fileOutput.close();
    close(clientSd); //Close socket
    return 0;
}
