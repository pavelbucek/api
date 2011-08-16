/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
 * 
 * The contents at this file are subject to the terms at either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy at the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 * 
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section at the License
 * file that accompanied this code.
 * 
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name at copyright owner]"
 * 
 * Contributor(s):
 * If you wish your version at this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice at license, a
 * recipient has the option to distribute your version at this file under
 * either the CDDL, the GPL Version 2 or to extend the choice at license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package jaxrs.examples.client;

import java.util.List;
import java.util.concurrent.Future;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientConfiguration;
import javax.ws.rs.client.ClientFactory;
import javax.ws.rs.client.DefaultClientConfiguration;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.Target;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ClientBuilderFactory;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Bill Burke
 * @author Marek Potociar
 */
public class BasicExamples {

    public static abstract class MyClient implements Client {

        public MyClient(ClientConfiguration configuration) { }
    }

    public static class MyClientBuilder implements Client.Builder<MyClientConfiguration> {

        @Override
        public MyClient build() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public MyClient build(MyClientConfiguration config) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    public static class MyClientConfiguration extends DefaultClientConfiguration {

        public void enableCaching() { /* not implemented */ }
    }

    public static class MyClientBuilderFactory implements ClientBuilderFactory<MyClientBuilder> {

        @Override
        public MyClientBuilder newBuilder() {
            return new MyClientBuilder();
        }
    }

    @XmlRootElement
    public static class Customer {

        private final String name;

        public Customer(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public void clientBootstrapping() {
        // Default newClient instantiation using default configuration
        Client defaultClient = ClientFactory.newClient();
        assert defaultClient != null;

        // Default newClient instantiation using custom configuration
        ClientConfiguration cfg = new DefaultClientConfiguration();
        cfg.enable("CUSTOM_FEATURE");

        Client defaultConfiguredClient = ClientFactory.newClient(cfg);
        assert defaultConfiguredClient != null;

        ///////////////////////////////////////////////////////////
        
        // Custom newClient instantiation using default configuration
        MyClient myClient = ClientFactory.newClientBy(MyClientBuilderFactory.class).build();
        assert myClient != null;

        // Custom newClient instantiation using custom configuration
        MyClientConfiguration myCfg = new MyClientConfiguration();
        myCfg.enableCaching();

        MyClient myConfiguredClient = ClientFactory.newClientBy(MyClientBuilderFactory.class).build(myCfg);
        assert myConfiguredClient != null;
    }
    
    public void creatingResourceAndSubResourceUris() {
        // Target( http://jaxrs.examples.org/jaxrsApplication/customers/ )
        Target customersUri = ClientFactory.newClient().at("http://jaxrs.examples.org/jaxrsApplication/customers");        
        // Target( http://jaxrs.examples.org/jaxrsApplication/customers/{id}/ )
        Target anyCustomerUri = customersUri.path("{id}");
        // Target( http://jaxrs.examples.org/jaxrsApplication/customers/123/ )
        Target customer123 = anyCustomerUri.pathParam("id", 123);
        
        assert customer123 != null;
    }
    
    public void creatingClientRequestsAndInvocations() {
        final Client client = ClientFactory.newClient();
        
        HttpResponse response = client.at("http://jaxrs.examples.org/jaxrsApplication/customers").get()
                .accept(MediaType.APPLICATION_XML).header("Foo", "Bar").invoke();        
        assert response.getStatusCode() == 200;        
    }

    public void creatingResourceUriRequestsAndInvocations() {
        final Client client = ClientFactory.newClient();
        final Target customersUri = client.at("http://jaxrs.examples.org/jaxrsApplication/customers");
        
        // Create at request, customize it and invoke using newClient
        HttpResponse response = customersUri.get().accept(MediaType.APPLICATION_XML).header("Foo", "Bar").invoke();        
        assert response.getStatusCode() == 200;
    }

    public void defaultResponse() {
        Customer customer;
        HttpResponse response;

        final Target customersUri = ClientFactory.newClient().at("http://jaxrs.examples.org/jaxrsApplication/customers");

        response = customersUri.path("{id}").get().pathParam("id", 123).invoke();
        customer = response.getEntity(Customer.class);
        assert customer != null;

        response = customersUri.post().entity(new Customer("Marek")).type("application/xml").invoke();
        assert response.getStatusCode() == 201;
    }

    public void typedResponse() {
        Customer customer = ClientFactory.newClient()
                .at("http://jaxrs.examples.org/jaxrsApplication/customers/{id}")
                .get()
                .pathParam("id", 123)
                .invoke(Customer.class);
        assert customer != null;
    }

    public void typedGenericResponse() {
        List<Customer> customers = ClientFactory.newClient()
                .at("http://jaxrs.examples.org/jaxrsApplication/customers")
                .get()
                .invoke(new GenericType<List<Customer>>() { });
        assert customers != null;
    }

    public void responseUsingSubResourceClient() {
        Target customersUri = ClientFactory.newClient().at("http://jaxrs.examples.org/jaxrsApplication/customers");
        Target customer = customersUri.path("{id}");

        // Create a customer
        HttpResponse response = customersUri.post()
                .entity(new Customer("Bill")).type("application/xml").invoke();
        assert response.getStatusCode() == 201;

        Customer favorite;
        // view a customer
        favorite = customer.get() // Invocation (extends HttpRequest)
                .pathParam("id", 123).invoke(Customer.class);
        assert favorite != null;

        // view a customer (alternative)
        favorite = customer.pathParam("id", 123) // Target ("http://jaxrs.examples.org/jaxrsApplication/customers/123/")
                .get() // Invocation (extends HttpRequest)
                .invoke(Customer.class);
        assert favorite != null;
    }

    public void asyncResponse() throws Exception {
        Future<HttpResponse> future = ClientFactory.newClient()
                .at("http://jaxrs.examples.org/jaxrsApplication/customers/{id}")
                .get()
                .pathParam("id", 123)
                .submit();

        HttpResponse response = future.get();
        Customer customer = response.getEntity(Customer.class);
        assert customer != null;
    }

    public void typedAsyncResponse() throws Exception {
        Future<Customer> customer = ClientFactory.newClient()
                .at("http://jaxrs.examples.org/jaxrsApplication/customers/{id}")
                .get()
                .pathParam("id", 123)
                .submit(Customer.class);
        assert customer.get() != null;
    }

    public void asyncCallback() {
        final Client client = ClientFactory.newClient();
        Invocation request = client.at("http://jaxrs.examples.org/jaxrsApplication/customers/{id}").get();
        request.pathParam("id", 123).submit(new InvocationCallback<Customer>() {

            @Override
            public void onComplete(Future<Customer> future) {
                // Do something
            }
        });

        // invoke another request in background
        Future<?> handle = request.pathParam("id", 456).submit(new InvocationCallback<HttpResponse>() {

            @Override
            public void onComplete(Future<HttpResponse> future) {
                // do something
            }
        });
        handle.cancel(true);
    }

    public void asyncCallbackUsingSubResourceClient() throws Exception {
        final Client client = ClientFactory.newClient();
        Target anyCustomerUri = client.at("http://jaxrs.examples.org/jaxrsApplication/customers/{id}");

        // invoke a request in background
        Future<Customer> handle = anyCustomerUri.pathParam("id", 123) // Target
                .get() // Invocation (extends HttpRequest)
                .submit(new InvocationCallback<Customer>() {

            @Override
            public void onComplete(Future<Customer> future) {
                // Do something
            }
        });
        handle.cancel(true);

        // invoke another request in background
        anyCustomerUri.pathParam("id", 456) // Target
                .get() // Invocation (extends HttpRequest)
                .submit(new InvocationCallback<HttpResponse>() {

            @Override
            public void onComplete(Future<HttpResponse> future) {
                // do something
            }
        });
        
        // invoke one more request using newClient
        Future<HttpResponse> response = anyCustomerUri.pathParam("id", 789).get()
                .cookie(new Cookie("fooName", "XYZ")).submit();
        assert response.get() != null;
    }        
}