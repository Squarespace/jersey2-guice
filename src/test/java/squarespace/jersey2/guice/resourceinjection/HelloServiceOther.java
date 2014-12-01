package squarespace.jersey2.guice.resourceinjection;

public class HelloServiceOther implements HelloService {

    public static final String HELLO = "hello (annotaded)";

    @Override
    public String hello() {
        return HELLO;
    }

}
