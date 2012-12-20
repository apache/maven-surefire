The 1-line error summary
========================

Surefire 2.13 introduced a compact one-line format for quickly being able to locate test failures. This format
is intended to give an overview and does necessarily lose some details, which can be found in the main
report of the run or the files on disk.

### Example output:

    Failed tests:
      Test1.assertion1:59 Bending maths expected:<[123]> but was:<[312]>
      Test1.assertion2:64 True is false

    Tests in error:
      Test1.nullPointerInLibrary:38 » NullPointer
      Test1.failInMethod:43->innerFailure:68 NullPointer Fail here
      Test1.failInLibInMethod:48 » NullPointer
      Test1.failInNestedLibInMethod:54->nestedLibFailure:72 » NullPointer
      Test2.test6281:33 Runtime FailHere

The main rules of the format are:
 * Assertion failures only show the message
 * Exception/Error is stripped from the Exception name to save space.
 * The exception message is trimmed to an approximate 80 chars.
 * The » symbol means that the exception happened below the method shown (in library code called by test)
 * Methods in superclasses are normally shown as SuperClassName.methodName
 * If the first method in the stacktrace is in a a superclass it will be show as this: TestClass>Superclass.method
