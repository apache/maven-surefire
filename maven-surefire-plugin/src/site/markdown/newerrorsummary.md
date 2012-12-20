The 1-line error summary
========================

Surefire 2.13 introduced a compact one-line format for quickly being able to locate test failures. This format
is intended to give an overview and does necessarily lose some details, which can be found in the main
report of the run or the files on disk.

### Example output:

    Failed tests:
      Test1#assertion1(59) Bending maths expected:<[123]> but was:<[312]>
      Test1#assertion2(64) True is false

    Tests in error:
      Test1#nullPointerInLibrary(38) >> NullPointerException
      Test1#failInNestedLibInMethod(54).nestedLibFailure(72) >> NullPointerException
      Test1#failInLibInMethod(48) >> NullPointerException
      Test1#failInMethod(43).innerFailure(68) NullPointerException Fail here
      Test2#test6281(33) RuntimeException FailHere

The format of the report is quite "packed", so some explanation is required, there are three different formats:


### Format 1, assertion failure.
#### Class#method(line number)...methodN(lineN) "Assertion failure message"

    Test1#assertion2(64) True is false
    Test1#assertion1(59) Bending maths expected:<[123]> but was:<[312]>

### Format 2, Exception in test.
#### Class#method(line number)...methodN(lineN) Exception "Message"
In this case the exception was actually thrown on the line in question.

    Test1#failInMethod(43).innerFailure(68) NullPointerException Fail here
    Test2#test6281(33) RuntimeException FailHere

### Format 3: Exception in code called by test.
#### Same format as 2 but >> added before exception
In this case the exception is thrown inside some code that was called from this line of the
test. We do not show where the actual exception happened, only which line(s) of the test
that were involved in the call.

    Test1#failInLibInMethod(48) >> NullPointerException
    Test1#failInNestedLibInMethod(54).nestedLibFailure(72) >> NullPointerException

