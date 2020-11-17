#include <iostream>
#include <cstring>
#include <sys/socket.h>
#include <netdb.h>
#include <unistd.h>
#include <fstream>

using namespace std;

const string THREAD_MESSAGE = "New thread of count: ";
const int CONNECTION_REQUEST_SIZE = 10;
const string OK_RESPONSE = "HTTP/1.1 200 OK\r\n";
const string UNAUTHORIZED_RESPONSE = "HTTP/1.1 401 Unauthorized\r\n";
const string FORBIDDEN_RESPONSE = "HTTP/1.1 403 Forbidden\r\n";
const string SECRET_FILE = "SecretFile.html";
const int MAX_CONNECTIONS = 10;
int serverSd;
int newSd;


struct thread_data{
   int socket;
};

string prepareResponseData(string &filePath){

    string fileContent = "";
    string statusCode;
    int pathLength = filePath.length();
    if(pathLength >= 15){
        //handle attempts at accessing secret file by checking last 15 (length of file string)
        if(filePath.substr(pathLength - 15, pathLength) == SECRET_FILE){
            fileContent = "HTTP/1.1 401 Unauthorized\r\n";
            statusCode = "401"; //*****Should this be UNAUTHORIZED_RESPONSE?****
        }
    }
    else if(filePath.substr(0, 2) == ".."){
        //handles attempts at accessing files outside of current server directory
        fileContent = "HTTP/1.1 403 Forbidden\r\n";
        statusCode = "403"; //*****Should this be FORBIDDEN_RESPONSE?****
    }
    else{ //handles valid responses
        filePath.insert(0,".");
        FILE *file = fopen(filePath.c_str(), "r");
        if(file == nullptr){
            cout << "ERROR: File does not exist or permission denied" << endl;
            fileContent = "HTTP/1.1 404 Not Found\r\n";
            statusCode = "404"; //*****Should this be NOT_FOUND_RESPONSE?****
        }
        else{
            while (!feof(file)){
                char fileData = fgetc(file);

                if(fileData != '\n' && fileData != '\r' && fileData > 0){
                    fileContent += fileData;
                }

                if(fileData == '\n')
                    fileContent += '\n';

                if(fileData == '\r')
                    fileContent += '\r';

            }

            statusCode = "HTTP/1.1 200 OK\r\n";
            fclose(file);
        }
    }
    
    return fileContent;
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

string getFilePathFromHeader(string header) {
    int indexOfHTTP = header.find(" HTTP/1.1");
    return header.substr(4, header.length() - indexOfHTTP);
}

bool checkIfGET(string header) {
    if (header.substr(0, 3) == "GET") {
        return true;
    } else {
        return false;
    }
}

void *handleGETRequest(void* dataIn){
    struct thread_data *data;
    data = (struct thread_data *) dataIn;
    socket = data->socket;
    string header = getHeader(socket);
    string message = "";
    if (checkIfGET(header)) {
        message = prepareResponseData(getFilePathFromHeader(header));
    } else {
        message = "HTTP/1.1 400 Bad Request\r\n";
        //statusCode = "400"; //*****Should this be BAD_REQUEST_RESPONSE?****
    }
    int sent = send(socket, message.c_str(), message.length(), 0);
    if (sent <= 0) {
        cout << "could not send message" << endl;
        //return -1;
    } else {
        //return 0;
    }
}

int main(int argc, char *argv[]) {
    

    int port = 80;  // 80 for http default

    if (argc == 2) {
        port = stoi(argv[1]);
    }

    if (port != 80) { //check valid port number input
        if (port < 1024 || port > 65535) {
            cout << "port must be between 1024 and 65535" << endl;
        return -1;
        }
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
        thread_data.socket = newSd;
        pthread_create(&thread, NULL, handleGETRequest, (void*) &thread_data);
    }
    return 0;
}