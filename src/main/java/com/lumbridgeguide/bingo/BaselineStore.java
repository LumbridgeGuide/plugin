package com.lumbridgeguide.bingo;

import java.util.Map;

public interface BaselineStore {

    Map<String, Long> load(String boardId);

    void save(String boardId, Map<String, Long> baselines);
}
