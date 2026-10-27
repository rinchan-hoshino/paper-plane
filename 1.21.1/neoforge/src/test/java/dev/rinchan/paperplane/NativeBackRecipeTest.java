package dev.rinchan.paperplane;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.InputStreamReader;
import java.util.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class NativeBackRecipeTest {
    @BeforeAll static void bootstrap() {
        net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        ((net.minecraft.core.MappedRegistry<Item>)BuiltInRegistries.ITEM).unfreeze();
        for (String id : List.of("paper_plane", "back_paper_plane", "ender_back_paper_plane")) {
            var key = ResourceLocation.parse("paper_plane:" + id);
            net.minecraft.core.Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties()));
        }
    }

    @Test void nativeRecipeCodecAndCraftingMatchBothOwnerApprovedPatterns() throws Exception {
        var lookup = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        for (String name : List.of("back_paper_plane", "ender_back_paper_plane")) {
            try (var input = getClass().getResourceAsStream("/data/paper_plane/recipe/" + name + ".json")) {
                var recipe = (ShapedRecipe)Recipe.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, lookup),
                    JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(input)))).getOrThrow();
                var back = name.equals("back_paper_plane");
                var ingredient = back ? Items.BONE_MEAL : Items.ENDER_PEARL;
                var center = BuiltInRegistries.ITEM.get(ResourceLocation.parse("paper_plane:" + (back ? "paper_plane" : "back_paper_plane")));
                var grid = new ArrayList<ItemStack>();
                for (int i = 0; i < 9; i++) grid.add(i == 4 ? new ItemStack(center) : back && i % 2 == 0 ? ItemStack.EMPTY : new ItemStack(ingredient));
                var crafting = CraftingInput.of(3, 3, grid);
                assertTrue(recipe.matches(crafting, null));
                var output = recipe.assemble(crafting, lookup);
                assertTrue(output.is(BuiltInRegistries.ITEM.get(ResourceLocation.parse("paper_plane:" + name))));
                assertEquals(1, output.getCount());
                grid.set(1, ItemStack.EMPTY);
                assertFalse(recipe.matches(CraftingInput.of(3, 3, grid), null), "Every bone meal or pearl in the shaped pattern is required");
            }
        }
    }
}
