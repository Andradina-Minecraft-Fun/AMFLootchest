# tarefas

- [x] criar o arquivo chests.yaml
- [x] adicionar o comando `/amdlootchest create loot_table tempo_minutos INFO_TYPE permission`, que ao clicar no bau, guarda a posição e mundo, e o loottable no arquivos chests (info type é "nao abrir" "exibir quando pode abrir", "exibir que pode abrir de x em x tempo", "abrir vazio")
- [x] verificar se o chest ja existe
- [x] remover o chest
- [x] editar o chest (só criar por cima)
- [x] quando remover um bau, tem que ver se ele é um lootchest
- [x] carregar o arquivo chests.yaml num vetor de lootschests, que armazena as configurações do bau, criar a classe lootchest
- [x] ao quebrar, ver se tem permissão de quebrar
- [x] ao abrir, ver se tem permissão de abrir
- [x] ao abrir, ver se pode abrir pelo tempo, e retorna conforme INFO_TYPE
- [x] criar comando de reconfigurar
- [x] comando que salva os itens do bau no loot_table `/amdlootchest saveloot loot_table`
- [x] quando fechar o bau, limpa para prevenir dup

- [ ] verificar se tem bau conectado (bau duplo) e se tiver, tambem criar esse bau na lista de configuração automaticamente
- [ ] verificar se tem bau conectado (bau duplo) e se tiver, quando abrir, tambem salvar a data que abriu nesse bau
- [ ] achar uma forma de adicionar atributos num item, json meta?


# Join 
[![Discord](https://i.imgur.com/tyZLFHl.png)](https://discord.gg/D47yfBPga5)

# Lootchest

Create a chest for your dungeons on your 1.18.2 servers, with random loot and time to open

# Install

Download the jar from [releases page](https://github.com/Andradina-Minecraft-Fun/OwnWarp/releases) and put it into plugins server folder. Load with plugman or restart the server

# Usage

Place a chest, attach a sign in one of the sides with first line [loot] and second line the drops list identifier


![Imgur](https://i.imgur.com/wzGBV1w.png)


When you create a new loot chest, an file with the same name will be created on plugins/Lootchest/loots/NAME.yml

Configure the loot and time here

# Config

 ```yml
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

```

# Permissions

```yml
# can create lootchests
- amf.lootchest.create

# can destroy chest and/or sign
- amf.lootchest.destroy

# can open any chest
- amf.lootchest.open

# can open some especific chest
- amf.lootchest.open.LOOT_ID
```
