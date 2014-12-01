package squarespace.jersey2.guice.resourceinjection;

public class HelloServiceSimple implements HelloService {

    public static final String HELLO = "hello (named)";

    @Override
    public String hello() {
        return HELLO;
    }

}
