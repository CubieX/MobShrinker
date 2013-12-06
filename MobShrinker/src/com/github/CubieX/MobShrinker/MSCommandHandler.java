package com.github.CubieX.MobShrinker;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MSCommandHandler implements CommandExecutor
{
   private MobShrinker plugin = null;
   private MSConfigHandler cHandler = null;

   public MSCommandHandler(MobShrinker plugin, MSConfigHandler cHandler) 
   {
      this.plugin = plugin;
      this.cHandler = cHandler;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
   {  
      Player player = null;

      if (sender instanceof Player) 
      {
         player = (Player) sender;
      }

      if (cmd.getName().equalsIgnoreCase("ms"))
      {
         if (args.length == 0)
         { //no arguments, so help will be displayed
            return false;
         }
         else if(args.length==1)
         {
            if (args[0].equalsIgnoreCase("version"))
            {               
               sender.sendMessage(ChatColor.GREEN + "This server is running " + plugin.getDescription().getName() + " version " + plugin.getDescription().getVersion());
               return true;
            }

            if (args[0].equalsIgnoreCase("reload"))
            {
               if(sender.isOp() || sender.hasPermission("mobshrinker.admin"))
               {                        
                  cHandler.reloadConfig(sender);                  
               }
               else
               {
                  sender.sendMessage(ChatColor.RED + "You do not have sufficient permission to reload " + plugin.getDescription().getName() + "!");
               }
               
               return true;
            }

            if (args[0].equalsIgnoreCase("activate"))
            {
               if(sender.isOp() || sender.hasPermission("mobshrinker.admin"))
               {
                  if(!MobShrinker.isActive)
                  {
                     MobShrinker.isActive = true;
                     sender.sendMessage(MobShrinker.logPrefix + " ist jetzt " + ChatColor.GREEN + " AKTIVIERT!");
                  }
                  else
                  {
                     sender.sendMessage(MobShrinker.logPrefix + " ist bereits aktiviert.");
                  }
               }
               else
               {
                  sender.sendMessage(ChatColor.RED + "Du hast keine Berechtigung um " + MobShrinker.logPrefix + "zu aktivieren!");
               }
               
               return true;
            }

            if (args[0].equalsIgnoreCase("deactivate"))
            {
               if(sender.isOp() || sender.hasPermission("mobshrinker.admin"))
               {
                  if(MobShrinker.isActive)
                  {
                     MobShrinker.isActive = false;
                     sender.sendMessage(MobShrinker.logPrefix + " ist jetzt " + ChatColor.RED + " DEAKTIVIERT!");
                  }
                  else
                  {
                     sender.sendMessage(MobShrinker.logPrefix + " ist bereits deaktiviert.");
                  }
               }
               else
               {
                  sender.sendMessage(ChatColor.RED + "Du hast keine Berechtigung um " + MobShrinker.logPrefix + "zu deaktivieren!");
               }
               
               return true;
            }        

            if (args[0].equalsIgnoreCase("status"))
            {
               if(sender.isOp() || sender.hasPermission("mobshrinker.use"))
               {
                  if(MobShrinker.isActive)
                  {
                     sender.sendMessage(MobShrinker.logPrefix + " ist momentan " + ChatColor.GREEN + " AKTIVIERT.");
                  }
                  else
                  {
                     sender.sendMessage(MobShrinker.logPrefix + " ist momentan " + ChatColor.RED + " DEAKTIVIERT.");
                  }
               }

               return true;
            }

            // ##################################################################################
            if(!MobShrinker.isActive)
            {
               sender.sendMessage(MobShrinker.logPrefix + " ist momentan " + ChatColor.RED + " DEAKTIVIERT.");
               return true;            
            }
            // ##################################################################################

            // add commands here (will not be executed if !isActive())

         }
         else
         {
            sender.sendMessage(ChatColor.YELLOW + "Falsche Parameteranzahl.");
         }         
      }
      return false;
   }
   // ##########################################################################


}
