package junitplatformenginejqwik;

import net.jqwik.api.Example;

class BasicJQwikTest
{

    @Example
    boolean exampleFor1Plus3Equals4()
    {
        return ( 1 + 3 ) == 4;
    }

}
