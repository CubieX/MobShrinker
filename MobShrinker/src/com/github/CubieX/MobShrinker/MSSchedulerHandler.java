package com.github.CubieX.MobShrinker;

public class MSSchedulerHandler
{
   private MobShrinker plugin = null;

   public MSSchedulerHandler(MobShrinker plugin)
   {
      this.plugin = plugin;
   }

   public void startPlayerInWaterCheckScheduler_SynchRepeating()
   {
      plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable()
      {
         public void run()
         {
                    
         }
      }, 10 * 20L, 1 * 20L); // 10 seconds delay, 1 second cycle
   }
}
