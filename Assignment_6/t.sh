rm *.class
sed -i 's/= false/= true/' ConnectedHandler.java
javac Talk_2.java
java Talk_2 88888 127.0.0.1:9999
sed -i 's/= true/= false/' ConnectedHandler.java
