package net.brian.scriptedquests.api.objectives;

import com.google.gson.Gson;
import me.clip.placeholderapi.PlaceholderAPI;
import net.brian.playerdatasync.PlayerDataSync;
import net.brian.scriptedquests.api.objectives.data.ObjectiveData;
import net.brian.scriptedquests.data.SerializedQuestData;
import net.brian.scriptedquests.data.PlayerQuestDataImpl;
import net.brian.scriptedquests.api.conditions.Condition;
import net.brian.scriptedquests.api.quests.Quest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public abstract class PersistentObjective<T extends ObjectiveData> extends QuestObjective{

    private static final Gson gson = new Gson();

    private Map<UUID,T> cachedData;


    private InstructionQuery<T> instructionQuery;

    public PersistentObjective(Quest quest, String objectiveID, Condition... conditions) {
        super(quest, objectiveID,conditions);
        cachedData = new HashMap<>();
    }

    public abstract Class<T> getDataClass();
    public abstract T newObjectiveData();

    @Override
    public void start(Player player){
        super.start(player);
        T data = newObjectiveData();
        cachedData.put(player.getUniqueId(),data);
        PlayerQuestDataImpl.get(player.getUniqueId()).ifPresent(playerData->{
            playerData.setQuestData(quest.getQuestID(), objectiveID,data.toString());
        });
    }

    @Override
    public void cancel(Player player){
        super.cancel(player);
        cachedData.remove(player.getUniqueId());
    }

    @Override
    public void cancelAll(){
        super.cancelAll();
        cachedData.clear();
    }

    @Override
    public void finish(Player player){
        super.finish(player);
        cachedData.remove(player.getUniqueId());
    }


    public Optional<T> getData(UUID uuid){
        return Optional.ofNullable(cachedData.get(uuid));
    }

    @Override
    public void cachePlayer(Player player,String args){
        super.cachePlayer(player,args);
        if (cachedData == null){
            cachedData = new HashMap<>();
        }
        cachedData.put(player.getUniqueId(),gson.fromJson(args,getDataClass()));
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event){
        T data = cachedData.get(event.getPlayer().getUniqueId());
        if(data != null){
            PlayerQuestDataImpl.get(event.getPlayer().getUniqueId()).ifPresent(playerData->{
                playerData.setQuestData(quest.getQuestID(),objectiveID,data.toString());
            });
        }
    }

    @Override
    public String getInstruction(Player player){
        if(instructionQuery == null){
            return super.getInstruction(player);
        }
        T data = cachedData.get(player.getUniqueId());
        if(data == null) data = newObjectiveData();
        if(data != null){
            return PlaceholderAPI.setPlaceholders(player,instructionQuery.get(data));
        }
        else return super.getInstruction(player);
    }

    public PersistentObjective<T> setInstruction(InstructionQuery<T> instruction){
        this.instructionQuery = instruction;
        return this;
    }

    @Override
    public void unregister(){
        super.unregister();
        cachedData.clear();
    }

    @Override
    protected void removeCache(Player player){
        super.removeCache(player);
        cachedData.remove(player.getUniqueId());
    }
}
