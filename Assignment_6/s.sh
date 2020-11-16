rm *.class
sed -i 's/= false/= true/' ConnectedHandler.java
java Talk_2 9999 127.0.0.1:8888
sed -i 's/= true/= false/' ConnectedHandler.java
