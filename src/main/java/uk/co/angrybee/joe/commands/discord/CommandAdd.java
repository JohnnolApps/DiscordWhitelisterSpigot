package uk.co.angrybee.joe.commands.discord;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import uk.co.angrybee.joe.AuthorPermissions;
import uk.co.angrybee.joe.DiscordClient;
import uk.co.angrybee.joe.DiscordWhitelister;
import uk.co.angrybee.joe.shaded.net.dv8tion.jda.api.EmbedBuilder;
import uk.co.angrybee.joe.shaded.net.dv8tion.jda.api.entities.Member;
import uk.co.angrybee.joe.shaded.net.dv8tion.jda.api.entities.MessageEmbed;
import uk.co.angrybee.joe.shaded.net.dv8tion.jda.api.entities.Role;
import uk.co.angrybee.joe.shaded.net.dv8tion.jda.api.entities.User;
import uk.co.angrybee.joe.shaded.net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import uk.co.angrybee.joe.shaded.net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import uk.co.angrybee.joe.stores.InGameRemovedList;
import uk.co.angrybee.joe.stores.RemovedList;
import uk.co.angrybee.joe.stores.UserList;
import uk.co.angrybee.joe.stores.WhitelistedPlayers;

public class CommandAdd {
   public static void ExecuteCommand(SlashCommandInteractionEvent event, String mc_user, Member target) {
      AuthorPermissions authorPermissions = new AuthorPermissions(event);
      User author = event.getUser();
      TextChannel channel = event.getChannel().asTextChannel();
      Member member = event.getMember();
      int timesWhitelisted = 0;
      char[] finalNameToWhitelistChar = mc_user.toLowerCase().toCharArray();
      boolean onlyHasLimitedAdd = false;
      if (DiscordClient.usernameValidation) {
         for(char c : finalNameToWhitelistChar) {
            if ((new String(DiscordClient.validCharacters)).indexOf(c) == -1) {
               EmbedBuilder embedBuilderInvalidChar;
               if (!DiscordWhitelister.useCustomMessages) {
                  embedBuilderInvalidChar = DiscordClient.CreateEmbeddedMessage("Invalid Username", author.getAsMention() + ", the username you have specified contains invalid characters. **Only letters, numbers and underscores are allowed**.", DiscordClient.EmbedMessageType.FAILURE);
               } else {
                  String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("invalid-characters-warning-title");
                  String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("invalid-characters-warning");
                  customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                  embedBuilderInvalidChar = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.FAILURE);
               }

               DiscordClient.ReplyAndRemoveAfterSeconds(event, embedBuilderInvalidChar.build());
               return;
            }
         }

         if (mc_user.length() < 3 || mc_user.length() > 16) {
            EmbedBuilder embedBuilderLengthInvalid;
            if (!DiscordWhitelister.useCustomMessages) {
               embedBuilderLengthInvalid = DiscordClient.CreateEmbeddedMessage("Invalid Username", author.getAsMention() + ", the username you have specified either contains too few or too many characters. **Usernames can only consist of 3-16 characters**.", DiscordClient.EmbedMessageType.FAILURE);
            } else {
               String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("invalid-length-warning-title");
               String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("invalid-length-warning");
               customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
               embedBuilderLengthInvalid = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.FAILURE);
            }

            DiscordClient.ReplyAndRemoveAfterSeconds(event, embedBuilderLengthInvalid.build());
            return;
         }
      }

      if (!authorPermissions.isUserCanAddRemove() && !authorPermissions.isUserCanAdd()) {
         if (!authorPermissions.isUserHasLimitedAdd() || !DiscordClient.limitedWhitelistEnabled) {
            DiscordClient.ReplyAndRemoveAfterSeconds(event, DiscordClient.CreateInsufficientPermsMessage(author));
            return;
         }

         onlyHasLimitedAdd = true;
         if (DiscordWhitelister.useOnBanEvents && authorPermissions.isUserIsBanned()) {
            if (!DiscordWhitelister.useCustomMessages) {
               MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage("You have been banned!", author.getAsMention() + ", you cannot use this bot as you have been banned!", DiscordClient.EmbedMessageType.FAILURE).build();
               DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);
            } else {
               String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("banned-title");
               String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("banned-message");
               customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
               MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.FAILURE).build();
               DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);
            }

            return;
         }

         if (UserList.getUserList().getString(author.getId()) == null) {
            UserList.getUserList().set(author.getId(), new ArrayList());
            UserList.SaveStore();
         }

         boolean usedAllWhitelists = false;

         try {
            usedAllWhitelists = UserList.getRegisteredUsersCount(author.getId()) >= DiscordClient.maxWhitelistAmount && !authorPermissions.isUserCanAddRemove() && !authorPermissions.isUserCanAdd();
         } catch (NullPointerException exception) {
            exception.printStackTrace();
         }

         timesWhitelisted = UserList.getRegisteredUsersCount(author.getId());
         if (timesWhitelisted > DiscordClient.maxWhitelistAmount) {
            timesWhitelisted = DiscordClient.maxWhitelistAmount;
         }

         if (usedAllWhitelists) {
            if (!DiscordWhitelister.useCustomMessages) {
               String var81 = author.getAsMention();
               MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage("No Whitelists Remaining", var81 + ", unable to whitelist. You have **" + (DiscordClient.maxWhitelistAmount - timesWhitelisted) + " out of " + DiscordClient.maxWhitelistAmount + "** whitelists remaining.", DiscordClient.EmbedMessageType.INFO).build();
               DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);
            } else {
               String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("no-whitelists-remaining-title");
               String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("no-whitelists-remaining");
               customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
               customMessage = customMessage.replaceAll("\\{RemainingWhitelists}", String.valueOf(DiscordClient.maxWhitelistAmount - timesWhitelisted));
               customMessage = customMessage.replaceAll("\\{MaxWhitelistAmount}", String.valueOf(DiscordClient.maxWhitelistAmount));
               MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.INFO).build();
               DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);
            }

            return;
         }

         Logger var72 = DiscordWhitelister.getPlugin().getLogger();
         String var75 = author.getName();
         var72.info(var75 + "(" + author.getId() + ") attempted to whitelist: " + mc_user + ", " + (DiscordClient.maxWhitelistAmount - timesWhitelisted) + " whitelists remaining");
      } else {
         if (UserList.getUserList().getString(target.getId()) == null) {
            UserList.getUserList().set(target.getId(), new ArrayList());
            UserList.SaveStore();
         }

         Logger var10000 = DiscordWhitelister.getPlugin().getLogger();
         String var10001 = author.getName();
         var10000.info(var10001 + "(" + author.getId() + ") attempted to whitelist: " + mc_user);
      }

      // Check if player has logged into the server before (Applies only when regular users link/whitelist themselves)
      if (onlyHasLimitedAdd) {
         OfflinePlayer checkedPlayer = Bukkit.getOfflinePlayer(mc_user);
         if (!checkedPlayer.hasPlayedBefore()) {
            EmbedBuilder embedBuilderNotPlayed = DiscordClient.CreateEmbeddedMessage(
               "Connection Required", 
               author.getAsMention() + ", you cannot whitelist `" + mc_user + "`. **You must have been on the server at one point** before you can register.", 
               DiscordClient.EmbedMessageType.FAILURE
            );
            DiscordClient.AddWhitelistRemainingCount(embedBuilderNotPlayed, timesWhitelisted);
            DiscordClient.ReplyAndRemoveAfterSeconds(event, embedBuilderNotPlayed.build());
            return;
         }
      }

      boolean alreadyOnWhitelist = false;
      if (WhitelistedPlayers.usingEasyWhitelist) {
         if (WhitelistedPlayers.CheckForPlayerEasyWhitelist(mc_user)) {
            alreadyOnWhitelist = true;
         }
      } else if (WhitelistedPlayers.CheckForPlayer(mc_user)) {
         alreadyOnWhitelist = true;
      }

      if (alreadyOnWhitelist) {
         if (!DiscordWhitelister.useCustomMessages) {
            String var80 = author.getAsMention();
            MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage("User already on the whitelist", var80 + ", cannot add user as `" + mc_user + "` is already on the whitelist!", DiscordClient.EmbedMessageType.INFO).build();
            DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);
         } else {
            String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("already-on-whitelist-title");
            String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("already-on-whitelist");
            customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
            customMessage = customMessage.replaceAll("\\{MinecraftUsername}", mc_user);
            MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.INFO).build();
            DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);
         }

      } else {
         if (RemovedList.CheckStoreForPlayer(mc_user)) {
            if (onlyHasLimitedAdd) {
               EmbedBuilder embedBuilderRemovedByStaff;
               if (!DiscordWhitelister.useCustomMessages) {
                  String var79 = author.getAsMention();
                  embedBuilderRemovedByStaff = DiscordClient.CreateEmbeddedMessage("This user was previously removed by a staff member", var79 + ", this user was previously removed by a staff member (<@" + String.valueOf(RemovedList.getRemovedPlayers().get(mc_user.toLowerCase())) + ">).\nPlease ask a user with higher permissions to add this user.\n", DiscordClient.EmbedMessageType.FAILURE);
               } else {
                  String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("user-was-removed-title");
                  String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("user-was-removed");
                  FileConfiguration var74 = RemovedList.getRemovedPlayers();
                  String staffMemberMention = "<@" + String.valueOf(var74.get(mc_user.toLowerCase())) + ">";
                  customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                  customMessage = customMessage.replaceAll("\\{StaffMember}", staffMemberMention);
                  embedBuilderRemovedByStaff = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.FAILURE);
               }

               DiscordClient.AddWhitelistRemainingCount(embedBuilderRemovedByStaff, timesWhitelisted);
               DiscordClient.ReplyAndRemoveAfterSeconds(event, embedBuilderRemovedByStaff.build());
               return;
            }

            RemovedList.getRemovedPlayers().set(mc_user.toLowerCase(), (Object)null);
            RemovedList.SaveStore();
            DiscordWhitelister.getPlugin().getLogger().info(mc_user + " has been removed from the removed list by " + author.getName() + "(" + author.getId() + ")");
         }

         if (DiscordWhitelister.useInGameAddRemoves && InGameRemovedList.CheckStoreForPlayer(mc_user.toLowerCase())) {
            if (onlyHasLimitedAdd) {
               EmbedBuilder embedBuilderRemovedByInGameStaff;
               if (!DiscordWhitelister.useCustomMessages) {
                  String var78 = author.getAsMention();
                  embedBuilderRemovedByInGameStaff = DiscordClient.CreateEmbeddedMessage("This user was previously removed by a staff member", var78 + ", this user was previously removed by a staff member in-game (" + String.valueOf(InGameRemovedList.getRemovedPlayers().get(mc_user.toLowerCase())) + ").\nPlease ask a user with higher permissions to add this user.\n", DiscordClient.EmbedMessageType.FAILURE);
               } else {
                  String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("user-was-removed-in-game-title");
                  String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("user-was-removed-in-game");
                  String inGameStaffMember = InGameRemovedList.getRemovedPlayers().getString(mc_user.toLowerCase());
                  customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                  customMessage = customMessage.replaceAll("\\{StaffMember}", inGameStaffMember);
                  embedBuilderRemovedByInGameStaff = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.FAILURE);
               }

               DiscordClient.AddWhitelistRemainingCount(embedBuilderRemovedByInGameStaff, timesWhitelisted);
               DiscordClient.ReplyAndRemoveAfterSeconds(event, embedBuilderRemovedByInGameStaff.build());
               return;
            }

            InGameRemovedList.RemoveUserFromStore(mc_user.toLowerCase());
            DiscordWhitelister.getPlugin().getLogger().info(mc_user + " has been removed from in-game-removed-list.yml by " + author.getName() + "(" + author.getId() + ")");
         }

         String playerUUID = DiscordClient.minecraftUsernameToUUID(mc_user);
         boolean invalidMinecraftName = playerUUID == null;
         EmbedBuilder embedBuilderWhitelistSuccess;
         if (!DiscordWhitelister.useCustomMessages) {
            String var73 = mc_user + " is now whitelisted!";
            String var76 = author.getAsMention();
            embedBuilderWhitelistSuccess = DiscordClient.CreateEmbeddedMessage(var73, var76 + " has added `" + mc_user + "` to the whitelist.", DiscordClient.EmbedMessageType.SUCCESS);
         } else {
            String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("whitelist-success-title");
            customTitle = customTitle.replaceAll("\\{MinecraftUsername}", mc_user);
            String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("whitelist-success");
            customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
            customMessage = customMessage.replaceAll("\\{MinecraftUsername}", mc_user);
            embedBuilderWhitelistSuccess = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.SUCCESS);
         }

         if (onlyHasLimitedAdd) {
            DiscordClient.AddWhitelistRemainingCount(embedBuilderWhitelistSuccess, timesWhitelisted + 1);
         }

         if (DiscordWhitelister.showPlayerSkin) {
            if (!DiscordWhitelister.mainConfig.getFileConfiguration().getBoolean("use-crafatar-for-avatars")) {
               embedBuilderWhitelistSuccess.setThumbnail("https://minotar.net/armor/bust/" + playerUUID + "/100.png");
            } else {
               embedBuilderWhitelistSuccess.setThumbnail("https://crafatar.com/avatars/" + playerUUID + "?size=100&default=MHF_Steve&overlay.png");
            }
         }

         EmbedBuilder embedBuilderWhitelistFailure;
         if (!DiscordWhitelister.useCustomMessages) {
            String var77 = author.getAsMention();
            embedBuilderWhitelistFailure = DiscordClient.CreateEmbeddedMessage("Failed to whitelist", var77 + ", failed to add `" + mc_user + "` to the whitelist. This is most likely due to an invalid Minecraft username.", DiscordClient.EmbedMessageType.FAILURE);
         } else {
            String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("whitelist-failure-title");
            String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("whitelist-failure");
            customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
            customMessage = customMessage.replaceAll("\\{MinecraftUsername}", mc_user);
            embedBuilderWhitelistFailure = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.FAILURE);
         }

         if (onlyHasLimitedAdd) {
            DiscordClient.AddWhitelistRemainingCount(embedBuilderWhitelistFailure, timesWhitelisted);
         }

         if (onlyHasLimitedAdd && timesWhitelisted < DiscordClient.maxWhitelistAmount) {
            int var67 = timesWhitelisted + 1;
         }

         AtomicBoolean successfulWhitelist = new AtomicBoolean(false);
         if (!WhitelistedPlayers.usingEasyWhitelist && authorPermissions.isUserCanUseCommand()) {
            DiscordClient.ExecuteServerCommand("whitelist add " + mc_user);
         }

         if (DiscordWhitelister.mainConfig.getFileConfiguration().getBoolean("use-geyser/floodgate-compatibility")) {
            addBedrockUser(mc_user);
         }

         if (WhitelistedPlayers.usingEasyWhitelist && !invalidMinecraftName && authorPermissions.isUserCanUseCommand()) {
            DiscordClient.ExecuteServerCommand("easywl add " + mc_user);
         }

         DiscordWhitelister.getPlugin().getServer().getScheduler().callSyncMethod(DiscordWhitelister.getPlugin(), () -> {
            if (WhitelistedPlayers.usingEasyWhitelist && !invalidMinecraftName && WhitelistedPlayers.CheckForPlayerEasyWhitelist(mc_user) || !WhitelistedPlayers.usingEasyWhitelist && WhitelistedPlayers.CheckForPlayer(mc_user)) {
               event.replyEmbeds(embedBuilderWhitelistSuccess.build(), new MessageEmbed[0]).queue();
               successfulWhitelist.set(true);
               DiscordClient.AssignPerms(mc_user);
               if (DiscordWhitelister.useOnWhitelistCommands) {
                  for(String command : DiscordWhitelister.onWhitelistCommandsConfig.getFileConfiguration().getStringList("on-whitelist-commands")) {
                     DiscordClient.CheckAndExecuteCommand(command, mc_user);
                  }
               }

               if (DiscordClient.whitelistedRoleAutoAdd) {
                  List<Role> whitelistRoles = new LinkedList();

                  try {
                     if (!DiscordWhitelister.useIdForRoles) {
                        for(String whitelistedRoleName : DiscordClient.whitelistedRoleNames) {
                           List<Role> rolesFoundWithName = channel.getGuild().getRolesByName(whitelistedRoleName, false);
                           whitelistRoles.addAll(rolesFoundWithName);
                        }
                     } else {
                        for(String whitelistedRoleName : DiscordClient.whitelistedRoleNames) {
                           if (channel.getGuild().getRoleById(whitelistedRoleName) != null) {
                              whitelistRoles.add(channel.getGuild().getRoleById(whitelistedRoleName));
                           }
                        }
                     }

                     if (!whitelistRoles.isEmpty()) {
                        whitelistRoles.forEach((role) -> member.getGuild().addRoleToMember(target, role).queue());
                     }
                  } catch (Exception e) {
                     DiscordWhitelister.getPlugin().getLogger().severe("Could not add role with name/id to " + target.getEffectiveName() + ", check the config and that the bot has the Manage Roles permission");
                     e.printStackTrace();
                  }

                  if (successfulWhitelist.get() && DiscordWhitelister.mainConfig.getFileConfiguration().getBoolean("send-instructional-message-on-whitelist")) {
                     if (!DiscordWhitelister.mainConfig.getFileConfiguration().getBoolean("use-timer-for-instructional-message")) {
                        event.replyEmbeds(DiscordClient.CreateInstructionalMessage(), new MessageEmbed[0]).queue();
                     } else {
                        int waitTime = DiscordWhitelister.mainConfig.getFileConfiguration().getInt("timer-wait-time-in-seconds");
                        Thread whitelisterTimerThread = new Thread(() -> {
                           try {
                              TimeUnit.SECONDS.sleep((long)waitTime);
                              event.replyEmbeds(DiscordClient.CreateInstructionalMessage(), new MessageEmbed[0]).queue();
                           } catch (InterruptedException e) {
                              e.printStackTrace();
                           }

                        });
                        whitelisterTimerThread.start();
                     }
                  }
               }

               UserList.addRegisteredUser(target.getId(), mc_user.toLowerCase());
               Logger var10000 = DiscordWhitelister.getPluginLogger();
               String var10001 = author.getName();
               var10000.info(var10001 + "(" + author.getId() + ") successfully added " + mc_user + " to the whitelist and linked " + mc_user + " to " + target.getEffectiveName() + "(" + target.getId() + ").");
            } else {
               DiscordClient.ReplyAndRemoveAfterSeconds(event, embedBuilderWhitelistFailure.build());
            }

            return null;
         });
      }
   }

   public static void ExecuteCommand(SlashCommandInteractionEvent event, String mc_user) {
      AuthorPermissions authorPermissions = new AuthorPermissions(event);
      User author = event.getUser();
      TextChannel channel = event.getChannel().asTextChannel();
      Member member = event.getMember();
      int timesWhitelisted = 0;
      char[] finalNameToWhitelistChar = mc_user.toLowerCase().toCharArray();
      boolean onlyHasLimitedAdd = false;
      if (DiscordClient.usernameValidation) {
         for(char c : finalNameToWhitelistChar) {
            if ((new String(DiscordClient.validCharacters)).indexOf(c) == -1) {
               EmbedBuilder embedBuilderInvalidChar;
               if (!DiscordWhitelister.useCustomMessages) {
                  embedBuilderInvalidChar = DiscordClient.CreateEmbeddedMessage("Invalid Username", author.getAsMention() + ", the username you have specified contains invalid characters. **Only letters, numbers and underscores are allowed**.", DiscordClient.EmbedMessageType.FAILURE);
               } else {
                  String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("invalid-characters-warning-title");
                  String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("invalid-characters-warning");
                  customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                  embedBuilderInvalidChar = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.FAILURE);
               }

               DiscordClient.ReplyAndRemoveAfterSeconds(event, embedBuilderInvalidChar.build());
               return;
            }
         }

         if (mc_user.length() < 3 || mc_user.length() > 16) {
            EmbedBuilder embedBuilderLengthInvalid;
            if (!DiscordWhitelister.useCustomMessages) {
               embedBuilderLengthInvalid = DiscordClient.CreateEmbeddedMessage("Invalid Username", author.getAsMention() + ", the username you have specified either contains too few or too many characters. **Usernames can only consist of 3-16 characters**.", DiscordClient.EmbedMessageType.FAILURE);
            } else {
               String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("invalid-length-warning-title");
               String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("invalid-length-warning");
               customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
               embedBuilderLengthInvalid = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.FAILURE);
            }

            DiscordClient.ReplyAndRemoveAfterSeconds(event, embedBuilderLengthInvalid.build());
            return;
         }
      }

      if (!authorPermissions.isUserCanAddRemove() && !authorPermissions.isUserCanAdd()) {
         if (!authorPermissions.isUserHasLimitedAdd() || !DiscordClient.limitedWhitelistEnabled) {
            DiscordClient.ReplyAndRemoveAfterSeconds(event, DiscordClient.CreateInsufficientPermsMessage(author));
            return;
         }

         onlyHasLimitedAdd = true;
         if (DiscordWhitelister.useOnBanEvents && authorPermissions.isUserIsBanned()) {
            if (!DiscordWhitelister.useCustomMessages) {
               MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage("You have been banned!", author.getAsMention() + ", you cannot use this bot as you have been banned!", DiscordClient.EmbedMessageType.FAILURE).build();
               DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);
            } else {
               String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("banned-title");
               String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("banned-message");
               customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
               MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.FAILURE).build();
               DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);
            }

            return;
         }

         if (UserList.getUserList().getString(author.getId()) == null) {
            UserList.getUserList().set(author.getId(), new ArrayList());
            UserList.SaveStore();
         }

         boolean usedAllWhitelists = false;

         try {
            usedAllWhitelists = UserList.getRegisteredUsersCount(author.getId()) >= DiscordClient.maxWhitelistAmount && !authorPermissions.isUserCanAddRemove() && !authorPermissions.isUserCanAdd();
         } catch (NullPointerException exception) {
            exception.printStackTrace();
         }

         timesWhitelisted = UserList.getRegisteredUsersCount(author.getId());
         if (timesWhitelisted > DiscordClient.maxWhitelistAmount) {
            timesWhitelisted = DiscordClient.maxWhitelistAmount;
         }

         if (usedAllWhitelists) {
            if (!DiscordWhitelister.useCustomMessages) {
               String var80 = author.getAsMention();
               MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage("No Whitelists Remaining", var80 + ", unable to whitelist. You have **" + (DiscordClient.maxWhitelistAmount - timesWhitelisted) + " out of " + DiscordClient.maxWhitelistAmount + "** whitelists remaining.", DiscordClient.EmbedMessageType.INFO).build();
               DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);
            } else {
               String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("no-whitelists-remaining-title");
               String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("no-whitelists-remaining");
               customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
               customMessage = customMessage.replaceAll("\\{RemainingWhitelists}", String.valueOf(DiscordClient.maxWhitelistAmount - timesWhitelisted));
               customMessage = customMessage.replaceAll("\\{MaxWhitelistAmount}", String.valueOf(DiscordClient.maxWhitelistAmount));
               MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.INFO).build();
               DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);
            }

            return;
         }

         Logger var71 = DiscordWhitelister.getPlugin().getLogger();
         String var74 = author.getName();
         var71.info(var74 + "(" + author.getId() + ") attempted to whitelist: " + mc_user + ", " + (DiscordClient.maxWhitelistAmount - timesWhitelisted) + " whitelists remaining");
      } else {
         Logger var10000 = DiscordWhitelister.getPlugin().getLogger();
         String var10001 = author.getName();
         var10000.info(var10001 + "(" + author.getId() + ") attempted to whitelist: " + mc_user);
      }

      // Check if player has logged into the server before (Applies only when regular users link/whitelist themselves)
      if (onlyHasLimitedAdd) {
         OfflinePlayer checkedPlayer = Bukkit.getOfflinePlayer(mc_user);
         if (!checkedPlayer.hasPlayedBefore()) {
            EmbedBuilder embedBuilderNotPlayed = DiscordClient.CreateEmbeddedMessage(
               "Connection Required", 
               author.getAsMention() + ", you cannot whitelist `" + mc_user + "`. **You must have been on the server at one point** before you can register.", 
               DiscordClient.EmbedMessageType.FAILURE
            );
            DiscordClient.AddWhitelistRemainingCount(embedBuilderNotPlayed, timesWhitelisted);
            DiscordClient.ReplyAndRemoveAfterSeconds(event, embedBuilderNotPlayed.build());
            return;
         }
      }

      boolean alreadyOnWhitelist = false;
      if (WhitelistedPlayers.usingEasyWhitelist) {
         if (WhitelistedPlayers.CheckForPlayerEasyWhitelist(mc_user)) {
            alreadyOnWhitelist = true;
         }
      } else if (WhitelistedPlayers.CheckForPlayer(mc_user)) {
         alreadyOnWhitelist = true;
      }

      if (alreadyOnWhitelist) {
         if (!DiscordWhitelister.useCustomMessages) {
            String var79 = author.getAsMention();
            MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage("User already on the whitelist", var79 + ", cannot add user as `" + mc_user + "` is already on the whitelist!", DiscordClient.EmbedMessageType.INFO).build();
            DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);
         } else {
            String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("already-on-whitelist-title");
            String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("already-on-whitelist");
            customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
            customMessage = customMessage.replaceAll("\\{MinecraftUsername}", mc_user);
            MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.INFO).build();
            DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);
         }

      } else {
         if (RemovedList.CheckStoreForPlayer(mc_user)) {
            if (onlyHasLimitedAdd) {
               EmbedBuilder embedBuilderRemovedByStaff;
               if (!DiscordWhitelister.useCustomMessages) {
                  String var78 = author.getAsMention();
                  embedBuilderRemovedByStaff = DiscordClient.CreateEmbeddedMessage("This user was previously removed by a staff member", var78 + ", this user was previously removed by a staff member (<@" + String.valueOf(RemovedList.getRemovedPlayers().get(mc_user.toLowerCase())) + ">).\nPlease ask a user with higher permissions to add this user.\n", DiscordClient.EmbedMessageType.FAILURE);
               } else {
                  String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("user-was-removed-title");
                  String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("user-was-removed");
                  FileConfiguration var73 = RemovedList.getRemovedPlayers();
                  String staffMemberMention = "<@" + String.valueOf(var73.get(mc_user.toLowerCase())) + ">";
                  customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                  customMessage = customMessage.replaceAll("\\{StaffMember}", staffMemberMention);
                  embedBuilderRemovedByStaff = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.FAILURE);
               }

               DiscordClient.AddWhitelistRemainingCount(embedBuilderRemovedByStaff, timesWhitelisted);
               DiscordClient.ReplyAndRemoveAfterSeconds(event, embedBuilderRemovedByStaff.build());
               return;
            }

            RemovedList.getRemovedPlayers().set(mc_user.toLowerCase(), (Object)null);
            RemovedList.SaveStore();
            DiscordWhitelister.getPlugin().getLogger().info(mc_user + " has been removed from the removed list by " + author.getName() + "(" + author.getId() + ")");
         }

         if (DiscordWhitelister.useInGameAddRemoves && InGameRemovedList.CheckStoreForPlayer(mc_user.toLowerCase())) {
            if (onlyHasLimitedAdd) {
               EmbedBuilder embedBuilderRemovedByInGameStaff;
               if (!DiscordWhitelister.useCustomMessages) {
                  String var77 = author.getAsMention();
                  embedBuilderRemovedByInGameStaff = DiscordClient.CreateEmbeddedMessage("This user was previously removed by a staff member", var77 + ", this user was previously removed by a staff member in-game (" + String.valueOf(InGameRemovedList.getRemovedPlayers().get(mc_user.toLowerCase())) + ").\nPlease ask a user with higher permissions to add this user.\n", DiscordClient.EmbedMessageType.FAILURE);
               } else {
                  String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("user-was-removed-in-game-title");
                  String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("user-was-removed-in-game");
                  String inGameStaffMember = InGameRemovedList.getRemovedPlayers().getString(mc_user.toLowerCase());
                  customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                  customMessage = customMessage.replaceAll("\\{StaffMember}", inGameStaffMember);
                  embedBuilderRemovedByInGameStaff = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.FAILURE);
               }

               DiscordClient.AddWhitelistRemainingCount(embedBuilderRemovedByInGameStaff, timesWhitelisted);
               DiscordClient.ReplyAndRemoveAfterSeconds(event, embedBuilderRemovedByInGameStaff.build());
               return;
            }

            InGameRemovedList.RemoveUserFromStore(mc_user.toLowerCase());
            DiscordWhitelister.getPlugin().getLogger().info(mc_user + " has been removed from in-game-removed-list.yml by " + author.getName() + "(" + author.getId() + ")");
         }

         String playerUUID = DiscordClient.minecraftUsernameToUUID(mc_user);
         boolean invalidMinecraftName = playerUUID == null;
         EmbedBuilder embedBuilderWhitelistSuccess;
         if (!DiscordWhitelister.useCustomMessages) {
            String var72 = mc_user + " is now whitelisted!";
            String var75 = author.getAsMention();
            embedBuilderWhitelistSuccess = DiscordClient.CreateEmbeddedMessage(var72, var75 + " has added `" + mc_user + "` to the whitelist.", DiscordClient.EmbedMessageType.SUCCESS);
         } else {
            String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("whitelist-success-title");
            customTitle = customTitle.replaceAll("\\{MinecraftUsername}", mc_user);
            String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("whitelist-success");
            customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
            customMessage = customMessage.replaceAll("\\{MinecraftUsername}", mc_user);
            embedBuilderWhitelistSuccess = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.SUCCESS);
         }

         if (onlyHasLimitedAdd) {
            DiscordClient.AddWhitelistRemainingCount(embedBuilderWhitelistSuccess, timesWhitelisted + 1);
         }

         if (DiscordWhitelister.showPlayerSkin) {
            if (!DiscordWhitelister.mainConfig.getFileConfiguration().getBoolean("use-crafatar-for-avatars")) {
               embedBuilderWhitelistSuccess.setThumbnail("https://minotar.net/armor/bust/" + playerUUID + "/100.png");
            } else {
               embedBuilderWhitelistSuccess.setThumbnail("https://crafatar.com/avatars/" + playerUUID + "?size=100&default=MHF_Steve&overlay.png");
            }
         }

         EmbedBuilder embedBuilderWhitelistFailure;
         if (!DiscordWhitelister.useCustomMessages) {
            String var76 = author.getAsMention();
            embedBuilderWhitelistFailure = DiscordClient.CreateEmbeddedMessage("Failed to whitelist", var76 + ", failed to add `" + mc_user + "` to the whitelist. This is most likely due to an invalid Minecraft username.", DiscordClient.EmbedMessageType.FAILURE);
         } else {
            String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("whitelist-failure-title");
            String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("whitelist-failure");
            customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
            customMessage = customMessage.replaceAll("\\{MinecraftUsername}", mc_user);
            embedBuilderWhitelistFailure = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.FAILURE);
         }

         if (onlyHasLimitedAdd) {
            DiscordClient.AddWhitelistRemainingCount(embedBuilderWhitelistFailure, timesWhitelisted);
         }

         int tempTimesWhitelisted = timesWhitelisted;
         if (onlyHasLimitedAdd && timesWhitelisted < DiscordClient.maxWhitelistAmount) {
            tempTimesWhitelisted = timesWhitelisted + 1;
         }

         AtomicBoolean successfulWhitelist = new AtomicBoolean(false);
         if (!WhitelistedPlayers.usingEasyWhitelist && authorPermissions.isUserCanUseCommand()) {
            DiscordClient.ExecuteServerCommand("whitelist add " + mc_user);
         }

         if (DiscordWhitelister.mainConfig.getFileConfiguration().getBoolean("use-geyser/floodgate-compatibility")) {
            addBedrockUser(mc_user);
         }

         if (WhitelistedPlayers.usingEasyWhitelist && !invalidMinecraftName && authorPermissions.isUserCanUseCommand()) {
            DiscordClient.ExecuteServerCommand("easywl add " + mc_user);
         }

         DiscordWhitelister.getPlugin().getServer().getScheduler().callSyncMethod(DiscordWhitelister.getPlugin(), () -> {
            if (WhitelistedPlayers.usingEasyWhitelist && !invalidMinecraftName && WhitelistedPlayers.CheckForPlayerEasyWhitelist(mc_user) || !WhitelistedPlayers.usingEasyWhitelist && WhitelistedPlayers.CheckForPlayer(mc_user)) {
               event.replyEmbeds(embedBuilderWhitelistSuccess.build(), new MessageEmbed[0]).queue();
               successfulWhitelist.set(true);
               DiscordClient.AssignPerms(mc_user);
               if (DiscordWhitelister.useOnWhitelistCommands) {
                  for(String command : DiscordWhitelister.onWhitelistCommandsConfig.getFileConfiguration().getStringList("on-whitelist-commands")) {
                     DiscordClient.CheckAndExecuteCommand(command, mc_user);
                  }
               }

               if (DiscordClient.whitelistedRoleAutoAdd) {
                  List<Role> whitelistRoles = new LinkedList();

                  try {
                     if (!DiscordWhitelister.useIdForRoles) {
                        for(String whitelistedRoleName : DiscordClient.whitelistedRoleNames) {
                           List<Role> rolesFoundWithName = channel.getGuild().getRolesByName(whitelistedRoleName, false);
                           whitelistRoles.addAll(rolesFoundWithName);
                        }
                     } else {
                        for(String whitelistedRoleName : DiscordClient.whitelistedRoleNames) {
                           if (channel.getGuild().getRoleById(whitelistedRoleName) != null) {
                              whitelistRoles.add(channel.getGuild().getRoleById(whitelistedRoleName));
                           }
                        }
                     }

                     if (!whitelistRoles.isEmpty()) {
                        whitelistRoles.forEach((role) -> member.getGuild().addRoleToMember(member, role).queue());
                     }
                  } catch (Exception e) {
                     DiscordWhitelister.getPlugin().getLogger().severe("Could not add role with name/id to " + author.getName() + ", check the config and that the bot has the Manage Roles permission");
                     e.printStackTrace();
                  }

                  if (successfulWhitelist.get() && DiscordWhitelister.mainConfig.getFileConfiguration().getBoolean("send-instructional-message-on-whitelist")) {
                     if (!DiscordWhitelister.mainConfig.getFileConfiguration().getBoolean("use-timer-for-instructional-message")) {
                        channel.sendMessageEmbeds(DiscordClient.CreateInstructionalMessage(), new MessageEmbed[0]).queue();
                     } else {
                        int waitTime = DiscordWhitelister.mainConfig.getFileConfiguration().getInt("timer-wait-time-in-seconds");
                        Thread whitelisterTimerThread = new Thread(() -> {
                           try {
                              TimeUnit.SECONDS.sleep((long)waitTime);
                              channel.sendMessageEmbeds(DiscordClient.CreateInstructionalMessage(), new MessageEmbed[0]).queue();
                           } catch (InterruptedException e) {
                              e.printStackTrace();
                           }

                        });
                        whitelisterTimerThread.start();
                     }
                  }
               }

               if (onlyHasLimitedAdd) {
                  UserList.addRegisteredUser(author.getId(), mc_user.toLowerCase());
                  Logger var10000 = DiscordWhitelister.getPluginLogger();
                  String var10001 = author.getName();
                  var10000.info(var10001 + "(" + author.getId() + ") successfully added " + mc_user + " to the whitelist, " + (DiscordClient.maxWhitelistAmount - tempTimesWhitelisted) + " whitelists remaining.");
               }
            } else {
               DiscordClient.ReplyAndRemoveAfterSeconds(event, embedBuilderWhitelistFailure.build());
            }

            return null;
         });
      }
   }

   private static void addBedrockUser(String finalNameToAdd) {
      String bedrockPrefix = DiscordWhitelister.mainConfig.getFileConfiguration().getString("geyser/floodgate prefix");
      String bedrockName = bedrockPrefix + finalNameToAdd;
      if (bedrockName.length() > 16) {
         bedrockName = bedrockName.substring(0, 16);
      }

      if (finalNameToAdd.length() < bedrockPrefix.length() || !finalNameToAdd.substring(0, bedrockPrefix.length() - 1).equals(bedrockPrefix)) {
         DiscordClient.ExecuteServerCommand("whitelist add " + bedrockName);
      }

   }

   private static boolean checkMcUsername(String nameToCheck) {
      if (DiscordClient.usernameValidation) {
         if (nameToCheck.length() < 3 || nameToCheck.length() > 16) {
            return false;
         }

         for(char c : nameToCheck.toCharArray()) {
            if ((new String(DiscordClient.validCharacters)).indexOf(c) == -1) {
               return false;
            }
         }
      }

      return true;
   }
}
