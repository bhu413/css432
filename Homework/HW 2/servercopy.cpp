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
    int pathLength = filePath.length();
    if(pathLength >= 15){
        //handle attempts at accessing secret file by checking last 15 (length of file string)
        if(filePath.substr(pathLength - 15, pathLength) == SECRET_FILE){
            fileContent = "HTTP/1.1 401 Unauthorized\r\nContent-Length: 0\r\n\r\n";
        }
    }
    else if(filePath.substr(0, 2) == ".."){
        //handles attempts at accessing files outside of current server directory
        fileContent = "HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\n\r\n";
    }
    else{ //handles valid responses
        filePath.insert(0,".");
        ifstream file (filePath);
        if (!file.is_open()) {
            cout << "ERROR: File does not exist or permission denied" << endl;
            fileContent = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n";
        }
        else{
            
            string line;
            string filestring = "";
            while (getline(file, line)){
                filestring += line;
            }
            file.close();
            fileContent = "HTTP/1.1 200 OK\r\nContent-Length: " + to_string(filestring.length()) + "\r\n\r\n";
            
            fileContent += filestring;
            
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
    string path;
    int indexOfHTTP = header.find(" HTTP/1.1");
    path = header.substr(4, indexOfHTTP - 4);
    return path;
}

bool checkIfGET(string header) {
    if (header.substr(0, 3) == "GET") {
        return true;
    } else {
        return false;
    }
}

void *handleGETRequest(void* dataIn){
    //get socket
    struct thread_data *data;
    data = (struct thread_data *) dataIn;

    //get request header
    string header = getHeader(data->socket);

    //get filepath from header
    string filepath = getFilePathFromHeader(header);

    string message = "";
    //check if it is GET request
    if (checkIfGET(header)) {
        //create message
        message = prepareResponseData(filepath);
    } else {
        message = "HTTP/1.1 400 Bad Request\r\nContent-Length: 0\r\n\r\n";
        //statusCode = "400"; //*****Should this be BAD_REQUEST_RESPONSE?****
    }

    //send message
    int sent = send(data->socket, message.c_str(), message.length(), 0);
    if (sent <= 0) {
        cout << "could not send message" << endl;
    }
    return 0;
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
        cout << "could not bind socket to ip address" << endl;
        return -1;
    }

    listen( serverSd, MAX_CONNECTIONS ); //listen up to 10 client requests at a time

    //looping forever to create multiple threads for multiple connections
    while(true){
        sockaddr_in newSockAddr;
        socklen_t newSockAddrSize = sizeof( newSockAddr );
        newSd = accept( serverSd, ( sockaddr *)&newSockAddr, &newSockAddrSize );
        pthread_t thread;
        struct thread_data data;
        data.socket = newSd;
        pthread_create(&thread, NULL, handleGETRequest, (void*) &data);
    }
    return 0;
}