# Codeforces

## Introduction

Codeforces is a website that hosts competitive programming contests. The site presents several thousand programming and 
algorithm tasks. The main feature of Codeforces is that if the solution sent to the testing system does not pass some 
test from a set prepared by the authors of the problem, then this test will be not shown to the user. That is, you have 
to try to come up with the input data yourself, on which the solution gives a wrong answer (verdict "Wrong answer") or 
falls with an unhandled exception (verdict "Runtime error"). This is almost always very difficult, especially if the 
mistake is not obvious.

## Detecting the mistake with UnitTestBot

To find the needed test on which the solution falls, we suggest using UnitTestBot. Consider two cases separately:

- "Runtime error". In this case, you just need to run the test generation for a written solution, and, perhaps, 
  UnitTestBot will find the test case on which the program will fail.
- "Wrong answer". This case is more difficult. You can try to do one of the following (or all at once):
  - The mistake can be that during the program's execution, some invariants cease to be true, but they are important to
    your solution. Then it is worth writing `assert` instructions in those places where you can check these invariants. 
    For example, if you are sure that the result of the calculation of some function is always greater than zero, then 
    you can add the `assert result > 0;` before returning `result` as an answer. Then UnitTestBot will try to find a 
    test on which `assert` fails.
  - If the final answer can be easily verified for correctness, you can write the `assert` instructions again. For 
    example, if you are sure that the answer is a non-empty sorted `array`, then you need to write something like:
    ```java
    for (int i = 0; i < array.length - 1; ++i) {
        assert array[i] < array[i + 1];
    }
    ```
    Now UnitTestBot will again try to find a test where these conditions are not met.
  - If you have a slow, but the correct solution, then you can try to do something like:
    ```java
    var answer1 = fastButWrongSolve(input);
    var answer2 = slowButCorrectSolve(input);
    assert answer1 == answer2;
    ```

### Details of the UnitTestBot usage for this case

There are two reasons why you can't just run UnitTestBot on a solution and expect it will find a bug (it's not possible
now, maybe it is a future plan):

- You need to read the input data from `stdin` and write the answer to `stdout` in Codeforces tasks. This is a problem
  because UnitTestBot can generate tests for a specific function that has input parameters with specific types and a 
  specific return value.
- Quite strong restrictions are often imposed on the input data. For example, the number `n` that is in the input,
  always is in the range from 1 to 100. Another example is the input string is not any but consists only of small
  letters of the English alphabet.

Therefore, it is proposed to transform the solution to a "good" form before starting the test generation:

```java
public class Main {

    public static /* answer type */ solve(/* input parameters */) {
        assume(/* constraints from the problem as the boolean predicate */);
        // solve
        return answer;
    }

    public static void main(String[] args) {
        // read input
        var answer = solve(/* input */);
        // write output
    }
}
```

The `assume` method is located in the class `org.utbot.api.mock.UtMock`. 

Now you can start the test generation for the `solve` function.

### Example

Let's look at an example of the described transformation on the 
[solution](https://codeforces.com/contest/70/submission/25719914), which got the verdict "Runtime Error" in the 
Codeforces testing system.

It is said in the [problem](https://codeforces.com/contest/70/problem/A) that the input is an integer `n` in the range
from 0 up to 1000. And that the answer should be printed modulo 1e6 + 3, so the answer should be less than 1e6 + 3.

The transformed solution can look like:

```java
import java.util.Scanner;
import static org.utbot.api.mock.UtMock.assume;

public class Task_70A {

    static final int mod = (int) 1e6 + 3;

    public static int solve(int n) {
        assume(0 <= n && n <= 1000);                 // assumption

        int[] dp = new int[n];
        dp[0] = 1;
        for(int i = 1; i < n; ++i) {
            dp[i] = (3 * dp[i - 1]) % mod;
        }

        assert 0 <= dp[n - 1] && dp[n - 1] < mod;    // assertion
        return dp[n - 1];
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();                        // input
        int answer = solve(n);                       // solve
        System.out.println(answer);                  // output
    }
}
```

Now we can run the test generation for `solve` method.

**The result**: UnitTestBot managed to generate the test `solve(0)` that leads to 
`java.lang.ArrayIndexOutOfBoundsException` at the line `dp[0] = 1;`.

### Experiments

Experiments were conducted to understand whether the described approach can be used in practice. During the analysis,
several dozen incorrect solutions from Codeforces were selected (manually or automatically, using the 
[Codeforces API](https://codeforces.com/apiHelp )). The solutions were transformed to the "good" form manually, and 
after that, the test generation was run for them.

Consider the results for verdicts "Runtime error" and "Wrong answer" separately.

#### Testing solutions with Runtime error

We managed to find several examples when UnitTestBot can find an error in the solution. The full list of such solutions
located at the `./utbot-junit-contest/src/main/resources/projects/codeforces`. Now let's look at one interesting 
example in more detail.

[Original sumbit](https://codeforces.com/contest/1702/submission/167461682)

Transformed solution:

```java
import static org.utbot.api.mock.UtMock.assume;

import java.util.Scanner;

public class Expected_SIOBE {

    public static int solve(String s) {
        // ======== assumptions =========
        for (int i = 0; i < s.length(); ++i) {
            assume('a' <= s.charAt(i) && s.charAt(i) <= 'z');
        }

        // ========== solution ==========
        int counter = 0;
        int count = 0;
        while (count < s.length()) {
            counter++;
            char mem1 = s.charAt(count++);
            while (count < s.length() && s.charAt(count) == mem1) {
                count++;
            }
            if (count >= s.length()) {
                break;
            }
            char mem2 = s.charAt(count++);
            while (count < s.length() && (s.charAt(count) == mem1 || s.charAt(count) == mem2)) {
                count++;
            }
            char mem3 = s.charAt(count++);
            while (count < s.length() && (s.charAt(count) == mem1 || s.charAt(count) == mem2 || s.charAt(count) == mem3)) {
                count++;
            }
        }
        return counter;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int t = sc.nextInt();              // multiple tests
        while (t-- > 0) {
            String s = sc.next();          // read input
            System.out.println(solve(s));  // write output
        }
    }
}
```

**The result**: UniTestBot can detect an unexpected exception `java.lang.StringIndexOutOfBoundsException` 
at the line `char mem3 = s.charAt(count++);`. The mistake is not obvious, so UnitTestBot really helps.

#### Testing solutions with Wrong answer

For all solutions that were compared with the principle "slow, but correct" vs "fast, but wrong", UnitTestBot 
could not find the bug. Most likely, it is because the tool has poor performance. In other words, it is not
applicable in this case yet.

Anyway, let's look at the example of correct and wrong solutions. The example is quite simple, but UnitTestBot is 
failed to find a bug.

- [Original submit WA](https://codeforces.com/contest/1718/submission/168870501)
- [Original submit AC](https://codeforces.com/contest/1718/submission/168871045)

Transformed code:

```java
import static org.utbot.api.mock.UtMock.assume;

import java.util.*;

class Compare {

    public static void check(int n, int[] a) {
        assume(1 <= n && n <= 1e5);
        assume(a.length == n);
        for (int e : a) {
            assume(e >= 0);
        }
        assert wrongSolve(n, a) == correctSolve(n, a);
    }

    public static int wrongSolve(int n, int[] a) {
        int ans = 0, len = 0, xor = 0;
        for (int i = 0; i < n; i++) {
            xor ^= a[i];
            if (xor == 0 || a[i] == 0) {
                ans += len;
                len = xor = 0;
            } else {
                len++;
            }
        }
        ans += len;
        return ans;
    }

    public static int correctSolve(int n, int[] a) {
        int xor = 0, ans = 0;
        Set<Integer> set = new HashSet<>();
        set.add(0);
        for (int i = 0; i < n; i++) {
            xor ^= a[i];
            if (set.contains(xor)) {
                set.clear();
                ans++;
                xor = 0;
                set.add(0);
            } else {
                set.add(xor);
            }
        }
        ans = n - ans;
        return ans;
    }

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        int t = sc.nextInt();
        while (t-- > 0) {
            int n = sc.nextInt();
            int[] a = new int[n];
            for (int i = 0; i < n; i++) {
                a[i] = sc.nextInt();
            }
            System.out.println(wrongSolve(n, a));
        }
    }
}
```

**The result**: After the test generation for the method `check` we want to get a test case that would show 
the difference between the two solutions, but UnitTestBot can't generate it (even with a large time budget ~ 10 min).
