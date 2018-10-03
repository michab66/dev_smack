
rem TODO ensure ANT_HOME is set...

Rem clear path.
path ;
set JAVA_HOME=

set JDK_NAME=jdk1.6.0_45
set JAVA_HOME=c:\Progra~1\Java\%JDK_NAME%
call %ANT_HOME%\bin\ant.bat clean dist
