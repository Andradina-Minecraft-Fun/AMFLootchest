# Lootchest

Create a chest for your dungeons on your 1.16.1 servers, with random loot and time to open

# Install

Download the jar from [releases page](https://github.com/Andradina-Minecraft-Fun/OwnWarp/releases) and put it into plugins server folder. Load with plugman or restart the server

# Usage

Place a chest, attach a sign in one of the sides with first line [loot] and second line the drops list identifier

@todo: IMAGE

When you create a new loot chest, an file with the same name will be created on plugins/Lootchest/loots/NAME.yml

Configure the loot and time here

# Config

 ```
name: test                                      # name of loot identifier
items:                                          # item list
  item01:
    id: diamond
    quantity: 15
    chance: 0.25
  item02:
    id: dirt
    quantity: 1
    chance: 1
    lore:
    - '&7Lore 1'
  item03:                                        # item id, can be anythink
    id: netherite_sword                          # minecraft ID
    quantity: 1                                  # max quantity (will be 0 to quantity)
    chance: 0.1                                  # % of chance to get this drop (1->100% | 0->0%)
    name: Espada de Sangue                       # (optional) Custom name
    lore:                                        # (optional) Custom lore
    - '&6&oP. Atk %rand:5:10%'
    - '&4Lore 2'
last_open: 26-07-2020 23:08:41                  # Don't change this. This is plugin field to control when can be opened again
open_after: 0                                   # Number of minutes that the chest can be opened again

``

