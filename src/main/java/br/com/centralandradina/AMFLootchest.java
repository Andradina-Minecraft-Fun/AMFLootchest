package br.com.centralandradina;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AMFLootchest extends JavaPlugin implements Listener {

    FileConfiguration config;
    List<FileConfiguration> loots_list = new ArrayList<>();

    @Override
    public void onEnable() {
        // Registra o listenner
        Bukkit.getServer().getPluginManager().registerEvents(this, this);

        // Config
        config = this.getConfig();
        config.addDefault("prefix", "");
        config.options().copyDefaults(true);
        saveConfig();

        // Cria o diretório de loots
        File loots_dir = new File(this.getDataFolder() + "/loots/");
        if(!loots_dir.exists()) {
            loots_dir.mkdirs();
        }
        else {
            // Carrega os loots
            for (File file : new File(getDataFolder(), "loots").listFiles()) {
                String loot_id = file.getName().toLowerCase().replace(".yml", "");
                this.loots_list.add(this.loadLoot(loot_id));
            }
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    /**
     *
     * @param event
     */
    @EventHandler
    public void onSignPlace(SignChangeEvent event)
    {
        Block block = event.getBlock();
        if(event.getLine(0).toLowerCase().equals("[loot]")) {
            if(block.getType().toString().toLowerCase().contains("wall_sign")) {
                BlockFace blockFace = ((Directional) block.getBlockData()).getFacing();
                Block blockBehindSign = null;
                switch (blockFace) {
                    case WEST:
                        blockBehindSign = block.getRelative(BlockFace.EAST);
                        break;
                    case EAST:
                        blockBehindSign = block.getRelative(BlockFace.WEST);
                        break;
                    case NORTH:
                        blockBehindSign = block.getRelative(BlockFace.SOUTH);
                        break;
                    case SOUTH:
                        blockBehindSign = block.getRelative(BlockFace.NORTH);
                        break;
                    default:
                        break;
                }

                if(blockBehindSign.getType().toString().toLowerCase().contains("chest")) {

                    String loot_id = event.getLine(1);

                    if(loot_id.length() > 0) {
                        // Carrega o loot
                        this.loots_list.add(this.loadLoot(loot_id));
                    }
                    else {
                        event.setLine(1, this.color("&4Second Line"));
                        event.setLine(2, this.color("&4Must by the drop list ID"));
                    }

                }
                else {
                    event.setLine(1, this.color("&4Wrong place"));
                    event.setLine(2, this.color("&4Put this sign"));
                    event.setLine(3, this.color("&4on chest"));
                }
            }
            else {
                event.setLine(1, this.color("&4Wrong place"));
                event.setLine(2, this.color("&4Put this sign"));
                event.setLine(3, this.color("&4on chest"));
            }
        }
    }

    /**
     *
     */
    @EventHandler
    public void InventoryClose(InventoryCloseEvent event) {
        if(event.getInventory().getHolder() instanceof Chest) {
            Chest c = (Chest)event.getInventory().getHolder();
            Player p = (Player) event.getPlayer();

            // Recupera a placa em volta
            Sign sign = this.getSignAround(c.getBlock());

            if(sign != null) {
                if(sign.getLine(0).toLowerCase().equals("[loot]")) {
                    c.getInventory().clear();
                }
            }
        }
    }

    /**
     * @todo Fazer funcionar em bau duplo
     * @todo Bau muito proximo um do outro, abre a placa do outro. Verificar realmente a placa esta estacada no bau
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void PlayerInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block b = event.getClickedBlock();
            if (b.getType() == Material.CHEST) {

                // Recupera a placa em volta
                Sign sign = this.getSignAround(b);

                if(sign != null) {

                    if(!sign.getLine(0).toLowerCase().equals("[loot]")) {
                        return;
                    }

                    // Get chest inventory
                    Inventory inv = ((Chest) b.getState()).getInventory();
                    inv.clear();

                    // Recupera o loot
                    String loot_id = sign.getLine(1).toLowerCase();
                    for (FileConfiguration loot : loots_list) {
                        if(loot.getString("name").equals(loot_id)) {

                            // Verifica se pode abrir
                            try {
                                Date last_open = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").parse(loot.getString("last_open"));

                                Integer open_after = loot.getInt("open_after");

                                if(this.addMinutesToDate(last_open, open_after).compareTo(new Date()) > 0) {
                                    p.sendMessage(this.color(config.getString("prefix") + "Este baú só pode ser aberto a cada " + this.minutesToTime(open_after)));
                                    event.setCancelled(true);
                                    return;
                                }

                            }
                            catch(ParseException e) {
                                getLogger().info("Cannot parse last_open date from " + loot_id);
                            }

                            // Percorre os itens
                            ConfigurationSection sec = loot.getConfigurationSection("items");
                            for(String key : sec.getKeys(false)){

                                // Da o parse nos parametros
                                Double chance = loot.getDouble("items." + key + ".chance");
                                if(Math.random() > chance) {
                                    continue;
                                }
                                String item_id = loot.getString("items." + key + ".id");
                                Integer quantity = loot.getInt("items." + key + ".quantity");
                                String name = null;
                                if(loot.isSet("items." + key + ".name")) {
                                    name = this.color(loot.getString("items." + key + ".name"));
                                }
                                List<String> lore = new ArrayList<>();
                                if(loot.isSet("items." + key + ".lore")) {
                                    for(String lore_line : loot.getStringList("items." + key + ".lore")) {
                                        lore.add(this.color(this.parsePlaceholder(lore_line)));
                                    }
                                }

                                // Cria o item
                                Material m = Material.matchMaterial(item_id);
                                ItemStack stack = new ItemStack(m, (int)(Math.random()*quantity)+1);

                                // Configura as metas
                                ItemMeta meta = stack.getItemMeta();
                                meta.setDisplayName(name);
                                meta.setLore(lore);

                                // Adiciona o item
                                stack.setItemMeta(meta);
                                inv.addItem(stack);
                            }

                            // Salva a ultima vez aberto
                            Date now = new Date();
                            SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                            loot.set("last_open", format.format(now));

                            // Salva o arquivo
                            File file = new File(getDataFolder(), "loots" + File.separator + loot_id + ".yml");
                            try {
                                loot.save(file);
                            } catch (IOException e) {
                                getLogger().info(e.getMessage() + "!");
                            }

                            break;
                        }
                    }


                }
            }
        }
    }

    /**
     * Carrega o loot
     */
    public FileConfiguration loadLoot(String loot_id) {
        File file = new File(getDataFolder(), "loots" + File.separator + loot_id + ".yml");
        FileConfiguration loot_config;

        if (!file.exists()) {
            try {
                file.createNewFile();

                loot_config = YamlConfiguration.loadConfiguration(file);
                loot_config.addDefault("name", loot_id);
                loot_config.addDefault("last_open", "");
                loot_config.addDefault("open_after", 0);
                loot_config.addDefault("items.item01.id", "stone");
                loot_config.addDefault("items.item01.quantity", 15);
                loot_config.addDefault("items.item01.chance", 1);
                loot_config.addDefault("items.item02.id", "dirt");
                loot_config.addDefault("items.item02.quantity", 35);
                loot_config.addDefault("items.item02.chance", 0.25);

                loot_config.options().copyDefaults(true);
                loot_config.save(file);
            }
            catch (IOException e) {
                getLogger().info(e.getMessage() + "!");
            }
        }

        loot_config = YamlConfiguration.loadConfiguration(file);

        return  loot_config;
    }

    /**
     *
     */
    public Sign getSignAround(Block block) {
        Block testBlock;
        Sign sign = null;

        Location currentLocation = block.getLocation();

        currentLocation.add(1, 0, 0);
        testBlock = block.getWorld().getBlockAt(currentLocation);
        if(testBlock.getType().toString().toLowerCase().contains("sign")) {
            sign = (Sign) testBlock.getState();
        }

        currentLocation.add(-1, 0, 1);
        testBlock = block.getWorld().getBlockAt(currentLocation);
        if(testBlock.getType().toString().toLowerCase().contains("sign")) {
            sign = (Sign) testBlock.getState();
        }

        currentLocation.add(0, 0, -2);
        testBlock = block.getWorld().getBlockAt(currentLocation);
        if(testBlock.getType().toString().toLowerCase().contains("sign")) {
            sign = (Sign) testBlock.getState();
        }

        currentLocation.add(-1, 0, +1);
        testBlock = block.getWorld().getBlockAt(currentLocation);
        if(testBlock.getType().toString().toLowerCase().contains("sign")) {
            sign = (Sign) testBlock.getState();
        }

        return sign;
    }

    /**
     *
     */
    public String color(String color)
    {
        return ChatColor.translateAlternateColorCodes('&', color);
    }

    /**
     *
     */
    public Date addMinutesToDate(Date date, int minutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, minutes);
        return calendar.getTime();
    }

    /**
     *
     */
    public String minutesToTime(Integer minutos)
    {
        if(minutos < 60) {
            String retorno = minutos + " minuto";
            if(minutos > 1) {
                retorno += "s";
            }

            return retorno;
        }
        else {
            Integer horas = (int) minutos / 60;
            Integer resto = minutos % 60;

            String retorno = horas + " hora";
            if(horas > 1) {
                retorno += "s";
            }
            if(resto > 0) {
                retorno += " e " + resto + " minuto";
                if(resto > 1) {
                    retorno += "s";
                }
            }

            return retorno;
        }
    }

    /**
     * Faz a troca de RAND
     */
    public String parsePlaceholder(String lore)
    {
        Pattern patt = Pattern.compile("%rand:[0-9]*:[0-9]*%", Pattern.CASE_INSENSITIVE);
        Matcher matc = patt.matcher(lore);

        String theGroup = "";

        while (matc.find()) {
            String found = matc.group(0);

            String parts[] = found.replaceAll("%", "").split(":");

            Integer minValue = Integer.parseInt(parts[1])-1;
            Integer maxValue = Integer.parseInt(parts[2]);

            Integer randomized = (int)(Math.random()*(maxValue-minValue))+1+minValue;

            lore = lore.replace(found, randomized+"");
        }

        return lore;
    }

}
