#! /bin/bash

printf "\ntesting access to real server\n";
./retriever http://google.com/index.html;

printf "\n\ntesting accessing file from own server\n";
./retriever http://localhost/test.html 8080;

printf "\n\ntesting request unauthorized file\n";
./retriever http://localhost/SecretFile.html 8080;

printf "\n\ntesting request forbidden file\n";
./retriever http://localhost/.. 8080;

printf "\n\ntesting requesting non-existent file\n";
./retriever http://localhost/nothing.html 8080 ;

printf "\n\ntesting malformed request\n";
./retriever http://localhost/ 8080;


