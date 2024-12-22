
# Join 
[![Discord](https://i.imgur.com/tyZLFHl.png)](https://discord.gg/D47yfBPga5)

# Lootchest

Create a chest for your dungeons on your 1.18.2 servers, with random loot and time to open

# Install

Download the jar from [releases page](https://github.com/Andradina-Minecraft-Fun/AMFLootchest/releases) and put it into plugins server folder. Load with plugman or restart the server

# Usage

### create loot table

put a chest, and add itens inside. run command `/amdlootchest saveloot loot_table_name` and click on chest. Now you have `loot_table_name` on `config.yml`. You can configure chances now

### create a random chest

put a chest, run command `/amdlootchest create loot_table_name time info_type permission`, and click on chest. Now you have this chest on `config.yml` too

- `loot_table_name` is the name of previous created loot table
- `time` is the time in minutes, that chest can be reopened
- `info_type` is the player feedback type: `0 - do nothing`, `1 - show to player that the chest can be opened every X minutes`, `2 - show how many time shows how much time is left to open the chest`, `3 - open the chest, but empty`
- `permission` is the permission needed to open chest

# Permissions

- `amf.lootchest.create` - create chest
- `amf.lootchest.delete` - delete/remove chest
- `amf.lootchest.reload` - can reload config
- `amf.lootchest.saveloot` - create loot tables
- `amf.lootchest.open.*` - can open any chest
- `amf.lootchest.open.permission` - can open chest with permission "permission"

# Config

 ```yml
# prefix
prefix: ''

# loot table
loots:

  # default example loot table
  default:

    # and more: add all itens to a chest, run /amflootchest saveloot LOOT_TABLE_NAME and click in chest to save loot table
    items:
      item0:
        # chance 0.01 from 1
        chance: 0.75

        # meta
        meta:
          type: DIAMOND
          amount: 15

# chests
chests:
    # location of chest
  - location:
      x: 1
      y: 1
      z: 1
      world: world

    # permissão para quem pode abrir
    permission: 'default'

    # minutes to wait to open again  
    open_after: 0

    # last time chest was opened
    last_open:  ''

     # loot table to use
    loot_table: default

    # 0 = do nothing
    # 1 = return message to player that this chest open X in X time
    # 2 = return message when can be opened
    # 3 = open chest, but nothing inside
    info_type: 0 
  

```

