package be.batiste.protectedZones;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.bukkit.plugin.Plugin;

public class VersionChecker {
   private Plugin plugin;

   public VersionChecker(Plugin pluginReference) {
      this.plugin = pluginReference;
   }

   public void checkVersion(String url) {
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();

      try {
         HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
         String[] parts = ((String)response.body()).split("\\|");
         if (parts[0].equals("ok")) {
            this.plugin.getLogger().info(parts[1]);
         } else if (parts[0].equals("error")) {
            this.plugin.getLogger().warning(parts[1]);
         }
      } catch (IOException var6) {
         var6.printStackTrace();
      } catch (InterruptedException var7) {
         var7.printStackTrace();
      }

   }
}