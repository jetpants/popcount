# popcount
Seven alternative algorithms for calculating a popcount (population count, i.e., counting the number of set bits in an integer).

There's tests to make sure they all give the same answer and a benchmark harness to compare their speeds:
```
  naive()                        46.1 ns
  naiveOptimised()               38.8 ns
  hammingWeight()                11.0 ns
  hammingWeightOptimised()       13.4 ns
  SWAR()                         13.1 ns
  java.lang.Integer.bitCount()   12.1 ns
  popcount_String()              75.3 ns
```  
Run the program to get the timings with:
```
  java -jar popcount.jar time
``` 
Test it against all integers from `Int.MIN_VALUE` to `Int.MAX_VALUE`:
```
  java -jar popcount.jar test
``` 
To re-compile:
```
  kotlinc -include-runtime popcount.kt -d popcount.jar
```
These algorithms are little works of art. Amazing to think that the Hamming Weight algorithm was invented by James W. L. Glaisher in goddamn 1899!
