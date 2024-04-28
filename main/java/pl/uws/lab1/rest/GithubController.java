package pl.uws.lab1.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.uws.lab1.services.ConnectionService;

import java.io.IOException;
import java.net.HttpURLConnection;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class GithubController {
    private final String githubURL = "https://github.com/login/oauth/authorize?client_id=";
    @Value("${github.secret}")
    private String githubSecret;

    @Value("${github.client.id}")
    private String githubClientId;

    private final String frontURL = "http://localhost:4200";
    private final String codeURL = "https://github.com/login/oauth/authorize?client_id=";
    private final String accessTokenURL = "https://github.com/login/oauth/access_token";
    private final String apiURL = "https://api.github.com";

    private final ConnectionService connectionService;;
    private final ObjectMapper mapper = new ObjectMapper();

    public GithubController(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @GetMapping("/login")
    public ResponseEntity<Void> login(HttpServletResponse response) throws IOException {
        String redirectUrl = codeURL + githubSecret;
        response.sendRedirect(redirectUrl);

        return ResponseEntity.status(HttpStatus.FOUND).build();
    }

    @GetMapping("/login/oauth2/code/github")
    public ResponseEntity<Void> oauth2Code(@RequestParam("code") String code, HttpServletResponse httpResponse) throws IOException {
        String redirectUrl = accessTokenURL + "?client_id=" + githubClientId + "&client_secret=" + githubSecret + "&code=" + code;
        HttpURLConnection connection = connectionService.createConnection(redirectUrl, null);
        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            JsonNode parsed = mapper.readTree(connectionService.getResponseFromConnection(connection));
            httpResponse.sendRedirect(frontURL + "?token=" + connectionService.getValue(parsed, "access_token"));
            return ResponseEntity.status(HttpStatus.FOUND).build();
        } else {
            httpResponse.sendRedirect(frontURL);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/scope")
    public ResponseEntity<String> getUsersData(@RequestParam String token, @RequestParam String scope) throws IOException {
        String redirectUrl = apiURL + "/" + scope;
        HttpURLConnection connection = connectionService.createConnection(redirectUrl, token);
        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            String result = connectionService.getResponseFromConnection(connection);
            return ResponseEntity.ok().body(result);
        } else
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

    }
}
