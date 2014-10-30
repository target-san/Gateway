## Overview



## Getting started

To construct your first gateway, you'll need:

0. Place multiblock platform, 3x3, consisting of 4 obsidian, 4 glass and 1 redstone block ![][images/gateway-construct-from.png]
0. Ignite redstone block in center with flint'n'steel ![][images/gateway-ignite.png]
0. Construct will transform into gateway almost instantly ![][images/gateway.png]

## Exit search algorithm

0. Exit location pivot is found
  * Pivot's X and Z are source gateway's ones, adjusted by worlds' movement factors. For Overworld-Nether pair, they're effectively divided by 8
  * Y is always Nether middle height, which is 64
0. Lookup volume around pivot is scanned.
