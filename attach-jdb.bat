@echo off

REM Open monitor.bat and select the application to debug first.

set JAVA_HOME="c:\Program Files\Java\jdk1.7.0_25\bin"
%JAVA_HOME%\jdb -sourcepath .\src\main\java  -connect com.sun.jdi.SocketAttach:hostname=127.0.0.1,port=8700
