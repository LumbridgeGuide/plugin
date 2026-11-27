package com.lumbridgeguide.bingo;

import com.google.gson.Gson;
import com.lumbridgeguide.api.ApiResponse;
import com.lumbridgeguide.api.LumbridgeGuideClient;
import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginTeamData;
import com.lumbridgeguide.bingo.data.PluginTileData;
import okhttp3.OkHttpClient;
import org.junit.Test;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProofServiceTest {

    private ApiResponse syncResponse;

    private final LumbridgeGuideClient client = new LumbridgeGuideClient(new OkHttpClient(), new Gson(), null) {
        @Override
        public boolean hasApiKey() {
            return true;
        }

        @Override
        public void post(String path, Object body, Consumer<ApiResponse> onSuccess,
                         Consumer<ApiResponse> onFailure) {
            (syncResponse.getStatusCode() == 200 ? onSuccess : onFailure).accept(syncResponse);
        }
    };
    private final BoardDataService boards = new BoardDataService(client, null, new BingoChatAnnouncer(null, null));
    private final ProofService proofService = new ProofService(null, boards, client, null, null, null,
            new TileProgressTracker(null, null, null, null, null, null), null, null, null, null, null, null);

    @Test
    public void keepsQueuedProofWhenTheApiKeyIsRejected() {
        PluginBoardData board = runningBoard();
        ProofOffer offer = offer(board);
        sync(200, "{\"changed\":true,\"cursor\":1,\"versions\":{\"board\":1},\"boards\":["
                + new Gson().toJson(board) + "]}");

        sync(401, "");

        assertFalse(proofService.isEnded(offer));
    }

    @Test
    public void dropsQueuedProofOnceItsBoardHasEnded() {
        ProofOffer offer = offer(runningBoard());

        sync(200, "{\"changed\":true,\"cursor\":1,\"versions\":{},\"boards\":[]}");

        assertTrue(proofService.isEnded(offer));
    }

    private void sync(int status, String body) {
        syncResponse = new ApiResponse(status, body);
        boards.refresh();
    }

    private static PluginBoardData runningBoard() {
        PluginTeamData team = new PluginTeamData();
        team.setId("team");
        PluginTileData tile = new PluginTileData();
        tile.setId("tile");
        PluginBoardData board = new PluginBoardData();
        board.setId("board");
        board.setStartsAt(Instant.now().minusSeconds(60).toString());
        board.setEndsAt(Instant.now().plusSeconds(3600).toString());
        board.setMyTeam(team);
        board.setTiles(List.of(tile));
        return board;
    }

    private static ProofOffer offer(PluginBoardData board) {
        return new ProofOffer("offer", board, board.getTiles().get(0), "Drop received", null, null,
                null, null, -1, null);
    }
}
