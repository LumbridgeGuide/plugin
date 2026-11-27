package com.lumbridgeguide.bingo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@AllArgsConstructor
public class KillCount {

    private static final Pattern MESSAGE = Pattern.compile(
            "Your (?:completion count for |subdued |completed )?(.+?) "
                    + "(?:(?:kill|harvest|lap|completion|success|Total Ticket) )?(?:count )?is: ?([\\d,]+)");

    private String monster;
    private long count;

    public static Optional<KillCount> parse(String message) {
        if (message == null) {
            return Optional.empty();
        }

        Matcher matcher = MESSAGE.matcher(message.replaceAll("<[^>]*>", ""));
        if (!matcher.find()) {
            return Optional.empty();
        }

        return Optional.of(new KillCount(matcher.group(1).trim(), Long.parseLong(matcher.group(2).replace(",", ""))));
    }
}
