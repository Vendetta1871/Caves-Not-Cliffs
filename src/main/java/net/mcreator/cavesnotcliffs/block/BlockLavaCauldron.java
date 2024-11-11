
package net.mcreator.cavesnotcliffs.block;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.event.ModelRegistryEvent;

import net.mcreator.cavesandcliffs.block.BlockTopStalactite;
import net.mcreator.cavesandcliffs.block.BlockMiddleStalactite;
import net.mcreator.cavesandcliffs.block.BlockBottomStalactite;

import net.minecraft.world.World;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.Explosion;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.NonNullList;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumFacing;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.network.NetworkManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBanner;
import net.minecraft.item.Item;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.inventory.Container;
import net.minecraft.init.Blocks;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntityBanner;
import net.minecraft.entity.Entity;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.SoundType;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.mcreator.cavesnotcliffs.ElementsCavesNotCliffs;
import net.minecraft.init.Items;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.item.ItemBucket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.potion.PotionUtils;
import net.minecraft.init.PotionTypes;
import net.minecraft.world.storage.WorldInfo;

@ElementsCavesNotCliffs.ModElement.Tag
public class BlockLavaCauldron extends ElementsCavesNotCliffs.ModElement {
	@GameRegistry.ObjectHolder("cavesnotcliffs:lavacauldron")
	public static final Block block = null;
	public BlockLavaCauldron(ElementsCavesNotCliffs instance) {
		super(instance, 8);
	}

	@Override
	public void initElements() {
		elements.blocks.add(() -> new BlockCustom().setRegistryName("lavacauldron"));
		elements.items.add(() -> new ItemBlock(block).setRegistryName(block.getRegistryName()));
	}

	@Override
	public void init(FMLInitializationEvent event) {
		GameRegistry.registerTileEntity(TileEntityCustom.class, "cavesnotcliffs:tileentitylavacauldron");
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void registerModels(ModelRegistryEvent event) {
		ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
				new ModelResourceLocation("cavesnotcliffs:lavacauldron", "inventory")); // поменять текстуру!
	}
	public static class BlockCustom extends Block implements ITileEntityProvider {
		// 0 empty, 1-3 water, 4-6 lava 
		public static final PropertyInteger LEVEL = PropertyInteger.create("level", 0, 6);
    	protected static final AxisAlignedBB AABB_LEGS = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.3125D, 1.0D);
    	protected static final AxisAlignedBB AABB_WALL_NORTH = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 0.125D);
    	protected static final AxisAlignedBB AABB_WALL_SOUTH = new AxisAlignedBB(0.0D, 0.0D, 0.875D, 1.0D, 1.0D, 1.0D);
    	protected static final AxisAlignedBB AABB_WALL_EAST = new AxisAlignedBB(0.875D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    	protected static final AxisAlignedBB AABB_WALL_WEST = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 0.125D, 1.0D, 1.0D);
		
		public BlockCustom() {
			super(Material.IRON, MapColor.STONE);
        	this.setDefaultState(this.blockState.getBaseState().withProperty(LEVEL, Integer.valueOf(0)));
        	this.setTickRandomly(true);
			setUnlocalizedName("lavacauldron");
		}

		@Override
		public TileEntity createNewTileEntity(World worldIn, int meta) {
			return new TileEntityCustom();
		}

		@Override
		public boolean eventReceived(IBlockState state, World worldIn, BlockPos pos, int eventID, int eventParam) {
			super.eventReceived(state, worldIn, pos, eventID, eventParam);
			TileEntity tileentity = worldIn.getTileEntity(pos);
			return tileentity == null ? false : tileentity.receiveClientEvent(eventID, eventParam);
		}

		public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean isActualState)
	    {
	        addCollisionBoxToList(pos, entityBox, collidingBoxes, AABB_LEGS);
	        addCollisionBoxToList(pos, entityBox, collidingBoxes, AABB_WALL_WEST);
	        addCollisionBoxToList(pos, entityBox, collidingBoxes, AABB_WALL_NORTH);
	        addCollisionBoxToList(pos, entityBox, collidingBoxes, AABB_WALL_EAST);
	        addCollisionBoxToList(pos, entityBox, collidingBoxes, AABB_WALL_SOUTH);
	    }
	
	    /**
	     * @deprecated call via {@link IBlockState#getBoundingBox(IBlockAccess,BlockPos)} whenever possible.
	     * Implementing/overriding is fine.
	     */
	    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
	    {
	        return FULL_BLOCK_AABB;
	    }
	
	    /**
	     * Used to determine ambient occlusion and culling when rebuilding chunks for render
	     * @deprecated call via {@link IBlockState#isOpaqueCube()} whenever possible. Implementing/overriding is fine.
	     */
	    public boolean isOpaqueCube(IBlockState state)
	    {
	        return false;
	    }
	
	    /**
	     * @deprecated call via {@link IBlockState#isFullCube()} whenever possible. Implementing/overriding is fine.
	     */
	    public boolean isFullCube(IBlockState state)
	    {
	        return false;
	    }
	
	    /**
	     * Called When an Entity Collided with the Block
	     */
	    public void onEntityCollidedWithBlock(World worldIn, BlockPos pos, IBlockState state, Entity entityIn)
	    {
	        int i = ((Integer)state.getValue(LEVEL)).intValue();
	        int j = i > 3 ? (i - 3) : i; 
	        float f = (float)pos.getY() + (6.0F + (float)(3 * j)) / 16.0F;

	        if (!worldIn.isRemote && (entityIn.getEntityBoundingBox().minY <= (double)f)) { 
		        if (i > 0 && i < 4 && entityIn.isBurning())
		        {
		            entityIn.extinguish();
		            this.setWaterLevel(worldIn, pos, state, i - 1);
		        }
		        else if (i > 3) {
		        	entityIn.setFire(5);
		        }
	        }
	    }

	    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos)
        {
        	if (((Integer)state.getValue(LEVEL)).intValue() > 3) {
        		return 12;
        	}
        	else {
            	return super.getLightValue(state, world, pos);
        	}
        }
	
	    /**
	     * Called when the block is right clicked by a player.
	     */
	    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
	    {
	        ItemStack itemstack = playerIn.getHeldItem(hand);
	
	        if (itemstack.isEmpty())
	        {
	            return true;
	        }
	        else
	        {
	            int i = ((Integer)state.getValue(LEVEL)).intValue();
	            Item item = itemstack.getItem();
	
	            if (item == Items.WATER_BUCKET)
	            {
	                if (i < 3 && !worldIn.isRemote)
	                {
	                    if (!playerIn.capabilities.isCreativeMode)
	                    {
	                        playerIn.setHeldItem(hand, new ItemStack(Items.BUCKET));
	                    }
	
	                    playerIn.addStat(StatList.CAULDRON_FILLED);
	                    this.setWaterLevel(worldIn, pos, state, 3);
	                    worldIn.playSound((EntityPlayer)null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
	                }
	
	                return true;
	            }
	            if (item == Items.LAVA_BUCKET)
	            {
	                if ((i != 1 && i != 2 && i != 3) && !worldIn.isRemote)
	                {
	                    if (!playerIn.capabilities.isCreativeMode)
	                    {
	                        playerIn.setHeldItem(hand, new ItemStack(Items.BUCKET));
	                    }
	
	                    playerIn.addStat(StatList.CAULDRON_FILLED);
	                    this.setLavaLevel(worldIn, pos, state, 3);
	                    worldIn.playSound((EntityPlayer)null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
	                }
	
	                return true;
	            }
	            else if (item == Items.BUCKET)
	            {
	                if (i == 3 && !worldIn.isRemote)
	                {
	                    if (!playerIn.capabilities.isCreativeMode)
	                    {
	                        itemstack.shrink(1);
	
	                        if (itemstack.isEmpty())
	                        {
	                            playerIn.setHeldItem(hand, new ItemStack(Items.WATER_BUCKET));
	                        }
	                        else if (!playerIn.inventory.addItemStackToInventory(new ItemStack(Items.WATER_BUCKET)))
	                        {
	                            playerIn.dropItem(new ItemStack(Items.WATER_BUCKET), false);
	                        }
	                    }
	
	                    playerIn.addStat(StatList.CAULDRON_USED);
	                    this.setWaterLevel(worldIn, pos, state, 0);
	                    worldIn.playSound((EntityPlayer)null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
	                }
	                if (i == 6 && !worldIn.isRemote)
	                {
	                    if (!playerIn.capabilities.isCreativeMode)
	                    {
	                        itemstack.shrink(1);
	
	                        if (itemstack.isEmpty())
	                        {
	                            playerIn.setHeldItem(hand, new ItemStack(Items.LAVA_BUCKET));
	                        }
	                        else if (!playerIn.inventory.addItemStackToInventory(new ItemStack(Items.LAVA_BUCKET)))
	                        {
	                            playerIn.dropItem(new ItemStack(Items.LAVA_BUCKET), false);
	                        }
	                    }
	
	                    playerIn.addStat(StatList.CAULDRON_USED);
	                    this.setLavaLevel(worldIn, pos, state, 0);
	                    worldIn.playSound((EntityPlayer)null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
	                }
	
	                return true;
	            }
	            else if (item == Items.GLASS_BOTTLE)
	            {
	                if (i > 0 && i < 4 && !worldIn.isRemote)
	                {
	                    if (!playerIn.capabilities.isCreativeMode)
	                    {
	                        ItemStack itemstack3 = PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.WATER);
	                        playerIn.addStat(StatList.CAULDRON_USED);
	                        itemstack.shrink(1);
	
	                        if (itemstack.isEmpty())
	                        {
	                            playerIn.setHeldItem(hand, itemstack3);
	                        }
	                        else if (!playerIn.inventory.addItemStackToInventory(itemstack3))
	                        {
	                            playerIn.dropItem(itemstack3, false);
	                        }
	                        else if (playerIn instanceof EntityPlayerMP)
	                        {
	                            ((EntityPlayerMP)playerIn).sendContainerToPlayer(playerIn.inventoryContainer);
	                        }
	                    }
	
	                    worldIn.playSound((EntityPlayer)null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
	                    this.setWaterLevel(worldIn, pos, state, i - 1);
	                }
	
	                return true;
	            }
	            else if (item == Items.POTIONITEM && PotionUtils.getPotionFromItem(itemstack) == PotionTypes.WATER)
	            {
	                if (i < 3 && !worldIn.isRemote)
	                {
	                    if (!playerIn.capabilities.isCreativeMode)
	                    {
	                        ItemStack itemstack2 = new ItemStack(Items.GLASS_BOTTLE);
	                        playerIn.addStat(StatList.CAULDRON_USED);
	                        playerIn.setHeldItem(hand, itemstack2);
	
	                        if (playerIn instanceof EntityPlayerMP)
	                        {
	                            ((EntityPlayerMP)playerIn).sendContainerToPlayer(playerIn.inventoryContainer);
	                        }
	                    }
	
	                    worldIn.playSound((EntityPlayer)null, pos, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
	                    this.setWaterLevel(worldIn, pos, state, i + 1);
	                }
	
	                return true;
	            }
	            else
	            {
	                if (i > 0 && i < 4 && item instanceof ItemArmor)
	                {
	                    ItemArmor itemarmor = (ItemArmor)item;
	
	                    if (itemarmor.getArmorMaterial() == ItemArmor.ArmorMaterial.LEATHER && itemarmor.hasColor(itemstack) && !worldIn.isRemote)
	                    {
	                        itemarmor.removeColor(itemstack);
	                        this.setWaterLevel(worldIn, pos, state, i - 1);
	                        playerIn.addStat(StatList.ARMOR_CLEANED);
	                        return true;
	                    }
	                }
	
	                if (i > 0 && i < 4 && item instanceof ItemBanner)
	                {
	                    if (TileEntityBanner.getPatterns(itemstack) > 0 && !worldIn.isRemote)
	                    {
	                        ItemStack itemstack1 = itemstack.copy();
	                        itemstack1.setCount(1);
	                        TileEntityBanner.removeBannerData(itemstack1);
	                        playerIn.addStat(StatList.BANNER_CLEANED);
	
	                        if (!playerIn.capabilities.isCreativeMode)
	                        {
	                            itemstack.shrink(1);
	                            this.setWaterLevel(worldIn, pos, state, i - 1);
	                        }
	
	                        if (itemstack.isEmpty())
	                        {
	                            playerIn.setHeldItem(hand, itemstack1);
	                        }
	                        else if (!playerIn.inventory.addItemStackToInventory(itemstack1))
	                        {
	                            playerIn.dropItem(itemstack1, false);
	                        }
	                        else if (playerIn instanceof EntityPlayerMP)
	                        {
	                            ((EntityPlayerMP)playerIn).sendContainerToPlayer(playerIn.inventoryContainer);
	                        }
	                    }
	
	                    return true;
	                }
	                else
	                {
	                    return false;
	                }
	            }
	        }
	    }
	
	    public void setWaterLevel(World worldIn, BlockPos pos, IBlockState state, int level)
	    {
	    	if (((Integer)state.getValue(LEVEL)).intValue() < 4) {
	        	worldIn.setBlockState(pos, state.withProperty(LEVEL, Integer.valueOf(MathHelper.clamp(level, 0, 3))), 2);
	        	worldIn.updateComparatorOutputLevel(pos, this);
	    	}
	    }

	    public void setLavaLevel(World worldIn, BlockPos pos, IBlockState state, int level)
	    {
	    	int i = ((Integer)state.getValue(LEVEL)).intValue();
			if (i < 1 || i > 3) {
			    level = level < 0 ? 0 : level;
	    		level = level > 3 ? 3 : level;
	    		level = level == 0 ? 0 : level + 3; 
		        worldIn.setBlockState(pos, state.withProperty(LEVEL, Integer.valueOf(level)), 2);
		        worldIn.updateComparatorOutputLevel(pos, this);
		    }
	    }

	    protected boolean isLiquidAbove(World worldIn, BlockPos pos, Material liquidType) {
	    	int x = pos.getX();
	    	int y = pos.getY();
	    	int z = pos.getZ();
	    	for (int i = 0; (i < 9) && worldIn.isAirBlock(new BlockPos(x, y + 1, z)); ++i) {
	    		y += 1;
	    	}
	    	if (worldIn.getBlockState(new BlockPos(x, y + 1, z)).getBlock() != BlockTopStalactite.block.getDefaultState().getBlock()) {
	    		return false;
	    	}
	    	if (worldIn.getBlockState(new BlockPos(x, y + 2, z)).getBlock() == BlockMiddleStalactite.block.getDefaultState().getBlock()) {
	    		y += 1;
	    		if (worldIn.getBlockState(new BlockPos(x, y + 2, z)).getBlock() == BlockBottomStalactite.block.getDefaultState().getBlock()) {
	    			y += 1;
	    		}
	    	}
	    	return worldIn.getBlockState(new BlockPos(x, y + 3, z)).getMaterial() == liquidType;
	    }

	    public void randomTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
	    	super.randomTick(worldIn, pos, state, rand);
	    	if (rand.nextInt(2) == 1) {
	    		int level = ((Integer)state.getValue(LEVEL)).intValue();
		    	if (isLiquidAbove(worldIn, pos, Material.LAVA)) {
				    if ((level == 0) || (level > 3)) {
				    	this.setLavaLevel(worldIn, pos, state, level + 1);
				    }
			    }
			    else if (isLiquidAbove(worldIn, pos, Material.WATER)) {
			    	if (level < 3) {
			    		this.setWaterLevel(worldIn, pos, state, level + 1);
			    	}
		    	}
	    	}
	    	worldIn.scheduleUpdate(pos, this, this.tickRate(worldIn));
	    }

	    public void randomDisplayTick(IBlockState stateIn, World worldIn, BlockPos pos, Random rand)
    	{
    		super.randomDisplayTick(stateIn, worldIn, pos, rand);
    	    if ((rand.nextInt(10) + 1) <= 3) {
	    		int x = pos.getX();
		    	int y = pos.getY();
		    	int z = pos.getZ();
		    	if (isLiquidAbove(worldIn, pos, Material.LAVA)) {
		    		worldIn.spawnParticle(EnumParticleTypes.LAVA, x, y + 1, z, 0, 1, 0);
		    	}
		    	else if (isLiquidAbove(worldIn, pos, Material.WATER)) {
		    		worldIn.spawnParticle(EnumParticleTypes.WATER_DROP, x, y + 1, z, 0, 1, 0);
		   		}
	    	}
    	}

	    
	    //@Override
	    //public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
	    //}
	
	    /**
	     * Called similar to random ticks, but only when it is raining.
	     */
	    public void fillWithRain(World worldIn, BlockPos pos)
	    {
	        if (worldIn.rand.nextInt(20) == 1)
	        {
	            float f = worldIn.getBiome(pos).getTemperature(pos);
	
	            if (worldIn.getBiomeProvider().getTemperatureAtHeight(f, pos.getY()) >= 0.15F)
	            {
	                IBlockState iblockstate = worldIn.getBlockState(pos);
	
	                if (((Integer)iblockstate.getValue(LEVEL)).intValue() < 3)
	                {
	                    worldIn.setBlockState(pos, iblockstate.cycleProperty(LEVEL), 2);
	                }
	            }
	        }
	    }
	
	    /**
	     * Get the Item that this Block should drop when harvested.
	     */
	    public Item getItemDropped(IBlockState state, Random rand, int fortune)
	    {
	        return Items.CAULDRON;
	    }
	
	    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state)
	    {
	        return new ItemStack(Items.CAULDRON);
	    }
	
	    /**
	     * @deprecated call via {@link IBlockState#hasComparatorInputOverride()} whenever possible. Implementing/overriding
	     * is fine.
	     */
	    public boolean hasComparatorInputOverride(IBlockState state)
	    {
	        return true;
	    }
	
	    /**
	     * @deprecated call via {@link IBlockState#getComparatorInputOverride(World,BlockPos)} whenever possible.
	     * Implementing/overriding is fine.
	     */
	    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos)
	    {
	    	int i = ((Integer)blockState.getValue(LEVEL)).intValue();
	        return i - 3 * (i / 4);
	    }
	
	    /**
	     * Convert the given metadata into a BlockState for this Block
	     */
	    public IBlockState getStateFromMeta(int meta)
	    {
	        return this.getDefaultState().withProperty(LEVEL, Integer.valueOf(meta));
	    }
	
	    /**
	     * Convert the BlockState into the correct metadata value
	     */
	    public int getMetaFromState(IBlockState state)
	    {
	        return ((Integer)state.getValue(LEVEL)).intValue();
	    }
	
	    protected BlockStateContainer createBlockState()
	    {
	        return new BlockStateContainer(this, new IProperty[] {LEVEL});
	    }
	
	    /**
	     * Determines if an entity can path through this block
	     */
	    public boolean isPassable(IBlockAccess worldIn, BlockPos pos)
	    {
	        return true;
	    }
	
	    /**
	     * Get the geometry of the queried face at the given position and state. This is used to decide whether things like
	     * buttons are allowed to be placed on the face, or how glass panes connect to the face, among other things.
	     * <p>
	     * Common values are {@code SOLID}, which is the default, and {@code UNDEFINED}, which represents something that
	     * does not fit the other descriptions and will generally cause other things not to connect to the face.
	
	     * @return an approximation of the form of the given face
	     * @deprecated call via {@link IBlockState#getBlockFaceShape(IBlockAccess,BlockPos,EnumFacing)} whenever possible.
	     * Implementing/overriding is fine.
	     */
	    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face)
	    {
	        if (face == EnumFacing.UP)
	        {
	            return BlockFaceShape.BOWL;
	        }
	        else
	        {
	            return face == EnumFacing.DOWN ? BlockFaceShape.UNDEFINED : BlockFaceShape.SOLID;
	        }
	    }
	}
	public static class TileEntityCustom extends TileEntityLockableLoot {
		private NonNullList<ItemStack> stacks = NonNullList.<ItemStack>withSize(9, ItemStack.EMPTY);
		@Override
		public int getSizeInventory() {
			return 9;
		}

		@Override
		public boolean isEmpty() {
			for (ItemStack itemstack : this.stacks)
				if (!itemstack.isEmpty())
					return false;
			return true;
		}

		@Override
		public boolean isItemValidForSlot(int index, ItemStack stack) {
			return true;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return stacks.get(slot);
		}

		@Override
		public String getName() {
			return "container.lavacauldron";
		}

		@Override
		public void readFromNBT(NBTTagCompound compound) {
			super.readFromNBT(compound);
			this.stacks = NonNullList.<ItemStack>withSize(this.getSizeInventory(), ItemStack.EMPTY);
			if (!this.checkLootAndRead(compound))
				ItemStackHelper.loadAllItems(compound, this.stacks);
		}

		@Override
		public NBTTagCompound writeToNBT(NBTTagCompound compound) {
			super.writeToNBT(compound);
			if (!this.checkLootAndWrite(compound))
				ItemStackHelper.saveAllItems(compound, this.stacks);
			return compound;
		}

		@Override
		public SPacketUpdateTileEntity getUpdatePacket() {
			return new SPacketUpdateTileEntity(this.pos, 0, this.getUpdateTag());
		}

		@Override
		public NBTTagCompound getUpdateTag() {
			return this.writeToNBT(new NBTTagCompound());
		}

		@Override
		public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
			this.readFromNBT(pkt.getNbtCompound());
		}

		@Override
		public void handleUpdateTag(NBTTagCompound tag) {
			this.readFromNBT(tag);
		}

		@Override
		public int getInventoryStackLimit() {
			return 1;
		}

		@Override
		public String getGuiID() {
			return "cavesnotcliffs:lavacauldron";
		}

		@Override
		public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn) {
			this.fillWithLoot(playerIn);
			return new ContainerChest(playerInventory, this, playerIn);
		}

		@Override
		protected NonNullList<ItemStack> getItems() {
			return this.stacks;
		}
	}
}
