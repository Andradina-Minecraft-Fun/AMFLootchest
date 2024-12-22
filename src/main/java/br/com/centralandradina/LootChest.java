package br.com.centralandradina;

import org.bukkit.Location;

/**
 * AMFLootchest
 */
public final class LootChest
{
	public Location location;
	public String loot_table;
	public int open_after;
	public String last_open;
	public int info_type;
	public String permission;
	
	/**
	 * construtor
	 */
	public LootChest(Location location, String loot_table, int open_after, String last_open, int info_type, String permission)
	{
		this.location = location;
		this.loot_table = loot_table;
		this.open_after = open_after;
		this.last_open = last_open;
		this.info_type = info_type;
		this.permission = permission;
	}
}