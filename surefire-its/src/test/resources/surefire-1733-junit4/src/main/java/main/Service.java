package main;

import java.io.IOException;
import java.util.Scanner;

/**
 *
 */
public class Service
{
    public String getNormalResource()
    {
        try ( Scanner scanner = new Scanner( getClass().getResourceAsStream( "/main/a.txt" ) ) )
        {
            return scanner.nextLine();
        }
    }

    public String getResourceByJPMS() throws IOException
    {
        try ( Scanner scanner = new Scanner( getClass().getModule().getResourceAsStream( "main/a.txt" ) ) )
        {
            return scanner.nextLine();
        }
    }
}
