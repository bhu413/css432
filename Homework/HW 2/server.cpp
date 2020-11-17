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



void prepareResponseData(string &filePath , bool isGET , string &statusCode , string &fileContent){

    fileContent = "";
    if(isGET){
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
    }
    else {
        fileContent = "HTTP/1.1 400 Bad Request\r\n";
        statusCode = "400"; //*****Should this be BAD_REQUEST_RESPONSE?****
    }
}

void *handleGETRequest(){

}