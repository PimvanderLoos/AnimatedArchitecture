package nl.pim16aap2.bigDoors.customEntities;

import net.minecraft.server.EntityArmorStand;
import net.minecraft.server.Vector3f;
import net.minecraft.server.v1_11_R1.EntityLiving;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftLivingEntity;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

public class CraftArmorStand extends CraftLivingEntity {


    public CraftArmorStand(org.bukkit.craftbukkit.v1_11_R1.CraftServer server, EntityLiving entity) {
		super(server, entity);
	}

	@Override
    public void setGravity(boolean gravity) {
        super.setGravity(gravity);
        getHandle().noclip = gravity;
    }
}
