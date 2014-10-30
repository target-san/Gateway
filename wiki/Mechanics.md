## Overview

Portals, or gateways, created with this mod, have several properties making them a better alternative to vanilla portals
* They work from any dimension. This means you can establish gateway not only from overworld, but also from End, ExtraUtils' Deep Dark, Mystcraft ages and so on. All will lead to Nether equally.
* Gateways are completely indestructible. Even such nasty beasts as Enderdragon and Wither won't do any harm to that structure. Only its owner, the person who created gateway, can remove it via special means
* Gateways support teleporting of mounts and riders at the same moment
* Any entities coming through gateway will maintain their direction and momentum
* Exit location is perfectly predictable. This, along with previous property, means that you'll have no trouble establishing minecart tracks
* Transport cooldown is around 4 seconds. It actually exists just to prevent item entities infini-jumping
* No stupid zombie pigmen spawning

Though this comes at the cost of some limitations

* You cannot construct gateway from Nether, since Nether is used as inter-dimensional travel hub
* Gateway exits in Nether can't be adjacent to each other. Actually, their cores will always have their X and Z differ by at least 8. This doesn't mean their entrances in Overworld should have at least 64 blocks in-between, since there's a deviation in exit positions. See [Exit search algorithm](#exit-search-algorithm) for details. This limitation also means that gateways from different dimensions might conflict for exit location. Place with care.
* Unlike with vanilla portals, their construction isn't cost-free. You won't get back all the materials 

## Getting started

To construct your first gateway, you'll need:

0. Place multiblock platform, 3x3, consisting of 4 obsidian, 4 glass and 1 redstone block ![](images/construct-from.png)
0. Ignite redstone block in center with flint'n'steel ![](images/ignite.png)
0. Construct will transform into gateway almost instantly ![](images/gateway.png)

## Removal

Only owner is allowed to remove gateway.
To do so, you need to light 4 blocks directly adjacent to central pillar with flint'n'steel. All 4 blocks must be lit by the owner, otherwise the trick won't work
![](images/removal.png)

## Exit search algorithm

0. Exit location pivot is found
  * Pivot's X and Z are source gateway's ones, adjusted by worlds' movement factors. For Overworld-Nether pair, they're effectively divided by 8
  * Y is always Nether middle height, which is 64
0. Lookup volume around pivot is scanned.
