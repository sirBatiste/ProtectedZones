package be.batiste.protectedZones;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Nameable;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Player.Spigot;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class ProtectedZones extends JavaPlugin implements Listener {
   Dictionary<Player, Villager> activeInspections = new Hashtable();
   FileLogger logger;
   int radius = 1;

   public void onEnable() {
      this.getServer().getPluginManager().registerEvents(this, this);
      this.saveDefaultConfig();
      new Metrics(this, 21392);
      (new VersionChecker(this)).checkVersion("https://batiste.be/api/checkVersion/protectedzones/0.1");
      this.logger = new FileLogger(this);
      new BatisteAdmin(this);
   }

   @EventHandler
   public void protectorBlockPlace(BlockPlaceEvent event) {
      if (event.getBlock().getType() == Material.TRAPPED_CHEST) {
         Chunk chunk = event.getBlock().getChunk();
         Block block = event.getBlock();
         Player player = event.getPlayer();
         PersistentDataContainer playerData = player.getPersistentDataContainer();
         Spigot var8;
         ChatMessageType var9;
         String var10004;
         if (playerData.has(new NamespacedKey(this, "chestOwned"), PersistentDataType.STRING)) {
            this.giveCompass(event);
            var8 = player.spigot();
            var9 = ChatMessageType.ACTION_BAR;
            var10004 = String.valueOf(ChatColor.RED);
            var8.sendMessage(var9, new TextComponent(var10004 + String.valueOf(ChatColor.BOLD) + "███Ya tenías una protección activa███"));
            event.setCancelled(true);
         } else if (this.adjacentChunkIsProtected(event)) {
            var8 = player.spigot();
            var9 = ChatMessageType.ACTION_BAR;
            var10004 = String.valueOf(ChatColor.RED);
            var8.sendMessage(var9, new TextComponent(var10004 + String.valueOf(ChatColor.BOLD) + "███No se puede proteger la zona, la protección se solapa con otra███"));
         } else {
            BlockState blockState = block.getState();
            Nameable nameable = (Nameable)blockState;
            nameable.setCustomName(player.getName());
            blockState.update();
            this.buildChunkProtection(player, block);
            FileLogger var10000 = this.logger;
            String var10001 = player.getName();
            var10000.log("created " + var10001 + " " + block.getWorld().getName() + " " + block.getX() + "," + block.getY() + "," + block.getZ());
         }
      }
   }

   private void buildChunkProtection(Player player, Block block) {
      for(int x = -this.radius; x <= this.radius; ++x) {
         for(int z = -this.radius; z <= this.radius; ++z) {
            PersistentDataContainer playerData = player.getPersistentDataContainer();
            Chunk chunk = block.getChunk();
            chunk = chunk.getWorld().getChunkAt(chunk.getX() + x, chunk.getZ() + z);
            PersistentDataContainer chunkData = chunk.getPersistentDataContainer();
            chunkData.set(new NamespacedKey(this, "chunkOwner"), PersistentDataType.STRING, player.getName());
            NamespacedKey var10001 = new NamespacedKey(this, "chestOwned");
            PersistentDataType var10002 = PersistentDataType.STRING;
            int var10003 = block.getX();
            playerData.set(var10001, var10002, var10003 + "," + block.getY() + "," + block.getZ());
            Spigot var10000 = player.spigot();
            ChatMessageType var19 = ChatMessageType.ACTION_BAR;
            String var10004 = String.valueOf(ChatColor.RED);
            var10000.sendMessage(var19, new TextComponent(var10004 + String.valueOf(ChatColor.BOLD) + "███Has protegido el chunk███"));
            BlockState chestState = block.getState();
            Container container = (Container)chestState;
            Inventory inventory = container.getInventory();
            ItemStack[] contents = inventory.getContents();
            ArrayList<String> allowingList = new ArrayList();
            ItemStack[] var16 = contents;
            int var15 = contents.length;

            for(int var14 = 0; var14 < var15; ++var14) {
               ItemStack item = var16[var14];
               if (item != null) {
                  String name = item.getItemMeta().getDisplayName();
                  if (item.getItemMeta().hasDisplayName()) {
                     allowingList.add(name);
                  }
               }
            }

            String allowed = allowingList.toString();
            allowed = allowed.replace("[", "").replace("]", "").replace(" ", "");
            chunkData.set(new NamespacedKey(this, "chunkAllowed"), PersistentDataType.STRING, allowed);
         }
      }

   }

   private void buildAllowed(Block block) {
      Chunk chunk = block.getChunk();
      PersistentDataContainer chunkData = chunk.getPersistentDataContainer();
      chunkData.set(new NamespacedKey(this, "chunkAllowed"), PersistentDataType.STRING, "");
   }

   @EventHandler
   public void protectorChestClose(InventoryCloseEvent event) {
      Inventory inventory = event.getInventory();
      Player player = (Player)event.getPlayer();
      InventoryHolder holder = inventory.getHolder();
      if (!(holder instanceof DoubleChest)) {
         if (inventory.getType() == InventoryType.CHEST) {
            Chest chest = (Chest)inventory.getHolder();
            if (chest != null) {
               if (chest.getBlock().getType() == Material.TRAPPED_CHEST) {
                  this.buildChunkProtection(player, chest.getBlock());
               }
            }
         }
      }
   }

   private boolean adjacentChunkIsProtected(BlockPlaceEvent event) {
      for(int x = -this.radius; x <= this.radius; ++x) {
         for(int z = -this.radius; z <= this.radius; ++z) {
            Chunk chunk = event.getBlock().getChunk();
            chunk = chunk.getWorld().getChunkAt(chunk.getX() + x, chunk.getZ() + z);
            PersistentDataContainer chunkData = chunk.getPersistentDataContainer();
            if (chunkData.has(new NamespacedKey(this, "chunkOwner"), PersistentDataType.STRING)) {
               return true;
            }
         }
      }

      return false;
   }

   @EventHandler
   private void playerMove(PlayerMoveEvent event) {
      Player player = event.getPlayer();
      Location from = event.getFrom();
      Location to = event.getTo();
      int singleFrom = from.getBlockX() + from.getBlockZ();
      int singleTo = to.getBlockX() + to.getBlockZ();
      if (singleFrom != singleTo) {
         this.redstoneWaller(event);
         PersistentDataContainer chunkData;
         if (from.getChunk() != to.getChunk()) {
            chunkData = to.getChunk().getPersistentDataContainer();
            if (chunkData.has(new NamespacedKey(this, "chunkOwner"), PersistentDataType.STRING)) {
               String chunkOwner = (String)chunkData.get(new NamespacedKey(this, "chunkOwner"), PersistentDataType.STRING);
               Spigot var10000 = player.spigot();
               ChatMessageType var10001 = ChatMessageType.ACTION_BAR;
               String var10004 = String.valueOf(ChatColor.RED);
               var10000.sendMessage(var10001, new TextComponent(var10004 + String.valueOf(ChatColor.BOLD) + "███Propiedad de " + chunkOwner + "███"));
               if (player.getInventory().getItemInMainHand().getType() == Material.BARRIER) {
                  chunkData.remove(new NamespacedKey(this, "chunkOwner"));
                  player.getPersistentDataContainer().remove(new NamespacedKey(this, "chestOwned"));
               }
            }
         }

         if (player.getInventory().getItemInMainHand().getType() == Material.BARRIER) {
            chunkData = to.getChunk().getPersistentDataContainer();
            if (chunkData.has(new NamespacedKey(this, "chunkOwner"), PersistentDataType.STRING)) {
               chunkData.remove(new NamespacedKey(this, "chunkOwner"));
               player.sendMessage("borrado");
            }
         }
      }

   }

   private void redstoneWaller(PlayerMoveEvent event) {
      Player player = event.getPlayer();
      Chunk playerChunk = player.getLocation().getChunk();
      PersistentDataContainer chunkData = playerChunk.getPersistentDataContainer();
      if (player.getInventory().getItemInMainHand().getType() == Material.REDSTONE_TORCH || player.getInventory().getItemInOffHand().getType() == Material.REDSTONE_TORCH) {
         if (chunkData.has(new NamespacedKey(this, "chunkOwner"), PersistentDataType.STRING)) {
            int radius = 3;

            for(int x = -radius; x <= radius; ++x) {
               for(int z = -radius; z <= radius; ++z) {
                  Location location = event.getTo();
                  Block targetBlock = location.getWorld().getBlockAt(location.getBlockX() - x, location.getBlockY(), location.getBlockZ() - z);
                  if (!targetBlock.getChunk().getPersistentDataContainer().has(new NamespacedKey(this, "chunkOwner"), PersistentDataType.STRING)) {
                     this.drawDust(targetBlock.getLocation().add(0.5D, 0.5D, 0.5D), Color.RED);
                  } else {
                     this.drawDust(targetBlock.getLocation().add(0.5D, 0.0D, 0.5D), Color.LIME);
                  }
               }
            }
         }

      }
   }

   private void drawDust(Location location, Color color) {
      int count = 1;
      double offsetX = 0.0D;
      double offsetZ = 0.0D;
      double offsetY = 0.0D;
      double extra = 0.0D;
      double targetLayers = 0.0D;
      if (color == Color.RED) {
         targetLayers = 5.0D;
      } else {
         targetLayers = 0.0D;
      }

      Particle particle = Particle.REDSTONE;
      DustOptions options = new DustOptions(color, 3.0F);

      for(double layer = -targetLayers; layer <= targetLayers; ++layer) {
         Location locb = new Location(location.getWorld(), location.getX(), location.getY(), location.getZ());
         locb.add(0.0D, layer, 0.0D);
         locb.getWorld().spawnParticle(Particle.REDSTONE, locb, count, offsetX, offsetY, offsetZ, extra, options, true);
      }

   }

   @EventHandler
   public void protectionBlockBreak(BlockBreakEvent event) {
      if (event.getBlock().getType() == Material.TRAPPED_CHEST) {
         Chunk chunk = event.getBlock().getChunk();
         PersistentDataContainer data = chunk.getPersistentDataContainer();
         PersistentDataContainer playerData = event.getPlayer().getPersistentDataContainer();
         Player player = event.getPlayer();
         if (data.has(new NamespacedKey(this, "chunkOwner"), PersistentDataType.STRING)) {
            String owner = (String)data.get(new NamespacedKey(this, "chunkOwner"), PersistentDataType.STRING);
            if (player.getName().equals(owner)) {
               Block block = event.getBlock();
               FileLogger var10000 = this.logger;
               String var10001 = player.getName();
               var10000.log("removed " + var10001 + " " + block.getWorld().getName() + " " + block.getX() + "," + block.getY() + "," + block.getZ());

               for(int x = -this.radius; x <= this.radius; ++x) {
                  for(int z = -this.radius; z <= this.radius; ++z) {
                     this.freeChunk(event, x, z);
                  }
               }
            } else {
               Spigot var10 = player.spigot();
               ChatMessageType var11 = ChatMessageType.ACTION_BAR;
               String var10004 = String.valueOf(ChatColor.RED);
               var10.sendMessage(var11, new TextComponent(var10004 + String.valueOf(ChatColor.BOLD) + "███Solo el propietario puede███"));
               event.setCancelled(true);
            }
         }

      }
   }

   private void freeChunk(BlockBreakEvent event, int x, int z) {
      Chunk chunk = event.getBlock().getChunk();
      chunk = chunk.getWorld().getChunkAt(chunk.getX() + x, chunk.getZ() + z);
      PersistentDataContainer data = chunk.getPersistentDataContainer();
      PersistentDataContainer playerData = event.getPlayer().getPersistentDataContainer();
      data.remove(new NamespacedKey(this, "chunkOwner"));
      Player player = event.getPlayer();
      Spigot var10000 = player.spigot();
      ChatMessageType var10001 = ChatMessageType.ACTION_BAR;
      String var10004 = String.valueOf(ChatColor.RED);
      var10000.sendMessage(var10001, new TextComponent(var10004 + String.valueOf(ChatColor.BOLD) + "███Has eliminado tu protección███"));
      playerData.remove(new NamespacedKey(this, "chestOwned"));
   }

   private void giveCompass(BlockPlaceEvent event) {
      Player player = event.getPlayer();
      PersistentDataContainer playerData = player.getPersistentDataContainer();
      String ownedString = (String)playerData.get(new NamespacedKey(this, "chestOwned"), PersistentDataType.STRING);
      String[] parts = ownedString.split(",");
      Location target = event.getBlock().getWorld().getBlockAt(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])).getLocation();
      ItemStack item = new ItemStack(Material.COMPASS);
      CompassMeta meta = (CompassMeta)item.getItemMeta();
      meta.setLodestone(target);
      meta.setDisplayName("Localiza tu cofre de protección");
      meta.setLodestoneTracked(false);
      item.setItemMeta(meta);
      target.getWorld().dropItem(event.getBlock().getLocation(), item);
   }

   @EventHandler
   public void protectorChestOpen(InventoryOpenEvent event) {
      Inventory inventory = event.getInventory();
      Player player = (Player)event.getPlayer();
      InventoryHolder holder = inventory.getHolder();
      if (!(holder instanceof DoubleChest)) {
         if (inventory.getType() == InventoryType.CHEST) {
            Chest chest = (Chest)inventory.getHolder();
            if (chest != null) {
               if (chest.getBlock().getType() == Material.TRAPPED_CHEST) {
                  PersistentDataContainer data = chest.getChunk().getPersistentDataContainer();
                  String owner = (String)data.get(new NamespacedKey(this, "chunkOwner"), PersistentDataType.STRING);
                  if (!player.getName().equals(owner)) {
                     event.setCancelled(true);
                  }

               }
            }
         }
      }
   }

   @EventHandler
   public void blockBreak(BlockBreakEvent event) {
      if (!this.isAllowed(event.getPlayer(), event.getBlock().getChunk())) {
         event.setCancelled(true);
      }

   }

   @EventHandler
   public void playerInteract(PlayerInteractEvent event) {
      if (event.getClickedBlock() != null && !this.isAllowed(event.getPlayer(), event.getClickedBlock().getChunk())) {
         event.setCancelled(true);
      }

   }

   private boolean isAllowed(Player player, Chunk chunk) {
      String playerName = player.getName();
      PersistentDataContainer data = chunk.getPersistentDataContainer();
      if (!data.has(new NamespacedKey(this, "chunkOwner"), PersistentDataType.STRING)) {
         return true;
      } else {
         String owner = (String)data.get(new NamespacedKey(this, "chunkOwner"), PersistentDataType.STRING);
         if (playerName.equals(owner)) {
            return true;
         } else {
            if (data.has(new NamespacedKey(this, "chunkAllowed"), PersistentDataType.STRING)) {
               String allowed = (String)data.get(new NamespacedKey(this, "chunkAllowed"), PersistentDataType.STRING);
               String[] parts = allowed.split(",");
               if (Arrays.asList(parts).contains(playerName)) {
                  return true;
               }
            }

            return false;
         }
      }
   }
}