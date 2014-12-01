package squarespace.jersey2.guice.resourceinjection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("hello")
public final class HelloResource {

    @Inject
    HelloService service;

    @Inject
    @Named("simple")
    HelloService namedService;

    @Inject
    @Other
    HelloService annotatedService;

    @GET
    public String hello() {
        return service.hello();
    }

    @GET
    @Path("named")
    public String helloNamed() {
        return namedService.hello();
    }

    @GET
    @Path("annotated")
    public String helloAnnotated() {
        return annotatedService.hello();
    }
}