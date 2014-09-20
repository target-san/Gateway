Gateway
=======

Cross-dimensional portals for Minecraft Forge done right and without overpower

## Build

Get sources, then just

    gradlew build

## Development

Get sources, then

    gradlew setupDecompWorkspace

then, for your favorite IDE,

    gradlew eclipse

or

    gradlew idea

## Overview

This mod started when I was playing Railcraft actively and wanted some portals which are more controllable and predictable than vanilla ones, but not OP, unlike Mystcraft, DimDoors, Enhanced Portals or the like.

This mod, as for now, introduces new kind of portal called gateway. It essentially still uses Nether as a travelling dimension - if you need a direct portal, just use something else. I, in my turn, was thinking about making some things easier for some other kind of errort.
The tradeoff is shortening distance in exchange for making travel (or essentially preparing road) more dangerous.
The other important property is velocity being kept and exit location being completely predictable. The entity's XZ velocity vector is taken, then the entity is translated thru pillar along that XZ vector just until it stops touching it. So if some cart enters from north, it will leave right to south. This way you can create 4 rail tracks coming thorugh gateway.

So let's get to the business.

## Where to start

Gateways can be essentially created in any non-Nether dimension. This includes End, Deep Dark, Mystcraft ages.
And they always lead to Nether. So be prepared to construct some pathways and protect them from ghasts, zombie pigmen and other nasty dwellers.
As it was stated above, this shortens distance around 8 times. As such, gateways become a not-so OP transport and travelling system.

NB: I didn't have opportunity to test gateways with Mystcraft ages - because there's no public Mystcraft for 1.7.10 on 2014-09-20.
Though there's no logic which checks explicitly for some specific dimension - except Nether, of course.
Also, gateways were tested with End and Deep Dark. Everything should work fine.

## Your first gateway

You'll need only vanilla blocks - 4 obsidian, 4 glass and 1 redstone block.
Just put them in 3x3 horizontal pattern - O for obsidian, G for glass, R for redstone block. Please note that only vanilla materials are supported as for now, so no clear glass/stained glass etc.
```
OGO
GRG
OGO
```

Like with vanilla portal, you can construct this without diamond pick by putting lava and then cooling it with water. The only difference is that you'll need only 4 obsidian in floor, and no vertical frame, which is much easier.
The last step is to use flint'n'steel on redstone block - this will construct gateway. Or won't. Though it's very unlikely that gateway construction will fail.

## On the other side

Going through gateway will bring you to Nether, like with vanilla.
Some explanation about how the proper location for gateway is searched:

1. The center of search zone is calculated by taking XZ of the source block (the one where redstone resided), then translating it to nether coordinates by dividing by 8 - this is movement factor between overworld and nether.
   Y coordinate is just Nether's height divided by two - 63.
2. The search zone is from one fourth to three fourths of Nether height. XZ square zone has 'linear' radius of 5.
   Also, each gateway creates a vertical 'dead zone' with radius 8, where no other gateway can open.
   This means that both X and Z between adjacent gateway core blocks (the ones righ below pillar, where redstone resided) should differ by at least 8.
   This dead zone is for *any* source dimension. It doesn't matter where gateway leads from. So be careful when connecting several dimensions.
3. Next, all positions in dead zone are evaluated for their 'weight'. The less weight, the better position. Weight is influenced by:
    * non-air blocks above future platform in 3x3x3 volume, where any non-air block adds to weight
    * non-solid blocks in 5x1x5 volume centered around future core, where each non-solid block adds to weight
    * other gateway's dead zone or nearby liquid (in 7x5x7 volume, with core at the bottom center) completely blocks construction

To sum up, the closer position to search volume center, the less blocks are need to destroy for air pocket and the less blocks need to be solidified to create platform, the better. And there's protection against submerging gateway into lava.
If there's no suitable position, gateway isn't constructed, and message is shown.

## Anchors

You might want to place exit gate in the spot you want. To do so, just place redstone block in the search volume and outside any dead zones. Then ignite gateway as usual - exit core will be right where you paced redstone block.

## Removal

You might've already discovered that all gateway blocks are effectively indestructible. To remove them, you need to ignite 4 fires with flint'n'steel on the blocks where glass blocks were placed. After this, if you're gateway creator, it will be severed.
Though playing with gates isn't cost-free, as you will get back only obsidian. In normal world, former glass will become gravel, and core is replaced with netherrack. In Nether, gate platform will become obsidian surrounded with netherrack.

## Features in progress:

1. Connectors - gateways should allow to pump items, liquids, power, AE cables and so on.

## TODO list

1. End: gateway shouldn't be openable until dragin is dead
2. End, Nether: achievements
3. Some kind of locator tool - click on block in any world, then see corresponding search volume in Nether being visualized.
4. Some method of less lossy gateway shutdown.
5. Biometric control - check who can pass and who can't. Want to use existing biometrics API.
6. Maybe, an OP variant which can be placed anywhere.
