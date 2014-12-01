package squarespace.jersey2.guice.resourceinjection;

public class HelloServiceBase implements HelloService {

    private static final String HELLO = "hello";

    @Override
    public String hello() {
        return HELLO;
    }

}
