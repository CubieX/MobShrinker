/*
 * MobShrinker - A CraftBukkit plugin to pack animals into spawner eggs
 * Copyright (C) 2013  CubieX
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program; if not,
 * see <http://www.gnu.org/licenses/>.
 */
package com.github.CubieX.MobShrinker;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import net.minecraft.server.v1_7_R1.AttributeInstance;
import net.minecraft.server.v1_7_R1.EntityInsentient;
import net.minecraft.server.v1_7_R1.GenericAttributes;
import org.bukkit.craftbukkit.v1_7_R1.entity.CraftLivingEntity;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.DyeColor;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Ocelot.Type;
import org.bukkit.entity.Player;
import org.bukkit.entity.Horse.Color;
import org.bukkit.entity.Horse.Style;
import org.bukkit.entity.Horse.Variant;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class MobShrinker extends JavaPlugin
{
   public static final Logger log = Bukkit.getServer().getLogger();
   public static final String logPrefix = "[MobShrinker] "; // Prefix to go in front of all log entries
   public static boolean isActive = false; // master switch to activate and deactivate MobShrikers functionality

   private MSCommandHandler comHandler = null;
   private MSConfigHandler cHandler = null;
   private MSEntityListener eListener = null;
   //private STSchedulerHandler schedHandler = null;

   public static ArrayList<String> allowedEntites = new ArrayList<String>();

   // config values
   static boolean debug = false;
   static String item = "DIAMOND";
   static int amount = 1;

   //*************************************************
   static String usedConfigVersion = "1"; // Update this every time the config file version changes, so the plugin knows, if there is a suiting config present
   //*************************************************

   @Override
   public void onEnable()
   {
      cHandler = new MSConfigHandler(this);

      if(!checkConfigFileVersion())
      {
         log.severe(logPrefix + "Outdated or corrupted config file(s). Please delete your config files."); 
         log.severe(logPrefix + "will generate a new config for you.");
         log.severe(logPrefix + "will be disabled now. Config file is outdated or corrupted.");
         getServer().getPluginManager().disablePlugin(this);
         return;
      }

      readConfigValues();

      eListener = new MSEntityListener(this);     
      comHandler = new MSCommandHandler(this, cHandler);      
      getCommand("ms").setExecutor(comHandler);

      //schedHandler = new LWSchedulerHandler(this);

      populateAllowedEntitesList();

      log.info(logPrefix + " version " + getDescription().getVersion() + " is enabled!");

      //schedHandler.startPlayerInWaterCheckScheduler_SynchRepeating();
   }

   private boolean checkConfigFileVersion()
   {      
      boolean configOK = false;     

      if(cHandler.getConfig().isSet("config_version"))
      {
         String configVersion = getConfig().getString("config_version");

         if(configVersion.equals(usedConfigVersion))
         {
            configOK = true;
         }
      }

      return (configOK);
   }  

   public void readConfigValues()
   {
      boolean exceed = false;
      boolean invalid = false;

      if(getConfig().contains("debug")){debug = getConfig().getBoolean("debug");}else{invalid = true;}
      if((getConfig().contains("item")) && (null != Material.getMaterial(item))){item = getConfig().getString("item");}else{invalid = true;}
      if(getConfig().contains("amount"))
      {
         amount = getConfig().getInt("amount"); if(amount <= 0){amount = 1; exceed = true;} if(amount > 64){amount = 64; exceed = true;}
      }
      else
      {
         invalid = true;
      }      

      if(exceed)
      {
         log.warning(logPrefix + "One or more config values are exceeding their allowed range. Please check your config file!");
      }

      if(invalid)
      {
         log.warning(logPrefix + "One or more config values are invalid. Please check your config file!");
      }
   }

   @Override
   public void onDisable()
   {     
      this.getServer().getScheduler().cancelTasks(this);
      cHandler = null;
      eListener = null;
      comHandler = null;
      //schedHandler = null; // TODO ACTIVATE THIS AGAIN IF USED!
      log.info(this.getDescription().getName() + " version " + getDescription().getVersion() + " is disabled!");
   }

   // ########################################################################################

   private void populateAllowedEntitesList()
   {
      //allowedEntites.add("CHICKEN");
      //allowedEntites.add("COW");
      allowedEntites.add("HORSE");        // only tamed ones
      //allowedEntites.add("MUSHROOM_COW");
      allowedEntites.add("OCELOT");       // only tamed ones (= cats)
      //allowedEntites.add("PIG");
      //allowedEntites.add("SHEEP");
      allowedEntites.add("WOLF");         // only tamed ones
   }

   /**
    * Creates a special spawner egg of an animal.
    * The egg will include Itemmeta data.
    * 
    * Animals packed by this method will be packed into the egg with all their
    * original attributes. Like type, speed, jump strength, maxHealth
    * and DisplayName for example.
    * So this method allows to re-spawn the original animal by using the egg.
    * 
    * ECXEPTION: Contents of chests carried by a donkey are NOT retained!
    */
   public ItemStack getSpawnerEggOfMob(LivingEntity livEnt, Player player)
   {
      ItemStack spawnerEgg = null;

      if((null != livEnt) && (null != player))
      {
         String customName = null;
         ItemMeta im = null;
         String name = "";
         List<String> lore = null;

         switch(livEnt.getType())
         {
         case HORSE:
            Horse mount = (Horse)livEnt;

            Variant variant = mount.getVariant();
            Color color = mount.getColor();
            Style style = mount.getStyle();
            double maxHealth = mount.getMaxHealth();
            double currHealth = mount.getHealth();
            double jumpStrength = mount.getJumpStrength();            
            byte isSaddled = 1;        // byte to have shorter value in Lore (shorter than "true or false")
            int armorType = 0;         // 0 means, no armor. Otherwise this will be the ItemID.
            byte carriesChest = 0;     // byte to have shorter value in Lore (shorter than "true or false")
            customName = mount.getCustomName(); // may be null (must be set by using a NameTag)

            if(null == mount.getInventory().getSaddle())
            {
               isSaddled = 0;  
            }

            if(mount.isCarryingChest())
            {
               carriesChest = 1;
            }

            ItemStack armor = mount.getInventory().getArmor();            

            if(null != armor)
            {
               armorType = mount.getInventory().getArmor().getTypeId(); //FIXME deprecated -> ersetzen
            }

            double speed = getHorseSpeed(mount);

            // create spawner egg which will spawn a cat with all necessary attributes
            spawnerEgg = new ItemStack(Material.MONSTER_EGG, 1, (short)mount.getType().getTypeId()); //FIXME deprecated -> ersetzen
            im = spawnerEgg.getItemMeta();
            name = variant.name();
            im.setDisplayName(name);
            lore = new ArrayList<String>();

            lore.add(variant.name() + "|" + color.name() + "|" + style.name()); // only lore is save from modification by players (with anvil). Therefore save this also in lore.

            lore.add(Math.ceil(maxHealth) + "|" + Math.ceil(currHealth) + "|" + String.valueOf((double)Math.round(speed * 100000) / 100000) + "|" +
                  String.valueOf((double)Math.round(jumpStrength * 100000) / 100000) + "|" + isSaddled + "|" + armorType + "|" + carriesChest);

            if(null != customName) // mob has custom name (set with NameTag for example)
            {
               lore.add(customName);
            }
            else
            {
               customName = "";
            }

            im.setLore(lore);
            spawnerEgg.setItemMeta(im);

            if(MobShrinker.debug){player.sendMessage(name + " | MaxHP: " + Math.ceil(maxHealth) + " | currHP: " + Math.ceil(currHealth) +
                  " | Speed: " + String.valueOf((double)Math.round(speed * 100000) / 100000) + " | JumpStr: " +
                  String.valueOf((double)Math.round(jumpStrength * 100000) / 100000) + " | saddled: " + isSaddled + " | Armor: " + armorType + " | " + carriesChest + " | " + customName);}
            break;
         case WOLF:
            Wolf wolf = (Wolf)livEnt;

            DyeColor collarColor = wolf.getCollarColor(); 
            customName = wolf.getCustomName(); // may be null (must be set by using a NameTag)

            // create spawner egg which will spawn a wolf with all necessary attributes
            spawnerEgg = new ItemStack(Material.MONSTER_EGG, 1, (short)wolf.getType().getTypeId()); //FIXME deprecated -> ersetzen
            im = spawnerEgg.getItemMeta();
            name = "Wolf";
            im.setDisplayName(name);
            lore = new ArrayList<String>();            

            lore.add(name); // only lore is save from modification by players (with anvil). Therefore save this also in lore.
            lore.add(collarColor.name());

            if(null != customName) // mob has custom name (set with NameTag for example)
            {
               lore.add(customName);
            }
            else
            {
               customName = "";
            }

            im.setLore(lore);
            spawnerEgg.setItemMeta(im);

            if(MobShrinker.debug){player.sendMessage(name + " | " + collarColor.name() + " | " + customName);}
            break;
         case OCELOT:
            Ocelot cat = (Ocelot)livEnt;

            Type type = cat.getCatType();
            customName = cat.getCustomName(); // may be null (must be set by using a NameTag)

            // create spawner egg which will spawn a cat with all necessary attributes
            spawnerEgg = new ItemStack(Material.MONSTER_EGG, 1, (short)cat.getType().getTypeId()); //FIXME deprecated -> ersetzen
            im = spawnerEgg.getItemMeta();
            name = "Cat";
            im.setDisplayName(name);
            lore = new ArrayList<String>();

            lore.add("Cat|" + type.name());  // only lore is save from modification by players (with anvil). Therefore save this also in lore.

            if(null != customName) // mob has custom name (set with NameTag for example)
            {               
               lore.add(customName);
            }
            else
            {
               customName = "";
            }

            im.setLore(lore);
            spawnerEgg.setItemMeta(im);

            if(MobShrinker.debug){player.sendMessage(type.name() + " - " + customName);}
            break;
         default: //FIXME deprecated -> ersetzen
            spawnerEgg = new ItemStack(Material.MONSTER_EGG, 1, (short)livEnt.getType().getTypeId()); // pack animal into egg without storing special attributes
         }
      }

      return spawnerEgg;
   }

   // FIXME Use Bukkit API instead of CraftBukkit calls for this, as soon as it is available!
   public double getHorseSpeed(Horse h)
   {   
      AttributeInstance attributes = ((EntityInsentient)((CraftLivingEntity)h).getHandle()).getAttributeInstance(GenericAttributes.d);
      return attributes.getValue();
   }
}


