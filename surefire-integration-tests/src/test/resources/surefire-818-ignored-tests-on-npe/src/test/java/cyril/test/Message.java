package cyril.test;

public class Message
{

    private String content;

    public Message( String content )
    {
        this.content = content;
    }

    public int hashCode()
    {
        throw new NullPointerException();
    }

}
