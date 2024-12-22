package br.com.centralandradina;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AMFLootchest
 */
public final class AMFLootchest extends JavaPlugin implements Listener 
{

	FileConfiguration config;
	List<FileConfiguration> loots_list = new ArrayList<>();
	List<LootChest> chests_list = new ArrayList<>();

	// controle se está deletando um bau
	boolean deletingChest = false;

	// controle se está salvando um loot table
	boolean savingLoot = false;
	String editingLootName = "";

	// quem está editando
	UUID editingPlayer = null;

	// variavel de controle para saber se o player está editando um bau
	boolean editingChest = false;
	String editingLootTable = "";
	String editingPermission = "";
	int editingOpenAfter = 0;
	int editingInfoType = 0;

	/**
	 * habilita o plugin
	 */
	@Override
	public void onEnable() 
	{
		// registra o listenner
		Bukkit.getServer().getPluginManager().registerEvents(this, this);

		// config
		FileConfiguration config;
		config = this.getConfig();
        config.options().copyDefaults(true);
        this.saveConfig();

		// carrega os chests
		this.loadChests();
	}

	/**
	 * carrega os baus do arquivo de configuração
	 */
	public void loadChests()
	{
		// limpa a lista
		this.chests_list.clear();

		// carrega a lista de baus do arquivo de configuração
		List<Map<String, Object>> chests = (List<Map<String, Object>>) getConfig().getList("chests");


		// percorre a lista de baus
		for (Map<String, Object> item : chests) {
			
			// cria a localização
			Map<?, ?> lLocation = (Map<?, ?>) item.get("location");
			int x = (int) lLocation.get("x");
			int y = (int) lLocation.get("y");
			int z = (int) lLocation.get("z");
			String world = (String) lLocation.get("world");

			Location location = new Location(Bukkit.getWorld(world), x, y, z);

			String loot_table = (String) item.get("loot_table");
			int open_after = (int) item.get("open_after");
			String last_open = (String) item.get("last_open");
			int info_type = (int) item.get("info_type");
			String permission = (String) item.get("permission");


			// cria o objeto do chest
			LootChest chest = new LootChest(location, loot_table, open_after, last_open, info_type, permission);

			// adiciona o bau à lista
			chests_list.add(chest);
		}
	}

	/**
	 * salva a lista de baus no arquivo de configuração
	 */
	public void saveChests()
	{
		// salva a lista no arquivo de configuração
		List<Map<String, Object>> chests = new ArrayList<>();
		for (LootChest item : this.chests_list) {

			// cria o mapa do bau
			Map<String, Object> chestMap = Map.of(
				"location", Map.of(
					"x", item.location.getBlockX(),
					"y", item.location.getBlockY(),
					"z", item.location.getBlockZ(),
					"world", item.location.getWorld().getName()
				),
				"loot_table", item.loot_table,
				"open_after", item.open_after,
				"last_open", item.last_open,
				"info_type", item.info_type
			);

			// adiciona o bau à lista
			chests.add(chestMap);
		}

		// salvar a lista no arquivo de configuração
		this.getConfig().set("chests", chests);
		this.saveConfig();
	}

	/**
	 * desabilita o plugin
	 */
	@Override
	public void onDisable() {}

	/**
	 * commands
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) 
	{
		// verifica se o comando é lootchest
		if (command.getName().equalsIgnoreCase("amflootchest")) {

			// verifica se o parametro é create
			if((args.length > 0) && (args[0].equalsIgnoreCase("create"))) {

				// verifica se o jogador é um player
				if(!(sender instanceof Player)) {
					sender.sendMessage(this.color(this.getConfig().getString("prefix") + "Apenas jogadores podem criar baus"));
					return true;
				}

				// verifica se tem a permissão amd.lootchest.create
				if(!sender.hasPermission("amf.lootchest.create")) {
					sender.sendMessage(this.color(this.getConfig().getString("prefix") + "Você não pode criar um bau"));
					return true;
				}

				// verifica se tem argumento 1, que é o nome do loot table
				if(args.length > 1) {
					this.editingLootTable = args[1];
				}
				else {
					this.editingLootTable = "default";
				}

				// verifica se tem argumento 2, que é o nome tempo de abertura
				if(args.length > 2) {
					try {
						this.editingOpenAfter = Integer.parseInt(args[2]);
					}
					catch(NumberFormatException e) {
						sender.sendMessage(args[2] + " não é um número válido");
						return true;
					}
				}
				else {
					this.editingOpenAfter = 0;
				}

				// verifica se tem argumento 3, que é o tipo de retorno que o clique vai dar ao player
				if(args.length > 3) {
					this.editingInfoType = Integer.parseInt(args[3]);
				}
				else {
					this.editingInfoType = 1;
				}

				// verifica se tem argumento 4, que é a permissão para abrir o bau
				if(args.length > 4) {
					this.editingPermission = args[4];
				}
				else {
					this.editingPermission = "default";
				}
				
				// recupera o uuid do player
				this.editingPlayer = ((Player) sender).getUniqueId();

				// entra em modo de edição
				this.editingChest = true;
				this.deletingChest = false;
				this.savingLoot = false;

				// envia a mensagem para o jogador
				sender.sendMessage(this.color(this.getConfig().getString("prefix") + "Clique no bau para criar um lootchest"));
				
			}

			// verifica se o parametro é delete
			else if((args.length > 0) && (args[0].equalsIgnoreCase("delete"))) {
				// verifica se o jogador é um player
				if(!(sender instanceof Player)) {
					sender.sendMessage(this.color(this.getConfig().getString("prefix") + "Apenas jogadores podem deletar baus"));
					return true;
				}

				// verifica se tem a permissão amd.lootchest.delete
				if(!sender.hasPermission("amf.lootchest.delete")) {
					sender.sendMessage(this.color(this.getConfig().getString("prefix") + "Você não pode deletar um bau"));
					return true;
				}

				// recupera o uuid do player
				this.editingPlayer = ((Player) sender).getUniqueId();

				// entra em modo de deleção
				this.deletingChest = true;
				this.editingChest = false;
				this.savingLoot = false;

				// envia a mensagem para o jogador
				sender.sendMessage(this.color(this.getConfig().getString("prefix") + "Clique no bau para criar um lootchest"));
			}

			// verifica se o parametro é reload
			else if((args.length > 0) && (args[0].equalsIgnoreCase("reload"))) {

				// verifica se tem a permissão amd.lootchest.reload
				if(!sender.hasPermission("amf.lootchest.reload")) {
					sender.sendMessage(this.color(this.getConfig().getString("prefix") + "Você não pode recarregar o plugin"));
					return true;
				}

				// recarrega o arquivo de configuração
				this.reloadConfig();

				// recarrega os baus
				this.loadChests();

				// envia a mensagem para o jogador
				sender.sendMessage(this.color(this.getConfig().getString("prefix") + "Plugin recarregado"));
			}

			// verifica se o parametro é saveloot
			else if((args.length > 0) && (args[0].equalsIgnoreCase("saveloot"))) {

				// verifica se tem a permissão amd.lootchest.saveloot
				if(!sender.hasPermission("amf.lootchest.saveloot")) {
					sender.sendMessage(this.color(this.getConfig().getString("prefix") + "Você não pode salvar um loot"));
					return true;
				}

				// verifica se tem argumento 1, que é o nome do loot table
				if(args.length > 1) {
					this.editingLootName = args[1];
				}
				else {
					this.editingLootName = "default";
				}

				// recupera o uuid do player
				this.editingPlayer = ((Player) sender).getUniqueId();

				// informa que está salvando
				this.deletingChest = false;
				this.editingChest = false;
				this.savingLoot = true;

				// envia a mensagem para o jogador
				sender.sendMessage(this.color(this.getConfig().getString("prefix") + "Clique no bau para criar um lootchest"));
			}

			// nada disso
			else {

				// é admin?
				if((sender.hasPermission("amf.lootchest.create")) || (sender.hasPermission("amf.lootchest.delete")) || (sender.hasPermission("amf.lootchest.reload"))) {
					sender.sendMessage(this.color(this.getConfig().getString("prefix") + "Comandos disponíveis:"));
					sender.sendMessage(this.color(this.getConfig().getString("prefix") + "/amflootchest create [loot_table] [open_after] [info_type] [permission]"));
					sender.sendMessage(this.color(this.getConfig().getString("prefix") + "/amflootchest delete"));
					sender.sendMessage(this.color(this.getConfig().getString("prefix") + "/amflootchest reload"));
					return true;
				}

				// player normal
				else {
					sender.sendMessage(this.color(this.getConfig().getString("prefix") + "Você não tem permissão para usar esse comando"));
					return true;
				}
			}
			
		}
		return false;
	}

	/**
	 * quando um player interage com um bau
	 * @param event
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerInteract(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();



		// verifica se o player clicou em um bau
		Block block = event.getClickedBlock();
		if((block != null) && (block.getType() == Material.CHEST)) {

			// verifica se está deletando um bau
			if(this.deletingChest) {

				// verifica se o uuid do player que clicou é o de quem executou o comando
				if((this.editingPlayer == null) || (!player.getUniqueId().equals(this.editingPlayer))) {
					return;
				}

				// percorre a lista de baus
				for (int i = 0; i < this.chests_list.size(); i++) {
					LootChest chest = this.chests_list.get(i);

					// verifica se o bau existe
					if(chest.location.equals(block.getLocation())) {

						// remove o bau da lista
						this.chests_list.remove(i);

						player.sendMessage(this.color(this.getConfig().getString("prefix") + "bau removido"));

						// sai do modo edição
						this.deletingChest = false;
						this.editingPlayer = null;

						// salva os baus no arquivo de configuração
						this.saveChests();

						return;
					}
				}

				// se chegar até aqui, é porque o bau não existe
				player.sendMessage(this.color(this.getConfig().getString("prefix") + "bau não encontrado"));
			}

			// verifica se o player está editando um bau
			else if(this.editingChest) {

				// verifica se o uuid do player que clicou é o de quem executou o comando
				if((this.editingPlayer == null) || (!player.getUniqueId().equals(this.editingPlayer))) {
					return;
				}

				// percorre a lista de baus
				for (int i = 0; i < this.chests_list.size(); i++) {
					LootChest chest = this.chests_list.get(i);

					// verifica se o bau já existe
					if(chest.location.equals(block.getLocation())) {

						// remove o bau
						this.chests_list.remove(i);

						// avisa o player sobre a edição
						player.sendMessage(this.color(this.getConfig().getString("prefix") + "Esse bau já existe, removendo para recria-lo"));
					}
				}

				// cria o objeto do chest
				LootChest chest = new LootChest(block.getLocation(), this.editingLootTable, this.editingOpenAfter, "", this.editingInfoType, this.editingPermission);

				// adiciona o bau à lista
				chests_list.add(chest);

				// envia a mensagem para o jogador
				player.sendMessage(this.color(this.getConfig().getString("prefix") + "Bau criado com sucesso"));

				// sai do modo de edição
				this.editingChest = false;
				this.editingPlayer = null;

				// salva os baus no arquivo de configuração
				this.saveChests();
			}

			// verifica se o player esta salvando um loot
			else if(this.savingLoot) {

				// verifica se o uuid do player que clicou é o de quem executou o comando
				if((this.editingPlayer == null) || (!player.getUniqueId().equals(this.editingPlayer))) {
					return;
				}

				// recupera o bau
				Inventory inv = ((Chest) block.getState()).getInventory();

				// percorre os itens do inventario
				int counter = 0;
				for(ItemStack item : inv.getContents()) {
					if(item == null) {
						continue;
					}

					// salva a chance e o item serializado
					this.getConfig().set("loots." + this.editingLootName + ".items.item" + counter + ".chance", 1);
					this.getConfig().set("loots." + this.editingLootName + ".items.item" + counter + ".meta", item.serialize());

					counter++;

				}

				// salva o arquivo e recarrega
				this.saveConfig();
				this.reloadConfig();

				// envia a mensagem para o jogador
				player.sendMessage(this.color(this.getConfig().getString("prefix") + "Loot table criado com sucesso"));

				// sai do modo de edição
				this.savingLoot = false;
				this.editingPlayer = null;
			}

			// verifica se o bau é um lootchest
			else {

				// verifica se é pra abrir o bau
				if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {

					// percorre a lista de baus
					for (int i = 0; i < this.chests_list.size(); i++) {
						LootChest chest = this.chests_list.get(i);

						// verifica se o bau já existe
						if(chest.location.equals(block.getLocation())) {

							// recupera o bau
							LootChest lootchest = this.chests_list.get(i);

							// verifica se tem permissão para abrir o bau
							if((player.hasPermission("amf.lootchest.open."+lootchest.permission)) || (player.hasPermission("amf.lootchest.open.*"))) {
								
								// verifica se pode abrir
								try {

									String lastOpen = lootchest.last_open;

									// se tem data
									if(!lastOpen.equals("")) {
										
										// da o parse na data
										Date last_open = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").parse(lastOpen);

										// se nao pode abrir
										if(this.addMinutesToDate(last_open, lootchest.open_after).compareTo(new Date()) > 0) {

											// verifica se é pra mostrar de quanto em quanto tempo pode abrir
											if(lootchest.info_type == 1) {
												player.sendMessage(this.color(this.getConfig().getString("prefix") + "Este baú só pode ser aberto a cada " + this.minutesToTime(lootchest.open_after)));

												event.setCancelled(true);
												return;
											}

											// verifica se é pra mostra quanto tempo falta para abrir
											else if(lootchest.info_type == 2) {

												// calcula quanto tempo falta para abrir
												Date now = new Date();
												long diff = this.addMinutesToDate(last_open, lootchest.open_after).getTime() - now.getTime();
												long diffMinutes = diff / (60 * 1000) % 60;
												long diffHours = diff / (60 * 60 * 1000) % 24;
												long diffSeconds = diff / 1000 % 60;

												// se tem horas
												if(diffHours > 0) {
													player.sendMessage(this.color(this.getConfig().getString("prefix") + "Este baú só pode ser aberto em " + diffHours + " horas, " + diffMinutes + " minutos e " + diffSeconds + " segundos"));
												}

												// se tem minutos
												else if(diffMinutes > 0) {
													player.sendMessage(this.color(this.getConfig().getString("prefix") + "Este baú só pode ser aberto em " + diffMinutes + " minutos e " + diffSeconds + " segundos"));
												}

												// se so tem segundos
												else {
													player.sendMessage(this.color(this.getConfig().getString("prefix") + "Este baú só pode ser aberto em " + diffSeconds + " segundos"));
												}

												event.setCancelled(true);
												return;
											}

											// verifica se é pra abrir o bau vazio
											else if(lootchest.info_type == 3) {
												
												Inventory inv = ((Chest) block.getState()).getInventory();
												inv.clear();

												return;

											}

											// qualquer outra coisa
											else {
												event.setCancelled(true);
												return;
											}
										}
									}

								}
								catch(ParseException e) {
									getLogger().info("Cannot parse last_open date from chest on " + lootchest.location.getX() + ":" + lootchest.location.getY() + ":" + lootchest.location.getZ());
								}

								// get chest inventory
								Inventory inv = ((Chest) block.getState()).getInventory();
								inv.clear();

								// recupera o loot do bau
								String loot_table = lootchest.loot_table;
								if(!this.getConfig().isSet("loots." + loot_table)) {
									player.sendMessage(this.color(this.getConfig().getString("prefix") + "Loot table " + loot_table + " não encontrada"));
									event.setCancelled(true);
									return;
								}

								// recupera a loot table
								ConfigurationSection loot = this.getConfig().getConfigurationSection("loots." + loot_table);

								// percorre os itens
								ConfigurationSection items = loot.getConfigurationSection("items");
								for(String key : items.getKeys(false)) {

									// da o parse nos parametros
									Double chance = loot.getDouble("items." + key + ".chance");
									if(Math.random() > chance) {
										continue;
									}

									// da o parse no item
									ItemStack stack = ItemStack.deserialize(this.getConfig().getConfigurationSection("loots." + loot_table + ".items." + key + ".meta").getValues(false));

									// salva
									inv.addItem(stack);
								}

								// @todo Se for um bau conectado, recupera o bau conectado e adiciona a data de abertura tambem nele
								
								// salva a ultima vez aberto
								Date now = new Date();
								SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
								lootchest.last_open = format.format(now);

								// salva o arquivo
								this.saveChests();

								// sai do loop
								return;

							}
							else {
								player.sendMessage(this.color(this.getConfig().getString("prefix") + "Você não pode abrir esse bau"));
								event.setCancelled(true);
								return;
							}


						}
					}
				}

			}




		}


	}

	/**
	 * evento para quando quebrar um bau
	 */
	@EventHandler()
	public void onBlockBreak(final BlockBreakEvent event)
	{
		Player player = event.getPlayer();
		Block block = event.getBlock();

		// verifica se é um bau
		if(block.getType().toString().toLowerCase().contains("chest")) {

			// percorre a lista de baus, procurando se o bau quebrado estava na lista
			for (int i = 0; i < this.chests_list.size(); i++) {
				LootChest chest = this.chests_list.get(i);

				// verifica se o bau existe
				if(chest.location.equals(block.getLocation())) {

					// verifica se tem permissão
					if(!player.hasPermission("amf.lootchest.destroy")) {
						player.sendMessage(this.color(this.getConfig().getString("prefix") + "Você não pode quebrar esse bau"));
						event.setCancelled(true);
						return;
					}
					
					// remove o bau da lista
					this.chests_list.remove(i);

					player.sendMessage(this.color(this.getConfig().getString("prefix") + "bau removido"));

					// salva os baus no arquivo de configuração
					this.saveChests();

					return;
				}
			}
		}
	}

	/**
	 * quando fecha o bau, limpa o inventario
	 */
	@EventHandler
	public void InventoryClose(InventoryCloseEvent event) 
	{
		if(event.getInventory().getHolder() instanceof Chest) {
			Chest holder = (Chest)event.getInventory().getHolder();
			Block block = holder.getBlock();

			// percorre a lista de baus, procurando se o bau quebrado estava na lista
			for (int i = 0; i < this.chests_list.size(); i++) {
				LootChest chest = this.chests_list.get(i);

				// verifica se o bau existe
				if(chest.location.equals(block.getLocation())) {
					holder.getInventory().clear();
				}
			}
		}
	}

	/**
	 *
	 
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
			*/

	/**
	 * @todo Fazer funcionar em bau duplo
	 * @todo Bau muito proximo um do outro, abre a placa do outro. Verificar realmente a placa esta estacada no bau
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void PlayerInteract(PlayerInteractEvent event) {
		Player p = event.getPlayer();
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Block b = event.getClickedBlock();
			if (b.getType() == Material.CHEST) {

				// Recupera a placa em volta
				Sign sign = this.getSignAround(b);
				if(sign != null) {

					// Verifica se é uma placa de loot
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

							// Verifica se tem permissão para abrir o loot
							Player player = event.getPlayer();
							if((!player.hasPermission("amf.lootchest.open."+loot_id)) && (!player.hasPermission("amf.lootchest.open"))) {
								player.sendMessage(this.color(config.getString("prefix") + "Você não pode abrir esse bau"));
								event.setCancelled(true);
								return;
							}

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

							// Sai do loop
							break;
						}
					}


				}
			}
		}
	}
	 */

	/**
	 * Carrega o loot
	 
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
		*/

	/**
	 *
	
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
		 */

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
