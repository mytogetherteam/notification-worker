package org.th.notification;
import com.rabbitmq.client.ConnectionFactory;

public class TestRabbitUri {
    public static void main(String[] args) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUri("amqp://QcfEim7Okf5344pf:Ex2I5L19g67klkzfI0RX0kLBw~X_Pdf1@rabbitmq-od0r.railway.internal:5672");
            System.out.println("Host: " + factory.getHost());
            System.out.println("User: " + factory.getUsername());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
