#! /bin/bash
g++ -o retriever retriever.cpp;
g++ -o server server.cpp -pthread;
