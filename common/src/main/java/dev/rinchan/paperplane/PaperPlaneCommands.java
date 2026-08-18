package dev.rinchan.paperplane;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class PaperPlaneCommands {
    private static final String REQUEST_ID = "request_id";

    private PaperPlaneCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpaccept")
                .then(Commands.argument(REQUEST_ID, StringArgumentType.string())
                        .executes(context -> PaperPlane.acceptTeleportRequest(
                                context.getSource().getPlayerOrException(),
                                StringArgumentType.getString(context, REQUEST_ID)))));
        dispatcher.register(Commands.literal("tpdeny")
                .then(Commands.argument(REQUEST_ID, StringArgumentType.string())
                        .executes(context -> PaperPlane.denyTeleportRequest(
                                context.getSource().getPlayerOrException(),
                                StringArgumentType.getString(context, REQUEST_ID)))));
    }
}
