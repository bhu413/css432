#! /bin/bash
g++ -o retriever Code/retriever.cpp;
g++ -o HTML_Pages/server Code/server.cpp -pthread;
