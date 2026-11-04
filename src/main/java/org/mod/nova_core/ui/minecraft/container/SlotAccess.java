package org.mod.nova_core.ui.minecraft.container;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

// Позиции слотов Slot.x и Slot.y финальные, но нужны только клиенту: сервер работает с номерами слотов.
// Поля находятся по значениям на пробном слоте, поэтому не зависят от названий (dev и релиз) и не требуют Mixin
// Slot.x and Slot.y are final but only the client needs them: the server works with slot numbers.
// The fields are found by values on a probe slot, so they do not depend on names (dev and release) and need no Mixin
public final class SlotAccess {
    private static final Field X;
    private static final Field Y;

    static {
        Field fx = null;
        Field fy = null;
        try {
            Slot probe = new Slot(new SimpleContainer(1), 0, 7001, 7002);
            for (Field f : Slot.class.getDeclaredFields()) {
                if (f.getType() != int.class || Modifier.isStatic(f.getModifiers())) continue;
                f.setAccessible(true);
                int value = f.getInt(probe);
                if (value == 7001) fx = f;
                else if (value == 7002) fy = f;
            }
        } catch (Throwable ex) {
            fx = null;
            fy = null;
        }
        X = fx;
        Y = fy;
    }

    private SlotAccess() {}

    // false = поля не найдены: ванильные экраны лучше не подменять / false = the fields were not found: better not to replace vanilla screens
    public static boolean isAvailable() {
        return X != null && Y != null;
    }

    public static void setPosition(Slot slot, int x, int y) {
        if (!isAvailable()) return;
        try {
            X.setInt(slot, x);
            Y.setInt(slot, y);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Cannot move a slot", ex);
        }
    }
}