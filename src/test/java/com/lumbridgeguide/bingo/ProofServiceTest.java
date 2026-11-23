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
        public void get(String path, Consumer<ApiResponse> onSuccess, Consumer<ApiResponse> onFailure) {
            (syncResponse.getStatusCode() == 200 ? onSuccess : onFailure).accept(syncResponse);
        }
    };
    private final BoardDataService boards = new BoardDataService(client, new Gson(), null);
    private final ProofService proofService = new ProofService(null, boards, client, null, null, null,
            new TileProgressTracker(null, null, null, null, null, null), null, null, null, null, null, null,
            new Gson());

    @Test
    public void keepsQueuedProofWhenTheApiKeyIsRejected() {
        PluginBoardData board = runningBoard();
        ProofService.Offer offer = offer(board);
        sync(200, "{\"boards\":[" + new Gson().toJson(board) + "]}");

        sync(401, "");

        assertFalse(proofService.isEnded(offer));
    }

    @Test
    public void dropsQueuedProofOnceItsBoardHasEnded() {
        ProofService.Offer offer = offer(runningBoard());

        sync(200, "{\"boards\":[]}");

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

    private static ProofService.Offer offer(PluginBoardData board) {
        return new ProofService.Offer("offer", board, board.getTiles().get(0), "Drop matched a tile", null, null,
                null, null, -1, null);
    }
}
