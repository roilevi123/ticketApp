import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class Main {

    public static void main(String[] args) {
        try {

            String requestBody =
                    "action_type=pay" +
                            "&amount=100" +
                            "&currency=USD" +
                            "&card_number=2222333344445555" +
                            "&month=4" +
                            "&year=2027" +
                            "&holder=Test User" +
                            "&cvv=988" +      // תשחק עם 986 / 988
                            "&id=123456789";

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://damp-lynna-wsep-1984852e.koyeb.app/"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            System.out.println("Sending request...");

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("STATUS = " + response.statusCode());
            System.out.println("BODY   = [" + response.body() + "]");

        } catch (Exception e) {
            System.out.println("EXCEPTION:");
            e.printStackTrace();
        }
    }
}