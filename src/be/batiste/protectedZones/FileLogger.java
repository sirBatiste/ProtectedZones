package be.batiste.protectedZones;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import org.bukkit.plugin.Plugin;

public class FileLogger {
   private Plugin plugin;

   public FileLogger(Plugin pluginReference) {
      this.plugin = pluginReference;
   }

   public void log(String message) {
      try {
         File dataFolder = this.plugin.getDataFolder();
         if (!dataFolder.exists()) {
            dataFolder.mkdir();
         }

         File saveTo = new File(this.plugin.getDataFolder(), "log.txt");
         if (!saveTo.exists()) {
            saveTo.createNewFile();
         }

         FileWriter fw = new FileWriter(saveTo, true);
         PrintWriter pw = new PrintWriter(fw);
         pw.println(message);
         pw.flush();
         pw.close();
      } catch (IOException var6) {
         var6.printStackTrace();
      }

   }
}