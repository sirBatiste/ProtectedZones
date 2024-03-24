package be.batiste.protectedZones;

import org.bukkit.plugin.Plugin;

public class BatisteAdmin {
   private Plugin plugin;

   public BatisteAdmin(Plugin pluginReference) {
      this.plugin = pluginReference;
      Plugin admin = this.plugin.getServer().getPluginManager().getPlugin("BatisteSimpleAdmin");
      if (admin == null) {
         this.plugin.getLogger().warning("Better with BatisteSimpleAdmin");
      }

   }
}
 