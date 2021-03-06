package com.github.CubieX.MobShrinker;

import java.util.HashMap;

import net.minecraft.server.v1_7_R1.AttributeInstance;
import net.minecraft.server.v1_7_R1.EntityInsentient;
import net.minecraft.server.v1_7_R1.GenericAttributes;
import org.bukkit.craftbukkit.v1_7_R1.entity.CraftLivingEntity;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Horse.Color;
import org.bukkit.entity.Horse.Style;
import org.bukkit.entity.Horse.Variant;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Ocelot.Type;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class MSEntityListener implements Listener
{
   private MobShrinker plugin = null;

   public MSEntityListener(MobShrinker plugin)
   {        
      this.plugin = plugin;

      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   //================================================================================================
   @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
   public void onPlayerInteractEntity(PlayerInteractEntityEvent e)
   {      
      if(null != e.getPlayer().getItemInHand())
      {
         if(e.getPlayer().getItemInHand().getType() == Material.getMaterial(MobShrinker.item))
         {
            if(e.getPlayer().getInventory().containsAtLeast(new ItemStack(Material.getMaterial(MobShrinker.item)), MobShrinker.amount))
            {
               // pack an animal into special spawner egg
               if(MobShrinker.isActive)
               {
                  if(e.getPlayer().isOp() || e.getPlayer().hasPermission("mobshrinker.use"))
                  {
                     if(MobShrinker.allowedEntites.contains(e.getRightClicked().getType().toString()))
                     {
                        LivingEntity ent = (LivingEntity)e.getRightClicked();

                        if(ent instanceof Tameable) // if tameable animal, check owner before packing into egg
                        {
                           Tameable tameableEnt = (Tameable)ent;

                           if(tameableEnt.isTamed())
                           {
                              if(!e.getPlayer().getName().equals(tameableEnt.getOwner().getName()))
                              {                           
                                 e.getPlayer().sendMessage(ChatColor.RED + "Dieses Tier gehoert " + ChatColor.WHITE + tameableEnt.getOwner().getName() + ChatColor.RED + "!");
                                 return;
                              }
                           }
                           else
                           {
                              e.getPlayer().sendMessage(ChatColor.RED + "Wilde Tiere koennen nicht umgewandelt werden!");
                              return;
                           }
                        }

                        packLivingEntityToEgg(ent, e.getPlayer());
                     }
                     else
                     {
                        e.getPlayer().sendMessage(ChatColor.RED + "Dieser Mob-Typ kann nicht umgewandelt werden!");
                     }          
                  }
               }
               else
               {
                  e.getPlayer().sendMessage(MobShrinker.logPrefix + " ist momentan " + ChatColor.RED + " DEAKTIVIERT.");
               }
            }
            else
            {
               e.getPlayer().sendMessage(ChatColor.RED + " Du hast nicht die benoetigten " + ChatColor.WHITE + MobShrinker.amount + "x " + MobShrinker.item + ChatColor.RED + "!");
            }
         }
      }
   }

   //================================================================================================
   @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
   public void onPlayerInteract(PlayerInteractEvent e)
   {
      if(e.getAction() == Action.RIGHT_CLICK_BLOCK)
      {
         if(null != e.getPlayer().getItemInHand())
         {
            if(e.getPlayer().getItemInHand().getType() == Material.MONSTER_EGG)
            {
               if(e.getPlayer().getItemInHand().hasItemMeta())
               {                  
                  if(e.getPlayer().getItemInHand().getItemMeta().hasLore())
                  {
                     // RE-SPAWN A MOUNT FROM SPECIAL SPAWNER EGG ===============================================
                     if(e.getPlayer().getItemInHand().getItemMeta().getLore().get(0).startsWith("HORSE") ||
                           e.getPlayer().getItemInHand().getItemMeta().getLore().get(0).startsWith("DONKEY") ||
                           e.getPlayer().getItemInHand().getItemMeta().getLore().get(0).startsWith("MULE"))
                     {
                        String[] raceAndLook = e.getPlayer().getItemInHand().getItemMeta().getLore().get(0).split("\\|"); // escaping necessary, because | is a regex special char
                        // Order: variant|color|style

                        String[] attribs = e.getPlayer().getItemInHand().getItemMeta().getLore().get(1).split("\\|");
                        // Order: maxHealth|currHealth|speed|jumpStrength|isSaddled|armorType|carriesChest

                        String customName = null;

                        if(e.getPlayer().getItemInHand().getItemMeta().getLore().size() == 3) // may have length = 2 if no custom name was set
                        {
                           customName = e.getPlayer().getItemInHand().getItemMeta().getLore().get(2);                        
                        }

                        // find place to spawn mount
                        Horse mount = (Horse)e.getPlayer().getWorld().spawnEntity(e.getClickedBlock().getRelative(BlockFace.UP).getLocation(), EntityType.HORSE);

                        if((e.getPlayer().getInventory().getItemInHand().getAmount() - 1) > 0)
                        {
                           e.getPlayer().getInventory().getItemInHand().setAmount(e.getPlayer().getInventory().getItemInHand().getAmount() - 1);
                        }
                        else
                        {
                           e.getPlayer().getInventory().setItemInHand(new ItemStack(Material.AIR));
                        }

                        e.getPlayer().updateInventory();
                        // TODO check surroundings! And search suiting position nearby. Prevent spawning it in a wall!

                        // set mounts attributes
                        mount.setOwner(e.getPlayer()); // will tame the mount automaticly                     
                        mount.setVariant(Variant.valueOf(raceAndLook[0]));
                        mount.setColor(Color.valueOf(raceAndLook[1]));
                        mount.setStyle(Style.valueOf(raceAndLook[2]));                     
                        mount.setMaxHealth(Double.valueOf(attribs[0]));
                        mount.setHealth(Double.valueOf(attribs[1]));
                        setHorseSpeed(mount, Double.valueOf(attribs[2]));
                        mount.setJumpStrength(Double.valueOf(attribs[3]));

                        byte isSaddled = 1;
                        byte carriesChest = 0;
                        int armorType = 0;      // 0 means, no armor. Otherwise this will be the ItemID.

                        try
                        {
                           isSaddled = Byte.parseByte(attribs[4]);
                           armorType = Byte.parseByte(attribs[5]);
                           carriesChest = Byte.parseByte(attribs[6]);                              
                        }
                        catch(NumberFormatException ex)
                        {
                           MobShrinker.log.severe(MobShrinker.logPrefix + ex.getMessage()); // should never happen
                        }

                        if(1 == isSaddled)
                        {
                           mount.getInventory().setSaddle(new ItemStack(Material.SADDLE, 1));                        
                        }

                        if(0 != armorType)
                        {
                           mount.getInventory().setArmor(new ItemStack(Material.getMaterial(armorType), 1));                        
                        }

                        if(1 == carriesChest)
                        {
                           mount.setCarryingChest(true);
                        }

                        if(null != customName)
                        {
                           mount.setCustomName(customName);                        
                        }
                        else
                        {
                           customName = "Horse";
                        }

                        if(MobShrinker.debug){e.getPlayer().sendMessage(raceAndLook[0] + " - " + raceAndLook[1] + " - " + raceAndLook[2] + " | MaxHP: " + attribs[0] + " | currHP: " + attribs[1] +
                              " | Speed: " + attribs[2] + " | JumpStr: " +
                              attribs[3] + " | saddled: " + attribs[4] + " | Armor: " + attribs[5] + " | " + attribs[6] + " | " + customName);}

                        e.setCancelled(true); // cancel event to prevent spawning a creature out of the egg.                        
                     }
                     // RE-SPAWN A WOLF FROM SPECIAL SPAWNER EGG ===============================================
                     else if(e.getPlayer().getItemInHand().getItemMeta().getLore().get(0).startsWith("Wolf"))
                     {
                        String collarColor = e.getPlayer().getItemInHand().getItemMeta().getLore().get(1);
                        String customName = null;

                        if(e.getPlayer().getItemInHand().getItemMeta().getLore().size() == 3) // may have length = 2 if no custom name was set
                        {                              
                           customName = e.getPlayer().getItemInHand().getItemMeta().getLore().get(2);                              
                        }

                        // find place to spawn wolf
                        Wolf wolf = (Wolf)e.getPlayer().getWorld().spawnEntity(e.getClickedBlock().getRelative(BlockFace.UP).getLocation(), EntityType.WOLF);

                        if((e.getPlayer().getInventory().getItemInHand().getAmount() - 1) > 0)
                        {
                           e.getPlayer().getInventory().getItemInHand().setAmount(e.getPlayer().getInventory().getItemInHand().getAmount() - 1);
                        }
                        else
                        {
                           e.getPlayer().getInventory().setItemInHand(new ItemStack(Material.AIR));
                        }

                        e.getPlayer().updateInventory();
                        // TODO check surroundings! And search suiting position nearby. Prevent spawning it in a wall!

                        // set wolfs attributes
                        wolf.setAdult();
                        wolf.setOwner(e.getPlayer()); // will tame the wolf automaticly            

                        if(null != customName)
                        {
                           wolf.setCustomName(customName);                              
                        }
                        else
                        {
                           customName = "Wolf";                              
                        }

                        if(MobShrinker.debug){e.getPlayer().sendMessage(wolf.getType().getName() + " - " + customName + " - Owner: " + wolf.getOwner().getName() + " - isTamed: " + wolf.isTamed());}

                        e.setCancelled(true); // cancel event to prevent spawning a creature out of the egg.
                     }
                     // RE-SPAWN A CAT FROM SPECIAL SPAWNER EGG ===============================================
                     else if(e.getPlayer().getItemInHand().getItemMeta().getLore().get(0).startsWith("Cat"))
                     {
                        String type[] = e.getPlayer().getItemInHand().getItemMeta().getLore().get(0).split("\\|"); // escaping necessary, because | is a regex special char                     
                        String customName = null;

                        if(e.getPlayer().getItemInHand().getItemMeta().getLore().size() == 2) // may have length = 1 if no custom name was set
                        {
                           customName = e.getPlayer().getItemInHand().getItemMeta().getLore().get(1);                        
                        }

                        // find place to spawn cat
                        Ocelot cat = (Ocelot)e.getPlayer().getWorld().spawnEntity(e.getClickedBlock().getRelative(BlockFace.UP).getLocation(), EntityType.OCELOT);

                        if((e.getPlayer().getInventory().getItemInHand().getAmount() - 1) > 0)
                        {
                           e.getPlayer().getInventory().getItemInHand().setAmount(e.getPlayer().getInventory().getItemInHand().getAmount() - 1);
                        }
                        else
                        {
                           e.getPlayer().getInventory().setItemInHand(new ItemStack(Material.AIR));
                        }

                        e.getPlayer().updateInventory();
                        // TODO check surroundings! And search suiting position nearby. Prevent spawning it in a wall!

                        // set cats attributes                          
                        cat.setOwner(e.getPlayer()); // will tame the cat automaticly
                        cat.setCatType(Type.valueOf(type[1]));

                        if(null != customName)
                        {
                           cat.setCustomName(customName);                              
                        }
                        else
                        {
                           customName = "Cat";
                        }

                        if(MobShrinker.debug){e.getPlayer().sendMessage(cat.getType().getName() + " - " + customName + " - Owner: " + cat.getOwner().getName() + " - isTamed: " + cat.isTamed());}

                        e.setCancelled(true); // cancel event to prevent spawning a creature out of the egg.
                     }
                  }   
               }
            }
         }         
      }      
   }

   // ######################################################################################################

   // FIXME Use Bukkit API instead of CraftBukkit calls for this, as soon as it is available!
   private void setHorseSpeed(Horse h, double speed)
   {   
      // use about  2.25 for normalish speed  
      AttributeInstance attributes = ((EntityInsentient)((CraftLivingEntity)h).getHandle()).getAttributeInstance(GenericAttributes.d);
      attributes.setValue(speed);
   }

   /**
    * Packs an animal into a special spawner egg.
    * 
    * Attributes of animals will be saved in the egg as
    * ItemMeta data.
    */
   private void packLivingEntityToEgg(LivingEntity ent, Player player)
   {
      ItemStack spawnerEgg = plugin.getSpawnerEggOfMob(ent, player);

      if(null != spawnerEgg)
      {
         HashMap<Integer, ItemStack> notFit = player.getInventory().addItem(spawnerEgg);

         if(!notFit.isEmpty())
         {
            player.sendMessage(ChatColor.GOLD + "Kein freier Platz im Inventar! Bitte lege zuerst etwas ab.");                                    
         }
         else
         {
            player.getInventory().removeItem(new ItemStack(Material.getMaterial(MobShrinker.item), MobShrinker.amount));
            player.updateInventory();
            ent.remove();
            // FIXME deprecated stuff fixen!
            player.sendMessage(ChatColor.GREEN + "Dieses " + ChatColor.WHITE + ent.getType().getName() + ChatColor.GREEN + " wurde in ein Spanwer-Ei verwandelt!");
         }
      }
      else
      {
         player.sendMessage(ChatColor.WHITE + ent.getType().getName() + ChatColor.RED + " konnte nicht in ein Spawner-Ei verwandelt werden!");
      }      
   }
}
