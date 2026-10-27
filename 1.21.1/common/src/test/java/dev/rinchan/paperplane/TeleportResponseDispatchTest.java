package dev.rinchan.paperplane;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import dev.ftb.mods.ftbessentials.api.records.TPARequest;
import dev.ftb.mods.ftbessentials.commands.impl.teleporting.TPACommand;
import java.util.HashMap;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TeleportResponseDispatchTest {
  @BeforeAll
  static void bootstrap() {
    net.neoforged.fml.loading.LoadingModList.of(
        java.util.List.of(),
        java.util.List.of(),
        java.util.List.of(),
        java.util.List.of(),
        java.util.Map.of());
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
  }

  @Test
  void staleAndRepeatedChatButtonsStayOnTheNativeServerPath() {
    var requests = new HashMap<UUID, TPARequest>();
    try (var nativeState = mockStatic(TPACommand.class);
        var nativeCommands =
            mockConstruction(
                TPACommand.class,
                (command, context) -> {
                  when(command.tpaccept(any(), anyString()))
                      .thenAnswer(
                          call -> {
                            requests.remove(UUID.fromString(call.getArgument(1)));
                            return 1;
                          });
                })) {
      nativeState.when(TPACommand::requests).thenReturn(requests);
      var target = mock(ServerPlayer.class);
      when(target.getUUID()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000002"));
      var request = UUID.fromString("00000000-0000-0000-0000-000000000001");
      var recipient = mock(dev.ftb.mods.ftbessentials.util.FTBEPlayerData.class);
      when(recipient.getUuid()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000002"));
      var liveRequest = mock(TPARequest.class);
      when(liveRequest.target()).thenReturn(recipient);
      requests.put(request, liveRequest);
      var unrelated = mock(ServerPlayer.class);
      when(unrelated.getUUID()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000003"));
      assertTrue(PaperPlane.respondToTeleportRequest(unrelated, request, true));
      assertTrue(requests.containsKey(request), "A different recipient cannot consume the request");
      // With no live Paper Plane ledger entry, the native TPA owner still
      // decides whether the request is valid. Never emit a client fallback.
      assertTrue(PaperPlane.respondToTeleportRequest(target, request, true));
      assertTrue(PaperPlane.respondToTeleportRequest(target, request, true));
      assertTrue(PaperPlane.respondToTeleportRequest(target, request, false));
      assertEquals(1, nativeCommands.constructed().size());
      var nativeCommand = nativeCommands.constructed().getFirst();
      verify(nativeCommand).tpaccept(target, request.toString());
      verify(nativeCommand, never()).tpdeny(any(), anyString());
      assertTrue(requests.isEmpty());
    }
  }
}
