rm *.class
sed -i 's/= false/= true/' GroundLayer.java
javac Talk_2.java
java Talk_2 9999 127.0.0.1:8999
sed -i 's/= true/= false/' GroundLayer.java
